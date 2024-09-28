package com.example.smoke_detection;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SmokeDetectionModel {
    private Interpreter interpreter;

    public SmokeDetectionModel(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd("smoke_detection_model.tflite").getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = context.getAssets().openFd("smoke_detection_model.tflite").getStartOffset();
        long declaredLength = context.getAssets().openFd("smoke_detection_model.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String predict(float[][][][] input) {
        float[][] output = new float[1][1];
        interpreter.run(input, output);
        return output[0][0] > 0.5 ? "smoke" : "no_smoke";
    }
}
