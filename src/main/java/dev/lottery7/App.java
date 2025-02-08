package dev.lottery7;

import dev.lottery7.silerovad.SileroVadDetector;
import dev.lottery7.silerovad.SileroVadOnnxModel;
import dev.lottery7.util.AudioUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App {
    private static final String MODEL_PATH = "models/silero_vad.onnx";
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 1024;
    private static final float START_THRESHOLD = 0.5f;
    private static final int SILENCE_MS = 500;

    public static void main(String[] args) throws Exception {
        SileroVadOnnxModel model = new SileroVadOnnxModel(MODEL_PATH);
        SileroVadDetector detector = new SileroVadDetector(model, START_THRESHOLD, SAMPLE_RATE);

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        TargetDataLine targetDataLine;
        targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        targetDataLine.open(format);
        targetDataLine.start();

        boolean wasSpeech = false;
        boolean triggered = false;
        long timeSinceLastSpeech = 0;

        ByteArrayOutputStream speechStream = new ByteArrayOutputStream();

        int numSpeech = 0;

        ExecutorService executorService = Executors.newCachedThreadPool();

        List<Future<?>> futures = new ArrayList<>();

        while (targetDataLine.isOpen()) {
            byte[] data = new byte[BUFFER_SIZE];

            int numBytesRead = targetDataLine.read(data, 0, data.length);
            if (numBytesRead <= 0) {
                System.err.println("Error reading data from target data line.");
                continue;
            }

            boolean speechDetected = detector.apply(data);

            if (speechDetected) {
                if (!triggered) {
                    triggered = true;
                    System.out.println("Speech started");
                }
                speechStream.write(data, 0, numBytesRead);

                timeSinceLastSpeech = 0;
                wasSpeech = true;

            } else if (wasSpeech) {
                if (timeSinceLastSpeech < SILENCE_MS) {
                    speechStream.write(data, 0, numBytesRead);
                }


                long dt = AudioUtils.calculateDurationMillis(format, numBytesRead);
                timeSinceLastSpeech += dt;

                if (timeSinceLastSpeech >= SILENCE_MS && timeSinceLastSpeech - dt < SILENCE_MS) {
                    System.out.println("Speech ended");
                    triggered = false;

                    speechStream.flush();
                    byte[] speechBytes = speechStream.toByteArray();
                    String speechPath = String.format("audio/speech%d.wav", numSpeech);

                    futures.add(executorService.submit(() -> {
                        AudioUtils.writeAudioToFile(format, speechBytes, Path.of(speechPath));
                        return "Success";
                    }));

                    speechStream.reset();

                    numSpeech += 1;
                }
            }
        }

        for (var future : futures) {
            future.get();
        }

        targetDataLine.close();
    }
}
