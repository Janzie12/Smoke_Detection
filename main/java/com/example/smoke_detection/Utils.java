package com.example.smoke_detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import androidx.camera.core.ImageProxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Utils {
    public static MappedByteBuffer loadModelFile(Context context, String modelFileName) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd(modelFileName).getFileDescriptor())) {
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = context.getAssets().openFd(modelFileName).getStartOffset();
            long declaredLength = context.getAssets().openFd(modelFileName).getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        Image mediaImage = image.getImage();
        if (mediaImage == null) return null;

        Image.Plane[] planes = mediaImage.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int width = mediaImage.getWidth();
        int height = mediaImage.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }
}
