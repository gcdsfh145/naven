package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ModuleInfo(
        name = "AutoStuck",
        description = "Automatically use Stuck when falling into void and throw pearl if available",
        category = Category.MOVEMENT
)
public class AutoStuck extends Module {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> calculationTask;
    private boolean calculating = false;
    private boolean shouldThrowPearl = false;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private int pearlSlot = -1;
    private int attempts = 0;
    private final int maxAttempts = 3;
    private boolean wasInVoid = false;
    private boolean hasPearlInInventory = false;

    private float minFallDistance = 5.0f;
    private int minVoidDepth = 5;
    private boolean requireVoid = true;
    private boolean usePearl = true;

    @Override
    public void onEnable() {
        wasInVoid = false;
        attempts = 0;
        calculating = false;
        shouldThrowPearl = false;
        hasPearlInInventory = checkForPearl();
    }

    @Override
    public void onDisable() {
        if (calculationTask != null && !calculationTask.isDone()) {
            calculationTask.cancel(true);
        }

        Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
        if (stuck.isEnabled() && wasInVoid) {
            stuck.setEnabled(false);
        }

        wasInVoid = false;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE || mc.player == null) return;

        hasPearlInInventory = checkForPearl();

        if (usePearl && !hasPearlInInventory) {
            if (wasInVoid) {
                wasInVoid = false;
                Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
                if (stuck.isEnabled()) {
                    stuck.setEnabled(false);
                }
            }
            return;
        }

        boolean inVoid = isInVoid();

        if (inVoid && !wasInVoid) {
            wasInVoid = true;
            attempts = 0;

            Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
            if (!stuck.isEnabled()) {
                stuck.setEnabled(true);
            }

            if (usePearl && hasPearlInInventory) {
                checkAndPreparePearlThrow();
            }
        } else if (!inVoid && wasInVoid) {
            wasInVoid = false;

            Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
            if (stuck.isEnabled()) {
                stuck.setEnabled(false);
            }
        }

        if (shouldThrowPearl && pearlSlot != -1) {
            throwPearl();
            shouldThrowPearl = false;
        }
    }

    private boolean checkForPearl() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.ENDER_PEARL) {
                return true;
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.ENDER_PEARL) {
                return true;
            }
        }

        return false;
    }

    private boolean isInVoid() {
        if (mc.player == null || mc.level == null) return false;
        if (mc.player.fallDistance < minFallDistance || mc.player.onGround()) {
            return false;
        }

        BlockPos playerPos = mc.player.blockPosition();
        BlockPos belowPos = playerPos.below();

        if (!mc.level.isEmptyBlock(belowPos)) {
            return false;
        }

        int voidDepth = 0;
        for (int y = belowPos.getY(); y > mc.level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (!mc.level.isEmptyBlock(checkPos)) {
                break;
            }
            voidDepth++;
        }
        return voidDepth >= minVoidDepth;
    }

    private void checkAndPreparePearlThrow() {
        pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot == -1) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() == Items.ENDER_PEARL) {
                    pearlSlot = i;
                    break;
                }
            }
        }

        if (pearlSlot == -1) {
            return;
        }

        if (!calculating && attempts < maxAttempts) {
            calculating = true;
            attempts++;

            calculationTask = executor.submit(() -> {
                try {
                    calculateBestThrow();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    calculating = false;
                }
            });
        }
    }

    private void calculateBestThrow() {
        Vec3 playerPos = mc.player.position();
        Optional<Vec3> safeSpot = findSafeSpot(playerPos);

        if (safeSpot.isPresent()) {
            Vec3 target = safeSpot.get();
            Vec3 eyePos = playerPos.add(0, mc.player.getEyeHeight(), 0);

            double dx = target.x - eyePos.x;
            double dy = target.y - eyePos.y;
            double dz = target.z - eyePos.z;

            double dist = Math.sqrt(dx * dx + dz * dz);

            targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
            targetYaw = normalizeAngle(targetYaw);
            targetPitch = normalizeAngle(targetPitch);

            shouldThrowPearl = true;
        }
    }

    private float normalizeAngle(float angle) {
        angle %= 360.0f;
        if (angle > 180.0f) {
            angle -= 360.0f;
        } else if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    private Optional<Vec3> findSafeSpot(Vec3 playerPos) {
        int radius = 10;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos checkPos = new BlockPos(
                            (int) playerPos.x + x,
                            (int) playerPos.y + y,
                            (int) playerPos.z + z
                    );
                    if (!mc.level.isEmptyBlock(checkPos) &&
                            mc.level.isEmptyBlock(checkPos.above()) &&
                            mc.level.isEmptyBlock(checkPos.above(2))) {
                        return Optional.of(new Vec3(
                                checkPos.getX() + 0.5,
                                checkPos.getY() + 1,
                                checkPos.getZ() + 0.5
                        ));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private void throwPearl() {
        if (pearlSlot == -1 || mc.player == null) return;
        float originalYaw = mc.player.getYRot();
        float originalPitch = mc.player.getXRot();

        try {
            int prevSlot = mc.player.getInventory().selected;
            if (pearlSlot >= 9) {
                int emptyHotbarSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getItem(i).isEmpty()) {
                        emptyHotbarSlot = i;
                        break;
                    }
                }

                if (emptyHotbarSlot != -1) {
                    ItemStack pearlItem = mc.player.getInventory().getItem(pearlSlot);
                    mc.player.getInventory().setItem(emptyHotbarSlot, pearlItem);
                    mc.player.getInventory().setItem(pearlSlot, ItemStack.EMPTY);
                    pearlSlot = emptyHotbarSlot;
                } else {
                    ItemStack hotbarItem = mc.player.getInventory().getItem(0);
                    ItemStack pearlItem = mc.player.getInventory().getItem(pearlSlot);

                    mc.player.getInventory().setItem(0, pearlItem);
                    mc.player.getInventory().setItem(pearlSlot, hotbarItem);
                    pearlSlot = 0;
                }
            }
            mc.player.getInventory().selected = pearlSlot;
            RotationManager.rotations.x = targetYaw;
            RotationManager.rotations.y = targetPitch;
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
            mc.gameMode.useItem(mc.player, mc.player.getUsedItemHand());
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    if (mc.player != null) {
                        mc.player.getInventory().selected = prevSlot;
                        mc.player.setYRot(originalYaw);
                        mc.player.setXRot(originalPitch);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput e) {
        if (wasInVoid) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
            e.setJump(false);
            e.setSneak(false);
        }
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        wasInVoid = false;
        attempts = 0;
        hasPearlInInventory = checkForPearl(); // 重生后重新检查珍珠
        Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
        if (stuck.isEnabled()) {
            stuck.setEnabled(false);
        }
    }
}