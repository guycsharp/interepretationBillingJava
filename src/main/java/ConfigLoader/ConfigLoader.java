import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties props = new Properties();

    static {
        try (InputStream in = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IOException("config.properties not found in classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Unable to load config: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
