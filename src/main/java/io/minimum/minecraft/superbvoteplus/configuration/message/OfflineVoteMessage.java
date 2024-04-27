package io.minimum.minecraft.superbvoteplus.configuration.message;

import org.bukkit.command.CommandSender;

public interface OfflineVoteMessage {
    String getBaseMessage();

    String getWithOfflinePlayer(CommandSender to, MessageContext context);
}
