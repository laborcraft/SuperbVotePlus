package io.minimum.minecraft.superbvoteplus;

import io.minimum.minecraft.superbvoteplus.commands.RewardCommand;
import io.minimum.minecraft.superbvoteplus.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvoteplus.configuration.MainConfiguration;
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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SuperbVotePlus extends JavaPlugin {
    @Getter
    private static SuperbVotePlus plugin;
    @Getter
    private MainConfiguration configuration;
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
        reloadPlugin();

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

        topPlayerSignStorage = new TopPlayerSignStorage();
        try {
            topPlayerSignStorage.load(new File(getDataFolder(), "top_voter_signs.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst loading top player signs", e);
        }

        getServer().getPluginManager().registerEvents(new VoteProcessor(), this);
        getServer().getPluginManager().registerEvents(new TopPlayerSignListener(), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, voteStorage::save, 20, 20 * 30);
        getServer().getScheduler().runTaskTimerAsynchronously(this, queuedVotes::save, 20, 20 * 30);
        getServer().getScheduler().runTaskAsynchronously(this, SuperbVotePlus.getPlugin().getScoreboardHandler()::doPopulate);
        getServer().getScheduler().runTaskAsynchronously(this, new TopPlayerSignFetcher(topPlayerSignStorage.getSignList()));

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

    Command superbVotePlusCommand;
    Command voteCommand;
    Command voteStreakCommand;
    Command rewardCommand;

    public void reloadPlugin() {
        saveDefaultConfig();
        reloadConfig();
        configuration = new MainConfiguration(getConfig());
        if (configuration.isConfigurationError()) {
            BrokenNag.nag(getServer().getConsoleSender());
        }

        if(scoreboardHandler == null){
            scoreboardHandler = new ScoreboardHandler();
        } else {
            scoreboardHandler.reload();
        }
        voteServiceCooldown = new VoteServiceCooldown(getConfig().getInt("votes.cooldown-per-service", 3600));
        getServer().getScheduler().runTaskAsynchronously(this, getScoreboardHandler()::doPopulate);

        String namespace = getName();

        //register commands

        if(superbVotePlusCommand != null) superbVotePlusCommand.unregister(Bukkit.getCommandMap());
        superbVotePlusCommand = getCommand(configuration.getLang().getCommandNames().get("superbvoteplus"), new SuperbVoteCommand());
        Bukkit.getCommandMap().register(namespace, superbVotePlusCommand);

        if(voteStreakCommand != null) voteStreakCommand.unregister(Bukkit.getCommandMap());
        voteStreakCommand = getCommand(configuration.getLang().getCommandNames().get("votestreak"), configuration.getLang().getVoteStreakCommand());
        if(getConfig().getBoolean("streaks.enabled") && getConfig().getBoolean("streaks.command.enabled")) Bukkit.getCommandMap().register(namespace, voteStreakCommand);

        if(voteCommand != null) voteCommand.unregister(Bukkit.getCommandMap());
        voteCommand = getCommand(configuration.getLang().getCommandNames().get("vote"), configuration.getLang().getVoteCommand());
        if(getConfig().getBoolean("vote-command")) Bukkit.getCommandMap().register(namespace, voteCommand);

        if(rewardCommand != null) rewardCommand.unregister(Bukkit.getCommandMap());
        rewardCommand = getCommand(configuration.getLang().getCommandNames().get("reward"), new RewardCommand());
        if(getConfig().getBoolean("reward-command")) Bukkit.getCommandMap().register(namespace, rewardCommand);


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

    private Command getCommand(String name, TabExecutor executor) {
        return new Command(name) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
            @NotNull
            public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                List<String> returnValue = executor.onTabComplete(sender, this, name, args);
                if(returnValue != null)
                    return returnValue;
                return StringUtil.copyPartialMatches(
                        args[args.length-1],
                        Bukkit.getOnlinePlayers()
                                .stream()
                                .map(Player::getName)
                                .collect(Collectors.toList()),
                        new ArrayList<>()
                );
            }
        };
    }
}
