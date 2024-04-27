package io.minimum.minecraft.superbvoteplus.votes;

import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.configuration.message.MessageContext;
import io.minimum.minecraft.superbvoteplus.storage.ExtendedVoteStorage;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class VoteReminder implements Runnable {
    @Override
    public void run() {
        List<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("superbvote.notify")).map(Player::getUniqueId).collect(Collectors.toList());

        VoteStorage voteStorage = SuperbVotePlus.getPlugin().getVoteStorage();
        if (SuperbVotePlus.getPlugin().getConfiguration().getStreaksConfiguration().isPlaceholdersEnabled() && voteStorage instanceof ExtendedVoteStorage) {
            List<Map.Entry<PlayerVotes, VoteStreak>> noVotes = ((ExtendedVoteStorage) voteStorage).getAllPlayersAndStreaksWithNoVotesToday(onlinePlayers);
            for (Map.Entry<PlayerVotes, VoteStreak> entry : noVotes) {
                PlayerVotes pv = entry.getKey();
                VoteStreak voteStreak = entry.getValue();

                Player player = Bukkit.getPlayer(pv.getUuid());
                if (player != null) {
                    MessageContext context = new MessageContext(null, pv, voteStreak, player);
                    SuperbVotePlus.getPlugin().getConfiguration().getLang().getReminderMessage().sendAsReminder(player, context);
                }
            }
        } else {
            List<PlayerVotes> noVotes = voteStorage.getAllPlayersWithNoVotesToday(onlinePlayers);
            for (PlayerVotes pv : noVotes) {
                Player player = Bukkit.getPlayer(pv.getUuid());
                if (player != null) {
                    MessageContext context = new MessageContext(null, pv, null, player);
                    SuperbVotePlus.getPlugin().getConfiguration().getLang().getReminderMessage().sendAsReminder(player, context);
                }
            }
        }
    }
}
