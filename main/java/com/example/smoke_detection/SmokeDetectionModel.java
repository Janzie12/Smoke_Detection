package com.example.smoke_detection;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteOrder;


public class SmokeDetectionModel {
    private static final String TAG = "SmokeDetectionModel";
    private final Interpreter interpreter;

    public SmokeDetectionModel(Context context) throws IOException {
        Log.d(TAG, "SmokeDetectionModel: Loading model file");
        interpreter = new Interpreter(loadModelFile(context));
        Log.d(TAG, "SmokeDetectionModel: Model loaded successfully");
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("smoke_detection_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public boolean predict(Bitmap bitmap) {
        // Ensure the input bitmap matches the expected input size of the model
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true);

        // Prepare the input tensor in the format expected by the model
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // Allocate space for the model's output and run inference
        float[][] output = new float[1][1];
        interpreter.run(inputBuffer, output);

        // Interpret the model's output and return the prediction
        return output[0][0] > 0.5;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int[] intValues = new int[128 * 128];
        float[][][][] input = new float[1][128][128][3];

        bitmap.getPixels(intValues, 0, 128, 0, 0, 128, 128);

        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 128; ++j) {
                int pixelValue = intValues[i * 128 + j];
                input[0][i][j][0] = Color.red(pixelValue) / 255.0f;
                input[0][i][j][1] = Color.green(pixelValue) / 255.0f;
                input[0][i][j][2] = Color.blue(pixelValue) / 255.0f;
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(128 * 128 * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 128; ++j) {
                byteBuffer.putFloat(input[0][i][j][0]);
                byteBuffer.putFloat(input[0][i][j][1]);
                byteBuffer.putFloat(input[0][i][j][2]);
            }
        }

        return byteBuffer;
    }
}
