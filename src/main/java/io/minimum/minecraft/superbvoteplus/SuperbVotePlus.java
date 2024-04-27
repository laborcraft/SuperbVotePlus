package io.minimum.minecraft.superbvoteplus;

import io.minimum.minecraft.superbvoteplus.commands.RewardCommand;
import io.minimum.minecraft.superbvoteplus.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvoteplus.configuration.SuperbVoteConfiguration;
import io.minimum.minecraft.superbvoteplus.scoreboard.ScoreboardHandler;
import io.minimum.minecraft.superbvoteplus.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvoteplus.signboard.TopPlayerSignListener;
import io.minimum.minecraft.superbvoteplus.signboard.TopPlayerSignStorage;
import io.minimum.minecraft.superbvoteplus.storage.QueuedVotesStorage;
import io.minimum.minecraft.superbvoteplus.storage.RecentVotesStorage;
import io.minimum.minecraft.superbvoteplus.storage.VoteStorage;
import io.minimum.minecraft.superbvoteplus.util.BrokenNag;
import io.minimum.minecraft.superbvoteplus.util.SpigotUpdater;
import io.minimum.minecraft.superbvoteplus.util.cooldowns.VoteServiceCooldown;
import io.minimum.minecraft.superbvoteplus.votes.VoteProcessor;
import io.minimum.minecraft.superbvoteplus.votes.VoteReminder;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

public class SuperbVotePlus extends JavaPlugin {
    @Getter
    private static SuperbVotePlus plugin;
    @Getter
    private SuperbVoteConfiguration configuration;
    @Getter
    private VoteStorage voteStorage;
    @Getter
    private QueuedVotesStorage queuedVotes;
    @Getter
    private RecentVotesStorage recentVotesStorage;
    @Getter
    private ScoreboardHandler scoreboardHandler;
    @Getter
    private VoteServiceCooldown voteServiceCooldown;
    @Getter
    private TopPlayerSignStorage topPlayerSignStorage;
    private BukkitTask voteReminderTask;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        configuration = new SuperbVoteConfiguration(getConfig());

        if (configuration.isConfigurationError()) {
            BrokenNag.nag(getServer().getConsoleSender());
        }

        try {
            voteStorage = configuration.initializeVoteStorage();
        } catch (Exception e) {
            throw new RuntimeException("Exception whilst initializing vote storage", e);
        }

        try {
            queuedVotes = new QueuedVotesStorage(new File(getDataFolder(), "queued_votes.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst initializing queued vote storage", e);
        }

        recentVotesStorage = new RecentVotesStorage();

        scoreboardHandler = new ScoreboardHandler();
        voteServiceCooldown = new VoteServiceCooldown(getConfig().getInt("votes.cooldown-per-service", 3600));

        topPlayerSignStorage = new TopPlayerSignStorage();
        try {
            topPlayerSignStorage.load(new File(getDataFolder(), "top_voter_signs.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst loading top player signs", e);
        }

        getCommand("superbvoteplus").setExecutor(new SuperbVoteCommand());
        getCommand("vote").setExecutor(configuration.getVoteCommand());
        getCommand("votestreak").setExecutor(configuration.getVoteStreakCommand());
        getCommand("reward").setExecutor(new RewardCommand());

        getServer().getPluginManager().registerEvents(new VoteProcessor(), this);
        getServer().getPluginManager().registerEvents(new TopPlayerSignListener(), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, voteStorage::save, 20, 20 * 30);
        getServer().getScheduler().runTaskTimerAsynchronously(this, queuedVotes::save, 20, 20 * 30);
        getServer().getScheduler().runTaskAsynchronously(this, SuperbVotePlus.getPlugin().getScoreboardHandler()::doPopulate);
        getServer().getScheduler().runTaskAsynchronously(this, new TopPlayerSignFetcher(topPlayerSignStorage.getSignList()));

        if(getConfig().getBoolean("vote-reminder.repeat.enabled")){
            int r = getConfig().getInt("vote-reminder.repeat.seconds");
            String text = getConfig().getString("vote-reminder.message");
            if (text != null && !text.isEmpty()) {
                if (r > 0) {
                    voteReminderTask = getServer().getScheduler().runTaskTimerAsynchronously(this, new VoteReminder(), 20 * r, 20 * r);
                }
            }
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");
        }

        SpigotUpdater updater = new SpigotUpdater();
        getServer().getScheduler().runTaskAsynchronously(this, updater);
        getServer().getPluginManager().registerEvents(updater, this);
    }

    @Override
    public void onDisable() {
        if (voteReminderTask != null) {
            voteReminderTask.cancel();
            voteReminderTask = null;
        }
        voteStorage.save();
        queuedVotes.save();
        voteStorage.close();
        try {
            topPlayerSignStorage.save(new File(getDataFolder(), "top_voter_signs.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst saving top player signs", e);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configuration = new SuperbVoteConfiguration(getConfig());
        scoreboardHandler.reload();
        voteServiceCooldown = new VoteServiceCooldown(getConfig().getInt("votes.cooldown-per-service", 3600));
        getServer().getScheduler().runTaskAsynchronously(this, getScoreboardHandler()::doPopulate);
        getCommand("vote").setExecutor(configuration.getVoteCommand());
        getCommand("votestreak").setExecutor(configuration.getVoteStreakCommand());

        if (voteReminderTask != null) {
            voteReminderTask.cancel();
            voteReminderTask = null;
        }
        if(getConfig().getBoolean("vote-reminder.repeat.enabled")){
            int r = getConfig().getInt("vote-reminder.repeat.time");
            String text = getConfig().getString("vote-reminder.message");
            if (text != null && !text.isEmpty() && r > 0) {
                voteReminderTask = getServer().getScheduler().runTaskTimerAsynchronously(this, new VoteReminder(), 20 * r, 20 * r);
            }
        }
    }

    public ClassLoader _exposeClassLoader() {
        return getClassLoader();
    }
}
