package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.utils.MathHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MotionEvent;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.event.TickEvent;
import org.antlr.v4.runtime.misc.OrderedHashSet;
import org.apache.commons.lang3.RandomUtils;
import org.joml.Vector3d;
import sun.misc.Unsafe;

import static java.lang.Math.sqrt;

public class RotationUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   public static Rotation targetRotation;
   public static Rotation serverRotation = new Rotation(0.0F, 0.0F);
   private static int keepLength;
   private static final Unsafe unsafe;
   public static Rotation getRotationFromEyeHasPrev(double x, double y, double z) {
      double xDiff = x - (mc.player.xOld + (mc.player.getX() - mc.player.xOld));
      double yDiff = y - (mc.player.yOld + (mc.player.getY() - mc.player.yOld) + (mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY));
      double zDiff = z - (mc.player.zOld + (mc.player.getZ() - mc.player.zOld));
      double dist = (double)Math.sqrt(xDiff * xDiff + zDiff * zDiff);
      return new Rotation((float)(Math.atan2(zDiff, xDiff) * 180.0D / Math.PI) - 90.0F, (float)(-(Math.atan2(yDiff, dist) * 180.0D / Math.PI)));
   }

   public static Rotation getRotationFromEyeHasPrev(LivingEntity target) {
      double x = target.xOld + (target.getX() - target.xOld);
      double y = target.yOld + (target.getY() - target.yOld);
      double z = target.zOld + (target.getZ() - target.zOld);
      return getRotationFromEyeHasPrev(x, y, z);
   }

   public static Rotation getRotationFromEyeHasPrev(Vec3 vec) {
      return getRotationFromEyeHasPrev(vec.x, vec.y, vec.z);
   }

   public static float[] prevRotations = new float[2];

   public static double getRotationDifferences(Rotation a, Rotation b) {
      return Math.hypot((double)getAngleDifference(a.getYaw(), b.getYaw()), (double)(a.getPitch() - b.getPitch()));
   }

   public static float getRotationDifferences(float current, float target) {
      return Mth.wrapDegrees(target - current);
   }


   public static double getRotationDifferences(Rotation rotation) {
      return prevRotations == null ? 0.0D : getRotationDifferences(rotation, new Rotation(prevRotations[0], prevRotations[1]));
   }
   public static Rotation getNewRotations(Vec3 vec) {
      Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY + (double) mc.player.getEyeHeight(), mc.player.getZ());
      double d0 = vec.x - vec3.x;
      double d1 = vec.y + (double) mc.player.getBbHeight() / 2.0 - vec3.y;
      double d2 = vec.z - vec3.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      float f = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
      float f1 = (float) (-Math.atan2(d1, d3) * 180.0 / Math.PI);
      return new Rotation(f, f1);
   }
   public static Vector2f getRotationFromEyeToPoints(Vector3d point3d) {
      return calculates(new Vector3d(mc.player.getX(), mc.player.getBoundingBox().minY + mc.player.getEyeHeight(), mc.player.getZ()), point3d);
   }

   public static Vector2f calculates(Vector3d from, Vector3d to) {
      final double x = to.x - from.x;
      final double y = to.y - from.y;
      final double z = to.z - from.z;

      final double sqrt = Math.sqrt(x * x + z * z);

      final float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
      final float pitch = (float) (-Math.toDegrees(Math.atan2(y, sqrt)));

      return new Vector2f(yaw, Math.min(Math.max(pitch, -90), 90));
   }
   public static Vec3 getCenters(AABB bb) {
      return new Vec3(
              bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5
      );
   }
   public static Rotation getNCPRotationss(Vec3 vec, boolean predict) {
      Vec3 vec3 = new Vec3(
              mc.player.getX(), mc.player.getBoundingBox().minY + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()
      );
      if (predict) {
         vec3.add(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
      }

      double d0 = vec.x - vec3.x;
      double d1 = vec.y + (double) mc.player.getBbHeight() / 2.0 - vec3.y;
      double d2 = vec.z - vec3.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      return new Rotation((float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F, (float) (-Math.atan2(d1, d3) * 180.0 / Math.PI));
   }
   public static Rotation toRotations(Vec3 vec, boolean predict) {
      Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY + (double) mc.player.getEyeHeight(), mc.player.getZ());
      if (predict) {
         vec3.add(mc.player.getDeltaMovement());
      }

      double d0 = vec.x - vec3.x;
      double d1 = vec.y + (double) mc.player.getBbHeight() / 2.0 - vec3.y;
      double d2 = vec.z - vec3.z;
      return new Rotation(
              Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(d2, d0)) - 90.0F), Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2)))))
      );
   }
   public static float getDistanceToEntity(Entity target) {
      if (mc.player == null) return 0.0F;

      Vec3 eyes = mc.player.getEyePosition(1F);
      Vec3 pos = getNearestPointBB(eyes, target.getBoundingBox());
      double xDist = Math.abs(pos.x - eyes.x);
      double yDist = Math.abs(pos.y - eyes.y);
      double zDist = Math.abs(pos.z - eyes.z);
      return (float) Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
   }
   public static Vec3 getVectorForRotations(final Rotation rotation) {
      float yawCos = (float) Math.cos(-rotation.getYaw() * 0.017453292F - 3.1415927F);
      float yawSin = (float) Math.sin(-rotation.getYaw() * 0.017453292F - 3.1415927F);
      float pitchCos = (float) -Math.cos(-rotation.getPitch() * 0.017453292F);
      float pitchSin = (float) Math.sin(-rotation.getPitch() * 0.017453292F);
      return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
   }
   public static double getYaw(Entity entity) {
      if (mc.player == null) return 0.0D;

      return mc.player.getYRot() + Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90f - mc.player.getYRot());
   }
   public static float[] getAnglesss(Entity entity) {
      if (entity == null)
         return null;
      final LocalPlayer thePlayer = mc.player;

      final double diffX = entity.getX() - thePlayer.getX(),
              diffY = entity.getY() + entity.getEyeHeight() * 0.9 - (thePlayer.getY() + thePlayer.getEyeHeight()),
              diffZ = entity.getZ() - thePlayer.getZ(),
              dist = Math.sqrt(diffX * diffX + diffZ * diffZ); // @on

      final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F,
              pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);
      return new float[]{
              thePlayer.getYRot() + Mth.wrapDegrees(yaw - thePlayer.getYRot()),
              thePlayer.getXRot() + Mth.wrapDegrees(pitch - thePlayer.getXRot())
      };
   }
   public static Vector2f getNewRotation(Entity target) {
      double yDist = target.getY() - mc.player.getY();
      Vec3 pos = yDist >= 1.7 ? new Vec3(target.getX(), target.getY(), target.getZ()) :
              (yDist <= -1.7 ? new Vec3(target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ()) :
                      new Vec3(target.getX(), target.getY() + (double)(target.getEyeHeight() / 2.0f), target.getZ()));

      Vec3 vec = new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY + (double)mc.player.getEyeHeight(), mc.player.getZ());
      double xDist = pos.x - vec.x;
      double yDist2 = pos.y - vec.y;
      double zDist = pos.z - vec.z;
      float yaw = (float)Math.toDegrees(Math.atan2(zDist, xDist)) - 90.0f;
      float pitch = (float)(-Math.toDegrees(Math.atan2(yDist2, Math.sqrt(xDist * xDist + zDist * zDist))));

      return new Vector2f(yaw, Math.min(Math.max(pitch, -90.0f), 90.0f));
   }
   public static float[] getHVHRotations(Entity entity, double maxRange) {
      if (entity == null) {
         return null;
      } else {
         double diffX = entity.getX() - mc.player.getX();
         double diffZ = entity.getZ() - mc.player.getZ();
         Vec3 BestPos = getNearestPointBB(mc.player.getEyePosition(), entity.getBoundingBox());
         Vec3 myEyePos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ());

         double diffY;

         diffY = BestPos.y - myEyePos.y;
         double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
         float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
         float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / Math.PI));
         return new float[]{yaw, pitch};
      }
   }
   public static float[] getSimpleRotations(Entity aimingTarget) {
      if (mc.player == null) return new float[]{};

      Vector3d targetPos;
      final double yDist = aimingTarget.getY() - mc.player.getY();
      if (yDist >= 1.547) {
         targetPos = new Vector3d(aimingTarget.getX(), aimingTarget.getY(), aimingTarget.getZ());
      } else if (yDist <= -1.547) {
         targetPos = new Vector3d(aimingTarget.getX(), aimingTarget.getY() + aimingTarget.getEyeHeight(), aimingTarget.getZ());
      } else {
         targetPos = new Vector3d(aimingTarget.getX(), aimingTarget.getY() + aimingTarget.getEyeHeight() / 2, aimingTarget.getZ());
      }
      return getRotationFromEyeToPoint(targetPos);
   }
   public static float[] getRotationFromEyeToPoint(Vector3d point3d) {
      if (mc.player == null) return new float[]{};

      return getRotation(new Vector3d(mc.player.getX(), mc.player.getBoundingBox().minY + mc.player.getEyeHeight(), mc.player.getZ()), point3d);
   }
   public static float[] getRotation(Vector3d from, Vector3d to) {
      final double x = to.x - from.x;
      final double y = to.y - from.y;
      final double z = to.z - from.z;

      final double sqrt = Math.sqrt(x * x + z * z);

      final float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
      final float pitch = (float) (-Math.toDegrees(Math.atan2(y, sqrt)));

      return new float[]{yaw, Math.min(Math.max(pitch, -90), 90)};
   }

   public static float[] getHVHRotation(Entity entity) {
      if (entity == null || mc.player == null) return null;


      final Player player = mc.player;
      final double playerX = player.getX();
      final double playerY = player.getY() + player.getEyeHeight();
      final double playerZ = player.getZ();


      final Vec3 eyePosition = new Vec3(playerX, playerY, playerZ);
      final Vec3 bestPos = getClosestPoint(eyePosition, entity.getBoundingBox());
      if (bestPos == null) return null;


      final double diffX = bestPos.x - playerX;
      final double diffZ = bestPos.z - playerZ;
      final double diffY = bestPos.y - eyePosition.y;


      final double horizontalDistance = Math.hypot(diffX, diffZ);

      final float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      final float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontalDistance));

      return new float[]{
              Mth.wrapDegrees(yaw),
              Mth.wrapDegrees(pitch)
      };
   }
   public static Rotation getRotationForEntity(Entity entity) {
      if (mc.player == null) return null;

      Vec3 playerEyePos = mc.player.getEyePosition(1.0F);
      Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
      Vec3 direction = targetPos.subtract(playerEyePos).normalize();
      float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
      float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
      return new Rotation(yaw, pitch);
   }

   public static Vec3 getNearestPointBB(Vec3 eye, AABB box) {
      double[] origin = {eye.x, eye.y, eye.z};
      double[] destMinis = {box.minX, box.minY, box.minZ};
      double[] destMaxis = {box.maxX, box.maxY, box.maxZ};

      for (int i = 0; i < 3; i++) {
         if (origin[i] > destMaxis[i]) {
            origin[i] = destMaxis[i];
         } else if (origin[i] < destMinis[i]) {
            origin[i] = destMinis[i];
         }
      }

      return new Vec3(origin[0], origin[1], origin[2]);
   }

   public static Rotation calculate(final Vector3d position, final Direction direction) {
      double x = position.x + 0.5D;
      double y = position.y + 0.5D;
      double z = position.z + 0.5D;

      x += direction.getStepX() * 0.5D;
      y += direction.getStepY() * 0.5D;
      z += direction.getStepZ() * 0.5D;

      return calculate(new Vector3d(x, y, z));
   }


   public static Rotation calculate(Vector3d target) {
      if (mc.player == null) return null;

      Vec3 eyePosition = mc.player.getEyePosition(1.0F);
      double deltaX = target.x - eyePosition.x;
      double deltaY = target.y - eyePosition.y;
      double deltaZ = target.z - eyePosition.z;

      double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F);
      float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, distanceXZ));

      return new Rotation(yaw, pitch);
   }

   public static Rotation getAngless(Entity entity) {
      if (entity == null) return null;
      final LocalPlayer thePlayer = mc.player;

      if (thePlayer == null) return null;
      final double diffX = entity.getX() - thePlayer.getX(),
              diffY = entity.getY() + entity.getEyeHeight() * 0.9 - (thePlayer.getY() + thePlayer.getEyeHeight()),
              diffZ = entity.getZ() - thePlayer.getZ();
      final double dist = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
      final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
      final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);
      return new Rotation(
              thePlayer.getYRot() + Mth.wrapDegrees(yaw - thePlayer.getYRot()),
              thePlayer.getXRot() + Mth.wrapDegrees(pitch - thePlayer.getXRot())
      );
   }

   public static Rotation getAngles(Entity entity) {
      if (entity == null) return null;

      final LocalPlayer thePlayer = mc.player;

      if (thePlayer == null) return null;

      final double diffX = entity.getX() - thePlayer.getX(),
              diffY = entity.getY() + entity.getEyeHeight() * 0.9 - (thePlayer.getY() + thePlayer.getEyeHeight()),
              diffZ = entity.getZ() - thePlayer.getZ();

      final double dist = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));

      final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
      final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

      return new Rotation(
              thePlayer.getYRot() + Mth.wrapDegrees(yaw - thePlayer.getYRot()),
              thePlayer.getXRot() + Mth.wrapDegrees(pitch - thePlayer.getXRot())
      );
   }

   public static boolean isInViewRange(float fov, LivingEntity entity) {
      if (mc.player == null) return false;

      Vec3 playerPos = mc.player.getEyePosition(1.0F);
      Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
      Vec3 lookVec = mc.player.getLookAngle();
      Vec3 toTargetVec = targetPos.subtract(playerPos).normalize();

      double dotProduct = lookVec.dot(toTargetVec);
      double angle = Math.toDegrees(Math.acos(dotProduct));

      return fov == 360F || angle <= fov;
   }


   static {
      try {
         Field field = Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         unsafe = (Unsafe)field.get(null);
      } catch (Exception var1) {
         throw new RuntimeException(var1);
      }
   }

   public static float getAngleDifference(float a, float b) {
      return ((a - b) % 360.0F + 540.0F) % 360.0F - 180.0F;
   }

   public static Vec3 getLook() {
      return getLook(mc.player.getYRot(), mc.player.getXRot());
   }

   public static Vector2f getFixedRotation(float yaw, float pitch, float lastYaw, float lastPitch) {
      float f = (float)((Double)mc.options.sensitivity().get() * 0.6F + 0.2F);
      float gcd = f * f * f * 1.2F;
      float deltaYaw = yaw - lastYaw;
      float deltaPitch = pitch - lastPitch;
      float fixedDeltaYaw = deltaYaw - deltaYaw % gcd;
      float fixedDeltaPitch = deltaPitch - deltaPitch % gcd;
      float fixedYaw = lastYaw + fixedDeltaYaw;
      float fixedPitch = lastPitch + fixedDeltaPitch;
      return new Vector2f(fixedYaw, fixedPitch);
   }

   public static Rotation getNCPRotations(Vec3 vec, boolean predict) {
      Vec3 vec3 = new Vec3(
              mc.player.getX(), mc.player.getBoundingBox().minY + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()
      );
      if (predict) {
         vec3.add(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
      }



      double d0 = vec.x - vec3.x;
      double d1 = vec.y + (double) mc.player.getBbHeight() / 2.0 - vec3.y;
      double d2 = vec.z - vec3.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      return new Rotation((float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F, (float) (-Math.atan2(d1, d3) * 180.0 / Math.PI));
   }

   public static Vec3 getLook(float yaw, float pitch) {
      float f = Mth.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = Mth.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -Mth.cos(-pitch * (float) (Math.PI / 180.0));
      float f3 = Mth.sin(-pitch * (float) (Math.PI / 180.0));
      return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
   }

   public static boolean isVecInside(AABB self, Vec3 vec) {
      return vec.x > self.minX && vec.x < self.maxX && vec.y > self.minY && vec.y < self.maxY && vec.z > self.minZ && vec.z < self.maxZ;
   }

   public static Rotation getRotations(Vec3 eye, Vec3 target) {
      double x = target.x - eye.x;
      double y = target.y - eye.y;
      double z = target.z - eye.z;
      double diffXZ = Math.sqrt(x * x + z * z);
      float yaw = (float)Math.toDegrees(Math.atan2(z, x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(y, diffXZ)));
      return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
   }

   public static Rotation getNCPRotations2(final Vec3 vec, final boolean predict) {
      final Vec3 eyesPos = new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

      if (predict) {
         eyesPos.add(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
      }

      final double diffX = vec.x - eyesPos.x;
      final double diffY = vec.y + (mc.player.getBbHeight() / 2.0) - eyesPos.y;
      final double diffZ = vec.z - eyesPos.z;
      double hypotenuse = sqrt(diffX * diffX + diffZ * diffZ);

      return new Rotation(
              (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f,
              (float) (-Math.atan2(diffY, hypotenuse) * 180.0 / Math.PI)
      );
   }

   public static Rotation getRotations(BlockPos pos, float partialTicks) {
      Vec3 playerVector = new Vec3(
              mc.player.getX() + mc.player.getDeltaMovement().x * (double)partialTicks,
              mc.player.getY() + (double)mc.player.getEyeHeight() + mc.player.getDeltaMovement().y * (double)partialTicks,
              mc.player.getZ() + mc.player.getDeltaMovement().z * (double)partialTicks
      );
      double x = (double)pos.getX() - playerVector.x + 0.5;
      double y = (double)pos.getY() - playerVector.y + 0.5;
      double z = (double)pos.getZ() - playerVector.z + 0.5;
      return diffCalc(randomization(x), randomization(y), randomization(z));
   }

   public static Rotation diffCalc(double diffX, double diffY, double diffZ) {
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
   }

   private static double randomization(double value) {
      return value + MathUtils.getRandomDoubleInRange(0.05, 0.08) * (MathUtils.getRandomDoubleInRange(0.0, 1.0) * 2.0 - 1.0);
   }

   public static double getMinDistance(Entity target, Vector2f rotations) {
      double minDistance = Double.MAX_VALUE;
      Iterator var4 = getPossibleEyeHeights().iterator();

      while (var4.hasNext()) {
         double eye = (double)((Float)var4.next()).floatValue();
         Vec3 playerPosition = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
         Vec3 eyePos = playerPosition.add(0.0, eye, 0.0);
         minDistance = Math.min(minDistance, getDistance(target, eyePos, rotations));
      }

      return minDistance;
   }

   public static double getDistance(Entity target, Vec3 eyePos, Vector2f rotations) {
      AABB targetBox = getTargetBoundingBox(target);
      HitResult position = getIntercept(targetBox, rotations, eyePos, 6.0);
      if (position != null) {
         Vec3 intercept = position.getLocation();
         return intercept.distanceTo(eyePos);
      } else {
         return 1000.0;
      }
   }

   public static HitResult getIntercept(AABB targetBox, Vector2f rotations, Vec3 eyePos, double reach) {
      Vec3 vec31 = getLook(rotations.x, rotations.y);
      Vec3 vec32 = eyePos.add(vec31.x * reach, vec31.y * reach, vec31.z * reach);
      return ProjectileUtil.getEntityHitResult(
              mc.player, eyePos, vec32, targetBox, p_172770_ -> !p_172770_.isSpectator() && p_172770_.isPickable(), reach * reach
      );
   }

   public static HitResult getIntercept(AABB targetBox, Vector2f rotations, Vec3 eyePos) {
      return getIntercept(targetBox, rotations, eyePos, 6.0);
   }

   public static Vector2f diffCalcVector(double diffX, double diffY, double diffZ) {
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new Vector2f(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch));
   }

   public static Vector2f getRotationsVector(Vec3 vec) {
      Vec3 playerVector = new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(), mc.player.getZ());
      double x = vec.x - playerVector.x;
      double y = vec.y - playerVector.y;
      double z = vec.z - playerVector.z;
      return diffCalcVector(x, y, z);
   }

   private static boolean checkHitResult(Vec3 eyePos, HitResult result, Entity target) {
      if (result.getType() == Type.ENTITY && ((EntityHitResult)result).getEntity() == target) {
         Vec3 intercept = result.getLocation();
         return isVecInside(getTargetBoundingBox(target), eyePos) || intercept.distanceTo(eyePos) <= 3.0;
      } else {
         return false;
      }
   }

   private static HitResult rayTrace(Rotation rotations) {
      double d0 = (double)mc.gameMode.getPickRange();
      HitResult hitResult = RayTraceUtil.rayCast(d0, 1.0F, false, rotations);
      Vec3 vec3 = mc.player.getEyePosition(1.0F);
      boolean flag = false;
      double d1 = d0;
      if (mc.gameMode.hasFarPickRange()) {
         d1 = 6.0;
         d0 = d1;
      } else if (d0 > 3.0) {
         flag = true;
      }

      d1 *= d1;
      if (hitResult != null) {
         d1 = hitResult.getLocation().distanceToSqr(vec3);
      }

      Vec3 vec31 = getLook(rotations.getYaw(), rotations.getPitch());
      Vec3 vec32 = vec3.add(vec31.x * d0, vec31.y * d0, vec31.z * d0);
      AABB aabb = mc.player.getBoundingBox().expandTowards(vec31.scale(d0)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(
              mc.player, vec3, vec32, aabb, p_172770_ -> !p_172770_.isSpectator() && p_172770_.isPickable(), d1
      );
      if (entityhitresult != null) {
         Vec3 vec33 = entityhitresult.getLocation();
         double d2 = vec3.distanceToSqr(vec33);
         if (flag && d2 > 9.0) {
            hitResult = BlockHitResult.miss(vec33, Direction.getNearest(vec31.x, vec31.y, vec31.z), BlockPos.containing(vec33));
         } else if (d2 < d1 || hitResult == null) {
            hitResult = entityhitresult;
         }
      }

      return hitResult;
   }

   public static Vec3 getVectorForRotation(Rotation rotation) {
      float yawCos = (float)Math.cos((double)(-rotation.getYaw() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-rotation.getYaw() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-rotation.getPitch() * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-rotation.getPitch() * (float) (Math.PI / 180.0)));
      return new Vec3((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static Vec3 getVectorForRotations(float yaw, float pitch) {
      float yawCos = (float)Math.cos((double)(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-pitch * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-pitch * (float) (Math.PI / 180.0)));
      return new Vec3((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static RotationUtils.Data getRotationDataToEntity(Entity target) {
      Vec3 playerPosition = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      Vec3 eyePos = playerPosition.add(0.0, (double)mc.player.getEyeHeight(), 0.0);
      AABB targetBox = getTargetBoundingBox(target);
      double minX = targetBox.minX;
      double minY = targetBox.minY;
      double minZ = targetBox.minZ;
      double maxX = targetBox.maxX;
      double maxY = targetBox.maxY;
      double maxZ = targetBox.maxZ;
      double spacing = 0.1;
      Set<Vec3> points = new OrderedHashSet();
      points.add(new Vec3(minX + maxX / 2.0, minY + maxY / 2.0, minZ + maxZ / 2.0));
      points.add(getClosestPoint(eyePos, targetBox));

      for (double x = minX; x <= maxX; x += spacing) {
         for (double y = minY; y <= maxY; y += spacing) {
            points.add(new Vec3(x, y, minZ));
            points.add(new Vec3(x, y, maxZ));
         }
      }

      for (double x = minX; x <= maxX; x += spacing) {
         for (double z = minZ; z <= maxZ; z += spacing) {
            points.add(new Vec3(x, minY, z));
            points.add(new Vec3(x, maxY, z));
         }
      }

      for (double y = minY; y <= maxY; y += spacing) {
         for (double z = minZ; z <= maxZ; z += spacing) {
            points.add(new Vec3(minX, y, z));
            points.add(new Vec3(maxX, y, z));
         }
      }

      for (Vec3 point : points) {
         Rotation bruteRotations = getRotations(eyePos, point);
         HitResult bruteHitResult = rayTrace(bruteRotations);
         if (checkHitResult(eyePos, bruteHitResult, target)) {
            Vec3 location = bruteHitResult.getLocation();
            return new RotationUtils.Data(
                    eyePos,
                    location,
                    location.distanceTo(eyePos),
                    getFixedRotation(bruteRotations.getYaw(), bruteRotations.getPitch(), RotationManager.lastRotations.x, RotationManager.lastRotations.y)
            );
         }
      }

      return new RotationUtils.Data(eyePos, eyePos, 1000.0, null);
   }

   private static AABB getTargetBoundingBox(Entity entity) {
      return entity.getBoundingBox();
   }

   public static List<Float> getPossibleEyeHeights() {
      return List.of(mc.player.getEyeHeight());
   }

   public static Vec3 getClosestPoint(Vec3 vec, AABB aabb) {
      double closestX = Math.max(aabb.minX, Math.min(vec.x, aabb.maxX));
      double closestY = Math.max(aabb.minY, Math.min(vec.y, aabb.maxY));
      double closestZ = Math.max(aabb.minZ, Math.min(vec.z, aabb.maxZ));
      return new Vec3(closestX, closestY, closestZ);
   }

   public static Vector2f getRotations(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         double diffX = entity.getX() - mc.player.getX();
         double diffZ = entity.getZ() - mc.player.getZ();
         double diffY = entity.getY() + (double)entity.getEyeHeight() - (mc.player.getY() + (double)mc.player.getEyeHeight());
         return diffCalcVector(diffX, diffY, diffZ);
      }
   }

   public static float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
      return updateRotation(currentYaw, calcYaw, yawSpeed);
   }

   public static float updateRotation(float current, float calc, float maxDelta) {
      float f = MathHelper.wrapDegrees(calc - current);
      if (f > maxDelta) {
         f = maxDelta;
      }

      if (f < -maxDelta) {
         f = -maxDelta;
      }

      return current + f;
   }

   public static boolean inFoV(Entity entity, float fov) {
      Vector2f rotations = getRotations(entity);
      float diff = Math.abs(mc.player.getYRot() % 360.0F - rotations.x);
      float minDiff = Math.abs(Math.min(diff, 360.0F - diff));
      return minDiff <= fov;
   }

   public static float getDistanceBetweenAngles(float angle1, float angle2) {
      float angle3 = Math.abs(angle1 - angle2) % 360.0F;
      if (angle3 > 180.0F) {
         angle3 = 0.0F;
      }

      return angle3;
   }

   public static Vec3 getEyesPos() {
      return new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
   }

   public static float rotateToPitch(float speed, float currentPitch, float targetPitch) {
      float delta = Mth.wrapDegrees(targetPitch - currentPitch);
      if (delta > speed) delta = speed;
      if (delta < -speed) delta = -speed;
      return currentPitch + delta;
   }

   public static float rotateToPitch(float pitchSpeed, float[] currentRots, float calcPitch) {
      float pitch = rotateToPitch(
              pitchSpeed + RandomUtils.nextFloat(0.0F, 15.0F),
              currentRots[1],
              calcPitch
      );

      if (pitch != calcPitch) {
         pitch += (float) (RandomUtils.nextFloat(1.0F, 2.0F)
                 * Math.sin(currentRots[0] * Math.PI));
      }
      return pitch;
   }
   public static Rotation toRotation(Vec3 vec, boolean predict) {
      Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY + (double) mc.player.getEyeHeight(), mc.player.getZ());
      if (predict) {
         vec3.add(mc.player.getDeltaMovement());
      }

      double d0 = vec.x - vec3.x;
      double d1 = vec.y + (double) mc.player.getBbHeight() / 2.0 - vec3.y;
      double d2 = vec.z - vec3.z;
      return new Rotation(
              Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(d2, d0)) - 90.0F), Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2)))))
      );
   }

   public static double getRotationDifference(Entity entity) {
      Rotation rotation = toRotation(getCenter(entity.getBoundingBox()), true);
      return getRotationDifference(rotation, new Rotation(mc.player.getYRot(), mc.player.getXRot()));
   }

   public static double getRotationDifference(Rotation a, Rotation b) {
      return Math.hypot((double) getAngleDifference(a.getYaw(), b.getYaw()), (double) (a.getPitch() - b.getPitch()));
   }

   public static double getRotationDifference(Rotation rotation) {
      Rotation rotationx = new Rotation(mc.player.getYRot(), mc.player.getXRot());
      return getRotationDifference(rotation, rotationx);
   }

   public static Vec3 getCenter(AABB bb) {
      return new Vec3(bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5);
   }

   public static void setTargetRotation(Rotation rotation, int keepLength) {
      if (!Double.isNaN((double) rotation.getYaw())
              && !Double.isNaN((double) rotation.getPitch())
              && !(rotation.getPitch() > 90.0F)
              && !(rotation.getPitch() < -90.0F)) {
         targetRotation = rotation;
         RotationUtils.keepLength = keepLength;
      }
   }

   public static void reset() {
      keepLength = 0;
      targetRotation = null;
   }

   @EventTarget
   private void onTick(TickEvent event) {
      if (targetRotation != null) {
         keepLength--;
         if (keepLength <= 0) {
            reset();
         }
      }

      if (targetRotation != null) {
         Rotation rotation = new Rotation(0.0F, 0.0F);
         rotation.setYaw(targetRotation.getYaw());
         rotation.setPitch(targetRotation.getPitch());
      }
   }

   @EventTarget
   private void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
         ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.getPacket();
         try {
            // 使用反射访问受保护的字段
            Field yRotField = findField(packet.getClass(), "yRot", "f_134121_");
            Field xRotField = findField(packet.getClass(), "xRot", "f_134122_");
            Field hasRotField = findField(packet.getClass(), "hasRot", "f_134123_");

            if (targetRotation != null && (targetRotation.getYaw() != serverRotation.getYaw() || targetRotation.getPitch() != serverRotation.getPitch())) {
               yRotField.setFloat(packet, targetRotation.getYaw());
               xRotField.setFloat(packet, targetRotation.getPitch());
               hasRotField.setBoolean(packet, true);
            }

            if (hasRotField.getBoolean(packet)) {
               serverRotation = new Rotation(yRotField.getFloat(packet), xRotField.getFloat(packet));
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   @EventTarget
   private void onMotion(MotionEvent event) {
      if (targetRotation != null) {
         event.setYaw(targetRotation.getYaw());
         event.setPitch(targetRotation.getPitch());
      }
   }
   private static Field findField(Class<?> clazz, String... fieldNames) {
      if (clazz != null && fieldNames != null && fieldNames.length != 0) {
         Exception failed = null;

         for (Class<?> currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass()) {
            for (String fieldName : fieldNames) {
               if (fieldName != null) {
                  try {
                     Field f = currentClass.getDeclaredField(fieldName);
                     f.setAccessible(true);
                     if ((f.getModifiers() & 16) != 0) {
                        unsafe.putInt(f, (long)unsafe.arrayBaseOffset(boolean[].class), f.getModifiers() & -17);
                     }
                     return f;
                  } catch (Exception var9) {
                     failed = var9;
                  }
               }
            }
         }

         throw new UnableToFindFieldException(failed);
      } else {
         throw new IllegalArgumentException("Class and fieldNames must not be null or empty");
      }
   }
   private static class UnableToFindFieldException extends RuntimeException {
      private static final long serialVersionUID = 1L;

      public UnableToFindFieldException(Exception e) {
         super(e);
      }
   }

   public static class Data {
      private final Vec3 eye;
      private final Vec3 hitVec;
      private final double distance;
      private final Vector2f rotation;

      public Data(Vec3 eye, Vec3 hitVec, double distance, Vector2f rotation) {
         this.eye = eye;
         this.hitVec = hitVec;
         this.distance = distance;
         this.rotation = rotation;
      }

      public Vec3 getEye() {
         return this.eye;
      }

      public Vec3 getHitVec() {
         return this.hitVec;
      }

      public double getDistance() {
         return this.distance;
      }

      public Vector2f getRotation() {
         return this.rotation;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof RotationUtils.Data other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getDistance(), other.getDistance()) != 0) {
            return false;
         } else {
            Object this$eye = this.getEye();
            Object other$eye = other.getEye();
            if (this$eye == null ? other$eye == null : this$eye.equals(other$eye)) {
               Object this$hitVec = this.getHitVec();
               Object other$hitVec = other.getHitVec();
               if (this$hitVec == null ? other$hitVec == null : this$hitVec.equals(other$hitVec)) {
                  Object this$rotation = this.getRotation();
                  Object other$rotation = other.getRotation();
                  return this$rotation == null ? other$rotation == null : this$rotation.equals(other$rotation);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof RotationUtils.Data;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $distance = Double.doubleToLongBits(this.getDistance());
         result = result * 59 + (int)($distance >>> 32 ^ $distance);
         Object $eye = this.getEye();
         result = result * 59 + ($eye == null ? 43 : $eye.hashCode());
         Object $hitVec = this.getHitVec();
         result = result * 59 + ($hitVec == null ? 43 : $hitVec.hashCode());
         Object $rotation = this.getRotation();
         return result * 59 + ($rotation == null ? 43 : $rotation.hashCode());
      }

      @Override
      public String toString() {
         return "RotationUtils.Data(eye="
                 + this.getEye()
                 + ", hitVec="
                 + this.getHitVec()
                 + ", distance="
                 + this.getDistance()
                 + ", rotation="
                 + this.getRotation()
                 + ")";
      }
   }
}