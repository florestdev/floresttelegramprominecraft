package ru.florestdev.florestTelegramPRO;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;

public class TwoFactorDatabase {

    private final FlorestTelegramPRO plugin;
    private HikariDataSource dataSource;

    public TwoFactorDatabase(FlorestTelegramPRO plugin) {
        this.plugin = plugin;
        initializeDatabase();
        createTable();
    }

    private void initializeDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs(); // 👈 СОЗДАЁМ ПАПКУ, ЕСЛИ ЕЁ НЕТ
            }
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/twofa.db";

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");

            // Настройки пула
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // Настройки SQLite
            config.addDataSourceProperty("foreign_keys", "true");
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("cache_size", "10000");

            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("✅ HikariCP + SQLite инициализированы для 2FA!");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Не удалось инициализировать БД: " + e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS twofa (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(16) NOT NULL UNIQUE,
                telegram_id VARCHAR(64) NOT NULL,
                code VARCHAR(10),
                code_expires BIGINT,
                enabled BOOLEAN DEFAULT 0,
                attempts INTEGER DEFAULT 0,
                last_attempt BIGINT DEFAULT 0
            );
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("✅ Таблица twofa создана/проверена!");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка создания таблицы: " + e.getMessage());
        }
    }

    // Сохранение или обновление записи
    public void saveOrUpdate(String username, String telegramId, String code, long expires, boolean enabled) {
        String sql = """
            INSERT INTO twofa (username, telegram_id, code, code_expires, enabled)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(username) DO UPDATE SET
                telegram_id = excluded.telegram_id,
                code = excluded.code,
                code_expires = excluded.code_expires,
                enabled = excluded.enabled
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, telegramId);
            pstmt.setString(3, code);
            pstmt.setLong(4, expires);
            pstmt.setBoolean(5, enabled);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка сохранения 2FA данных: " + e.getMessage());
        }
    }

    // Получить данные игрока по имени
    public TwoFactorData getByUsername(String username) {
        String sql = "SELECT * FROM twofa WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new TwoFactorData(
                        rs.getString("username"),
                        rs.getString("telegram_id"),
                        rs.getString("code"),
                        rs.getLong("code_expires"),
                        rs.getBoolean("enabled"),
                        rs.getInt("attempts"),
                        rs.getLong("last_attempt")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка получения данных: " + e.getMessage());
        }
        return null;
    }

    // Обновить код и время
    public void updateCode(String username, String code, long expires) {
        String sql = "UPDATE twofa SET code = ?, code_expires = ? WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setLong(2, expires);
            pstmt.setString(3, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка обновления кода: " + e.getMessage());
        }
    }

    // Увеличить счётчик попыток
    public void incrementAttempts(String username) {
        String sql = "UPDATE twofa SET attempts = attempts + 1, last_attempt = ? WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка обновления попыток: " + e.getMessage());
        }
    }

    // Сбросить попытки
    public void resetAttempts(String username) {
        String sql = "UPDATE twofa SET attempts = 0 WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка сброса попыток: " + e.getMessage());
        }
    }

    // Включить/выключить 2FA
    public void setEnabled(String username, boolean enabled) {
        String sql = "UPDATE twofa SET enabled = ? WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка обновления статуса 2FA: " + e.getMessage());
        }
    }

    // Удалить запись
    public void delete(String username) {
        String sql = "DELETE FROM twofa WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Ошибка удаления: " + e.getMessage());
        }
    }

    // Закрыть пул
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("✅ HikariCP пул закрыт.");
        }
    }

    // Вспомогательный класс для хранения данных
    public static class TwoFactorData {
        public final String username;
        public final String telegramId;
        public final String code;
        public final long codeExpires;
        public final boolean enabled;
        public final int attempts;
        public final long lastAttempt;

        public TwoFactorData(String username, String telegramId, String code, long codeExpires, boolean enabled, int attempts, long lastAttempt) {
            this.username = username;
            this.telegramId = telegramId;
            this.code = code;
            this.codeExpires = codeExpires;
            this.enabled = enabled;
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }

        public boolean isCodeValid(String inputCode, long currentTime) {
            if (code == null || code.isEmpty()) return false;
            if (!code.equals(inputCode)) return false;
            return currentTime <= codeExpires;
        }

        public boolean isBlocked(int maxAttempts, long blockDuration) {
            if (attempts >= maxAttempts) {
                return (System.currentTimeMillis() - lastAttempt) < blockDuration;
            }
            return false;
        }
    }
}