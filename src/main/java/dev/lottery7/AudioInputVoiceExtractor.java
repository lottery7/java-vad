package dev.lottery7;

import dev.lottery7.util.AudioUtils;
import dev.lottery7.vad.VoiceActivityDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class AudioInputVoiceExtractor implements Runnable {
    private static final int BUFFER_SIZE = 1024;
    private static final int SILENCE_MS = 500;
    private static final Logger log = LoggerFactory.getLogger(AudioInputVoiceExtractor.class);

    private final BlockingQueue<TranscriptionTask> queue;
    private final VoiceActivityDetector detector;
    private final TargetDataLine audioInput;

    private final ByteArrayOutputStream speechStream = new ByteArrayOutputStream();

    private boolean wasSpeech = false;
    private boolean triggered = false;
    private long timeSinceLastSpeechMs = 0;

    public AudioInputVoiceExtractor(BlockingQueue<TranscriptionTask> queue,
                                    VoiceActivityDetector detector,
                                    TargetDataLine audioInput) {

        this.queue = queue;
        this.detector = detector;
        this.audioInput = audioInput;

        if (!audioInput.isOpen()) {
            throw new RuntimeException("audioInput has to be opened");
        }
    }

    @Override
    public void run() {
        try {
            while (audioInput.isOpen()) {
                listen();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void listen() throws IOException, InterruptedException {
        byte[] data = new byte[BUFFER_SIZE];

        int numBytesRead = audioInput.read(data, 0, data.length);
        if (numBytesRead <= 0) {
            log.error("Error reading data from audioInput line.");
        }

        if (detector.detect(data)) {

            if (!triggered) {
                triggered = true;
            }

            speechStream.write(data, 0, numBytesRead);

            timeSinceLastSpeechMs = 0;
            wasSpeech = true;

        } else if (wasSpeech) {

            if (timeSinceLastSpeechMs < SILENCE_MS) {
                speechStream.write(data, 0, numBytesRead);
            }

            long dt = AudioUtils.calculateDurationMillis(audioInput.getFormat(), numBytesRead);
            timeSinceLastSpeechMs += dt;

            if (timeSinceLastSpeechMs >= SILENCE_MS && timeSinceLastSpeechMs - dt < SILENCE_MS) {
                triggered = false;

                speechStream.flush();
                TranscriptionTask task = new TranscriptionTask(speechStream.toByteArray());
                queue.put(task);
                speechStream.reset();
            }

        }
    }
}
