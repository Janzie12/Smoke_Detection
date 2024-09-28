package com.example.smoke_detection;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.core.ImageProxy;
import androidx.room.Room;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.Menu;
import android.view.MenuItem;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.MimeMessage;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.PasswordAuthentication;


import javax.mail.MessagingException;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private PreviewView previewView;
    private MyModel model;
    private TextView predictionTextView;
    private EditText emailSubject, emailBody;
    private Button sendEmailButton;

    private AppDatabase db;
    private UserDao userDao;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Initialize TextView
        predictionTextView = findViewById(R.id.prediction_text);

        // Initialize PreviewView
        previewView = findViewById(R.id.view_finder);

        // Initialize email components
        emailSubject = findViewById(R.id.email_subject);
        emailBody = findViewById(R.id.email_body);
        sendEmailButton = findViewById(R.id.send_email_button);

        // Set up Send Email button functionality
        sendEmailButton.setOnClickListener(v -> {
            String subject = emailSubject.getText().toString();
            String body = emailBody.getText().toString();

            SharedPreferences sharedPreferences = getSharedPreferences("UserEmails", MODE_PRIVATE);
            String recipientEmail1 = sharedPreferences.getString("email1", "");
            String recipientEmail2 = sharedPreferences.getString("email2", "");

            MailSender mailSender = new MailSender("your_email@gmail.com", "your_password");

            try {
                executorService.execute(() -> {
                    try {
                        if (!recipientEmail1.isEmpty()) {
                            mailSender.sendEmail(recipientEmail1, subject, body);
                        }
                        if (!recipientEmail2.isEmpty()) {
                            mailSender.sendEmail(recipientEmail2, subject, body);
                        }
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Emails sent", Toast.LENGTH_SHORT).show());
                    } catch (MessagingException e) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to send email", Toast.LENGTH_SHORT).show());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error occurred", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize the database
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-database").build();
        userDao = db.userDao();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            initializeModel();
            startCamera();
        }
    }

    private void initializeModel() {
        try {
            model = new MyModel(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            Bitmap bitmap = ImageUtil.imageProxyToBitmap(image);
            if (bitmap != null) {
                float[] results = model.predict(bitmap);
                updatePredictionText(results);
            }
            image.close();
        });

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void updatePredictionText(float[] results) {
        runOnUiThread(() -> {
            if (results.length > 0) {
                String prediction = "Prediction: ";
                if (results[0] > results[1] && results[0] > results[2]) {
                    prediction += "Low Risk";
                } else if (results[1] > results[2]) {
                    prediction += "Moderate Risk";
                } else {
                    prediction += "High Risk";
                }
                predictionTextView.setText(prediction);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeModel();
                startCamera();
            } else {
                Log.e("MainActivity", "Camera permission denied");
            }
        }
    }

    private void insertUser(User user) {
        AsyncTask.execute(() -> userDao.insertUser(user));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_user_info) {
            showUserInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_user_info, null);

        TextView contactNumber = view.findViewById(R.id.contact_number);
        TextView address = view.findViewById(R.id.address);
        TextView email = view.findViewById(R.id.email);
        Button settingsButton = view.findViewById(R.id.settings_button);
        Button closeButton = view.findViewById(R.id.close_button);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-database").build();
        UserDao userDao = db.userDao();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            User user = userDao.getUser();
            runOnUiThread(() -> {
                if (user != null) {
                    contactNumber.setText("Contact: " + user.getPhoneNumber());
                    address.setText("Address: " + user.getAddress());
                    email.setText("Email: " + user.getEmail());
                }
            });
        });

        settingsButton.setOnClickListener(v -> showEmailSettingsDialog());

        builder.setView(view);
        AlertDialog dialog = builder.create();
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEmailSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_email_settings, null);

        EditText email1 = view.findViewById(R.id.email1);
        EditText email2 = view.findViewById(R.id.email2);
        EditText email3 = view.findViewById(R.id.email3);
        EditText email4 = view.findViewById(R.id.email4);
        Button saveButton = view.findViewById(R.id.save_button);
        Button cancelButton = view.findViewById(R.id.cancel_button);

        SharedPreferences sharedPreferences = getSharedPreferences("UserEmails", MODE_PRIVATE);
        email1.setText(sharedPreferences.getString("email1", ""));
        email2.setText(sharedPreferences.getString("email2", ""));
        email3.setText(sharedPreferences.getString("email3", ""));
        email4.setText(sharedPreferences.getString("email4", ""));

        AlertDialog dialog = builder.setView(view).create();

        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("email1", email1.getText().toString());
            editor.putString("email2", email2.getText().toString());
            editor.putString("email3", email3.getText().toString());
            editor.putString("email4", email4.getText().toString());
            editor.apply();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static class ImageUtil {
        public static Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            if (image == null) return null;

            YuvImage yuvImage = new YuvImage(convertYUV420888ToNV21(imageProxy), ImageFormat.NV21,
                    image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }

        private static byte[] convertYUV420888ToNV21(ImageProxy image) {
            int width = image.getWidth();
            int height = image.getHeight();

            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            int ySize = yBuffer.remaining();

            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int yRowStride = planes[0].getRowStride();
            int uvRowStride = planes[1].getRowStride();
            int uvPixelStride = planes[1].getPixelStride();

            byte[] nv21 = new byte[width * height * 3 / 2];

            // Copy Y data to NV21
            int pos = 0;
            for (int row = 0; row < height; row++) {
                yBuffer.position(row * yRowStride);
                yBuffer.get(nv21, pos, width);
                pos += width;
            }

            int uvHeight = height / 2;
            for (int row = 0; row < uvHeight; row++) {
                int uvPos = width * height + row * width;
                uBuffer.position(row * uvRowStride);
                vBuffer.position(row * uvRowStride);

                for (int col = 0; col < width / 2; col++) {
                    nv21[uvPos++] = vBuffer.get(col * uvPixelStride);
                    nv21[uvPos++] = uBuffer.get(col * uvPixelStride);
                }
            }

            return nv21;
        }
    }
        // Utility class for sending emails using Gmail's SMTP server
    private static class MailSender {
        private final String email;
        private final String password;

        public MailSender(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public void sendEmail(String recipient, String subject, String body) throws MessagingException {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}