package io.minimum.minecraft.superbvoteplus.storage;

import io.minimum.minecraft.superbvoteplus.util.PlayerVotes;
import io.minimum.minecraft.superbvoteplus.votes.VoteStreak;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExtendedVoteStorage extends VoteStorage {
    VoteStreak getVoteStreak(UUID player, boolean required);

    List<Map.Entry<PlayerVotes, VoteStreak>> getAllPlayersAndStreaksWithNoVotesToday(List<UUID> onlinePlayers);
}
