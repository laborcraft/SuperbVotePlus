package io.minimum.minecraft.superbvoteplus.configuration;

import io.minimum.minecraft.superbvoteplus.configuration.message.OfflineVoteMessage;
import lombok.Data;

@Data
public class TextLeaderboardConfiguration {
    private final int perPage;
    private final OfflineVoteMessage header;
    private final OfflineVoteMessage entryText;
    private final OfflineVoteMessage pageNumberText;
}
