package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.WorldUtils;
import dlindustries.vigillant.system.utils.rotation.Rotation;
import dlindustries.vigillant.system.utils.RotationUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class SafeAnchor extends Module implements TickListener {
    public enum LookMode { INSTA, LEGIT }

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 71, false)
            .setDescription(EncryptedString.of("Key to activate SafeAnchor sequence"));
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 1, 1)
            .setDescription(EncryptedString.of("Ticks between actions"));
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 1, 1)
            .setDescription(EncryptedString.of("Hotbar slot to switch to for exploding (1-9)"));
    private final ModeSetting<LookMode> lookMode = new ModeSetting<>(EncryptedString.of("Look Mode"), LookMode.INSTA, LookMode.class)
            .setDescription(EncryptedString.of("How to look at the anchor - Insta: instant snap, Legit: smooth look"));
    private final BooleanSetting autoExplode = new BooleanSetting(EncryptedString.of("Auto Explode"), true)
            .setDescription(EncryptedString.of("Automatically switch to totem slot and explode after charging"));

    private int step = 0;
    private int delayCounter = 0;
    private boolean isRunning = false;
    private BlockPos targetPos = null;

    public SafeAnchor() {
        super(EncryptedString.of("Safe Anchor"),
                EncryptedString.of("Places anchor, charges it safely, then explodes"),
                -1,
                Category.CrystalPvP);
        addSettings(activateKey, delay, totemSlot, lookMode, autoExplode);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        reset();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        if (mc.player == null || mc.world == null) return;

        if (!isRunning) {
            if (activateKey.getKey() != -1 && GLFW.glfwGetKey(mc.getWindow().getHandle(), activateKey.getKey()) == GLFW.GLFW_PRESS) {
                startSequence();
            }
            return;
        }

        if (targetPos == null || !BlockUtils.isBlock(targetPos, Blocks.RESPAWN_ANCHOR)) {
            reset();
            return;
        }

        if (delayCounter < delay.getValueInt()) {
            delayCounter++;
            return;
        }
        delayCounter = 0;

        lookAtAnchor();

        switch (step) {
            case 0 -> {
                if (BlockUtils.isAnchorNotCharged(targetPos)) {
                    if (InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE)) {
                        WorldUtils.placeBlock(new BlockHitResult(
                                targetPos.toCenterPos(),
                                mc.player.getHorizontalFacing(),
                                targetPos, false
                        ), true);
                    }
                } else {
                    step++;
                }
            }
            case 1 -> {
                if (BlockUtils.isAnchorCharged(targetPos)) {
                    if (autoExplode.getValue()) {
                        int slot = totemSlot.getValueInt() - 1;
                        InventoryUtils.setInvSlot(slot);
                        step++;
                    } else {
                        reset();
                    }
                }
            }
            case 2 -> {
                WorldUtils.placeBlock(new BlockHitResult(
                        targetPos.toCenterPos(),
                        mc.player.getHorizontalFacing(),
                        targetPos, false
                ), true);
                reset();
            }
        }
    }

    private void startSequence() {
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            targetPos = hit.getBlockPos();
            if (BlockUtils.isBlock(targetPos, Blocks.RESPAWN_ANCHOR)) {
                isRunning = true;
                step = 0;
                delayCounter = 0;
            }
        }
    }

    private void lookAtAnchor() {
        if (targetPos == null) return;
        Vec3d anchorCenter = targetPos.toCenterPos();

        if (lookMode.getMode() == LookMode.INSTA) {
            Rotation rotation = RotationUtils.getDirection(mc.player, anchorCenter);
            mc.player.setYaw((float) rotation.yaw());
            mc.player.setPitch((float) rotation.pitch());
        } else {
            Rotation targetRot = RotationUtils.getDirection(mc.player, anchorCenter);
            float targetYaw = (float) targetRot.yaw();
            float targetPitch = (float) targetRot.pitch();

            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();

            float deltaYaw = targetYaw - currentYaw;
            while (deltaYaw > 180) deltaYaw -= 360;
            while (deltaYaw < -180) deltaYaw += 360;

            mc.player.setYaw(currentYaw + deltaYaw * 0.3f);
            mc.player.setPitch(currentPitch + (targetPitch - currentPitch) * 0.3f);
        }
    }

    private void reset() {
        isRunning = false;
        step = 0;
        delayCounter = 0;
        targetPos = null;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
