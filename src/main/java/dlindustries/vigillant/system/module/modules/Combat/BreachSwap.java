package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class BreachSwap extends Module implements AttackListener, TickListener {
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
            .setDescription(EncryptedString.of("Switch back to sword after attack"));
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 250, 50, 1)
            .setDescription(EncryptedString.of("Delay in milliseconds for the macro"));
    private final NumberSetting breachSlot = new NumberSetting(EncryptedString.of("Breach Slot"), 1, 9, 7, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for where you put your breach mace"));
    private final BooleanSetting alwaysSwap = new BooleanSetting(EncryptedString.of("Always Swap"), false)
            .setDescription(EncryptedString.of("Swaps even if it misses, looks more legit"));
    private boolean shouldSwitchBack;
    private int originalSlot = -1;
    private long switchBackTime;
    public BreachSwap() {
        super(EncryptedString.of("Breach Swap"),
                EncryptedString.of("Swaps to selected breach mace slot when attacking with sword"),
                -1,
                Category.Combat);
        addSettings(switchBack, switchDelay, breachSlot, alwaysSwap);
    }
    @Override
    public void onEnable() {
        eventManager.add(AttackListener.class, this);
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(TickListener.class, this);
        if (shouldSwitchBack && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        super.onDisable();
    }
    @Override
    public void onAttack(AttackEvent event) {
        if (!alwaysSwap.getValue()) {
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
            Entity target = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (target == null) return;
        }
        ItemStack currentStack = mc.player.getMainHandStack();
        if (!currentStack.isIn(ItemTags.SWORDS)) return;
        if (mc.player.fallDistance > 1.5f) return;

        if (shouldSwitchBack) {
            switchBackTime = System.currentTimeMillis() + switchDelay.getValueInt();
            return;
        }
        int slotIndex = breachSlot.getValueInt() - 1;
        if (switchBack.getValue() && originalSlot == -1) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
        }
        mc.player.getInventory().setSelectedSlot(slotIndex);
        if (switchBack.getValue()) {
            shouldSwitchBack = true;
            switchBackTime = System.currentTimeMillis() + switchDelay.getValueInt();
        }
    }
    @Override
    public void onTick() {
        if (!shouldSwitchBack || originalSlot == -1) return;
        if (System.currentTimeMillis() < switchBackTime) return;
        mc.player.getInventory().setSelectedSlot(originalSlot);
        resetState();
    }
    private void resetState() {
        shouldSwitchBack = false;
        originalSlot = -1;
        switchBackTime = 0;
    }
}