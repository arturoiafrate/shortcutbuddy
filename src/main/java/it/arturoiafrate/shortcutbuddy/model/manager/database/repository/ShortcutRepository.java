package it.arturoiafrate.shortcutbuddy.model.manager.database.repository;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.AppShortcuts;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import it.arturoiafrate.shortcutbuddy.model.manager.database.DatabaseManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Singleton
public class ShortcutRepository {

    private static final String FIND_APP_SQL = "SELECT app_id, app_description, usage_count, user_defined FROM applications WHERE app_name = ?";
    private static final String FIND_SHORTCUTS_SQL = "SELECT shortcut_id, keys_storage, description, category FROM shortcuts WHERE app_id = ?";
    private static final String FIND_MOST_USED_APPS_SQL = "SELECT app_id, app_name, app_description, usage_count, user_defined FROM applications ORDER BY usage_count DESC LIMIT ?";
    private static final String UPDATE_APP_USAGE_SQL = "UPDATE applications SET usage_count = usage_count + ? WHERE app_name = ?";

    private final Gson gson;
    private final Type stringListType;
    private final DatabaseManager databaseManager;

    @Inject
    public ShortcutRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.gson = new Gson();
        this.stringListType = new TypeToken<List<String>>() {}.getType();
    }

    public void touch() {
        log.info("ShortcutRepository is ready to use.");
    }

    public List<AppShortcuts> findMostUsedApps(int limit) {
        List<AppShortcuts> appShortcutsList = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_MOST_USED_APPS_SQL)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long appId = rs.getLong("app_id");
                    String appName = rs.getString("app_name");
                    String appDescription = rs.getString("app_description");
                    long usageCount = rs.getLong("usage_count");
                    boolean userDefined = rs.getBoolean("user_defined");

                    List<Shortcut> shortcutList = new ArrayList<>();
                    try (PreparedStatement pstmtShortcuts = conn.prepareStatement(FIND_SHORTCUTS_SQL)) {
                        pstmtShortcuts.setLong(1, appId);
                        try (ResultSet rsShortcuts = pstmtShortcuts.executeQuery()) {
                            while (rsShortcuts.next()) {
                                shortcutList.add(mapRowToShortcut(rsShortcuts, appId));
                            }
                        }
                    }
                    appShortcutsList.add(new AppShortcuts(appName, appDescription, shortcutList, usageCount, userDefined));
                }
            }
        } catch (SQLException e) {
            log.error("Database error fetching most used apps", e);
        }

        return appShortcutsList;
    }

    public AppShortcuts findAppShortcutsByName(String appName) {
        if (StringUtils.isEmpty(appName)) {
            return null;
        }

        try (Connection conn = databaseManager.getConnection()) {

            long appId = -1;
            String appDescription = null;
            boolean userDefined = false;
            long usageCount = 0;

            try (PreparedStatement pstmtApp = conn.prepareStatement(FIND_APP_SQL)) {
                pstmtApp.setString(1, appName);
                try (ResultSet rsApp = pstmtApp.executeQuery()) {
                    if (rsApp.next()) {
                        appId = rsApp.getLong("app_id");
                        appDescription = rsApp.getString("app_description");
                        usageCount = rsApp.getLong("usage_count");
                        userDefined = rsApp.getBoolean("user_defined");
                    } else {
                        log.info("Application not found: {}", appName);
                        return null;
                    }
                }
            }

            List<Shortcut> shortcutList = new ArrayList<>();
            try (PreparedStatement pstmtShortcuts = conn.prepareStatement(FIND_SHORTCUTS_SQL)) {
                pstmtShortcuts.setLong(1, appId);
                try (ResultSet rsShortcuts = pstmtShortcuts.executeQuery()) {
                    while (rsShortcuts.next()) {
                        shortcutList.add(mapRowToShortcut(rsShortcuts, appId));
                    }
                }
            }
            return new AppShortcuts(appName, appDescription, shortcutList, usageCount, userDefined);

        } catch (SQLException e) {
            log.error("Database error searching for: {}", appName, e);
            return null;
        }
    }

    public boolean batchIncrementUsageCount(Map<String, AtomicInteger> increments){

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_APP_USAGE_SQL)) {
            conn.setAutoCommit(false);

            for (Map.Entry<String, AtomicInteger> entry : increments.entrySet()) {
                String appName = entry.getKey();
                int increment = entry.getValue().get();

                pstmt.setInt(1, increment);
                pstmt.setString(2, appName);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            log.error("Database error updating usage count", e);
            return false;
        }
    }

    private Shortcut mapRowToShortcut(ResultSet rs, long appId) throws SQLException {
        return new Shortcut(
                rs.getLong("shortcut_id"),
                appId,
                rs.getString("description"),
                deserializeKeys(rs.getString("keys_storage")),
                rs.getString("category")
        );
    }

    private List<String> deserializeKeys(String jsonKeys) {
        if (jsonKeys == null || jsonKeys.isBlank() || jsonKeys.equals("null")) {
            return new ArrayList<>();
        }
        try {
            List<String> keys = gson.fromJson(jsonKeys, this.stringListType);
            return keys != null ? keys : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            log.error("Error deserialize keys JSON: {}", jsonKeys, e);
            return new ArrayList<>();
        }
    }
}
