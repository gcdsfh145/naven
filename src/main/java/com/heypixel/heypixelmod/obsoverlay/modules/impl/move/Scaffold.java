package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.apache.commons.lang3.RandomUtils;
import org.joml.Vector4f;

import static com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD.headerColor;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks = Arrays.asList(
           Blocks.WATER,
           Blocks.LAVA,
           Blocks.ENCHANTING_TABLE,
           Blocks.GLASS_PANE,
           Blocks.GLASS_PANE,
           Blocks.IRON_BARS,
           Blocks.SNOW,
           Blocks.COAL_ORE,
           Blocks.DIAMOND_ORE,
           Blocks.EMERALD_ORE,
           Blocks.CHEST,
           Blocks.TRAPPED_CHEST,
           Blocks.TORCH,
           Blocks.ANVIL,
           Blocks.TRAPPED_CHEST,
           Blocks.NOTE_BLOCK,
           Blocks.JUKEBOX,
           Blocks.TNT,
           Blocks.GOLD_ORE,
           Blocks.IRON_ORE,
           Blocks.LAPIS_ORE,
           Blocks.STONE_PRESSURE_PLATE,
           Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
           Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
           Blocks.STONE_BUTTON,
           Blocks.LEVER,
           Blocks.TALL_GRASS,
           Blocks.RED_MUSHROOM,
           Blocks.BROWN_MUSHROOM,
           Blocks.VINE,
           Blocks.SUNFLOWER,
           Blocks.LADDER,
           Blocks.FURNACE,
           Blocks.SAND,
           Blocks.CACTUS,
           Blocks.DISPENSER,
           Blocks.DROPPER,
           Blocks.CRAFTING_TABLE,
           Blocks.COBWEB,
           Blocks.PUMPKIN,
           Blocks.COBBLESTONE_WALL,
           Blocks.OAK_FENCE,
           Blocks.REDSTONE_TORCH,
           Blocks.FLOWER_POT
   );
   public Vector2f correctRotation = new Vector2f();
   public Vector2f rots = new Vector2f();
   public Vector2f lastRots = new Vector2f();
   private int offGroundTicks = 0;
   private final BooleanValue lowBlockWarning = ValueBuilder.create(this, "Low Block Warning")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();
   private final FloatValue warningThreshold = ValueBuilder.create(this, "Warning Blocks")
           .setVisibility(() -> this.lowBlockWarning.getCurrentValue())
           .setDefaultFloatValue(15F)
           .setMinFloatValue(1F)
           .setMaxFloatValue(20F)
           .setFloatStep(1F)
           .build()
           .getFloatValue();
   private final FloatValue TellyRotationAngle = ValueBuilder.create(this, "Rotation Angle")
           .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
           .setDefaultFloatValue(180.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(360.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   private final FloatValue blinkFixRotationSpeed = ValueBuilder.create(this, "Rotation Speed")
           .setVisibility(() -> this.mode.isCurrentMode("ShaoYu"))
           .setDefaultFloatValue(180.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(360.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Normal", "ShaoYu", "Telly Bridge", "Keep Y").build().getModeValue();
   public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("Normal"))
           .build()
           .getBooleanValue();
   public BooleanValue sneak = ValueBuilder.create(this, "Sneak").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue noSwing = ValueBuilder.create(this, "No Swing").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue snap = ValueBuilder.create(this, "Snap")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("Normal"))
           .build()
           .getBooleanValue();
   public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
           .build()
           .getBooleanValue();
   public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue keepFoV = ValueBuilder.create(this, "Keep FoV").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
           .setDefaultFloatValue(1.15F)
           .setMaxFloatValue(2.0F)
           .setMinFloatValue(1.0F)
           .setFloatStep(0.05F)
           .setVisibility(() -> this.keepFoV.getCurrentValue())
           .build()
           .getFloatValue();
   private final FloatValue stepBlocks = ValueBuilder.create(this, "Blocks Per Step")
           .setDefaultFloatValue(4.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> this.mode.isCurrentMode("ShaoYu"))
           .build()
           .getFloatValue();
   int oldSlot;
   private boolean hasWarned = false;
   private int lastHotbarBlockCount = 0;
   private long jumpTime = 0;
   private boolean wasJumping = false;
   private Scaffold.BlockPosWithFacing pos;
   private int lastSneakTicks;
   public int baseY = -1;
   private int blockupdate = 0;
   public BooleanValue autoStuck = ValueBuilder.create(this, "Auto Stuck")
           .setVisibility(() -> this.selfRescue.getCurrentValue())
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   private boolean isAutoStucking = false;
   private int autoStuckTicks = 0;
   private int skipTick = 0;
   private BlockPos autoStuckTargetPos = null;
   private Direction autoStuckTargetFace = null;
   private Vec3 autoStuckHitVec = null;
   private int jumpCooldown = 0;
   public BooleanValue selfRescue = ValueBuilder.create(this, "SelfRescue")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public FloatValue selfRescueFallDist = ValueBuilder.create(this, "SelfRescue Fall Distance")
           .setVisibility(() -> this.selfRescue.getCurrentValue())
           .setDefaultFloatValue(3.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   public BooleanValue selfRescueDebug = ValueBuilder.create(this, "SelfRescue Debug")
           .setVisibility(() -> this.selfRescue.getCurrentValue())
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue fastSelfRescue = ValueBuilder.create(this, "Fast Self Rescue")
           .setVisibility(() -> this.selfRescue.getCurrentValue())
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public FloatValue rescuePlaceSpeed = ValueBuilder.create(this, "Rescue Place Speed")
           .setVisibility(() -> this.selfRescue.getCurrentValue() && this.fastSelfRescue.getCurrentValue())
           .setDefaultFloatValue(3.0F)
           .setFloatStep(0.5F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   private boolean fastRescueActive = false;
   private int fastRescueTicks = 0;
   private int fastRescuePlaceAttempts = 0;
   private BlockPos fastRescueTargetPos = null;
   private Direction fastRescueTargetFace = null;

   private int selfRescueAttempted;
   private boolean selfRescueEnabled;
   private boolean selfRescueCalculating;
   private CalculateThread selfRescueCalculateThread;
   private final Random random = new Random();

   private static final double T = 10;
   private static final double T_MIN = 0.001;
   private static final double ALPHA = 0.997;
   private boolean isCyclicFreezing = false;
   private long freezeStartTime = 0;
   private long unfreezeStartTime = 0;
   private int freezeCycle = 0;
   private boolean shouldSendNotReachableMessage = false;
   private final AtomicBoolean selfRescueThreadRunning = new AtomicBoolean(false);
   private final AtomicReference<Vector2f> selfRescueRotation = new AtomicReference<>(null);

   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
         return false;
      } else if (!InventoryUtils.isItemValid(stack)) {
         return false;
      } else {
         String string = stack.getDisplayName().getString();
         if (string.contains("Click") || string.contains("点击")) {
            return false;
         } else if (stack.getItem() instanceof ItemNameBlockItem) {
            return false;
         } else {
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block instanceof FlowerBlock) {
               return false;
            } else if (block instanceof BushBlock) {
               return false;
            } else if (block instanceof FungusBlock) {
               return false;
            } else if (block instanceof CropBlock) {
               return false;
            } else {
               return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
            }
         }
      }
   }

   private boolean isValidPlacementPos(BlockPos pos) {
      return mc.level.getBlockState(pos).isAir() || mc.level.getBlockState(pos).canBeReplaced();
   }

   private static Vec3 getVec3(BlockPos checkPosition, BlockState block) {
      VoxelShape shape = block.getShape(mc.level, checkPosition);
      double ex = MathHelper.clamp(mc.player.getX(), (double) checkPosition.getX(), checkPosition.getX() + shape.max(Direction.Axis.X));
      double ey = MathHelper.clamp(mc.player.getY(), (double) checkPosition.getY(), checkPosition.getY() + shape.max(Direction.Axis.Y));
      double ez = MathHelper.clamp(mc.player.getZ(), (double) checkPosition.getZ(), checkPosition.getZ() + shape.max(Direction.Axis.Z));
      return new Vec3(ex, ey, ez);
   }

   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.level
              .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double) (-sensitivity), 0.0, (double) (-sensitivity)))
              .iterator()
              .hasNext();
   }

   private boolean isSolid(BlockPos pos) {
      return !mc.level.getBlockState(pos).isAir() && mc.level.getBlockState(pos).isSolid();
   }

   @EventTarget
   public void onFoV(EventUpdateFoV e) {
      if (this.keepFoV.getCurrentValue() && MoveUtils.isMoving()) {
         e.setFov(this.fov.getCurrentValue() + (float) PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.oldSlot = mc.player.getInventory().selected;
         this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
         this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
         this.pos = null;
         this.baseY = 10000;
         int hotbarBlocks = countHotbarBlocks();
         if (hotbarBlocks == 0 && lowBlockWarning.getCurrentValue()) {
            ChatUtils.addChatMessage("§c[Scaffold Warning] NoBlocks!");
         }
      }
      resetSelfRescueState();
      this.hasWarned = false;
   }

   private void resetSelfRescueState() {
      this.selfRescueAttempted = 0;
      this.selfRescueEnabled = false;
      this.selfRescueCalculating = false;
      this.selfRescueThreadRunning.set(false);
      this.selfRescueRotation.set(null);
      this.fastRescueActive = false;
      this.fastRescueTicks = 0;
      this.fastRescuePlaceAttempts = 0;
      this.fastRescueTargetPos = null;
      this.fastRescueTargetFace = null;
      this.isCyclicFreezing = false;
      this.freezeCycle = 0;
      this.isAutoStucking = false;
      this.autoStuckTicks = 0;

      if (selfRescueCalculateThread != null && selfRescueCalculateThread.isAlive()) {
         selfRescueCalculateThread.stop = true;
         selfRescueCalculateThread.interrupt();
      }
   }

   @EventTarget
   public void onPlaceBlock(PlayerInteractEvent.RightClickBlock e) {
      if (!this.mode.isCurrentMode("ShaoYu")) return;
      blockupdate++;
      if (blockupdate >= stepBlocks.getCurrentValue()) {
         blockupdate = 0;
         if (mc.player != null) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
         }
      }
   }

   @EventTarget
   public void onMoveInput(EventMoveInput event) {
      if (this.isAutoStucking && this.autoStuck.getCurrentValue() && skipTick == 0) {
         event.setForward(0.0F);
         event.setStrafe(0.0F);
         event.setJump(false);
         event.setSneak(false);
         mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
      }

      if (isCyclicFreezing && freezeCycle == 1) {
         event.setForward(0.0F);
         event.setStrafe(0.0F);
         event.setJump(false);
         event.setSneak(false);
      }
   }

   @EventTarget
   public void onPacketReceive(EventPacket e) {
      if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
         skipTick = 2;
         this.isAutoStucking = false;
      }

      if (e.getPacket() instanceof ClientboundTeleportEntityPacket) {
         ClientboundTeleportEntityPacket packet = (ClientboundTeleportEntityPacket) e.getPacket();
         if (packet.getId() == mc.player.getId()) {
            skipTick = 2;
            this.isAutoStucking = false;
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && this.isAutoStucking && this.autoStuck.getCurrentValue()) {
         mc.player.setDeltaMovement(0, 0, 0);
         Vec3 currentPos = mc.player.position();
         mc.player.setPos(currentPos.x, currentPos.y, currentPos.z);
      }
      if (e.getType() == EventType.PRE && isCyclicFreezing && freezeCycle == 1) {
         mc.player.setDeltaMovement(0, 0, 0);
         Vec3 currentPos = mc.player.position();
         mc.player.setPos(currentPos.x, currentPos.y, currentPos.z);
      }
   }

   @Override
   public void onDisable() {
      boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
      boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
      mc.options.keyJump.setDown(isHoldingJump);
      mc.options.keyShift.setDown(isHoldingShift);
      mc.options.keyUse.setDown(false);
      mc.player.getInventory().selected = this.oldSlot;
      this.isAutoStucking = false;
      this.autoStuckTicks = 0;
      this.skipTick = 0;
      this.jumpCooldown = 0;
      this.autoStuckTargetPos = null;
      this.autoStuckTargetFace = null;
      this.autoStuckHitVec = null;
      if (selfRescueCalculateThread != null && selfRescueCalculateThread.isAlive()) {
         selfRescueCalculateThread.stop = true;
         selfRescueCalculateThread.interrupt();
      }
      selfRescueThreadRunning.set(false);
      selfRescueRotation.set(null);
      isCyclicFreezing = false;
      freezeCycle = 0;

   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND) {
         e.setItem(mc.player.getInventory().getItem(this.oldSlot));
      }
   }

   @EventTarget(1)
   public void onEventEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
         if (mc.player.onGround()) {
            resetSelfRescueState();
         }

         if (mc.player.input.jumping && !wasJumping) {
            jumpTime = System.currentTimeMillis();
            wasJumping = true;
         } else if (!mc.player.input.jumping) {
            wasJumping = false;
         }

         if (jumpCooldown > 0) {
            jumpCooldown--;
         }
         boolean shouldFreeze = isCyclicFreezing;
         long currentTime = System.currentTimeMillis();
         boolean isFallingFromJump = (currentTime - jumpTime) < 750;

         if (!isCyclicFreezing && this.selfRescue.getCurrentValue() &&
                 !mc.player.onGround() && mc.player.fallDistance > selfRescueFallDist.getCurrentValue() &&
                 jumpCooldown <= 0 && hasPlaceableAreaInView(5.0f) && !isBlockUnder() && !isFallingFromJump) {
            shouldFreeze = true;
         }

         if (shouldFreeze) {
            if (freezeCycle == 1) {
               if (currentTime - freezeStartTime >= 800) {
                  freezeCycle = 2;
                  unfreezeStartTime = currentTime;
                  shouldSendNotReachableMessage = true;
               }
            } else if (freezeCycle == 2) {
               if (currentTime - unfreezeStartTime >= 75) {
                  if (mc.player.onGround() || (fastRescueActive && fastRescuePlaceAttempts > 0) ||
                          (selfRescueThreadRunning.get() && selfRescueRotation.get() != null) || isBlockUnder()) {
                     isCyclicFreezing = false;
                     freezeCycle = 0;
                  } else {
                     if (hasPlaceableAreaInView(5.0f)) {
                        freezeCycle = 1;
                        freezeStartTime = currentTime;
                        if (shouldSendNotReachableMessage) {
                           ChatUtils.addChatMessage("Not reachable");
                           shouldSendNotReachableMessage = false;
                        }
                     } else {
                        isCyclicFreezing = false;
                        freezeCycle = 0;
                        ChatUtils.addChatMessage("别着急，你安全的很");
                     }
                  }
               }
            }
         }
         if (this.selfRescue.getCurrentValue() && !mc.player.onGround() &&
                 mc.player.fallDistance > selfRescueFallDist.getCurrentValue() && jumpCooldown <= 0 &&
                 hasPlaceableAreaInView(5.0f) && !isBlockUnder() && !isFallingFromJump) {
            this.handleSelfRescue();
            if (!isCyclicFreezing && !mc.player.onGround()) {
               isCyclicFreezing = true;
               freezeCycle = 1;
               freezeStartTime = System.currentTimeMillis();
            }
         }
         Vector2f rotation = selfRescueRotation.getAndSet(null);
         if (rotation != null) {
            mc.player.setYRot(rotation.getX());
            mc.player.setXRot(rotation.getY());
            this.placeBlockAtAngle(rotation.getX(), rotation.getY());
         }

         if (fastRescueActive) {
            fastRescueTicks++;
            mc.player.setDeltaMovement(0, 0, 0);
            if (fastRescueTicks % (int) (20 / rescuePlaceSpeed.getCurrentValue()) == 0) {
               this.tryFastRescuePlace();
            }
            if (mc.player.onGround() || fastRescueTicks > 100 || fastRescuePlaceAttempts > 10) {
               fastRescueActive = false;
               fastRescueTicks = 0;
               fastRescuePlaceAttempts = 0;

               if (selfRescueDebug.getCurrentValue()) {
                  ChatUtils.addChatMessage("[Scaffold] 跳过了一个喜欢你的虚空娘");
               }
            }
         }
         int slotID = -1;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
               slotID = i;
               break;
            }
         }
         if (mc.player.onGround()) {
            this.offGroundTicks = 0;
         } else {
            this.offGroundTicks++;
         }
         if (slotID != -1 && mc.player.getInventory().selected != slotID) {
            mc.player.getInventory().selected = slotID;
         }

         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
         if (this.mode.isCurrentMode("ShaoYu")) {
            int step = (int) this.stepBlocks.getCurrentValue();
            if (mc.player.tickCount % step == 0 || this.baseY == -1) {
               this.baseY = (int) Math.floor(mc.player.getY()) - 1;
            }
         } else {
            if (this.baseY == -1
                    || this.baseY > (int) Math.floor(mc.player.getY()) - 1
                    || mc.player.onGround()
                    || !PlayerUtils.movementInput()
                    || isHoldingJump
                    || this.mode.isCurrentMode("Normal")) {
               this.baseY = (int) Math.floor(mc.player.getY()) - 1;
            }
         }

         this.getBlockPos();
         if (this.pos != null) {
            this.correctRotation = this.getPlayerYawRotation();
            if (this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue()) {
               this.rots.setX(this.correctRotation.getX());
            } else {
               this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), this.correctRotation.getX()));
            }

            this.rots.setY(this.correctRotation.getY());
         }

         if (this.sneak.getCurrentValue()) {
            this.lastSneakTicks++;
            System.out.println(this.lastSneakTicks);
            if (this.lastSneakTicks == 18) {
               if (mc.player.isSprinting()) {
                  mc.options.keySprint.setDown(false);
                  mc.player.setSprinting(false);
               }

               mc.options.keyShift.setDown(true);
            } else if (this.lastSneakTicks >= 21) {
               mc.options.keyShift.setDown(false);
               this.lastSneakTicks = 0;
            }
         }

         if (this.mode.isCurrentMode("Telly Bridge")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            if (this.offGroundTicks < 1 && PlayerUtils.movementInput()) {
               this.rots.setX(RotationUtils.rotateToYaw(TellyRotationAngle.getCurrentValue(), this.rots.getX(), mc.player.getYRot()));
               this.lastRots.set(this.rots.getX(), this.rots.getY());
               return;
            }

         } else if (this.mode.isCurrentMode("ShaoYu")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            if (mc.player.onGround() && PlayerUtils.movementInput()) {
               this.rots.setX(RotationUtils.rotateToYaw(blinkFixRotationSpeed.getCurrentValue(), this.rots.getX(), mc.player.getYRot()));
               this.lastRots.set(this.rots.getX(), this.rots.getY());
               return;
            }
         } else if (this.mode.isCurrentMode("Keep Y")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
         } else {
            if (this.eagle.getCurrentValue()) {
               mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
            }

            if (this.snap.getCurrentValue() && !isHoldingJump) {
               this.doSnap();
            }
         }

         this.lastRots.set(this.rots.getX(), this.rots.getY());
      }
   }

   private boolean hasPlaceableAreaWithinRange(float range) {
      if (mc.player == null || mc.level == null) return false;
      Vec3 playerPos = mc.player.position();
      BlockPos playerBlockPos = new BlockPos(
              (int) Math.floor(playerPos.x),
              (int) Math.floor(playerPos.y) - 1,
              (int) Math.floor(playerPos.z)
      );
      if (mc.level.getBlockState(playerBlockPos).getBlock() instanceof AirBlock) {
         for (Direction face : Direction.values()) {
            BlockPos neighbor = playerBlockPos.relative(face);
            if (!(mc.level.getBlockState(neighbor).getBlock() instanceof AirBlock)) {
               return true;
            }
         }
      } else {
         return false;
      }
      int rangeInt = (int) Math.ceil(range);
      for (int x = -rangeInt; x <= rangeInt; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -rangeInt; z <= rangeInt; z++) {
               if (x == 0 && y == 0 && z == 0) continue;

               BlockPos checkPos = playerBlockPos.offset(x, y, z);
               double distance = Math.sqrt(x * x + y * y + z * z);
               if (distance > range) continue;
               if (mc.level.getBlockState(checkPos).getBlock() instanceof AirBlock) {
                  for (Direction face : Direction.values()) {
                     BlockPos neighbor = checkPos.relative(face);
                     if (!(mc.level.getBlockState(neighbor).getBlock() instanceof AirBlock)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private void doSnap() {
      boolean shouldPlaceBlock = false;
      HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
      if (objectPosition.getType() == Type.BLOCK) {
         BlockHitResult position = (BlockHitResult) objectPosition;
         if (position.getBlockPos().equals(this.pos) && position.getDirection() != Direction.UP) {
            shouldPlaceBlock = true;
         }
      }

      if (!shouldPlaceBlock) {
         this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      e.setCancelled(true);
      if (mc.screen == null && mc.player != null && this.pos != null && (!this.mode.isCurrentMode("Telly Bridge") || this.offGroundTicks >= 1)) {
         if (!this.checkPlace(this.pos)) {
            return;
         }

         this.placeBlock();
      }
   }

   private boolean hasPlaceableAreaInView(float range) {
      if (mc.player == null || mc.level == null) return false;
      Vec3 lookVec = mc.player.getLookAngle();
      Vec3 eyePos = mc.player.getEyePosition();
      int feetY = (int) Math.floor(mc.player.getY());
      for (float distance = 1.0f; distance <= range; distance += 0.5f) {
         Vec3 checkPosVec = eyePos.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);
         BlockPos checkPos = new BlockPos(
                 (int) Math.floor(checkPosVec.x),
                 (int) Math.floor(checkPosVec.y),
                 (int) Math.floor(checkPosVec.z)
         );
         if (checkPos.getY() < feetY) {
            continue;
         }
         if (mc.level.getBlockState(checkPos).getBlock() instanceof AirBlock) {
            for (Direction face : Direction.values()) {
               if (face == Direction.DOWN) continue;

               BlockPos neighbor = checkPos.relative(face);
               if (!(mc.level.getBlockState(neighbor).getBlock() instanceof AirBlock)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean checkPlace(Scaffold.BlockPosWithFacing data) {
      Vec3 center = new Vec3((double) data.position.getX() + 0.5, (double) ((float) data.position.getY() + 0.5F), (double) data.position.getZ() + 0.5);
      Vec3 hit = center.add(
              new Vec3((double) data.facing.getNormal().getX() * 0.5, (double) data.facing.getNormal().getY() * 0.5, (double) data.facing.getNormal().getZ() * 0.5)
      );
      Vec3 relevant = hit.subtract(mc.player.getEyePosition());
      return relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing.getNormal().multiply(-1)).normalize()) >= 0.0;
   }

   private void placeBlock() {
      if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
         Direction sbFace = this.pos.facing();
         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
         if (sbFace != null
                 && (sbFace != Direction.UP || mc.player.onGround() || !PlayerUtils.movementInput() || isHoldingJump || this.mode.isCurrentMode("Normal"))
                 && this.shouldBuild()) {
            InteractionResult result = mc.gameMode
                    .useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false));
            if (result == InteractionResult.SUCCESS) {
               // 只有在NoSwing关闭时才摆动手臂
               if (!this.noSwing.getCurrentValue()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
               }
               this.pos = null;
            }
         }
      }
   }

   private void handleSelfRescue() {
      if (mc.player.getDeltaMovement().y < 0.1 &&
              !isBlockUnder() &&
              mc.player.fallDistance > selfRescueFallDist.getCurrentValue()) {
         if (mc.player.getDeltaMovement().y >= -1 && !this.isEnabled()) {
         } else if (mc.player.getDeltaMovement().y < -1 &&
                 selfRescueAttempted <= 3 && !fastRescueActive && !selfRescueThreadRunning.get()) {
            selfRescueAttempted += 1;
            if (this.fastSelfRescue.getCurrentValue()) {
               if (findFastRescuePosition()) {
                  fastRescueActive = true;
                  fastRescueTicks = 0;
                  fastRescuePlaceAttempts = 0;
                  if (selfRescueDebug.getCurrentValue()) {
                     ChatUtils.addChatMessage("[Scaffold] Starting fast rescue #" + selfRescueAttempted);
                  }
               } else {
                  if (selfRescueDebug.getCurrentValue()) {
                     ChatUtils.addChatMessage("[Scaffold] No valid position for fast rescue");
                  }
               }
            } else {
               int blockSlot = findBlockSlot();
               if (blockSlot == -1) {
                  if (selfRescueDebug.getCurrentValue()) {
                     ChatUtils.addChatMessage("[Scaffold] No valid blocks found for rescue!");
                  }
                  return;
               }
               mc.player.getInventory().selected = blockSlot;
               selfRescueCalculating = true;
               selfRescueCalculateThread = new CalculateThread(
                       mc.player.getX(),
                       mc.player.getY(),
                       mc.player.getZ(),
                       0,
                       0
               );
               selfRescueThreadRunning.set(true);
               selfRescueCalculateThread.start();

               if (selfRescueDebug.getCurrentValue()) {
                  ChatUtils.addChatMessage("[Scaffold] Starting simulated annealing rescue #" + selfRescueAttempted);
               }
            }
         }
      }
   }

   private boolean findFastRescuePosition() {
      BlockPos playerPos = new BlockPos(
              (int) Math.floor(mc.player.getX()),
              (int) Math.floor(mc.player.getY()) - 1,
              (int) Math.floor(mc.player.getZ())
      );

      for (int yOffset = 0; yOffset <= 5; yOffset++) {
         BlockPos checkPos = playerPos.below(yOffset);

         if (isValidPlacementPos(checkPos)) {
            for (Direction face : Direction.values()) {
               BlockPos neighbor = checkPos.relative(face);
               if (mc.level.getBlockState(neighbor).isFaceSturdy(mc.level, neighbor, face.getOpposite())) {
                  fastRescueTargetPos = checkPos;
                  fastRescueTargetFace = face;
                  return true;
               }
            }
         }
      }
      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;

            BlockPos checkPos = playerPos.offset(x, 0, z);

            if (isValidPlacementPos(checkPos)) {
               for (Direction face : Direction.values()) {
                  BlockPos neighbor = checkPos.relative(face);

                  if (mc.level.getBlockState(neighbor).isFaceSturdy(mc.level, neighbor, face.getOpposite())) {
                     fastRescueTargetPos = checkPos;
                     fastRescueTargetFace = face;
                     return true;
                  }
               }
            }
         }
      }
      for (int y = 0; y <= 1; y++) {
         BlockPos checkPos = playerPos.above(y);

         if (isValidPlacementPos(checkPos)) {
            for (Direction face : Direction.values()) {
               BlockPos neighbor = checkPos.relative(face);

               if (mc.level.getBlockState(neighbor).isFaceSturdy(mc.level, neighbor, face.getOpposite())) {
                  fastRescueTargetPos = checkPos;
                  fastRescueTargetFace = face;
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void tryFastRescuePlace() {
      if (fastRescueTargetPos == null || fastRescueTargetFace == null) {
         fastRescueActive = false;
         return;
      }
      if (!isValidPlacementPos(fastRescueTargetPos)) {
         fastRescueActive = false;
         return;
      }
      BlockPos neighbor = fastRescueTargetPos.relative(fastRescueTargetFace);
      if (!mc.level.getBlockState(neighbor).isFaceSturdy(mc.level, neighbor, fastRescueTargetFace.getOpposite())) {
         fastRescueActive = false;
         return;
      }
      int blockSlot = findBlockSlot();
      if (blockSlot == -1) {
         fastRescueActive = false;
         return;
      }
      mc.player.getInventory().selected = blockSlot;
      Vec3 hitVec = getVec3(fastRescueTargetPos, fastRescueTargetFace);
      Vector2f rotations = RotationUtils.getRotations(BlockPos.containing(hitVec), 0.0F).toVec2f();
      mc.player.setYRot(rotations.getX());
      mc.player.setXRot(rotations.getY());
      InteractionResult result = mc.gameMode.useItemOn(
              mc.player,
              InteractionHand.MAIN_HAND,
              new BlockHitResult(hitVec, fastRescueTargetFace, fastRescueTargetPos, false)
      );

      if (result == InteractionResult.SUCCESS) {
         if (!this.noSwing.getCurrentValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
         }
         fastRescuePlaceAttempts++;
         if (selfRescueDebug.getCurrentValue()) {
            ChatUtils.addChatMessage("[Scaffold] 跳过了一个喜欢你的虚空娘, Attempts: " + fastRescuePlaceAttempts);
         }
         isCyclicFreezing = false;
         freezeCycle = 0;
      } else {
         fastRescuePlaceAttempts++;
      }
   }

   private int findBlockSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (isValidStack(stack)) {
            return i;
         }
      }
      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (isValidStack(stack)) {
            return i;
         }
      }

      return -1;

   }

   private boolean isBlockUnder() {
      if (mc.player == null || mc.level == null) return false;
      int playerX = (int) Math.floor(mc.player.getX());
      int playerZ = (int) Math.floor(mc.player.getZ());
      int playerY = (int) Math.floor(mc.player.getY());
      for (int yOffset = 1; yOffset <= 5; yOffset++) {
         int checkY = playerY - yOffset;
         BlockPos checkPos = new BlockPos(playerX, checkY, playerZ);
         if (!(mc.level.getBlockState(checkPos).getBlock() instanceof AirBlock)) {
            return true;
         }
      }

      return false;
   }

   public BlockPos getAutoStuckTargetPos() {
      return autoStuckTargetPos;
   }

   public Direction getAutoStuckTargetFace() {
      return autoStuckTargetFace;
   }

   public Vec3 getAutoStuckHitVec() {
      return autoStuckHitVec;
   }

   public int getAutoStuckTicks() {
      return autoStuckTicks;
   }

   private class CalculateThread extends Thread {
      private int iteration;
      private boolean completed;
      private double temperature, energy, solutionE;
      private float solutionYaw, solutionPitch;
      public boolean stop;
      private final double predictX, predictY, predictZ;
      private final double minMotionY, maxMotionY;

      private CalculateThread(double predictX, double predictY, double predictZ,
                              double minMotionY, double maxMotionY) {
         this.predictX = predictX;
         this.predictY = predictY;
         this.predictZ = predictZ;
         this.minMotionY = minMotionY;
         this.maxMotionY = maxMotionY;
         this.iteration = 0;
         this.temperature = T;
         this.energy = 0;
         this.stop = false;
         this.completed = false;
      }


      @Override
      public void run() {
         solutionYaw = (float) getRandomInRange(-180, 180);
         solutionPitch = (float) getRandomInRange(-90, 90);
         float currentYaw = solutionYaw;
         float currentPitch = solutionPitch;

         try {
            energy = assessRotation(solutionYaw, solutionPitch);
         } catch (Exception e) {
            if (selfRescueDebug.getCurrentValue()) {
               ChatUtils.addChatMessage("[Scaffold] Please place block manually");
            }
            selfRescueThreadRunning.set(false);
            return;
         }

         solutionE = energy;
         while (temperature >= T_MIN && !stop) {
            try {
               float newYaw = (float) (currentYaw + getRandomInRange(-temperature * 18, temperature * 18));
               float newPitch = (float) (currentPitch + getRandomInRange(-temperature * 9, temperature * 9));
               newPitch = Math.max(-90, Math.min(90, newPitch));
               double assessment = assessRotation(newYaw, newPitch);
               double deltaE = assessment - energy;
               if (deltaE >= 0 || random.nextDouble() < Math.exp(-deltaE / temperature * 100)) {
                  energy = assessment;
                  currentYaw = newYaw;
                  currentPitch = newPitch;

                  if (assessment > solutionE) {
                     solutionE = assessment;
                     solutionYaw = newYaw;
                     solutionPitch = newPitch;
                     if (selfRescueDebug.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Scaffold] Find a better solution: (" + solutionYaw +
                                ", " + solutionPitch + "), value: " + solutionE);
                     }
                  }
               }

               temperature *= ALPHA;
               iteration++;
            } catch (Exception e) {
            }
         }

         if (selfRescueDebug.getCurrentValue()) {
            ChatUtils.addChatMessage("[Scaffold] Simulated annealing completed within " + iteration + " iterations");
         }

         completed = true;
         if (completed && !stop) {
            selfRescueRotation.set(new Vector2f(solutionYaw, solutionPitch));
         }
         selfRescueThreadRunning.set(false);
      }

      private double assessRotation(float yaw, float pitch) {
         double score = 0.0;
         Vec3 lookVec = RotationUtils.getVectorForRotations(pitch, yaw);
         double horizontalAngle = Math.abs(pitch);
         score += (90 - horizontalAngle) * 0.5;
         Vec3 fallDir = mc.player.getDeltaMovement().normalize();
         double dotProduct = lookVec.dot(fallDir);
         score += dotProduct * 20;

         return score;
      }

      public double getPredictX() {
         return predictX;
      }

      public double getPredictY() {
         return predictY;
      }

      public double getPredictZ() {
         return predictZ;
      }

      public double getMinMotionY() {
         return minMotionY;
      }

      public double getMaxMotionY() {
         return maxMotionY;
      }
   }

   private void placeBlockAtAngle(float yaw, float pitch) {
      HitResult hitResult = RayTraceUtils.rayCast(1.0F, new Vector2f(yaw, pitch));
      if (hitResult.getType() == Type.BLOCK) {
         BlockHitResult blockHit = (BlockHitResult) hitResult;
         if (isValidStack(mc.player.getMainHandItem())) {
            InteractionResult result = mc.gameMode.useItemOn(
                    mc.player,
                    InteractionHand.MAIN_HAND,
                    blockHit
            );
            if (result == InteractionResult.SUCCESS) {
               if (!this.noSwing.getCurrentValue()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
               }
               if (selfRescueDebug.getCurrentValue()) {
                  ChatUtils.addChatMessage("[Scaffold] Placed block at yaw: " + yaw + ", pitch: " + pitch);
               }
               isCyclicFreezing = false;
               freezeCycle = 0;
               if (selfRescueDebug.getCurrentValue()) {
                  ChatUtils.addChatMessage("[Scaffold] Placed block at yaw: " + yaw + ", pitch: " + pitch);
               }
               isCyclicFreezing = false;
               freezeCycle = 0;
            }
         }
      } else {
         if (selfRescueDebug.getCurrentValue()) {
            ChatUtils.addChatMessage("[Scaffold] No block found to place at yaw: " + yaw + ", pitch: " + pitch);
         }
      }
   }

   private double getRandomInRange(double min, double max) {
      return min + (max - min) * random.nextDouble();
   }

   private Vector2f getPlayerYawRotation() {
      return mc.player != null && this.pos != null
              ? new Vector2f(RotationUtils.getRotations(this.pos.position(), 0.0F).getYaw(), RotationUtils.getRotations(this.pos.position(), 0.0F).getPitch())
              : new Vector2f(0.0F, 0.0F);
   }

   private boolean shouldBuild() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
      return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
   }
   private void getBlockPos() {
      Vec3 baseVec = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
      if (mc.player.getDeltaMovement().y < 0.01) {
         FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
         fallingPlayer.calculate(2);
         baseVec = new Vec3(baseVec.x, Math.max(fallingPlayer.y + (double) mc.player.getEyeHeight(), baseVec.y), baseVec.z);
      }

      BlockPos base = BlockPos.containing(baseVec.x, (double) ((float) this.baseY + 0.1F), baseVec.z);
      int baseX = base.getX();
      int baseZ = base.getZ();
      if (!mc.level.getBlockState(base).entityCanStandOn(mc.level, base, mc.player)) {
         if (!this.checkBlock(baseVec, base)) {
            for (int d = 1; d <= 6; d++) {
               if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY - d, baseZ))) {
                  return;
               }

               for (int x = 1; x <= d; x++) {
                  for (int z = 0; z <= d - x; z++) {
                     int y = d - x - z;

                     for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                           if (this.checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), this.baseY - y, baseZ + (rev2 == 0 ? z : -z)))) {
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean checkBlock(Vec3 baseVec, BlockPos bp) {
      if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
         return false;
      } else {
         Vec3 center = new Vec3((double) bp.getX() + 0.5, (double) ((float) bp.getY() + 0.5F), (double) bp.getZ() + 0.5);

         for (Direction sbface : Direction.values()) {
            Vec3 hit = center.add(
                    new Vec3((double) sbface.getNormal().getX() * 0.5, (double) sbface.getNormal().getY() * 0.5, (double) sbface.getNormal().getZ() * 0.5)
            );
            Vec3i baseBlock = bp.offset(sbface.getNormal());
            BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
            if (mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
               Vec3 relevant = hit.subtract(baseVec);
               if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
                  this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static Vec3 getVec3(BlockPos pos, Direction face) {
      double x = (double) pos.getX() + 0.5;
      double y = (double) pos.getY() + 0.5;
      double z = (double) pos.getZ() + 0.5;
      if (face != Direction.UP && face != Direction.DOWN) {
         y += 0.08;
      } else {
         x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
         z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.WEST || face == Direction.EAST) {
         z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.SOUTH || face == Direction.NORTH) {
         x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      return new Vec3(x, y, z);
   }

   public static record BlockPosWithFacing(BlockPos position, Direction facing) {
   }

   private final BooleanValue blockCounter = ValueBuilder.create(this, "Block Counter")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();
   private final FloatValue counterSize = ValueBuilder.create(this, "Counter Size")
           .setVisibility(this.blockCounter::getCurrentValue)
           .setDefaultFloatValue(0.4F)
           .setFloatStep(0.01F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(1.0F)
           .build()
           .getFloatValue();
   private final FloatValue counterYOffset = ValueBuilder.create(this, "Y Offset")
           .setVisibility(this.blockCounter::getCurrentValue)
           .setDefaultFloatValue(20.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(100.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   private List<Vector4f> blurMatrices = new ArrayList<>();

   @EventTarget
   public void onShader(EventShader e) {
      if (this.blockCounter.getCurrentValue() && e.getType() == EventType.SHADOW) {
         for (Vector4f blurMatrix : this.blurMatrices) {
            RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
         }
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      if (!blockCounter.getCurrentValue() || mc.player == null || mc.level == null) return;
      int screenWidth = mc.getWindow().getGuiScaledWidth();
      int screenHeight = mc.getWindow().getGuiScaledHeight();
      float crosshairX = screenWidth / 2f;
      float crosshairY = screenHeight / 2f;
      int totalBlocks = 0;
      for (ItemStack stack : mc.player.getInventory().items) {
         if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
            totalBlocks += stack.getCount();
         }
      }
      String text = "Blocks: " + totalBlocks;
      CustomTextRenderer font = Fonts.opensans;
      float scale = this.counterSize.getCurrentValue();
      float textWidth = font.getWidth(text, (double) scale);
      float textHeight = (float) font.getHeight(true, (double) scale);
      float rectX = crosshairX - textWidth / 2 - 4;
      float rectY = crosshairY + this.counterYOffset.getCurrentValue();
      float rectWidth = textWidth + 8;
      float rectHeight = textHeight + 6;
      this.blurMatrices.clear();
      this.blurMatrices.add(new Vector4f(rectX, rectY, rectWidth, rectHeight));
      StencilUtils.write(false);
      RenderUtils.drawRoundedRect(e.getStack(), rectX, rectY, rectWidth, rectHeight, 5.0F, new Color(0, 0, 0, 120).getRGB());
      StencilUtils.erase(true);
      RenderUtils.fillBound(e.getStack(), rectX, rectY, rectWidth, rectHeight, new Color(0, 0, 0, 120).getRGB());
      RenderUtils.fillBound(e.getStack(), rectX, rectY, rectWidth * (totalBlocks / 64.0F), 3.0F, headerColor);
      StencilUtils.dispose();
      font.render(e.getStack(), text, rectX + 4, rectY + 3, Color.WHITE, true, (double) scale);
   }
   private int countTotalBlocks() {
      int count = 0;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != null && isValidStack(stack)) {
            count += stack.getCount();
         }
      }
      return count;
   }
   private int countHotbarBlocks() {
      int count = 0;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != null && isValidStack(stack)) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private void checkLowBlockWarning(int currentCount) {
      if (!lowBlockWarning.getCurrentValue() || mc.level == null) return;
      if (currentCount != lastHotbarBlockCount) {
         if (currentCount <= warningThreshold.getCurrentValue() && currentCount > 0) {
            if (!hasWarned || currentCount < lastHotbarBlockCount) {
               ChatUtils.addChatMessage("§6[Scaffold Be careful] FewBlocks! (" + currentCount + "/" + warningThreshold.getCurrentValue() + ")");
               hasWarned = true;
            }
         } else if (currentCount == 0) {
            ChatUtils.addChatMessage("§c[Scaffold Warning] NoBlocks!");
            hasWarned = true;
         } else if (currentCount > warningThreshold.getCurrentValue()) {
            hasWarned = false;
         }

         lastHotbarBlockCount = currentCount;
      }
   }
}