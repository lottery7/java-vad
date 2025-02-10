package dev.lottery7;

import dev.lottery7.transcriber.VoiceTranscriber;
import dev.lottery7.transcriber.whisper.WhisperTranscriber;
import dev.lottery7.vad.VoiceActivityDetector;
import dev.lottery7.vad.silerovad.SileroVad;
import dev.lottery7.vad.silerovad.SileroVadOnnxModel;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.*;

public class App {
    private static final String SILERO_VAD_MODEL_PATH =
            App.class.getClassLoader().getResource("silero-vad.onnx").getPath().substring(1);

    private static final int SAMPLE_RATE = 16000;
    private static final float START_THRESHOLD = 0.5f;
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {

        System.out.println("Welcome to Realtime Voice Transcription! 🙌");
        System.out.println("Loading...");


        System.out.print("Silero VAD: ");

        VoiceActivityDetector detector;
        try {
            SileroVadOnnxModel vadModel = new SileroVadOnnxModel(SILERO_VAD_MODEL_PATH);
            detector = new SileroVad(vadModel, START_THRESHOLD, SAMPLE_RATE);
        } catch (Throwable e) {
            System.out.println("Failed");
            log.error("Error: ", e);
            return;
        }

        System.out.println("OK");


        System.out.print("Whisper: ");

        VoiceTranscriber transcriber;
        CloseableHttpClient client;
        try {
            client = HttpClients.createDefault();
            transcriber = new WhisperTranscriber("127.0.0.1", 8080, client);
        } catch (Throwable e) {
            System.out.println("Failed");
            log.error("Error: ", e);
            return;
        }

        System.out.println("OK");


        System.out.print("Microphone: ");

        AudioFormat format;
        TargetDataLine targetDataLine;
        try {
            format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        } catch (Throwable e) {
            System.out.println("Failed");
            log.error("Error: ", e);
            return;
        }

        try (client; targetDataLine; ExecutorService executorService = Executors.newFixedThreadPool(2)) {

            targetDataLine.open(format);
            targetDataLine.start();

            System.out.println("OK");


            System.out.println("Loaded successfully! 👌");
            System.out.println("You can speak now! 😁");


            BlockingQueue<TranscriptionTask> queue = new LinkedBlockingQueue<>();
            Runnable producer = new AudioInputVoiceExtractor(queue, detector, targetDataLine);
            Runnable consumer = new AudioInputVoiceTranscriber(queue, transcriber);

            executorService.submit(producer);
            executorService.submit(consumer);

            while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
            }

            executorService.shutdownNow();
        }

    }
}
