package io.minimum.minecraft.superbvoteplus.configuration.message;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class MiniMessageMessage extends MessageBase implements VoteMessage {
    private final String message;

    private static final MiniMessage mm = MiniMessage.miniMessage();
    public MiniMessageMessage(String miniMessage) {
        this.message = miniMessage;
    }


    @Override
    public void sendAsBroadcast(Player player, MessageContext context) {
        player.sendMessage(mm.deserialize(replace(message, context)));
    }

    @Override
    public void sendAsReminder(Player player, MessageContext context) {
        player.sendMessage(mm.deserialize(replace(message, context)));
    }
}
