package io.minimum.minecraft.superbvoteplus.votes.rewards.matchers;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public interface RewardMatcherFactory {
    Optional<RewardMatcher> create(ConfigurationSection section);
}
