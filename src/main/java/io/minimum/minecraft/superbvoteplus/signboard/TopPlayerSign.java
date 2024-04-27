package io.minimum.minecraft.superbvoteplus.signboard;

import io.minimum.minecraft.superbvoteplus.util.SerializableLocation;
import lombok.Data;

@Data
public class TopPlayerSign {
    private final SerializableLocation sign;
    private final int position;
}
