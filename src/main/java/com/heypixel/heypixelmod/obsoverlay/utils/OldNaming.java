package com.heypixel.heypixelmod.obsoverlay.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;

public class OldNaming {

    public static ServerboundInteractPacket C02PacketUseEntity(Entity target) {
        return ServerboundInteractPacket.createAttackPacket(target, false);
    }
    public static ServerboundMovePlayerPacket.StatusOnly C03PacketPlayer() {
        return new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround());
    }

    public static ServerboundMovePlayerPacket.StatusOnly C03PacketPlayer(boolean onGround) {
        return new ServerboundMovePlayerPacket.StatusOnly(onGround);
    }

    public static ServerboundCustomPayloadPacket C17CustomPayload(ResourceLocation pIdentifier, FriendlyByteBuf pData) {
        return new ServerboundCustomPayloadPacket(pIdentifier, pData);
    }


    public static ServerboundMovePlayerPacket.Pos C04PacketPlayerPosition(double x, double y, double z, boolean onGround) {
        return new ServerboundMovePlayerPacket.Pos(x, y, z, onGround);
    }

    public static ServerboundMovePlayerPacket.Rot C05PacketPlayerLook(float yaw, float pitch, boolean onGround) {
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround);
    }

    public static ServerboundMovePlayerPacket.PosRot C06PacketPlayerPosLook(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround);
    }

    public static ServerboundPlayerActionPacket C07PacketPlayerDigging(ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction) {
        return new ServerboundPlayerActionPacket(action, pos, direction);
    }

    public static ServerboundSetCarriedItemPacket C09PacketHeldItemChange(int slot) {
        return new ServerboundSetCarriedItemPacket(slot);
    }

    public static ServerboundSwingPacket C0APacketAnimation(InteractionHand hand) {
        return new ServerboundSwingPacket(hand);
    }

    public static ServerboundContainerClickPacket C0EPacketClickWindow(
            int containerId, int stateId, int slotNum, int buttonNum, ClickType clickType, ItemStack item, Int2ObjectMap<ItemStack> changedSlots
    ) {
        return new ServerboundContainerClickPacket(containerId, stateId, slotNum, buttonNum, clickType, item, changedSlots);
    }

    public static ServerboundPongPacket C0FPacketConfirmTransaction(int id) {
        return new ServerboundPongPacket(id);
    }

    public static ServerboundPlayerCommandPacket C0BPacketEntityAction(
            Player player, ServerboundPlayerCommandPacket.Action action
    ) {
        return new ServerboundPlayerCommandPacket(player, action);
    }

    public static ServerboundPlayerAbilitiesPacket C13PacketPlayerAbilities(Abilities abilities) {
        return new ServerboundPlayerAbilitiesPacket(abilities);
    }
}

