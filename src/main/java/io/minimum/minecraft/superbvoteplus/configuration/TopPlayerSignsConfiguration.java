package io.minimum.minecraft.superbvoteplus.configuration;

import io.minimum.minecraft.superbvoteplus.configuration.message.PlainStringMessage;
import lombok.Value;

import java.util.List;

@Value
public class TopPlayerSignsConfiguration {
    private final List<PlainStringMessage> signText;
}
