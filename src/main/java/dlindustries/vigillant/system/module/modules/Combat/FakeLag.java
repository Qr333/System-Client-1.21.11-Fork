package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.GameRenderListener;
import dlindustries.vigillant.system.event.events.PacketReceiveListener;
import dlindustries.vigillant.system.event.events.PacketSendListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ColorSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public final class FakeLag extends Module implements TickListener, PacketSendListener, PacketReceiveListener, GameRenderListener {
    private enum GhostTrajMode { Standart }
    private enum ColorMode { Client, Custom }

    private final NumberSetting minDelay = new NumberSetting(EncryptedString.of("Min Delay"), 50, 2000, 100, 50);
    private final NumberSetting maxDelay = new NumberSetting(EncryptedString.of("Max Delay"), 50, 2000, 300, 50);
    private final BooleanSetting cancelOnExplosion = new BooleanSetting(EncryptedString.of("Cancel on Explosion"), true);
    private final BooleanSetting cancelOnDamage = new BooleanSetting(EncryptedString.of("Cancel on Damage"), true);
    private final BooleanSetting showESP = new BooleanSetting(EncryptedString.of("Show ESP"), true);
    private final BooleanSetting onlyShowInF5 = new BooleanSetting(EncryptedString.of("Only Show ESP In F5"), true);

    
    private final ModeSetting<GhostTrajMode> ghostTraj = new ModeSetting<>(EncryptedString.of("Ghost Traj"), GhostTrajMode.Standart, GhostTrajMode.class);
    private final NumberSetting ghostCount = new NumberSetting(EncryptedString.of("Ghost Count"), 1, 16, 4, 1);
    private final NumberSetting ghostLength = new NumberSetting(EncryptedString.of("Ghost Length"), 0.1, 3.0, 0.7, 0.1);
    private final NumberSetting ghostRadius = new NumberSetting(EncryptedString.of("Ghost Radius"), 0.1, 4.0, 1.6, 0.1);
    private final NumberSetting ghostStartSize = new NumberSetting(EncryptedString.of("Ghost Start Sz"), 0.01, 1.0, 0.18, 0.01);
    private final NumberSetting ghostEndSize = new NumberSetting(EncryptedString.of("Ghost End Sz"), 0.001, 0.2, 0.01, 0.001);
    private final NumberSetting subdivisions = new NumberSetting(EncryptedString.of("Subdivisions"), 2, 16, 5, 1);

    private final ModeSetting<ColorMode> colorMode = new ModeSetting<>(EncryptedString.of("Color Mode"), ColorMode.Client, ColorMode.class);
    private final ColorSetting customColor = new ColorSetting("Custom Color", new Color(170, 75, 255, 150));

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean flushing;
    private Vec3d ghostPosition;
    private long lagUntil;
    private float lastHealth = -1f;

    public FakeLag() {
        super(EncryptedString.of("FakeLag"), EncryptedString.of("Queues movement packets and shows ghost path"), -1, Category.Combat);
        addSettings(minDelay, maxDelay, cancelOnExplosion, cancelOnDamage, showESP, onlyShowInF5,
                ghostTraj, ghostCount, ghostLength, ghostRadius, ghostStartSize, ghostEndSize, subdivisions,
                colorMode, customColor);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(PacketSendListener.class, this);
        eventManager.add(PacketReceiveListener.class, this);
        eventManager.add(GameRenderListener.class, this);
        packetQueue.clear();
        flushing = false;
        ghostPosition = mc.player != null ? new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()) : null;
        lastHealth = mc.player != null ? mc.player.getHealth() : -1f;
        scheduleNextFlush();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(PacketSendListener.class, this);
        eventManager.remove(PacketReceiveListener.class, this);
        eventManager.remove(GameRenderListener.class, this);
        flush();
        ghostPosition = null;
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (System.currentTimeMillis() >= lagUntil && !packetQueue.isEmpty()) flush();
        if (packetQueue.isEmpty()) ghostPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            flush();
            return;
        }
        if (cancelOnExplosion.getValue() && event.packet instanceof ExplosionS2CPacket) {
            flush();
            return;
        }
        if (cancelOnDamage.getValue() && event.packet instanceof HealthUpdateS2CPacket hp) {
            float newHealth = hp.getHealth();
            if (lastHealth >= 0 && newHealth < lastHealth) flush();
            lastHealth = newHealth;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || flushing) return;
        Packet<?> pkt = event.packet;
        if (isCritical(pkt)) return;
        if (pkt instanceof PlayerInteractEntityC2SPacket || pkt instanceof PlayerInteractBlockC2SPacket) {
            flush();
            return;
        }
        if (packetQueue.isEmpty()) ghostPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        packetQueue.add(pkt);
        event.cancel();
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (!showESP.getValue() || mc.player == null || ghostPosition == null || packetQueue.isEmpty()) return;
        if (onlyShowInF5.getValue() && mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

        Color c = resolveColor();
        int count = ghostCount.getValueInt();
        double len = ghostLength.getValue();
        double rad = ghostRadius.getValue();
        double start = ghostStartSize.getValue();
        double end = ghostEndSize.getValue();

        Vec3d cam = RenderUtils.getCameraPos();
        double dx = (mc.player.getX() - ghostPosition.x) / Math.max(1, count);
        double dy = (mc.player.getY() - ghostPosition.y) / Math.max(1, count);
        double dz = (mc.player.getZ() - ghostPosition.z) / Math.max(1, count);

        for (int i = 0; i < count; i++) {
            double t = count <= 1 ? 1.0 : (double) i / (double) (count - 1);
            double size = start + (end - start) * t;
            Vec3d p = new Vec3d(ghostPosition.x + dx * i, ghostPosition.y + dy * i, ghostPosition.z + dz * i).subtract(cam);
            RenderUtils.renderLine(event.matrices, c, p.add(-size * rad, 0, 0), p.add(size * rad, 0, 0));
            RenderUtils.renderLine(event.matrices, c, p.add(0, -size * len, 0), p.add(0, size * len, 0));
            RenderUtils.renderLine(event.matrices, c, p.add(0, 0, -size * rad), p.add(0, 0, size * rad));
        }
    }

    private Color resolveColor() {
        if (colorMode.isMode(ColorMode.Custom)) return customColor.getValue();
        return Utils.getMainColor(160, 0);
    }

    private boolean isCritical(Packet<?> p) {
        return p instanceof KeepAliveC2SPacket
                || p instanceof ResourcePackStatusC2SPacket
                || p instanceof ClientStatusC2SPacket
                || p instanceof UpdateSelectedSlotC2SPacket;
    }

    private void flush() {
        if (mc.getNetworkHandler() == null) {
            packetQueue.clear();
            return;
        }
        flushing = true;
        Packet<?> packet;
        while ((packet = packetQueue.poll()) != null) {
            try {
                mc.getNetworkHandler().getConnection().send(packet, null, false);
            } catch (Exception ignored) {}
        }
        flushing = false;
        PlayerEntity player = mc.player;
        if (player != null) {
            ghostPosition = new Vec3d(player.getX(), player.getY(), player.getZ());
            lastHealth = player.getHealth();
        }
        scheduleNextFlush();
    }

    private void scheduleNextFlush() {
        int lo = minDelay.getValueInt();
        int hi = maxDelay.getValueInt();
        if (lo > hi) {
            int tmp = lo;
            lo = hi;
            hi = tmp;
        }
        int d = (lo == hi) ? lo : ThreadLocalRandom.current().nextInt(lo, hi + 1);
        lagUntil = System.currentTimeMillis() + d;
    }
}
