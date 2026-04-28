package dlindustries.vigillant.system.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

/**
 * Local Spotify playback via <a href="https://github.com/LabyStudio/java-spotify-api">LabyStudio java-spotify-api</a>.
 * Uses reflection to avoid NoClassDefFoundError when the API is not available.
 */
public final class SpotifyService {

    private static volatile Object api; // SpotifyAPI instance (via reflection)
    private static volatile Object track; // Track instance (via reflection)
    private static volatile boolean playing;
    private static volatile boolean connected;
    private static volatile String lastCoverId = "";

    private static final Identifier COVER_TEX = Identifier.of("system_client", "hud/spotify_art");
    private static NativeImageBackedTexture coverBacking;

    private static boolean initStarted;
    private static boolean apiAvailable = true;

    // Cached reflection methods
    private static Class<?> spotifyAPIClass;
    private static Class<?> spotifyAPIFactoryClass;
    private static Class<?> spotifyListenerClass;
    private static Class<?> trackClass;
    private static Method factoryCreateMethod;
    private static Method apiInitializeMethod;
    private static Method apiRegisterListenerMethod;
    private static Method apiHasTrackMethod;
    private static Method apiGetTrackMethod;
    private static Method apiHasPositionMethod;
    private static Method apiGetPositionMethod;
    private static Method apiIsPlayingMethod;
    private static Method apiIsConnectedMethod;
    private static Method trackGetIdMethod;
    private static Method trackGetCoverArtMethod;

    private SpotifyService() {}

    private static boolean initReflection() {
        if (!apiAvailable) return false;
        try {
            if (spotifyAPIClass == null) {
                spotifyAPIClass = Class.forName("de.labystudio.spotifyapi.SpotifyAPI");
                spotifyAPIFactoryClass = Class.forName("de.labystudio.spotifyapi.SpotifyAPIFactory");
                spotifyListenerClass = Class.forName("de.labystudio.spotifyapi.SpotifyListener");
                trackClass = Class.forName("de.labystudio.spotifyapi.model.Track");

                factoryCreateMethod = spotifyAPIFactoryClass.getMethod("create");
                apiInitializeMethod = spotifyAPIClass.getMethod("initialize");
                apiRegisterListenerMethod = spotifyAPIClass.getMethod("registerListener", spotifyListenerClass);
                apiHasTrackMethod = spotifyAPIClass.getMethod("hasTrack");
                apiGetTrackMethod = spotifyAPIClass.getMethod("getTrack");
                apiHasPositionMethod = spotifyAPIClass.getMethod("hasPosition");
                apiGetPositionMethod = spotifyAPIClass.getMethod("getPosition");
                apiIsPlayingMethod = spotifyAPIClass.getMethod("isPlaying");
                apiIsConnectedMethod = spotifyAPIClass.getMethod("isConnected");
                trackGetIdMethod = trackClass.getMethod("getId");
                trackGetCoverArtMethod = trackClass.getMethod("getCoverArt");
            }
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            apiAvailable = false;
            return false;
        }
    }

    public static void ensureStarted() {
        if (initStarted) return;
        initStarted = true;

        if (!initReflection()) {
            connected = false;
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                Object a = factoryCreateMethod.invoke(null);
                apiInitializeMethod.invoke(a);
                api = a;
                connected = true;

                // Create dynamic proxy for SpotifyListener
                Object listener = java.lang.reflect.Proxy.newProxyInstance(
                        spotifyListenerClass.getClassLoader(),
                        new Class<?>[]{spotifyListenerClass},
                        (proxy, method, args) -> {
                            String name = method.getName();
                            switch (name) {
                                case "onConnect" -> connected = true;
                                case "onTrackChanged" -> {
                                    if (args.length > 0 && args[0] != null) {
                                        track = args[0];
                                        Object coverArt = trackGetCoverArtMethod.invoke(track);
                                        String id = (String) trackGetIdMethod.invoke(track);
                                        if (coverArt != null && !id.equals(lastCoverId)) {
                                            lastCoverId = id;
                                            uploadCover((BufferedImage) coverArt);
                                        }
                                    }
                                }
                                case "onPositionChanged" -> {}
                                case "onPlayBackChanged" -> {
                                    if (args.length > 0) {
                                        playing = (Boolean) args[0];
                                    }
                                }
                                case "onSync" -> {
                                    if (api != null) {
                                        try {
                                            track = apiGetTrackMethod.invoke(api);
                                            playing = (Boolean) apiIsPlayingMethod.invoke(api);
                                        } catch (Exception ignored) {}
                                    }
                                }
                                case "onDisconnect" -> connected = false;
                            }
                            return null;
                        }
                );

                apiRegisterListenerMethod.invoke(a, listener);
            } catch (Throwable t) {
                connected = false;
            }
        });
    }

    private static void uploadCover(BufferedImage img) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
                coverBacking = new NativeImageBackedTexture(() -> "spotify_art", ni);
                mc.getTextureManager().registerTexture(COVER_TEX, coverBacking);
            } catch (Exception ignored) {}
        });
    }

    public static Identifier getCoverTexture() {
        return COVER_TEX;
    }

    public static boolean hasCover() {
        return coverBacking != null;
    }

    public static Object getTrack() {
        if (!apiAvailable || api == null) return track;
        try {
            Boolean hasTrack = (Boolean) apiHasTrackMethod.invoke(api);
            return hasTrack ? apiGetTrackMethod.invoke(api) : track;
        } catch (Exception e) {
            return track;
        }
    }

    public static int getPositionMs() {
        if (!apiAvailable || api == null) return 0;
        try {
            Boolean hasPos = (Boolean) apiHasPositionMethod.invoke(api);
            return hasPos ? (Integer) apiGetPositionMethod.invoke(api) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isPlaying() {
        if (!apiAvailable || api == null) return false;
        try {
            return (Boolean) apiIsPlayingMethod.invoke(api);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isConnected() {
        if (!apiAvailable) return false;
        return connected && api != null;
    }

    public static String formatMs(int ms) {
        int s = ms / 1000;
        int m = s / 60;
        s = s % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
