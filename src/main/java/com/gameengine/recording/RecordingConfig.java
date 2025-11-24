package com.gameengine.recording;

public class RecordingConfig {
    public String outputPath;
    public float keyframeIntervalSec = 0.5f;
    public int sampleFps = 30;
    public float positionThreshold = 0.5f; // pixels
    public int quantizeDecimals = 2;
    public int queueCapacity = 2048;

    public RecordingConfig(String outputPath) {
        this.outputPath = outputPath;
    }
}
