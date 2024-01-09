package com.example.researchai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.Manifest;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.example.researchai.ml.Mobilenetv2model;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AnophelesClassificationActivity extends AppCompatActivity {


    private Button btnLoadFromGallery,btnPredict,btnCapture;
    private TextView tvPredictionResult;
    private ImageView imageViewTv;
    Bitmap bitmap;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private int imageSize = 160;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anopheles_classification);

        btnLoadFromGallery = findViewById(R.id.btnLoadFromGallery);
        btnPredict = findViewById(R.id.btnPredict);
        btnCapture = findViewById(R.id.btnCapture);
        tvPredictionResult = findViewById(R.id.tvPredictionResult);
        imageViewTv = findViewById(R.id.imageViewTv);

        btnLoadFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();

            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkCameraPermission()) {
                    dispatchTakePictureIntent();
                } else {
                    requestCameraPermission();
                }
            }
        });


//        btnPredict.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    Mobilenetv2model model = Mobilenetv2model.newInstance(AnophelesClassificationActivity.this);
//
//                    // Ensure the bitmap is not null before processing
//                    if (bitmap != null) {
//                        // Creates inputs for reference.
//                        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 160, 160, 3}, DataType.FLOAT32);
//                        bitmap = Bitmap.createScaledBitmap(bitmap, 160, 160, true);
//                        inputFeature0.loadBuffer(TensorImage.fromBitmap(bitmap).getBuffer());
//
//
//                        // Runs model inference and gets result.
//                        Mobilenetv2model.Outputs outputs = model.process(inputFeature0);
//                        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
//
//                        tvPredictionResult.setText(getMax(outputFeature0.getFloatArray()) + "");
//                    } else {
//                        // Handle the case where the bitmap is null (e.g., image loading failed)
//                        tvPredictionResult.setText("Error: Bitmap is null");
//                        Log.e("BitmapLoad", "Error: Bitmap is null");
//                    }
//
//                    // Releases model resources if no longer used.
//                    model.close();
//                } catch (IOException e) {
//                    // Handle the exception
//                    e.printStackTrace();
//                }
//            }
//        });


    }

    int getMax(float[] arr){
        int max = 0;
        for(int i =0;i<arr.length;i++){
            if(arr[i] >arr[max]) max=i;
        }
        return max;
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    // Check if camera permission is granted
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }



    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public void classifyImage(Bitmap imageBitmap){

        try {
            Mobilenetv2model model = Mobilenetv2model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 160, 160, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*imageSize*imageSize*3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            imageBitmap.getPixels(intValues,0,imageBitmap.getWidth(),0,0,imageBitmap.getWidth(),imageBitmap.getHeight());
            int pixel =0;

            for (int i=0;i<imageSize;i ++){
                for(int j = 0;j<imageSize;j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Mobilenetv2model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            float confidence = confidences[0];  // Assuming a single output for binary classification

            // You can set a threshold based on your needs
            double confidenceThreshold = 0.5;

            if (confidence > confidenceThreshold) {
                // The model predicts the positive class
                tvPredictionResult.setText(R.string.male);
            } else {
                // The model predicts the negative class
                tvPredictionResult.setText("Female");
            }


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // If the image was captured using the camera
            assert data != null;
            Bundle extras = data.getExtras();
            assert extras != null;
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            assert imageBitmap != null;
            int dimension = Math.min(imageBitmap.getWidth(),imageBitmap.getHeight());
            imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap,dimension,dimension);
            saveImageToFirebaseStorage(imageBitmap);
            imageViewTv.setImageBitmap(imageBitmap);

            imageBitmap = Bitmap.createScaledBitmap(imageBitmap,imageSize,imageSize,false);

            classifyImage(imageBitmap);

        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // If an image was picked from the gallery
            Uri selectedImageUri = data.getData();
            Bitmap imageBitmap = null;
            try {
                // Convert the selected image URI to a Bitmap and set it to the ImageView
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imageViewTv.setImageBitmap(imageBitmap);

                imageBitmap = Bitmap.createScaledBitmap(imageBitmap,imageSize,imageSize,false);
                classifyImage(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the camera
                dispatchTakePictureIntent();
            } else {
                // Permission denied, show a message or take appropriate action
                showToast("Camera permission denied. Cannot capture images.");
            }
        }
    }
    // Helper method to show a Toast message
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveImageToFirebaseStorage(Bitmap imageBitmap) {
        // Create a reference to the Firebase Storage location
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("Camera/"); // You can change "images" to your desired path

        // Create a reference to the file you want to upload
        StorageReference imageRef = storageRef.child("arabiensis.jpg"); // You can change "your_image.jpg" to your desired file name

        // Convert bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Upload the image
        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Image upload successful, now you can load the image into the ImageView
                loadImageFromFirebaseStorage(imageRef);
            } else {
                // Image upload failed, handle the error
                showToast("Failed to upload image to Firebase Storage");
            }
        });
    }

    private void loadImageFromFirebaseStorage(StorageReference imageRef) {
        // Download the image from Firebase Storage and load it into the ImageView
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Use an image loading library like Glide or Picasso to load the image into the ImageView
            Glide.with(this).load(uri).into(imageViewTv);
        }).addOnFailureListener(exception -> {
            // Handle failures
            showToast("Failed to load image from Firebase Storage");
        });
    }

}