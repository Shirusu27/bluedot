import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Default path points to the project directory (not src!)
    private static String url = "jdbc:ucanaccess://C://Files//bluedot3//bluedotDatabase.accdb";

    public static void setUrl(String newUrl) {
        url = newUrl;
    }

    public static String getUrl() {
        return url;
    }

    public static Connection connect() {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
