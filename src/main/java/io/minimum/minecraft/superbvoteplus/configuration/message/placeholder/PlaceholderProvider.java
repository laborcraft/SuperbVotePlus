package io.minimum.minecraft.superbvoteplus.configuration.message.placeholder;

import io.minimum.minecraft.superbvoteplus.configuration.message.MessageContext;

public interface PlaceholderProvider {
    String apply(String message, MessageContext context);

    boolean canUse();
}
