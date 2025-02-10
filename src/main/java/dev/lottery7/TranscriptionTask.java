package dev.lottery7;

public class TranscriptionTask {
    private final byte[] audioData;
    private final long publishTimeStamp;

    public TranscriptionTask(byte[] audioData) {
        this.audioData = audioData;
        this.publishTimeStamp = System.currentTimeMillis();
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public long getPublishTimeStamp() {
        return publishTimeStamp;
    }
}
