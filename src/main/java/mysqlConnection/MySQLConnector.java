package mysqlConnection;// MySQLConnector.java
// Provides a simple method to get a JDBC Connection using credentials from ConfigLoader
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ConfigLoader.ConfigLoader;

public class MySQLConnector {
    // Load URL, username, and password from config.properties
    private static final String URL      = ConfigLoader.get("db.url");
    private static final String USER     = ConfigLoader.get("db.username");
    private static final String PASSWORD = ConfigLoader.get("db.password");

    /**
     * Opens and returns a new Connection to the MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
