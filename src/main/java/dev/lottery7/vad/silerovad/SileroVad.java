package dev.lottery7.vad.silerovad;

import ai.onnxruntime.OrtException;
import dev.lottery7.vad.VoiceActivityDetector;
import dev.lottery7.util.AudioUtils;

public class SileroVad implements AutoCloseable, VoiceActivityDetector {
    private final SileroVadOnnxModel model;
    private final float speechThreshold;
    private final int sampleRate;

    public SileroVad(SileroVadOnnxModel model, float speechThreshold, int sampleRate) {
        if (!validateSampleRate(sampleRate)) {
            throw new IllegalArgumentException("Invalid sample rate. See DOCS for more info.");
        }

        this.model = model;
        this.speechThreshold = speechThreshold;
        this.sampleRate = sampleRate;

        model.resetStates();
    }

    private boolean validateSampleRate(int sampleRate) {
        return sampleRate == 8000 || sampleRate == 16000;
    }

    private boolean validateAudioBytes(byte[] audioBytes) {
        int n = audioBytes.length;
        return n == 512 && sampleRate == 8000 || n == 1024 && sampleRate == 16000;
    }


    private float[][] prepareAudioBytes(byte[] audioBytes) {
        return new float[][]{AudioUtils.convertAudioBytesToUnitFloats(audioBytes)};
    }

    private SileroVadOnnxModel.ModelInput prepareInput(byte[] audioBytes) {
        float[][] audioData = prepareAudioBytes(audioBytes);
        assert audioData.length == 1;
        return new SileroVadOnnxModel.ModelInput(audioData, sampleRate);
    }

    @Override
    public boolean detect(byte[] audioBytes) {
        if (!validateAudioBytes(audioBytes)) {
            throw new IllegalArgumentException(
                    "Invalid input size: has to be 512 bytes for 8k SampleRate or 1024 bytes for 16k SampleRate.");
        }

        try {
            float speechProb = model.call(prepareInput(audioBytes))[0];
            return speechProb >= speechThreshold;
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws OrtException {
        model.close();
    }
}
