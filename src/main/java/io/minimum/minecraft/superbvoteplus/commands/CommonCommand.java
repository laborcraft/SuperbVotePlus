package io.minimum.minecraft.superbvoteplus.commands;

import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.configuration.message.MessageContext;
import io.minimum.minecraft.superbvoteplus.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.BrokenNag;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CommonCommand implements CommandExecutor {
    private final VoteMessage message;
    private final boolean streakRelated;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (SuperbVotePlus.getPlugin().getConfiguration().isConfigurationError()) {
            BrokenNag.nag(player);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVotePlus.getPlugin(), () -> {
            VoteStorage voteStorage = SuperbVotePlus.getPlugin().getVoteStorage();
            MessageContext ctx = new MessageContext(null, voteStorage.getVotes(player.getUniqueId()), voteStorage.getVoteStreakIfSupported(player.getUniqueId(), streakRelated), player);
            message.sendAsReminder(player, ctx);
        });
        return true;
    }
}
