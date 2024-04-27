package io.minimum.minecraft.superbvoteplus.listener;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.BrokenNag;
import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.Vote;
import io.minimum.minecraft.superbvoteplus.votes.VoteProcessor;
import io.minimum.minecraft.superbvoteplus.votes.VoteStreak;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Date;
import java.util.logging.Level;

public class SuperbVoteListener implements Listener {


    @EventHandler
    public void onVote(final VotifierEvent event) {
        if (SuperbVotePlus.getPlugin().getConfiguration().isConfigurationError()) {
            SuperbVotePlus.getPlugin().getLogger().severe("Refusing to process vote because your configuration is invalid. Please check your logs.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVotePlus.getPlugin(), () -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(event.getVote().getUsername());
            String worldName = null;
            if (op.isOnline()) {
                worldName = op.getPlayer().getWorld().getName();
            }

            VoteStorage voteStorage = SuperbVotePlus.getPlugin().getVoteStorage();
            VoteStreak voteStreak = voteStorage.getVoteStreakIfSupported(op.getUniqueId(), false);
            PlayerVotes pvCurrent = voteStorage.getVotes(op.getUniqueId());
            PlayerVotes pv = new PlayerVotes(op.getUniqueId(), op.getName(), pvCurrent.getVotes() + 1, PlayerVotes.Type.FUTURE);
            Vote vote = new Vote(op.getName(), op.getUniqueId(), event.getVote().getServiceName(),
                    event.getVote().getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), worldName, new Date());

            if (!vote.isFakeVote()) {
                if (SuperbVotePlus.getPlugin().getConfiguration().getStreaksConfiguration().isSharedCooldownPerService()) {
                    if (voteStreak == null) {
                        // becomes a required value
                        voteStreak = voteStorage.getVoteStreakIfSupported(op.getUniqueId(), true);
                    }
                    if (voteStreak != null && voteStreak.getServices().containsKey(vote.getServiceName())) {
                        long difference = SuperbVotePlus.getPlugin().getVoteServiceCooldown().getMax() - voteStreak.getServices().get(vote.getServiceName());
                        if (difference > 0) {
                            SuperbVotePlus.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                                    vote.getServiceName() + ") due to [shared] service cooldown.");
                            return;
                        }
                    }
                }

                if (SuperbVotePlus.getPlugin().getVoteServiceCooldown().triggerCooldown(vote)) {
                    SuperbVotePlus.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                            vote.getServiceName() + ") due to service cooldown.");
                    return;
                }
            }

            boolean rewardNow =
                    !(!op.isOnline() && SuperbVotePlus.getPlugin().getConfiguration().requirePlayersOnline())
                            &&
                    SuperbVotePlus.getPlugin().getConfiguration().shouldGiveAutomatically();

            VoteProcessor.getInstance().processVote(
                    pv,
                    voteStreak,
                    vote,
                    SuperbVotePlus.getPlugin().getConfig().getBoolean("broadcast.enabled"),
                    !rewardNow, //queue
                    false //was queued
            );
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (SuperbVotePlus.getPlugin().getConfiguration().isConfigurationError()) {
            if (event.getPlayer().hasPermission("superbvote.admin")) {
                Player player = event.getPlayer();
                Bukkit.getScheduler().runTaskLater(SuperbVotePlus.getPlugin(), () -> BrokenNag.nag(player), 40);
            }
            return;
        }
        if(SuperbVotePlus.getPlugin().getConfiguration().shouldGiveAutomatically()){
            VoteProcessor.getInstance().reward(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) {
            SuperbVotePlus.getPlugin().getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");
        }
    }

}
