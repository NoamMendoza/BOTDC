package botdc;

import java.io.File;
import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;

public class Validaciones {
    private static final Dotenv dotenv = Dotenv.configure()
        .directory(".")
        .directory("./app")
        .directory("/app")
        .ignoreIfMissing()
        .load();
        
    public static void validateEnvironmentVariables() {
        if (dotenv.get("DISCORD_TOKEN") == null || dotenv.get("DISCORD_TOKEN").isEmpty()) {
            throw new IllegalStateException("DISCORD_TOKEN no está configurado en el archivo .env");
        }
        if (dotenv.get("USER_ID") == null || dotenv.get("USER_ID").isEmpty()) {
            throw new IllegalStateException("USER_ID no está configurado en el archivo .env");
        }
        if (dotenv.get("SOUND_FILE_PATH") == null || dotenv.get("SOUND_FILE_PATH").isEmpty()) {
            throw new IllegalStateException("SOUND_FILE_PATH no está configurado en el archivo .env");
        }
    }

    public static String sanitizePath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Ruta de archivo inválida: " + path);
        }
    }
}
