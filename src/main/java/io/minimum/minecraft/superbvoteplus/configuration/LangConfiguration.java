package io.minimum.minecraft.superbvoteplus.configuration;

import io.minimum.minecraft.superbvoteplus.commands.CommonCommand;
import io.minimum.minecraft.superbvoteplus.commands.MessageCommand;
import io.minimum.minecraft.superbvoteplus.configuration.message.VoteMessage;
import io.minimum.minecraft.superbvoteplus.configuration.message.VoteMessages;
import lombok.Getter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

@Getter
public class LangConfiguration extends CustomConfigFile {

    private final TabExecutor voteCommand, voteStreakCommand;
    private final VoteMessage reminderMessage;

    public LangConfiguration(String fileName) throws IllegalArgumentException {
        super("lang/"+fileName+".yml");

        voteCommand = new MessageCommand(
                config.getString("commands.vote")
        );
        voteStreakCommand = new MessageCommand(
                config.getString("commands.streak")
        );
        reminderMessage = VoteMessages.from(config, "messages.vote-reminder");
    }

    public Map<String,String> getCommandNames(){
        ConfigurationSection section = config.getConfigurationSection("command-names");
        Map<String,String> map = new HashMap<>();
        for(String key : section.getKeys(false))
            map.put(key, section.getString(key));
        return map;
    }

}
