package be.isach.ultracosmetics.mysql;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.suits.ArmorSlot;
import be.isach.ultracosmetics.cosmetics.type.GadgetType;
import be.isach.ultracosmetics.cosmetics.type.PetType;
import be.isach.ultracosmetics.log.SmartLogger;
import be.isach.ultracosmetics.log.SmartLogger.LogLevel;
import be.isach.ultracosmetics.mysql.hikari.IHikariHook;
import be.isach.ultracosmetics.mysql.hikari.OldHikariHook;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.sql.DataSource;

/**
 * Package: be.isach.ultracosmetics.mysql
 * Created by: sachalewin
 * Date: 5/08/16
 * Project: UltraCosmetics
 */
public class MySqlConnectionManager extends BukkitRunnable {
    private final String tableName;
    /**
     * UltraCosmetics instance.
     */
    private final UltraCosmetics ultraCosmetics;

    /**
     * MySQL Connection & Table.
     */
    private Table table;

    /**
     * Connecting pooling.
     */
    private final IHikariHook hikariHook;
    private final DataSource dataSource;
    private final String CREATE_TABLE;
    private final List<Column<?>> columns = new ArrayList<>();
    private final boolean debug;
    private boolean success = true;

    public MySqlConnectionManager(UltraCosmetics ultraCosmetics) {
        this.ultraCosmetics = ultraCosmetics;
        ConfigurationSection section = SettingsManager.getConfig().getConfigurationSection("MySQL");
        this.debug = section.getBoolean("debug", false);
        String hostname = section.getString("hostname");
        String port = section.getString("port");
        String database = section.getString("database");
        String username = section.getString("username");
        String password = section.getString("password");
        tableName = section.getString("table");

        hikariHook = createHikariHook(hostname, port, database, username, password);
        if (hikariHook == null) {
            // If we couldn't load the Hikari hook, finish initializing the final
            // fields and give up, an error has already been thrown.
            dataSource = null;
            CREATE_TABLE = null;
            return;
        }
        dataSource = hikariHook.getDataSource();

        // "PRIMARY KEY" implies UNIQUE NOT NULL.
        // String form of UUID is always exactly 36 chars so just store it that way.
        // This is not a StringColumn because StringColumns are varchars
        columns.add(new Column<>("uuid", "CHAR(36) PRIMARY KEY", String.class));
        columns.add(new Column<>("gadgetsEnabled", "BOOLEAN DEFAULT TRUE NOT NULL", Boolean.class));
        columns.add(new Column<>("selfmorphview", "BOOLEAN DEFAULT TRUE NOT NULL", Boolean.class));
        columns.add(new Column<>("treasureNotifications", "BOOLEAN DEFAULT TRUE NOT NULL", Boolean.class));
        columns.add(new Column<>("filterByOwned", "BOOLEAN DEFAULT FALSE NOT NULL", Boolean.class));
        columns.add(new Column<>("treasureKeys", "INTEGER DEFAULT 0 NOT NULL", Integer.class));
        for (GadgetType gadgetType : GadgetType.values()) {
            columns.add(new Column<>(gadgetType.getConfigName().toLowerCase(), "INTEGER DEFAULT 0 NOT NULL", Integer.class));
        }
        for (PetType petType : PetType.values()) {
            // Anvil can only hold 50 characters on 1.18
            columns.add(new StringColumn(petType.getConfigName().toLowerCase(), 50));
        }

        for (Category cat : Category.values()) {
            // 32 because it's about double the longest existing cosmetic name
            if (cat == Category.SUITS) {
                for (ArmorSlot slot : ArmorSlot.values()) {
                    columns.add(new StringColumn(cat.toString().toLowerCase() + "_" + slot.toString().toLowerCase(), 32));
                }
                continue;
            }
            
            columns.add(new StringColumn(cat.toString().toLowerCase(), 32));
        }

        StringJoiner columnJoiner = new StringJoiner(", ", "(", ")");
        for (Column<?> column : columns) {
            columnJoiner.add(column.toString());
        }
        CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `" + tableName + "`" + columnJoiner.toString();
    }

    public void start() {
        runTaskAsynchronously(ultraCosmetics);
    }

    @Override
    public void run() {
        try (Connection co = dataSource.getConnection()) {
            try (PreparedStatement sql = co.prepareStatement(CREATE_TABLE)) {
                sql.executeUpdate();
            }

            fixTable(co);

            table = new Table(dataSource, tableName);
        } catch (SQLException e) {
            reportFailure(e);
            return;
        }
        ultraCosmetics.getPlayerManager().initPlayers();
    }

    private void reportFailure(Throwable e) {
        success = false;
        UltraCosmeticsData.get().setFileStorage(true);
        SmartLogger log = ultraCosmetics.getSmartLogger();
        log.write(LogLevel.ERROR, "Could not connect to MySQL server!");
        log.write(LogLevel.ERROR, "Error:");
        e.printStackTrace();
    }

    public Table getTable() {
        return table;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean success() {
        return success;
    }

    public List<Column<?>> getColumns() {
        return columns;
    }

    public void shutdown() {
        hikariHook.close();
    }

    private IHikariHook createHikariHook(String hostname, String port, String database, String username, String password) {
        Class<?> hook;
        try {
            hook = Class.forName("be.isach.ultracosmetics.mysql.hikari.NewHikariHook");
            ultraCosmetics.getSmartLogger().write("Loading Hikari for Java 11...");
            // I think ClassNotFoundException would only be thrown if NewHikariHook isn't present, but we have to catch it anyway
        } catch (UnsupportedClassVersionError | ClassNotFoundException | NoClassDefFoundError e) {
            hook = OldHikariHook.class;
            ultraCosmetics.getSmartLogger().write("Loading Hikari for Java 8...");
        }
        try {
            return (IHikariHook) hook.getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class)
                    .newInstance(hostname, port, database, username, password);
        } catch (ReflectiveOperationException e) {
            ultraCosmetics.getSmartLogger().write(LogLevel.ERROR, "Failed to initialize Hikari handler");
            reportFailure(e);
            return null;
        }
    }

    /**
     * Based on the field 'columns', adds any missing columns to the database (in order).
     * 
     * @param co Connection to work with.
     */
    private void fixTable(Connection co) throws SQLException {
        DatabaseMetaData md = co.getMetaData();
        boolean upgradeAnnounced = false;
        try (ResultSet rs = md.getColumns(null, null, tableName, "id")) {
            if (rs.next()) {
                ultraCosmetics.getSmartLogger().write("You have an old UCData table. Attempting to upgrade it...");
                alter(co, "DROP COLUMN id");
                alter(co, "DROP COLUMN username");
                alter(co, "MODIFY uuid CHAR(36)");
                alter(co, "ADD PRIMARY KEY (uuid)");
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            Column<?> col = columns.get(i);
            try (ResultSet rs = md.getColumns(null, null, tableName, col.getName())) {
                if (!rs.next()) {
                    if (!upgradeAnnounced) {
                        ultraCosmetics.getSmartLogger().write("Upgrading database...");
                        upgradeAnnounced = true;
                    }
                    String afterPrevious;
                    if (i == 0) {
                        afterPrevious = "FIRST";
                    } else {
                        afterPrevious = "AFTER " + columns.get(i - 1).getName();
                    }
                    alter(co, "ADD " + col.toString() + " " + afterPrevious);
                    continue;
                }
                // if a varchar is the wrong size, adjust it
                if (col instanceof StringColumn && rs.getInt("COLUMN_SIZE") != ((StringColumn) col).getSize()) {
                    if (!upgradeAnnounced) {
                        ultraCosmetics.getSmartLogger().write("Upgrading database...");
                        upgradeAnnounced = true;
                    }
                    alter(co, "MODIFY COLUMN " + col.toString());
                }
            }
        }
        if (upgradeAnnounced) {
            ultraCosmetics.getSmartLogger().write("Upgrade finished.");
        }
    }

    private void alter(Connection co, String command) throws SQLException {
        String query = "ALTER TABLE `" + tableName + "` " + command;
        if (isDebug()) {
            UltraCosmeticsData.get().getPlugin().getSmartLogger().write("Executing SQL: " + query);
        }
        PreparedStatement ps = co.prepareStatement(query);
        ps.execute();
        ps.close();
    }
}
