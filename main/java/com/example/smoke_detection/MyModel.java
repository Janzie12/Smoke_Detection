package com.example.smoke_detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.ByteOrder;
import android.content.res.AssetFileDescriptor;

public class MyModel {

    private final Interpreter interpreter;
    private final int inputSize = 128; // Set to 128 for your model's input size
    private final int numClasses = 3;  // Adjust based on your model output classes

    public MyModel(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context);
        interpreter = new Interpreter(modelBuffer);
    }

    public float[] predict(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Resize bitmap to match the model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind(); // Ensure the buffer is in a clean state

        int[] intValues = new int[inputSize * inputSize];
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : intValues) {
            float r = Color.red(pixel) / 255.0f;
            float g = Color.green(pixel) / 255.0f;
            float b = Color.blue(pixel) / 255.0f;
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }

        // Ensure output shape matches the model's output
        float[][] output = new float[1][numClasses];
        interpreter.run(inputBuffer, output);

        return output[0];
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("fire_prediction_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getLength();
        return fileChannel.map(MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
