package io.minimum.minecraft.superbvoteplus.configuration;

import io.minimum.minecraft.superbvoteplus.commands.CommonCommand;
import io.minimum.minecraft.superbvoteplus.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvoteplus.configuration.message.VoteMessages;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

@Getter
public class LangConfiguration extends CustomConfigFile {

    private final TabExecutor voteCommand, voteStreakCommand;
    private final VoteMessage reminderMessage;
    private final Component noRewards;

    public LangConfiguration(String fileName) throws IllegalArgumentException {
        super("lang/"+fileName+".yml");

        ConfigurationSection commandSection = config.getConfigurationSection("commands");
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");

        VoteMessage voteMessage = VoteMessages.from(commandSection, "vote", false);
        voteCommand = new CommonCommand(voteMessage, false);

        VoteMessage voteStreakMessage = VoteMessages.from(commandSection, "streak", false);
        voteStreakCommand = new CommonCommand(voteStreakMessage, false);

        reminderMessage = VoteMessages.from(messagesSection, "vote-reminder");

        noRewards = MiniMessage.miniMessage().deserialize(messagesSection.getString("no-rewards"));
    }

    public Map<String,String> getCommandNames(){
        ConfigurationSection section = config.getConfigurationSection("command-names");
        Map<String,String> map = new HashMap<>();
        for(String key : section.getKeys(false))
            map.put(key, section.getString(key));
        return map;
    }

}
