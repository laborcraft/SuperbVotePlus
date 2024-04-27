package io.minimum.minecraft.superbvoteplus.migration;

public interface ProgressListener {
    void onStart(int records);

    void onRecordBatch(int num, int total);

    void onFinish(int records);
}
