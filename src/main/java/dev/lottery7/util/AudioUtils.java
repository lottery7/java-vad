package dev.lottery7.util;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
        outputFile.getParentFile().mkdirs();
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
}
