package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

public final class CrystalOptimizer extends Module implements TickListener {

    private final BooleanSetting autoPlace = new BooleanSetting(EncryptedString.of("Auto Place"), true);
    private final BooleanSetting autoBreak = new BooleanSetting(EncryptedString.of("Auto Break"), true);
    private final BooleanSetting clientRemove = new BooleanSetting(EncryptedString.of("Client Remove"), true);
    private final NumberSetting placeRange = new NumberSetting(EncryptedString.of("Place Range"), 1.0, 6.0, 4.5, 0.1);
    private final BooleanSetting pingAdapt = new BooleanSetting(EncryptedString.of("Ping Adapt"), true);

    private int hitCount;
    private int breakingBlockTick;

    public CrystalOptimizer() {
        super(EncryptedString.of("Crystal Optimizer"),
                EncryptedString.of("Crystal place / break optimizer"),
                -1,
                Category.CrystalPvP);
        addSettings(autoPlace, autoBreak, clientRemove, placeRange, pingAdapt);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        hitCount = 0;
        breakingBlockTick = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (mc.options.attackKey.isPressed()) breakingBlockTick++;
        else breakingBlockTick = 0;

        if (breakingBlockTick > 2) return;

        if (!mc.options.useKey.isPressed()) hitCount = 0;

        if (hitCount >= getLimitPackets()) return;

        if (autoBreak.getValue() && lookingAtCrystal()) {
            if (mc.options.attackKey.isPressed()) {
                EndCrystalEntity crystal = getCrystalLookingAt();
                if (crystal != null) {
                    if (clientRemove.getValue() && hitCount >= 1)
                        crystal.setRemoved(Entity.RemovalReason.KILLED);
                    hitCount++;
                }
            }
        }

        if (!autoPlace.getValue()) return;
        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        BlockHitResult lookPos = getLookPos();
        if (lookPos == null) return;

        if (mc.options.useKey.isPressed()
                && (isLookingAt(Blocks.OBSIDIAN, lookPos.getBlockPos())
                || isLookingAt(Blocks.BEDROCK, lookPos.getBlockPos()))) {

            ActionResult result = mc.interactionManager.interactBlock(
                    mc.player, Hand.MAIN_HAND, lookPos);

            if (result.isAccepted() && canPlaceCrystal(lookPos.getBlockPos()))
                mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private BlockHitResult getLookPos() {
        Vec3d camPos = mc.player.getEyePos();
        Vec3d lookVec = getLookVec();
        HitResult hr = mc.world.raycast(new RaycastContext(
                camPos,
                camPos.add(lookVec.multiply(placeRange.getValue())),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hr instanceof BlockHitResult bh ? bh : null;
    }

    private Vec3d getLookVec() {
        float f = (float) Math.PI / 180;
        float pi = (float) Math.PI;
        float f1 = MathHelper.cos(-mc.player.getYaw() * f - pi);
        float f2 = MathHelper.sin(-mc.player.getYaw() * f - pi);
        float f3 = -MathHelper.cos(-mc.player.getPitch() * f);
        float f4 = MathHelper.sin(-mc.player.getPitch() * f);
        return new Vec3d(f2 * f3, f4, f1 * f3).normalize();
    }

    private boolean isLookingAt(net.minecraft.block.Block block, BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == block;
    }

    private boolean lookingAtCrystal() {
        return mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof EndCrystalEntity;
    }

    private EndCrystalEntity getCrystalLookingAt() {
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof EndCrystalEntity crystal)
            return crystal;
        return null;
    }

    private boolean canPlaceCrystal(BlockPos block) {
        if (!mc.world.getBlockState(block).isOf(Blocks.OBSIDIAN)
                && !mc.world.getBlockState(block).isOf(Blocks.BEDROCK))
            return false;

        BlockPos above = block.up();
        if (!mc.world.isAir(above)) return false;

        double x = above.getX();
        double y = above.getY();
        double z = above.getZ();
        List<Entity> entities = mc.world.getOtherEntities(
                null,
                new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0));

        return entities.isEmpty();
    }

    private int getLimitPackets() {
        if (!pingAdapt.getValue()) return 1;
        int ping = getPing();
        return ping > 50 ? 2 : 1;
    }

    private int getPing() {
        if (mc.getNetworkHandler() == null) return 0;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}
