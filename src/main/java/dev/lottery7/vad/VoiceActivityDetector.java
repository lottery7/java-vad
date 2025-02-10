package dev.lottery7.vad;

public interface VoiceActivityDetector {
    boolean detect(byte[] audioBytes);
}
