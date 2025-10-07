package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttack;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.IntValue;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

@ModuleInfo(name = "AttackEffects", description = "AttackEffects", category = Category.RENDER)
public class AttackEffects extends Module {
    public final ModeValue particle = ValueBuilder.create(this, "Block Mods")
            .setModes( "None",
                    "Blood",
                    "Fire",
                    "Heart",
                    "Water",
                    "Smoke",
                    "Magic",
                    "Crits",
                    "Ash",
                    "Soul",
                    "Campfire",
                    "Flame",
                    "Witch",
                    "DragonBreath",
                    "EndRod",
                    "Snowball",
                    "Slime",
                    "Explosion")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    public final ModeValue sound = ValueBuilder.create(this, "Sound Type")
            .setModes("None", "Hit", "Orb", "Totem", "Explosion", "Anvil", "Firework", "Blaze", "Sword", "Critical", "Shield", "Crossbow", "Trident", "Smash", "Thunder")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    IntValue amount = ValueBuilder.create(this, "ParticleAmount")
            .setDefaultIntValue(1)
            .setMinIntValue(1)
            .setMaxIntValue(20)
            .build()
            .getIntValue();

    IntValue volume = ValueBuilder.create(this, "SoundVolume")
            .setDefaultIntValue(1)
            .setMinIntValue(1)
            .setMaxIntValue(20)
            .build()
            .getIntValue();

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.player != null && mc.level != null) {
            if (event.getTarget() instanceof LivingEntity livingentity) {
                for (int i = 0; i < this.amount.getCurrentValue(); i++) {
                    BlockPos blockpos = livingentity.blockPosition();
                    String s = this.particle.getCurrentMode().toLowerCase();
                    switch (s) {
                        case "blood":
                            mc.level
                                    .addParticle(
                                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                                            (double) blockpos.getX() + 0.5,
                                            (double) blockpos.getY() + 1.0,
                                            (double) blockpos.getZ() + 0.5,
                                            0.0,
                                            0.0,
                                            0.0
                                    );
                            break;
                        case "fire":
                            mc.level.addParticle(ParticleTypes.LAVA, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "heart":
                            mc.level.addParticle(ParticleTypes.HEART, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "water":
                            mc.level.addParticle(ParticleTypes.FALLING_WATER, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "smoke":
                            mc.level.addParticle(ParticleTypes.SMOKE, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "magic":
                            mc.level.addParticle(ParticleTypes.ENCHANTED_HIT, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "crits":
                            mc.level.addParticle(ParticleTypes.CRIT, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "ash":
                            mc.level.addParticle(ParticleTypes.ASH, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "soul":
                            mc.level.addParticle(ParticleTypes.SOUL, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "campfire":
                            mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "flame":
                            mc.level.addParticle(ParticleTypes.FLAME, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "witch":
                            mc.level.addParticle(ParticleTypes.WITCH, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "dragonbreath":
                            mc.level.addParticle(ParticleTypes.DRAGON_BREATH, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "endrod":
                            mc.level.addParticle(ParticleTypes.END_ROD, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "snowball":
                            mc.level.addParticle(ParticleTypes.ITEM_SNOWBALL, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "slime":
                            mc.level.addParticle(ParticleTypes.ITEM_SLIME, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                            break;
                        case "explosion":
                            mc.level.addParticle(ParticleTypes.EXPLOSION, livingentity.getX(), livingentity.getY(), livingentity.getZ(), 0.0, 0.0, 0.0);
                    }
                }

                String soundMode = this.sound.getCurrentMode();
                if (!soundMode.equals("None")) {
                    SoundEvent soundEvent = switch (soundMode) {
                        case "Hit" -> SoundEvents.ARROW_HIT;
                        case "Orb" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                        case "Totem" -> SoundEvents.TOTEM_USE;
                        case "Explosion" -> SoundEvents.GENERIC_EXPLODE;
                        case "Anvil" -> SoundEvents.ANVIL_LAND;
                        case "Firework" -> SoundEvents.FIREWORK_ROCKET_BLAST;
                        case "Blaze" -> SoundEvents.BLAZE_SHOOT;
                        case "Sword" -> SoundEvents.PLAYER_ATTACK_SWEEP;
                        case "Critical" -> SoundEvents.PLAYER_ATTACK_CRIT;
                        case "Shield" -> SoundEvents.SHIELD_BLOCK;
                        case "Crossbow" -> SoundEvents.CROSSBOW_SHOOT;
                        case "Trident" -> SoundEvents.TRIDENT_THROW;
                        case "Smash" -> SoundEvents.STONE_BREAK;
                        case "Thunder" -> SoundEvents.LIGHTNING_BOLT_IMPACT;
                        default -> null;
                    };

                    if (soundEvent != null) {
                        SoundManager soundmanager = mc.getSoundManager();
                        float volumeFloat = (float) this.volume.getCurrentValue();
                        SimpleSoundInstance simplesoundinstance = SimpleSoundInstance.forLocalAmbience(
                                soundEvent,
                                volumeFloat,
                                1.0F
                        );

                        soundmanager.play(simplesoundinstance);
                    }
                }
            }
        }
    }
}