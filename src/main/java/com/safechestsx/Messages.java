package com.safechestsx;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class Messages {
    private final FileConfiguration config;

    public Messages(FileConfiguration config) {
        this.config = config;
    }

    public String getRaw(String path) {
        return config.getString("messages." + path, "");
    }

    public String format(String path, Map<String, String> replacements) {
        String message = getRaw(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return color(message);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(color(getRaw("prefix") + getRaw(path)));
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        sender.sendMessage(color(getRaw("prefix") + format(path, replacements)));
    }

    public void sendList(CommandSender sender, String path, Map<String, String> replacements) {
        for (String line : config.getStringList("messages." + path)) {
            String message = line;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            sender.sendMessage(color(getRaw("prefix") + message));
        }
    }

    public String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
