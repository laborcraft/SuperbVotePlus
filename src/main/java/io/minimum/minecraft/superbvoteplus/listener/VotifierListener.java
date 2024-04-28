package io.minimum.minecraft.superbvoteplus.listener;

import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.Vote;
import io.minimum.minecraft.superbvoteplus.votes.VoteProcessor;
import io.minimum.minecraft.superbvoteplus.votes.VoteStreak;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;

import java.util.Date;
import java.util.logging.Level;

public abstract class VotifierListener implements Listener {

    public void handleVote(final com.vexsoftware.votifier.model.Vote votifierVote) {
        if (SuperbVotePlus.getPlugin().getConfiguration().isConfigurationError()) {
            SuperbVotePlus.getPlugin().getLogger().severe("Refusing to process vote because your configuration is invalid. Please check your logs.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVotePlus.getPlugin(), () -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(votifierVote.getUsername());
            String worldName = null;
            if (op.isOnline()) {
                worldName = op.getPlayer().getWorld().getName();
            }

            VoteStorage voteStorage = SuperbVotePlus.getPlugin().getVoteStorage();
            VoteStreak voteStreak = voteStorage.getVoteStreakIfSupported(op.getUniqueId(), false);
            PlayerVotes pvCurrent = voteStorage.getVotes(op.getUniqueId());
            PlayerVotes pv = new PlayerVotes(op.getUniqueId(), op.getName(), pvCurrent.getVotes() + 1, PlayerVotes.Type.FUTURE);
            Vote vote = new Vote(op.getName(), op.getUniqueId(), votifierVote.getServiceName(),
                    votifierVote.getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), worldName, new Date());

            if (!vote.isFakeVote()) {
                if (SuperbVotePlus.getPlugin().getConfiguration().getStreaksConfiguration().sharedCooldownPerService()) {
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
}
