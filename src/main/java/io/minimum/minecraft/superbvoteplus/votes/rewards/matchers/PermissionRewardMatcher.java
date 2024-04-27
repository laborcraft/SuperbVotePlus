package io.minimum.minecraft.superbvoteplus.votes.rewards.matchers;

import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.Vote;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

@RequiredArgsConstructor
@ToString
public class PermissionRewardMatcher implements RewardMatcher {
    static RewardMatcherFactory FACTORY = section -> {
        if (section.isString("permission")) {
            return Optional.of(new PermissionRewardMatcher(section.getString("permission")));
        }
        return Optional.empty();
    };

    private final String permission;

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        Player player = Bukkit.getPlayer(vote.getUuid());
        return player != null && player.hasPermission(permission);
    }
}
