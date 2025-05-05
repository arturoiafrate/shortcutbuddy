package it.arturoiafrate.shortcutbuddy.model.manager.database;

import it.arturoiafrate.shortcutbuddy.model.manager.AbstractManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Singleton
public class DatabaseManager extends AbstractManager {
    private static final String DB_NAME = "shortcutbuddy.sqlite";
    private static final String DEV_DB_PATH = "shortcutbuddy_dev.sqlite";
    private final SQLiteDataSource dataSource;

    @Inject
    public DatabaseManager() {
        String dbUrl = null;
        String dev = System.getenv("DEV");
        try {
            String filePath = getFilePath((!StringUtils.isEmpty(dev) && dev.equals("true")) ? DEV_DB_PATH : DB_NAME);
            dbUrl = "jdbc:sqlite:" + filePath;
            log.info("Configuring DataSource SQLite for URL: {}", dbUrl);
            this.dataSource = new SQLiteDataSource();
            this.dataSource.setUrl(dbUrl);
            this.dataSource.setEncoding("UTF-8");
            try (Connection testConn = this.dataSource.getConnection()) {
                log.info("DataSource created successfully for DB URL: {}", dbUrl);
            }
            initDatabaseStructure();
        } catch (Exception e) {
            log.error("CRITICAL ERROR during SQLite init '{}'.", dbUrl, e);
            throw new RuntimeException("Cannot create or configure SQLite DB", e);
        }
    }


    public Connection getConnection() throws SQLException {
        if(this.dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return this.dataSource.getConnection();
    }

    private void initDatabaseStructure(){
        try {
            log.info("Initializing database migration");
            Flyway flyway = Flyway.configure()
                    .dataSource(this.dataSource.getUrl(), null, null)
                    .loggers("slf4j")
                    .load();
            flyway.migrate();
            log.info("Database migration completed");
        } catch (Exception e) {
            log.error("Error while initializing database migration", e);
            throw new RuntimeException("Error while initializing database migration", e);
        }
    }
}
