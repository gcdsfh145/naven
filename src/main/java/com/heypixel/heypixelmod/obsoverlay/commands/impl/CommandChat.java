package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import dev.yalan.live.LiveClient;
import dev.yalan.live.netty.LiveProto;

@CommandInfo(name = "i", description = "Live聊天")
public class CommandChat extends Command {
    @Override
    public void onCommand(String[] args) {
        if (LiveClient.INSTANCE.isActive()) {
            final StringBuilder message = new StringBuilder();

            for (int i = 0; i < args.length; i++) {
                message.append(args[i]);

                if (i + 1 != args.length) {
                    message.append(' ');
                }
            }

            LiveClient.INSTANCE.sendPacket(LiveProto.createChat(message.toString()));
        } else {
            ChatUtils.addChatMessage("无法向Live发送消息: 无连接");
        }
    }

    @Override
    public String[] onTab(String[] var1) {
        return new String[0];
    }
}
