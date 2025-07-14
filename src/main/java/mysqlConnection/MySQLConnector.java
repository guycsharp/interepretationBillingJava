import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class manages connection to a MySQL database using JDBC.
 * It can be reused across any project needing database access.
 */
public class MySQLConnector {

    // 🔧 Replace these with your actual database info
    private static final String URL      = ConfigLoader.get("db.url");
    private static final String USER     = ConfigLoader.get("db.username");
    private static final String PASSWORD = ConfigLoader.get("db.password");

    /**
     * Establishes a connection to the MySQL database.
     *
     * @return Connection object if successful
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * For testing the connection independently.

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Connected to MySQL successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        }
    }
     */
}
