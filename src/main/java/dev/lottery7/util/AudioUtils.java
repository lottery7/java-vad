package dev.lottery7.util;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

public class AudioUtils {
    public static long calculateDurationMillis(AudioFormat format, long numBytes) {
        int frameSize = format.getFrameSize();
        float frameRate = format.getFrameRate();
        long numFrames = numBytes / frameSize;
        return (long) (numFrames / frameRate * 1000);
    }

    public static void writeAudioToFile(AudioFormat format, byte[] audioData, Path filePath) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = new AudioInputStream(
                byteArrayInputStream, format, audioData.length / format.getFrameSize());

        File outputFile = filePath.toFile();
        AudioSystem.write(audioInputStream, getFileType(filePath), outputFile);
    }

    private static AudioFileFormat.Type getFileType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        switch (fileExtension) {

            case "wav" -> {
                return AudioFileFormat.Type.WAVE;
            }

            case "aiff" -> {
                return AudioFileFormat.Type.AIFF;
            }

            case "au" -> {
                return AudioFileFormat.Type.AU;
            }

            default -> throw new RuntimeException("Unsupported file extension: " + fileExtension);

        }
    }

    public static float[] convertAudioBytesToUnitFloats(byte[] audioBytes) {
        int n = audioBytes.length / 2;
        float[] audioData = new float[n];
        for (int i = 0; i < n; i++) {
            audioData[i] = ((audioBytes[2 * i] & 0xff) | (audioBytes[2 * i + 1] << 8)) / 32767.0f;
        }
        return audioData;
    }

    public static byte[] toWavFormat(byte[] rawAudio, int sampleRate, int numChannels, int bitsPerSample) {
        int byteRate = sampleRate * numChannels * (bitsPerSample / 8);
        int subchunk2Size = rawAudio.length;
        int chunkSize = 36 + subchunk2Size;

        ByteBuffer buffer = ByteBuffer.allocate(44 + rawAudio.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // WAV Header
        buffer.put("RIFF".getBytes()); // ChunkID
        buffer.putInt(chunkSize); // ChunkSize
        buffer.put("WAVE".getBytes()); // Format
        buffer.put("fmt ".getBytes()); // Subchunk1ID
        buffer.putInt(16); // Subchunk1Size (PCM)
        buffer.putShort((short) 1); // AudioFormat (PCM)
        buffer.putShort((short) numChannels); // NumChannels
        buffer.putInt(sampleRate); // SampleRate
        buffer.putInt(byteRate); // ByteRate
        buffer.putShort((short) (numChannels * (bitsPerSample / 8))); // BlockAlign
        buffer.putShort((short) bitsPerSample); // BitsPerSample
        buffer.put("data".getBytes()); // Subchunk2ID
        buffer.putInt(subchunk2Size); // Subchunk2Size

        // Append raw audio data
        buffer.put(rawAudio);

        return buffer.array();
    }
}
