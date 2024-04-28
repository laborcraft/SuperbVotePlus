package io.minimum.minecraft.superbvoteplus.proxy.bungee;

import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import cz.foresttech.forestredis.shared.RedisManager;
import io.minimum.minecraft.superbvoteplus.listener.RedisListener;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/**
 * Simple Bungee plugin bridging nuVotifier to ForestRedisAPI
 */
public class SuperbVotePlus extends Plugin implements Listener {

    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
    }

    public void onDisable() {
        getProxy().getPluginManager().unregisterListener(this);
    }

    @EventHandler
    public void onVote(VotifierEvent e){
        RedisManager.getAPI().publishObject(RedisListener.CHANNEL, e.getVote());
    }

}
