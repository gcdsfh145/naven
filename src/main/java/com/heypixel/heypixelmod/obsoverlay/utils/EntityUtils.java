package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.AntiBots;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;

public class EntityUtils {
    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    public static boolean isAnimal(Entity entity) {
        return entity instanceof Animal
                || entity instanceof Villager
                || entity instanceof AbstractGolem
                || entity instanceof Bat
                || entity instanceof Fox
                || entity instanceof Dolphin
                || entity instanceof Panda
                || entity instanceof Parrot
                || entity instanceof Rabbit
                || entity instanceof Axolotl
                || entity instanceof Goat
                || entity instanceof Ocelot
                || entity instanceof Chicken
                || entity instanceof Cod
                || entity instanceof Cow
                || entity instanceof Donkey
                || entity instanceof Horse
                || entity instanceof Mule
                || entity instanceof Pig
                || entity instanceof Pufferfish
                || entity instanceof Salmon
                || entity instanceof Sheep
                || entity instanceof Squid
                || entity instanceof Strider
                || entity instanceof TropicalFish
                || entity instanceof Turtle;
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof Monster
                || entity instanceof Slime
                || entity instanceof Ghast
                || entity instanceof Shulker
                || entity instanceof Blaze
                || entity instanceof Creeper
                || entity instanceof Drowned
                || entity instanceof ElderGuardian
                || entity instanceof EnderDragon
                || entity instanceof Endermite
                || entity instanceof Evoker
                || entity instanceof Guardian
                || entity instanceof Hoglin
                || entity instanceof Husk
                || entity instanceof Illusioner
                || entity instanceof IronGolem
                || entity instanceof MagmaCube
                || entity instanceof Phantom
                || entity instanceof Pillager
                || entity instanceof Ravager
                || entity instanceof Silverfish
                || entity instanceof Skeleton
                || entity instanceof Spider
                || entity instanceof Stray
                || entity instanceof Vex
                || entity instanceof Vindicator
                || entity instanceof Witch
                || entity instanceof WitherSkeleton
                || entity instanceof Zoglin
                || entity instanceof Zombie
                || entity instanceof ZombieVillager;
    }

    public static boolean isInvisible(Entity entity) {
        return !(entity instanceof Player) ? false : entity.isInvisible();
    }

    public static boolean isTargetTypeValid(Entity entity, boolean antibot, boolean teams, boolean friend) {
        Target target = (Target) Naven.moduleManager.getModule(Target.class);
        return (!AntiBots.botName.contains(entity.getName().getString()) || !antibot) && (!Teams.isSameTeam(entity) || !teams) && (target.targetPlayer.getCurrentValue() && isPlayer(entity)
                || target.targetMobs.getCurrentValue() && isMob(entity)
                || target.targetAnimals.getCurrentValue() && isAnimal(entity)
                || target.targetInvisible.getCurrentValue() && isInvisible(entity)
                || target.targetDead.getCurrentValue() && isCorpse(entity));
    }

    public static boolean isCorpse(Entity entity) {
        return entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying();
    }

    public static boolean isBeingAttacked(Entity entity) {
        return entity instanceof LivingEntity livingEntity && livingEntity.hurtTime > 0;
    }
    public static boolean isValidTarget(Player livingEntity, Minecraft mc) {
        Teams teams = (Teams) Naven.moduleManager.getModule(Teams.class);

        return (livingEntity.getId() != mc.player.getId() && !livingEntity.getName().getString().isEmpty() && !livingEntity.isDeadOrDying() && !FriendManager.isFriend(livingEntity.getName().getString()) && (!teams.isEnabled() || !teams.isSameTeam(livingEntity)) && !(livingEntity.getName().getString().contains("@cet_npc_") || livingEntity.getName().getString().contains("CIT-") || livingEntity.getName().getString().contains("@cet_")));
    }
}

