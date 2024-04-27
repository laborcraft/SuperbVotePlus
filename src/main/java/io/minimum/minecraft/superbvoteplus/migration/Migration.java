package io.minimum.minecraft.superbvoteplus.migration;

public interface Migration {
    String getName();

    void execute(ProgressListener listener);
}
