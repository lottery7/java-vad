package dev.lottery7.silerovad;

import ai.onnxruntime.OrtException;

public class SileroVadDetector implements AutoCloseable {
    private final SileroVadOnnxModel model;
    private final float speechThreshold;
    private final int sampleRate;

    public SileroVadDetector(SileroVadOnnxModel model, float speechThreshold, int sampleRate) {
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
        int n = audioBytes.length / 2;
        float[][] audioData = new float[1][n];
        for (int i = 0; i < n; i++) {
            audioData[0][i] = ((audioBytes[i * 2] & 0xff) | (audioBytes[i * 2 + 1] << 8)) / 32767.0f;
        }
        return audioData;
    }

    private SileroVadOnnxModel.ModelInput prepareInput(byte[] audioBytes) {
        float[][] audioData = prepareAudioBytes(audioBytes);
        assert audioData.length == 1;
        return new SileroVadOnnxModel.ModelInput(audioData, sampleRate);
    }

    public boolean apply(byte[] audioBytes) {
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
