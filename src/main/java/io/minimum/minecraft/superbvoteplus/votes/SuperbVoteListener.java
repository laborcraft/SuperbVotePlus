package io.minimum.minecraft.superbvoteplus.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvoteplus.configuration.message.MessageContext;
import io.minimum.minecraft.superbvoteplus.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvoteplus.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.BrokenNag;
import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.rewards.VoteReward;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

            processVote(pv, voteStreak, vote, SuperbVotePlus.getPlugin().getConfig().getBoolean("broadcast.enabled"),
                    !op.isOnline() && SuperbVotePlus.getPlugin().getConfiguration().requirePlayersOnline(),
                    false);
        });
    }

    private void processVote(PlayerVotes pv, VoteStreak voteStreak, Vote vote, boolean broadcast, boolean queue, boolean wasQueued) {
        List<VoteReward> bestRewards = SuperbVotePlus.getPlugin().getConfiguration().getBestRewards(vote, pv);
        MessageContext context = new MessageContext(vote, pv, voteStreak, Bukkit.getOfflinePlayer(vote.getUuid()));
        boolean canBroadcast = SuperbVotePlus.getPlugin().getRecentVotesStorage().canBroadcast(vote.getUuid());
        SuperbVotePlus.getPlugin().getRecentVotesStorage().updateLastVote(vote.getUuid());

        Optional<Player> player = context.getPlayer().map(OfflinePlayer::getPlayer);
        boolean hideBroadcast = player.isPresent() && player.get().hasPermission("superbvote.bypassbroadcast");

        if (bestRewards.isEmpty()) {
            throw new RuntimeException("No vote rewards found for '" + vote + "'");
        }

        if (queue) {
            if (!SuperbVotePlus.getPlugin().getConfiguration().shouldQueueVotes()) {
                SuperbVotePlus.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                        vote.getServiceName() + ") because they aren't online.");
                return;
            }

            SuperbVotePlus.getPlugin().getLogger().log(Level.INFO, "Queuing vote from " + vote.getName() + " to be run later");
            for (VoteReward reward : bestRewards) {
                reward.broadcastVote(context, false, broadcast && SuperbVotePlus.getPlugin().getConfig().getBoolean("broadcast.queued") && canBroadcast && !hideBroadcast);
            }
            SuperbVotePlus.getPlugin().getQueuedVotes().addVote(vote);
        } else {
            if (!vote.isFakeVote() || SuperbVotePlus.getPlugin().getConfig().getBoolean("votes.process-fake-votes")) {
                SuperbVotePlus.getPlugin().getVoteStorage().addVote(vote);
            }

            if (!wasQueued) {
                for (VoteReward reward : bestRewards) {
                    reward.broadcastVote(context, SuperbVotePlus.getPlugin().getConfig().getBoolean("broadcast.message-player"), broadcast && canBroadcast && !hideBroadcast);
                }
                Bukkit.getScheduler().runTaskAsynchronously(SuperbVotePlus.getPlugin(), this::afterVoteProcessing);
            }

            Bukkit.getScheduler().runTask(SuperbVotePlus.getPlugin(), () -> bestRewards.forEach(reward -> reward.runCommands(vote)));
        }
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

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVotePlus.getPlugin(), () -> {
            // Update names in MySQL, if it is being used.
            if (SuperbVotePlus.getPlugin().getVoteStorage() instanceof MysqlVoteStorage) {
                ((MysqlVoteStorage) SuperbVotePlus.getPlugin().getVoteStorage()).updateName(event.getPlayer());
            }

            // Process queued votes.
            VoteStorage voteStorage = SuperbVotePlus.getPlugin().getVoteStorage();
            UUID playerUUID = event.getPlayer().getUniqueId();
            PlayerVotes pv = voteStorage.getVotes(playerUUID);
            VoteStreak voteStreak = voteStorage.getVoteStreakIfSupported(playerUUID, false);
            List<Vote> votes = SuperbVotePlus.getPlugin().getQueuedVotes().getAndRemoveVotes(playerUUID);
            if (!votes.isEmpty()) {
                for (Vote vote : votes) {
                    processVote(pv, voteStreak, vote, false, false, true);
                    pv = new PlayerVotes(pv.getUuid(), event.getPlayer().getName(),pv.getVotes() + 1, PlayerVotes.Type.CURRENT);
                }
                afterVoteProcessing();
            }

            // Remind players to vote.
            if (SuperbVotePlus.getPlugin().getConfig().getBoolean("vote-reminder.on-join") &&
                    event.getPlayer().hasPermission("superbvote.notify") &&
                    !SuperbVotePlus.getPlugin().getVoteStorage().hasVotedToday(event.getPlayer().getUniqueId())) {
                MessageContext context = new MessageContext(null, pv, voteStreak, event.getPlayer());
                SuperbVotePlus.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(event.getPlayer(), context);
            }
        });
    }

    private void afterVoteProcessing() {
        SuperbVotePlus.getPlugin().getScoreboardHandler().doPopulate();
        new TopPlayerSignFetcher(SuperbVotePlus.getPlugin().getTopPlayerSignStorage().getSignList()).run();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) {
            SuperbVotePlus.getPlugin().getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");
        }
    }
}
