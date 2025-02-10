package dev.lottery7.transcriber.whisper;

import dev.lottery7.transcriber.VoiceTranscriber;
import dev.lottery7.util.AudioUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class WhisperTranscriber implements VoiceTranscriber {
    private static final Logger log = LoggerFactory.getLogger(WhisperTranscriber.class);
    private final HttpClient client;
    private final String url;

    public WhisperTranscriber(String host, int port, HttpClient client) {
        this.client = client;
        url = String.format("http://%s:%d", host, port);
    }

    @Override
    public Optional<String> transcribe(byte[] audioData) {
        byte[] requestData = AudioUtils.toWavFormat(audioData, 16000, 1, 16);

        HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", requestData, ContentType.create("audio/wav"), "request-file.wav")
                .addTextBody("response_format", "text")
                .addTextBody("temperature", "0.5")
                .build();

        HttpPost httpPost = new HttpPost(url + "/inference");
        httpPost.setEntity(httpEntity);

        try {
            String text = client.execute(httpPost, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            return Optional.of(text.trim());
        } catch (IOException e) {
            log.error("Error happened during http request: ", e);
            return Optional.empty();
        }
    }
}
