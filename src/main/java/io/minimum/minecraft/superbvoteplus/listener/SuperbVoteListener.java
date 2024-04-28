package io.minimum.minecraft.superbvoteplus.listener;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;
import io.minimum.minecraft.superbvoteplus.util.BrokenNag;
import io.minimum.minecraft.superbvoteplus.votes.VoteProcessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class SuperbVoteListener extends VotifierListener {


    @EventHandler
    public void onVote(final VotifierEvent event) {
        handleVote(event.getVote());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (SuperbVotePlus.getPlugin().getConfiguration().isConfigurationError()) {
            if (event.getPlayer().hasPermission("superbvoteplus.admin")) {
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
