package io.minimum.minecraft.superbvoteplus.votes;

import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.configuration.message.MessageContext;
import io.minimum.minecraft.superbvoteplus.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvoteplus.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.rewards.VoteReward;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class VoteProcessor implements Listener {

    private static VoteProcessor instance;
    public static VoteProcessor getInstance(){
        if(instance == null) instance = new VoteProcessor();
        return instance;
    }

    /**
     *
     * @param pv
     * @param voteStreak streak data
     * @param vote the vote to process
     * @param broadcast
     * @param queue true if we should queue vote to be run later
     * @param wasQueued
     */
    public void processVote(PlayerVotes pv, VoteStreak voteStreak, Vote vote, boolean broadcast, boolean queue, boolean wasQueued) {
        List<VoteReward> bestRewards = SuperbVotePlus.getPlugin().getConfiguration().getBestRewards(vote, pv);
        MessageContext context = new MessageContext(vote, pv, voteStreak, Bukkit.getOfflinePlayer(vote.getUuid()));
        boolean canBroadcast = SuperbVotePlus.getPlugin().getRecentVotesStorage().canBroadcast(vote.getUuid());
        SuperbVotePlus.getPlugin().getRecentVotesStorage().updateLastVote(vote.getUuid());

        Optional<Player> player = context.getPlayer().map(OfflinePlayer::getPlayer);
        boolean hideBroadcast = player.isPresent() && player.get().hasPermission("superbvoteplus.bypassbroadcast");

        if (bestRewards.isEmpty()) {
            throw new RuntimeException("No vote rewards found for '" + vote + "'");
        }

        if (queue) {

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

    /**
     *
     * @param player
     * @param tellNoMessages if we should tell the player he has no messages. Used for commands.
     */
    public void reward(Player player, boolean tellNoMessages){
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVotePlus.getPlugin(), () -> {
            // Update names in MySQL, if it is being used.
            if (SuperbVotePlus.getPlugin().getVoteStorage() instanceof MysqlVoteStorage) {
                ((MysqlVoteStorage) SuperbVotePlus.getPlugin().getVoteStorage()).updateName(player);
            }

            // Process queued votes.
            VoteStorage voteStorage = SuperbVotePlus.getPlugin().getVoteStorage();
            UUID playerUUID = player.getUniqueId();
            PlayerVotes pv = voteStorage.getVotes(playerUUID);
            VoteStreak voteStreak = voteStorage.getVoteStreakIfSupported(playerUUID, false);
            List<Vote> votes = SuperbVotePlus.getPlugin().getQueuedVotes().getAndRemoveVotes(playerUUID);
            if (!votes.isEmpty()) {
                for (Vote vote : votes) {
                    processVote(pv, voteStreak, vote, false, false, true);
                    pv = new PlayerVotes(pv.getUuid(), player.getName(),pv.getVotes() + 1, PlayerVotes.Type.CURRENT);
                }
                afterVoteProcessing();
            } else {
                player.sendMessage(SuperbVotePlus.getPlugin().getConfiguration().getLang().getNoRewards());
            }

            // Remind players to vote.
            if (SuperbVotePlus.getPlugin().getConfig().getBoolean("vote-reminder.on-join.enabled") &&
                    player.hasPermission("superbvoteplus.notify") &&
                    !SuperbVotePlus.getPlugin().getVoteStorage().hasVotedToday(player.getUniqueId())) {
                MessageContext context = new MessageContext(null, pv, voteStreak, player);
                Bukkit.getScheduler().runTaskLater(
                        SuperbVotePlus.getPlugin(),
                        ()->SuperbVotePlus.getPlugin().getConfiguration().getLang().getReminderMessage().sendAsReminder(player, context),
                        SuperbVotePlus.getPlugin().getConfig().getLong("vote-reminder.on-join.tick-delay")
                );
            }
        });
    }

    private void afterVoteProcessing() {
        SuperbVotePlus.getPlugin().getScoreboardHandler().doPopulate();
        new TopPlayerSignFetcher(SuperbVotePlus.getPlugin().getTopPlayerSignStorage().getSignList()).run();
    }
}
