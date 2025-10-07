package dev.yalan.live;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import dev.yalan.live.events.EventLiveAuthenticationResult;
import dev.yalan.live.events.EventLiveChannelInactive;
import dev.yalan.live.events.EventLiveGenericMessage;
import dev.yalan.live.netty.LiveProto;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;

public class LiveComponent {
    private final Minecraft mc = Minecraft.getInstance();
    private final LiveClient live;

    LiveComponent(LiveClient live) {
        this.live = live;

        live.getEventManager().register(this);
    }

    @EventTarget
    public void onRenderTabOverlay(EventRenderTabOverlay e) {
        if (e.getType() == EventType.NAME && e.getPlayerInfo() != null) {
            final UUID mcUUID = UUIDUtil.getOrCreatePlayerUUID(e.getPlayerInfo().getProfile());
            final LiveUser liveUser = live.getLiveUserMap().get(mcUUID);

            if (liveUser != null) {
                e.setComponent(Component.literal(getLiveUserDisplayName(liveUser) + " ").append(e.getComponent()));
            }
        }
    }

    @EventTarget
    private void onLiveAuthenticationResult(EventLiveAuthenticationResult e) {
        if (e.isSuccess()) {
            if (mc.level != null) {
                ChatUtils.addChatMessage("重连成功!");

                if (mc.player != null) {
                    LiveClient.INSTANCE.sendPacket(LiveProto.createUpdateMinecraftProfile(
                            mc.player.getUUID(),
                            mc.player.getName().getString()
                    ));
                }
            }
        } else {
            if (LiveClient.INSTANCE.autoUsername != null && LiveClient.INSTANCE.autoPassword != null) {
                ChatUtils.addChatMessage("停止重连: 自动凭据失效");

                live.stopReconnectionThread();
            }
        }
    }

    @EventTarget
    private void onLiveGenericMessage(EventLiveGenericMessage e) {
        if (mc.level != null) {
            printSimpleChat("Server", e.getChannel() + "-" + e.getMessage());
        }
    }

    @EventTarget
    private void onLiveChannelInactive(EventLiveChannelInactive e) {
        if (mc.level != null) {
            ChatUtils.addChatMessage("与Live失去连接!");
        }
    }

    public void handleQueryResultMinecraftProfile(UUID mcUUID, String clientId, UUID userId, String userPayloadString) {
        final JsonObject payload = JsonParser.parseString(userPayloadString).getAsJsonObject();
        final LiveUser liveUser = new LiveUser(clientId, userId, payload);

        live.getLiveUserMap().put(mcUUID, liveUser);
    }

    public void handleChat(String channel, String payloadString) {
        final JsonObject payload = JsonParser.parseString(payloadString).getAsJsonObject();

        switch (channel) {
            case "LivePublic" -> {
                final String clientId = payload.get("clientId").getAsString();
                final String message = payload.get("message").getAsString();

                if ("ShaoYu".equals(clientId)) {
                    printPublicChat(clientId, message, payload.get("username").getAsString(), payload.get("rank").getAsString());
                } else {
                    printPublicChat(
                            clientId,
                            message,
                            payload.get("username").getAsString(),
                            Optional.ofNullable(payload.get("rank"))
                                    .map(JsonElement::getAsString)
                                    .orElse(null)
                    );
                }
            }
            case "ServerLog" -> {
                final String message = payload.get("message").getAsString();
                printSimpleChat(ChatFormatting.AQUA + "Server", message);
            }
            case "Broadcast" -> {
                final String message = payload.get("message").getAsString();
                printSimpleChat(ChatFormatting.GOLD + "Broadcast", message);
            }
        }
    }

    private void printPublicChat(String clientId, String message, String username, String rank) {
        final StringBuilder builder = new StringBuilder();

        builder.append(ChatFormatting.GRAY).append("[");
        builder.append(ChatFormatting.YELLOW).append("Live");
        builder.append(ChatFormatting.GRAY).append("-");
        builder.append(ChatFormatting.RED).append(clientId);
        builder.append(ChatFormatting.GRAY).append("] ");
        builder.append(ChatFormatting.RESET).append(username);

        if (rank != null) {
            builder.append(ChatFormatting.GRAY).append("(");
            builder.append(ChatFormatting.RESET).append(rank).append(ChatFormatting.RESET);
            builder.append(ChatFormatting.GRAY).append(")");
        }

        builder.append(ChatFormatting.GRAY).append(": ");
        builder.append(ChatFormatting.RESET).append(message);

        mc.gui.getChat().addMessage(Component.literal(builder.toString()));
    }

    private void printSimpleChat(String sender, String message) {
        final StringBuilder builder = new StringBuilder();

        builder.append(ChatFormatting.GRAY).append("[");
        builder.append(ChatFormatting.YELLOW).append("Live");
        builder.append(ChatFormatting.GRAY).append("-");
        builder.append(ChatFormatting.RESET).append(sender);
        builder.append(ChatFormatting.GRAY).append("]: ");
        builder.append(ChatFormatting.RESET).append(message);

        mc.gui.getChat().addMessage(Component.literal(builder.toString()));
    }

    public static String getLiveUserDisplayName(LiveUser liveUser) {
        final StringBuilder builder = new StringBuilder();

        builder.append(ChatFormatting.GRAY).append("[");
        builder.append(ChatFormatting.YELLOW).append("Live");
        builder.append(ChatFormatting.GRAY).append("-");
        builder.append(ChatFormatting.RED).append(liveUser.getClientId());
        builder.append(ChatFormatting.GRAY).append("-");
        builder.append(ChatFormatting.RESET).append(liveUser.getName());

        final String rank = liveUser.getRank();

        if (rank != null) {
            builder.append(ChatFormatting.GRAY).append("(");
            builder.append(ChatFormatting.RESET).append(rank).append(ChatFormatting.RESET);
            builder.append(ChatFormatting.GRAY).append(")");
        }

        builder.append(ChatFormatting.GRAY).append("]");

        return builder.toString();
    }
}
