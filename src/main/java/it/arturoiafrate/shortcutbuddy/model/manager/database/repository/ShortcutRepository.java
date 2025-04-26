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

/**
 * Repository class for managing shortcuts and applications in the database.
 * Provides methods for retrieving, creating, updating, and deleting shortcuts and applications.
 */
@Slf4j
@Singleton
public class ShortcutRepository {

    private static final String FIND_APP_SQL = "SELECT app_id, app_description, usage_count, user_defined FROM applications WHERE app_name = ?";
    private static final String FIND_SHORTCUTS_SQL = "SELECT shortcut_id, keys_storage, description, category, default_value, starred FROM shortcuts WHERE app_id = ?";
    private static final String FIND_MOST_USED_APPS_SQL = "SELECT app_id, app_name, app_description, usage_count, user_defined FROM applications ORDER BY usage_count DESC LIMIT ?";
    private static final String UPDATE_APP_USAGE_SQL = "UPDATE applications SET usage_count = usage_count + ? WHERE app_name = ?";
    private static final String GET_ALL_APPS_SQL = "SELECT app_id, app_name, app_description, usage_count, user_defined FROM applications";
    private static final String INSERT_SHORTCUT_SQL = "INSERT INTO shortcuts (app_id, keys_storage, description, category, user_defined, default_value, starred) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SHORTCUT_SQL = "UPDATE shortcuts SET keys_storage = ?, description = ?, category = ? WHERE shortcuts.shortcut_id = ?";
    private static final String RESTORE_DEFAULT_SHORTCUT_SQL = "UPDATE shortcuts SET keys_storage = default_value WHERE shortcuts.shortcut_id = ?";
    private static final String DELETE_SHORTCUT_SQL = "DELETE FROM shortcuts WHERE shortcuts.shortcut_id = ?";
    private static final String INSERT_APP_SQL = "INSERT INTO applications (app_name, app_description, user_defined) VALUES (?, ?, ?)";
    private static final String UPDATE_SHORTCUT_STARRED_SQL = "UPDATE shortcuts SET starred = ? WHERE shortcuts.shortcut_id = ?";

    private final Gson gson;
    private final Type stringListType;
    private final DatabaseManager databaseManager;

    /**
     * Constructs a new ShortcutRepository with the provided DatabaseManager.
     * 
     * @param databaseManager The database manager to use for database operations
     */
    @Inject
    public ShortcutRepository(DatabaseManager databaseManager) {
        log.debug("Initializing ShortcutRepository");
        this.databaseManager = databaseManager;
        this.gson = new Gson();
        this.stringListType = new TypeToken<List<String>>() {}.getType();
        log.debug("ShortcutRepository initialized");
    }

    /**
     * Verifies that the repository is ready to use.
     * This method is called during application startup to ensure the repository is properly initialized.
     */
    public void touch() {
        log.info("ShortcutRepository is ready to use");
    }

    /**
     * Retrieves all applications from the database.
     * 
     * @return A list of all applications without their shortcuts
     */
    public List<AppShortcuts> getAllAppList() {
        log.debug("Retrieving all applications");
        List<AppShortcuts> appShortcutsList = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_APPS_SQL);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                long appId = rs.getLong("app_id");
                String appName = rs.getString("app_name");
                String appDescription = rs.getString("app_description");
                long usageCount = rs.getLong("usage_count");
                boolean userDefined = rs.getBoolean("user_defined");
                appShortcutsList.add(new AppShortcuts(appId, appName, appDescription, null, usageCount, userDefined));
            }
            log.debug("Retrieved {} applications", appShortcutsList.size());
        } catch (SQLException e) {
            log.error("Database error fetching all applications", e);
        }
        return appShortcutsList;
    }

    /**
     * Finds the most frequently used applications.
     * 
     * @param limit The maximum number of applications to return
     * @return A list of the most used applications with their shortcuts
     */
    public List<AppShortcuts> findMostUsedApps(int limit) {
        log.debug("Finding {} most used applications", limit);
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

                    log.debug("Loading shortcuts for app: {}", appName);
                    List<Shortcut> shortcutList = new ArrayList<>();
                    try (PreparedStatement pstmtShortcuts = conn.prepareStatement(FIND_SHORTCUTS_SQL)) {
                        pstmtShortcuts.setLong(1, appId);
                        try (ResultSet rsShortcuts = pstmtShortcuts.executeQuery()) {
                            while (rsShortcuts.next()) {
                                shortcutList.add(mapRowToShortcut(rsShortcuts, appId));
                            }
                        }
                    }
                    log.debug("Loaded {} shortcuts for app: {}", shortcutList.size(), appName);
                    appShortcutsList.add(new AppShortcuts(appName, appDescription, shortcutList, usageCount, userDefined));
                }
            }
            log.debug("Found {} most used applications", appShortcutsList.size());
        } catch (SQLException e) {
            log.error("Database error fetching most used apps", e);
        }

        return appShortcutsList;
    }

    /**
     * Finds an application and its shortcuts by name.
     * 
     * @param appName The name of the application to find
     * @return The application with its shortcuts, or null if not found
     */
    public AppShortcuts findAppShortcutsByName(String appName) {
        if (StringUtils.isEmpty(appName)) {
            log.debug("Empty app name provided, returning null");
            return null;
        }

        log.debug("Finding application shortcuts for: {}", appName);
        try (Connection conn = databaseManager.getConnection()) {

            long appId = -1;
            String appDescription = null;
            boolean userDefined = false;
            long usageCount = 0;

            // First find the application
            try (PreparedStatement pstmtApp = conn.prepareStatement(FIND_APP_SQL)) {
                pstmtApp.setString(1, appName);
                try (ResultSet rsApp = pstmtApp.executeQuery()) {
                    if (rsApp.next()) {
                        appId = rsApp.getLong("app_id");
                        appDescription = rsApp.getString("app_description");
                        usageCount = rsApp.getLong("usage_count");
                        userDefined = rsApp.getBoolean("user_defined");
                        log.debug("Found application: {} (ID: {})", appName, appId);
                    } else {
                        log.info("Application not found: {}", appName);
                        return null;
                    }
                }
            }

            // Then find its shortcuts
            List<Shortcut> shortcutList = new ArrayList<>();
            try (PreparedStatement pstmtShortcuts = conn.prepareStatement(FIND_SHORTCUTS_SQL)) {
                pstmtShortcuts.setLong(1, appId);
                try (ResultSet rsShortcuts = pstmtShortcuts.executeQuery()) {
                    while (rsShortcuts.next()) {
                        shortcutList.add(mapRowToShortcut(rsShortcuts, appId));
                    }
                }
            }
            log.debug("Found {} shortcuts for application: {}", shortcutList.size(), appName);
            return new AppShortcuts(appName, appDescription, shortcutList, usageCount, userDefined);

        } catch (SQLException e) {
            log.error("Database error searching for: {}", appName, e);
            return null;
        }
    }

    /**
     * Batch updates the usage count for multiple applications.
     * 
     * @param increments A map of application names to their usage count increments
     * @return True if the update was successful, false otherwise
     */
    public boolean batchIncrementUsageCount(Map<String, AtomicInteger> increments){
        log.debug("Batch incrementing usage count for {} applications", increments.size());

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_APP_USAGE_SQL)) {
            conn.setAutoCommit(false);

            for (Map.Entry<String, AtomicInteger> entry : increments.entrySet()) {
                String appName = entry.getKey();
                int increment = entry.getValue().get();

                log.debug("Incrementing usage count for {} by {}", appName, increment);
                pstmt.setInt(1, increment);
                pstmt.setString(2, appName);
                pstmt.addBatch();
            }

            int[] updateCounts = pstmt.executeBatch();
            conn.commit();
            log.debug("Successfully updated usage counts for {} applications", updateCounts.length);
            return true;
        } catch (SQLException e) {
            log.error("Database error updating usage count", e);
            return false;
        }
    }

    /**
     * Maps a database row to a Shortcut object.
     * 
     * @param rs The ResultSet containing the shortcut data
     * @param appId The ID of the application this shortcut belongs to
     * @return A new Shortcut object populated with data from the ResultSet
     * @throws SQLException If there is an error accessing the ResultSet
     */
    private Shortcut mapRowToShortcut(ResultSet rs, long appId) throws SQLException {
        String defaultValue = rs.getString("default_value");
        boolean starred = rs.getBoolean("starred");
        return new Shortcut(
                rs.getLong("shortcut_id"),
                appId,
                rs.getString("description"),
                deserializeKeys(rs.getString("keys_storage")),
                rs.getString("category"),
                deserializeKeys(defaultValue),
                starred
        );
    }

    /**
     * Deserializes a JSON string into a list of keys.
     * 
     * @param jsonKeys The JSON string representing a list of keys
     * @return A list of keys, or an empty list if the input is invalid
     */
    private List<String> deserializeKeys(String jsonKeys) {
        if (jsonKeys == null || jsonKeys.isBlank() || jsonKeys.equals("null")) {
            log.debug("Empty or null JSON keys, returning empty list");
            return new ArrayList<>();
        }
        try {
            List<String> keys = gson.fromJson(jsonKeys, this.stringListType);
            return keys != null ? keys : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            log.error("Error deserializing keys JSON: {}", jsonKeys, e);
            return new ArrayList<>();
        }
    }

    /**
     * Serializes a list of keys into a JSON string.
     * 
     * @param keys The list of keys to serialize
     * @return A JSON string representing the list of keys, or null if the input is invalid
     */
    private String serializeKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.debug("Empty or null keys list, returning null");
            return null;
        }
        try {
            String json = gson.toJson(keys, this.stringListType);
            log.debug("Serialized keys: {}", json);
            return json;
        } catch (JsonSyntaxException e) {
            log.error("Error serializing keys: {}", keys, e);
            return null;
        }
    }

    /**
     * Inserts a new application into the database.
     * 
     * @param app The application to insert
     * @return True if the insertion was successful, false otherwise
     */
    public boolean insertApp(AppShortcuts app) {
        log.debug("Inserting new application: {}", app.getAppName());
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_APP_SQL)) {

            pstmt.setString(1, app.getAppName());
            pstmt.setString(2, app.getAppDescription());
            pstmt.setBoolean(3, true);
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                log.debug("Successfully inserted application: {}", app.getAppName());
            } else {
                log.warn("Failed to insert application: {}", app.getAppName());
            }
            return result;
        } catch (SQLException e) {
            log.error("Database error inserting application: {}", app.getAppName(), e);
            return false;
        }
    }

    /**
     * Inserts a new shortcut into the database.
     * 
     * @param shortcut The shortcut to insert
     * @return True if the insertion was successful, false otherwise
     */
    public boolean insertShortcut(Shortcut shortcut) {
        log.debug("Inserting new shortcut: {} for app ID: {}", shortcut.getDescription(), shortcut.getAppId());
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SHORTCUT_SQL)) {

            String keysJson = serializeKeys(shortcut.getKeys());
            pstmt.setLong(1, shortcut.getAppId());
            pstmt.setString(2, keysJson);
            pstmt.setString(3, shortcut.getDescription());
            pstmt.setString(4, shortcut.getCategory());
            pstmt.setBoolean(5, true);
            pstmt.setString(6, keysJson);
            pstmt.setBoolean(7, shortcut.isStarred());

            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                log.debug("Successfully inserted shortcut: {}", shortcut.getDescription());
            } else {
                log.warn("Failed to insert shortcut: {}", shortcut.getDescription());
            }
            return result;
        } catch (SQLException e) {
            log.error("Database error inserting shortcut: {}", shortcut.getDescription(), e);
            return false;
        }
    }

    /**
     * Restores a shortcut to its default key configuration.
     * 
     * @param shortcutId The ID of the shortcut to restore
     * @return True if the restoration was successful, false otherwise
     */
    public boolean restoreDefaultShortcut(long shortcutId) {
        log.debug("Restoring default keys for shortcut ID: {}", shortcutId);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(RESTORE_DEFAULT_SHORTCUT_SQL)) {

            pstmt.setLong(1, shortcutId);
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                log.debug("Successfully restored default keys for shortcut ID: {}", shortcutId);
            } else {
                log.warn("Failed to restore default keys for shortcut ID: {}", shortcutId);
            }
            return result;
        } catch (SQLException e) {
            log.error("Database error restoring default shortcut ID: {}", shortcutId, e);
            return false;
        }
    }

    public boolean updateShortcut(Shortcut shortcut) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SHORTCUT_SQL)) {

            pstmt.setString(1, serializeKeys(shortcut.getKeys()));
            pstmt.setString(2, shortcut.getDescription());
            pstmt.setString(3, shortcut.getCategory());
            pstmt.setLong(4, shortcut.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Database error updating shortcut", e);
            return false;
        }
    }

    public boolean deleteShortcut(long shortcutId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SHORTCUT_SQL)) {

            pstmt.setLong(1, shortcutId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Database error deleting shortcut", e);
            return false;
        }
    }

    /**
     * Updates the starred status of a shortcut.
     * 
     * @param shortcutId The ID of the shortcut to update
     * @param starred The new starred status
     * @return True if the update was successful, false otherwise
     */
    public boolean updateShortcutStarred(long shortcutId, boolean starred) {
        log.debug("Updating starred status to {} for shortcut ID: {}", starred, shortcutId);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SHORTCUT_STARRED_SQL)) {

            pstmt.setBoolean(1, starred);
            pstmt.setLong(2, shortcutId);
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                log.debug("Successfully updated starred status to {} for shortcut ID: {}", starred, shortcutId);
            } else {
                log.warn("Failed to update starred status for shortcut ID: {}", shortcutId);
            }
            return result;
        } catch (SQLException e) {
            log.error("Database error updating starred status for shortcut ID: {}", shortcutId, e);
            return false;
        }
    }
}
