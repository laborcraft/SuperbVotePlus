package io.minimum.minecraft.superbvoteplus.listener;

import com.vexsoftware.votifier.model.Vote;
import cz.foresttech.forestredis.shared.RedisManager;
import cz.foresttech.forestredis.spigot.events.RedisMessageReceivedEvent;
import org.bukkit.event.EventHandler;

public class RedisListener extends VotifierListener {

    public static final String CHANNEL = "Votifier";

    public RedisListener(){
        RedisManager.getAPI().subscribe(CHANNEL);
    }

    @EventHandler
    public void onRedisMessageReceived(RedisMessageReceivedEvent event) {
        String channel = event.getChannel();
        if(!channel.equals(CHANNEL))
            return;
        handleVote(event.getMessageObject(Vote.class));
    }

}
