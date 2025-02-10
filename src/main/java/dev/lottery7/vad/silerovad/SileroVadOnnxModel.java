package dev.lottery7.vad.silerovad;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dev.lottery7.util.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

public class SileroVadOnnxModel implements AutoCloseable {
    private final OrtSession session;

    private float[][][] state;
    private float[][] context;

    private int lastSampleRate = 0;
    private int lastBatchSize = 0;

    public SileroVadOnnxModel(String modelPath) throws OrtException {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();

        opts.setInterOpNumThreads(1);
        opts.setIntraOpNumThreads(1);
        opts.addCPU(true);

        session = env.createSession(modelPath, opts);

        resetStates();
    }

    void resetStates() {
        state = new float[2][1][128];
        context = null;
        lastSampleRate = 0;
        lastBatchSize = 0;
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }

    public float[] call(ModelInput modelInput) throws OrtException {
        float[][] audioBytes = modelInput.audioBytes;
        int sampleRate = modelInput.sampleRate;
        int batchSize = audioBytes.length;
        int contextSize = sampleRate == 8000 ? 32 : 64;

        if (lastSampleRate != sampleRate || lastBatchSize != batchSize) {
            resetStates();
            context = new float[batchSize][contextSize];
        }

        float[][] input = ArrayUtils.concatenate(context, audioBytes);
        OrtEnvironment env = OrtEnvironment.getEnvironment();

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, input);
             OnnxTensor stateTensor = OnnxTensor.createTensor(env, state);
             OnnxTensor sampleRateTensor = OnnxTensor.createTensor(env, new long[]{sampleRate})) {

            Map<String, OnnxTensor> sessionInput = new HashMap<>();
            sessionInput.put("input", inputTensor);
            sessionInput.put("sr", sampleRateTensor);
            sessionInput.put("state", stateTensor);

            OrtSession.Result ortResult = session.run(sessionInput);
            float[][] result = (float[][]) ortResult.get(0).getValue();
            state = (float[][][]) ortResult.get(1).getValue();

            context = ArrayUtils.getLastColumns(input, contextSize);
            lastSampleRate = sampleRate;
            lastBatchSize = batchSize;

            return result[0];
        }
    }

    public static class ModelInput {
        public final float[][] audioBytes;
        public final int sampleRate;

        public ModelInput(float[][] audioBytes, int sampleRate) {
            this.audioBytes = audioBytes;
            this.sampleRate = sampleRate;
        }
    }
}