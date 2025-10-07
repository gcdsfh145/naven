package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CommandInfo(
        name = "help",
        description = "Get all commands",
        aliases = {"h"}
)
public class CommandHelp extends Command {

    @Override
    public void onCommand(String[] args) {
        ChatUtils.addChatMessage("Available commands:");

        Set<Command> displayedCommands = new HashSet<>();

        for (Map.Entry<String, Command> entry : CommandManager.aliasMap.entrySet()) {
            Command command = entry.getValue();

            if (displayedCommands.contains(command)) {
                continue;
            }

            displayedCommands.add(command);

            StringBuilder sb = new StringBuilder();
            sb.append(".").append(command.getName());

            for (String alias : command.getAliases()) {
                sb.append(", .").append(alias);
            }

            sb.append(" - ").append(command.getDescription());
            ChatUtils.addChatMessage(sb.toString());
        }

        ChatUtils.addChatMessage("Total unique commands: " + displayedCommands.size());
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}