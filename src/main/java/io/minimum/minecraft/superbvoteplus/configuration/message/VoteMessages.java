package io.minimum.minecraft.superbvoteplus.configuration.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VoteMessages {
    public static VoteMessage from(ConfigurationSection root, String section) {
        return from(root, section, false);
    }

    public static VoteMessage from(ConfigurationSection root, String section, boolean optional) {
        if (root.contains(section)) {
            if (root.isString(section)) {
                String message = root.getString(section);
                return new MiniMessageMessage(message);
            }
        } else if (optional) {
            return null;
        }

        throw new IllegalArgumentException("Section '" + section + "' (under " + root.getCurrentPath() + ") doesn't contain a valid message section.");
    }
}
