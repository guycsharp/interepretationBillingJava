// ConfigLoader.java
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static final String CONFIG_PATH =
            System.getProperty("user.dir") + File.separator
                    + "resources" + File.separator
                    + "config.properties";

    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Failed to load config.properties from " + CONFIG_PATH
            );
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
