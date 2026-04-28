package dlindustries.vigillant.system.utils;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persisted HUD element positions (scaled GUI coordinates).
 */
public final class HudLayout {

    public static int infoX = 14, infoY = 14;
    public static int coordsX = 14, coordsY = 44;
    public static int timeX = -1, timeY = 14;
    public static int spotifyX = 14, spotifyY = 120;
    public static int inventoryX = -1, inventoryY = -1;
    public static int arrayX = -1, arrayY = 120;

    public static float globalScale = 1f;
    public static float inventoryScale = 1f;
    public static float spotifyScale = 1f;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("system_client/hud_layout.properties");
    }

    public static void load() {
        Path p = path();
        if (!Files.isRegularFile(p)) return;
        try {
            Properties prop = new Properties();
            prop.load(Files.newInputStream(p));
            infoX = i(prop, "infoX", infoX);
            infoY = i(prop, "infoY", infoY);
            coordsX = i(prop, "coordsX", coordsX);
            coordsY = i(prop, "coordsY", coordsY);
            timeX = i(prop, "timeX", timeX);
            timeY = i(prop, "timeY", timeY);
            spotifyX = i(prop, "spotifyX", spotifyX);
            spotifyY = i(prop, "spotifyY", spotifyY);
            inventoryX = i(prop, "inventoryX", inventoryX);
            inventoryY = i(prop, "inventoryY", inventoryY);
            arrayX = i(prop, "arrayX", arrayX);
            arrayY = i(prop, "arrayY", arrayY);
            globalScale = f(prop, "globalScale", globalScale);
            inventoryScale = f(prop, "inventoryScale", inventoryScale);
            spotifyScale = f(prop, "spotifyScale", spotifyScale);
        } catch (IOException ignored) {}
    }

    public static void save() {
        Properties prop = new Properties();
        prop.setProperty("infoX", String.valueOf(infoX));
        prop.setProperty("infoY", String.valueOf(infoY));
        prop.setProperty("coordsX", String.valueOf(coordsX));
        prop.setProperty("coordsY", String.valueOf(coordsY));
        prop.setProperty("timeX", String.valueOf(timeX));
        prop.setProperty("timeY", String.valueOf(timeY));
        prop.setProperty("spotifyX", String.valueOf(spotifyX));
        prop.setProperty("spotifyY", String.valueOf(spotifyY));
        prop.setProperty("inventoryX", String.valueOf(inventoryX));
        prop.setProperty("inventoryY", String.valueOf(inventoryY));
        prop.setProperty("arrayX", String.valueOf(arrayX));
        prop.setProperty("arrayY", String.valueOf(arrayY));
        prop.setProperty("globalScale", String.valueOf(globalScale));
        prop.setProperty("inventoryScale", String.valueOf(inventoryScale));
        prop.setProperty("spotifyScale", String.valueOf(spotifyScale));
        try {
            Files.createDirectories(path().getParent());
            prop.store(Files.newOutputStream(path()), "system hud layout");
        } catch (IOException ignored) {}
    }

    private static int i(Properties p, String k, int def) {
        try {
            return Integer.parseInt(p.getProperty(k, String.valueOf(def)).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float f(Properties p, String k, float def) {
        try {
            return Float.parseFloat(p.getProperty(k, String.valueOf(def)).trim());
        } catch (Exception e) {
            return def;
        }
    }

    /** Call when chat closes to persist drag positions. */
    public static boolean needsSave;

    private HudLayout() {}
}
