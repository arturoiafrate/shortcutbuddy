package it.arturoiafrate.shortcutbuddy.model.manager.database.repository;

import it.arturoiafrate.shortcutbuddy.model.bean.Setting;
import it.arturoiafrate.shortcutbuddy.model.manager.database.DatabaseManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for managing settings in the database.
 * Provides methods for retrieving and updating settings.
 */
@Slf4j
@Singleton
public class SettingsRepository {

    private static final String GET_ALL_SETTINGS_SQL = "SELECT key, value, is_readonly, is_hidden, value_type, allowed_options, group_name, setting_order, dev_mode, conditional_enabling FROM settings ORDER BY setting_order";
    private static final String GET_SETTING_BY_KEY_SQL = "SELECT key, value, is_readonly, is_hidden, value_type, allowed_options, group_name, setting_order, dev_mode, conditional_enabling FROM settings WHERE key = ?";
    private static final String UPDATE_SETTING_SQL = "UPDATE settings SET value = ? WHERE key = ?";

    private final DatabaseManager databaseManager;

    /**
     * Constructs a new SettingsRepository with the provided DatabaseManager.
     * 
     * @param databaseManager The database manager to use for database operations
     */
    @Inject
    public SettingsRepository(DatabaseManager databaseManager) {
        log.debug("Initializing SettingsRepository");
        this.databaseManager = databaseManager;
        log.debug("SettingsRepository initialized");
    }

    /**
     * Verifies that the repository is ready to use.
     * This method is called during application startup to ensure the repository is properly initialized.
     */
    public void touch() {
        log.info("SettingsRepository is ready to use");
    }

    /**
     * Retrieves all settings from the database.
     * 
     * @return A list of all settings
     */
    public List<Setting> getAllSettings() {
        log.debug("Retrieving all settings");
        List<Setting> settingsList = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_SETTINGS_SQL);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                settingsList.add(mapRowToSetting(rs));
            }
            log.debug("Retrieved {} settings", settingsList.size());
        } catch (SQLException e) {
            log.error("Database error fetching all settings", e);
        }
        return settingsList;
    }

    /**
     * Retrieves a setting by its key.
     * 
     * @param key The key of the setting to retrieve
     * @return The setting with the specified key, or null if not found
     */
    public Setting getSettingByKey(String key) {
        if (StringUtils.isEmpty(key)) {
            log.debug("Empty key provided, returning null");
            return null;
        }

        log.debug("Retrieving setting with key: {}", key);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_SETTING_BY_KEY_SQL)) {

            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Setting setting = mapRowToSetting(rs);
                    log.debug("Retrieved setting: {}", setting);
                    return setting;
                } else {
                    log.debug("Setting with key {} not found", key);
                    return null;
                }
            }
        } catch (SQLException e) {
            log.error("Database error fetching setting with key: {}", key, e);
            return null;
        }
    }

    /**
     * Updates a setting in the database.
     * 
     * @param setting The setting to update
     * @return True if the update was successful, false otherwise
     */
    public boolean updateSetting(Setting setting) {
        if (setting == null || StringUtils.isEmpty(setting.getKey())) {
            log.debug("Invalid setting provided, update failed");
            return false;
        }

        log.debug("Updating setting with key: {}", setting.getKey());
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SETTING_SQL)) {

            pstmt.setString(1, setting.getValue());
            pstmt.setString(2, setting.getKey());
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                log.debug("Successfully updated setting with key: {}", setting.getKey());
            } else {
                log.warn("Failed to update setting with key: {}", setting.getKey());
            }
            return result;
        } catch (SQLException e) {
            log.error("Database error updating setting with key: {}", setting.getKey(), e);
            return false;
        }
    }

    /**
     * Maps a database row to a Setting object.
     * 
     * @param rs The ResultSet containing the row data
     * @return A Setting object populated with data from the row
     * @throws SQLException If an error occurs while accessing the ResultSet
     */
    private Setting mapRowToSetting(ResultSet rs) throws SQLException {
        String key = rs.getString("key");
        String value = rs.getString("value");
        String valueType = rs.getString("value_type");
        boolean readonly = rs.getBoolean("is_readonly");
        boolean isHide = rs.getBoolean("is_hidden");
        String allowedOptions = rs.getString("allowed_options");
        String groupName = rs.getString("group_name");
        int settingOrder = rs.getInt("setting_order");
        boolean devMode = rs.getBoolean("dev_mode");
        String conditionalEnabling = rs.getString("conditional_enabling");
        
        // Parse allowed options if available
        String[] options = null;
        if (allowedOptions != null && !allowedOptions.isEmpty()) {
            // Remove brackets and quotes, then split by comma
            String cleanOptions = allowedOptions.replaceAll("[\\[\\]\"]", "");
            options = cleanOptions.split(",");
        }
        
        // For simplicity, we're setting order to 0 as it's not in the database
        return new Setting(key, value, valueType, readonly, options, isHide, settingOrder, groupName, devMode, conditionalEnabling);
    }
}