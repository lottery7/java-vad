package dev.lottery7.transcriber;

import java.util.Optional;

public interface VoiceTranscriber {
    Optional<String> transcribe(byte[] audioData);
}
