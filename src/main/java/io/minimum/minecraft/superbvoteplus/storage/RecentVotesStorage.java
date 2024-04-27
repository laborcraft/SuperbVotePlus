package io.minimum.minecraft.superbvoteplus.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RecentVotesStorage {

    private final LoadingCache<UUID, UUID> lastVotes = CacheBuilder.newBuilder()
            .expireAfterWrite(SuperbVotePlus.getPlugin().getConfig().getInt("broadcast.antispam.time", 120), TimeUnit.SECONDS)
            .build(new CacheLoader<UUID, UUID>() {
                @Override
                public UUID load(UUID uuid) {
                    return uuid;
                }
            });

    public boolean canBroadcast(UUID uuid) {
        if (!SuperbVotePlus.getPlugin().getConfig().getBoolean("broadcast.antispam.enabled")) return true;
        return lastVotes.getIfPresent(uuid) != null;
    }

    public void updateLastVote(UUID uuid) {
        lastVotes.put(uuid, uuid);
    }
}
