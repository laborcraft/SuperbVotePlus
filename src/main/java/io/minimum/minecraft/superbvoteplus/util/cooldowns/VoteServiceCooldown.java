package io.minimum.minecraft.superbvoteplus.util.cooldowns;

import io.minimum.minecraft.superbvoteplus.votes.Vote;
import lombok.Value;

import java.util.UUID;

public class VoteServiceCooldown extends CooldownHandler<VoteServiceCooldown.IntData> {
    public VoteServiceCooldown(int max) {
        super(max);
    }

    public boolean triggerCooldown(Vote vote) {
        return super.triggerCooldown(new IntData(vote.getUuid(), vote.getServiceName()));
    }

    @Value
    static class IntData {
        private final UUID uuid;
        private final String serviceName;
    }
}
