package dev.lottery7;

import dev.lottery7.transcriber.VoiceTranscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

public class AudioInputVoiceTranscriber implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(AudioInputVoiceTranscriber.class);
    private final BlockingQueue<TranscriptionTask> queue;
    private final VoiceTranscriber transcriber;

    public AudioInputVoiceTranscriber(BlockingQueue<TranscriptionTask> queue, VoiceTranscriber transcriber) {
        this.queue = queue;
        this.transcriber = transcriber;
    }

    @Override
    public void run() {
        try {
            while (true) {
                TranscriptionTask task = queue.take();

                Optional<String> transcription;
                try {
                    transcription = transcriber.transcribe(task.getAudioData());
                } catch (Throwable e) {
                    log.error("Failed to transcribe: ", e);
                    continue;
                }

                if (transcription.isEmpty()) {
                    log.error("Failed to transcribe: Transcriber error");
                } else {
                    String text = transcription.get().trim();
                    if (text.isEmpty()) {
                        log.info("Empty transcription, skip");
                    } else {
                        System.out.println(text);
                    }
                }

                long executionTime = System.currentTimeMillis() - task.getPublishTimeStamp();
                log.info("Transcription latency: {} ms", executionTime);
            }
        } catch (InterruptedException ignored) {
        }
    }
}
