package it.arturoiafrate.shortcutbuddy.model.manager.database.repository;

import it.arturoiafrate.shortcutbuddy.model.bean.ClipboardEntry;
import it.arturoiafrate.shortcutbuddy.model.enumerator.ClipboardContentType;
import it.arturoiafrate.shortcutbuddy.model.manager.database.DatabaseManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class ClipboardHistoryRepository {

    private final DatabaseManager databaseManager;

    private static final String INSERT_ENTRY_SQL = "INSERT INTO clipboard_history (content_type, content, timestamp) VALUES (?, ?, ?)";
    private static final String LOAD_RECENT_SQL = "SELECT id, content_type, content, timestamp FROM clipboard_history ORDER BY timestamp DESC LIMIT ?";
    private static final String DELETE_OLDEST_SQL = "DELETE FROM clipboard_history WHERE id NOT IN (SELECT id FROM clipboard_history ORDER BY timestamp DESC LIMIT ?)";
    private static final String GET_MOST_RECENT_ENTRY_SQL = "SELECT content_type, content FROM clipboard_history ORDER BY timestamp DESC LIMIT 1";


    @Inject
    public ClipboardHistoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<ClipboardEntry> loadRecentEntries(int limit) {
        log.debug("Loading {} most recent clipboard entries from DB", limit);
        List<ClipboardEntry> entries = new ArrayList<>();
        String sql = LOAD_RECENT_SQL;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRowToEntry(rs));
                }
            }
            log.debug("Loaded {} entries from DB", entries.size());
        } catch (SQLException e) {
            log.error("Database error loading recent clipboard entries", e);
        }
        return entries;
    }

    public boolean saveEntries(List<ClipboardEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        log.debug("Saving {} new clipboard entries to DB", entries.size());
        String sql = INSERT_ENTRY_SQL;
        int insertedCount = 0;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (ClipboardEntry entry : entries) {
                pstmt.setString(1, entry.getContentType().name());
                pstmt.setString(2, entry.getContent());
                pstmt.setLong(3, entry.getTimestamp());
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            conn.commit();

            for (int result : results) {
                if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                    insertedCount++;
                } else if (result == Statement.EXECUTE_FAILED) {
                    log.warn("A batch insert failed for a clipboard entry.");
                }
            }
            log.debug("Successfully inserted {} clipboard entries", insertedCount);

        } catch (SQLException e) {
            log.error("Database error saving clipboard entries", e);
            return false;
        }
        return insertedCount > 0;
    }

    public boolean deleteOldEntries(int keepCount) {
        log.debug("Deleting old clipboard entries, keeping the latest {}", keepCount);
        String sql = DELETE_OLDEST_SQL;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, keepCount);
            int deletedRows = pstmt.executeUpdate();
            log.debug("Deleted {} old clipboard entries", deletedRows);
            return true;
        } catch (SQLException e) {
            log.error("Database error deleting old clipboard entries", e);
            return false;
        }
    }

    /**
     * Recupera tipo e contenuto dell'ultima entry inserita nel DB.
     * Usa Optional per chiarezza sul caso "nessuna entry trovata".
     * @return Un Optional contenente un array Object[2] con {ClipboardContentType, String content}, o Optional.empty().
     */
    public Optional<Object[]> getMostRecentEntryTypeAndContent() {
        log.trace("Getting most recent entry type and content from DB");
        String sql = GET_MOST_RECENT_ENTRY_SQL;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                try {
                    ClipboardContentType type = ClipboardContentType.valueOf(rs.getString("content_type"));
                    String content = rs.getString("content");
                    return Optional.of(new Object[]{type, content});
                } catch (IllegalArgumentException e) {
                    log.error("Invalid content_type '{}' found in database for most recent entry.", rs.getString("content_type"));
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            log.error("Database error getting most recent clipboard entry", e);
        }
        return Optional.empty();
    }


    private ClipboardEntry mapRowToEntry(ResultSet rs) throws SQLException {
        ClipboardContentType type;
        String typeString = rs.getString("content_type");
        try {
            type = ClipboardContentType.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown clipboard content type '{}' found in database (ID: {}), defaulting to TEXT.", typeString, rs.getLong("id"));
            type = ClipboardContentType.TEXT; // Fallback sicuro
        }
        return new ClipboardEntry(
                rs.getLong("id"),
                type, // Usa l'enum
                rs.getString("content"),
                rs.getLong("timestamp")
                // Rimosso is_favorite
        );
    }
}