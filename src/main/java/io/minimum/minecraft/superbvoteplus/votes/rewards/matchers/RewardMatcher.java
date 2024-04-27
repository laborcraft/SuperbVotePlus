package io.minimum.minecraft.superbvoteplus.votes.rewards.matchers;

import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.Vote;

public interface RewardMatcher {
    boolean matches(Vote vote, PlayerVotes pv);
}
