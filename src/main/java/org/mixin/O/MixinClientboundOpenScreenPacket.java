// Decompiled with: CFR 0.152
// Class Version: 17
package org.mixin.O;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ClientboundOpenScreenPacket.class})
public abstract class MixinClientboundOpenScreenPacket {
    @Inject(method = {"handle"}, at = {@At(value = "TAIL")}, cancellable = true)
    private void onHandle(ClientGamePacketListener listener, CallbackInfo ci) {
        ClientboundOpenScreenPacket packet = (ClientboundOpenScreenPacket) (Object) this;
        Component title = packet.getTitle();
        String rawTitle = title.getString();
        if (rawTitle.startsWith("选择举报")) {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player == null) {
                    return;
                }
                AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
                if (menu == null) {
                    return;
                }
                if (rawTitle.startsWith("选择举报")) {
                    for (int i = 0; i < menu.slots.size(); ++i) {
                        ItemStack stack = menu.getSlot(i).getItem();
                        if (stack.getItem() != Items.PAPER) continue;
                        Minecraft.getInstance().gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.PICKUP, Minecraft.getInstance().player);
                        break;
                    }
                    Minecraft.getInstance().setScreen(null);
                }
            });
            ci.cancel();
        }
    }
}