package com.example.researchai;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.researchai.ml.AutoModel1;
import com.example.researchai.ml.Mobilenetv2model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class GambieObjectDEActivity extends AppCompatActivity {

    private Paint paint = new Paint();
    private ImageView imageViewTv;
    private Button btnCapture,btnLoadFromGallery,btnRealTime;
    private Bitmap bitmap;
    private AutoModel1 model;
    private List<String> labels;
    private ImageProcessor imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();

    private List<Integer> colors = List.of(Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED);

    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private int imageSize = 300;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gambie_object_deactivity);

        try {
            model = AutoModel1.newInstance(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            labels = FileUtil.loadLabels(this,"labels.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        btnCapture = findViewById(R.id.btnCapture);
        btnLoadFromGallery = findViewById(R.id.btnLoadFromGallery);
        btnRealTime = findViewById(R.id.btnRealTime);
        imageViewTv = findViewById(R.id.imageViewTv);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        btnLoadFromGallery.setOnClickListener(v -> startActivityForResult(intent, 101));


        onclickListener();





    }

    private void onclickListener(){
        btnRealTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GambieObjectDEActivity.this, GambieRealtimeOBActivity.class));
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
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode==101 && resultCode == RESULT_OK && data != null){
//            Uri uri = data.getData();
//            try {
//                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                getPredictions();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // If the image was captured using the camera
            assert data != null;
            Bundle extras = data.getExtras();
            assert extras != null;
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            assert imageBitmap != null;
            int dimension = Math.min(imageBitmap.getWidth(),imageBitmap.getHeight());
            imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap,dimension,dimension);
//            saveImageToFirebaseStorage(imageBitmap);
            imageViewTv.setImageBitmap(imageBitmap);

            imageBitmap = Bitmap.createScaledBitmap(imageBitmap,imageSize,imageSize,false);

            getPredictions(imageBitmap);

        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // If an image was picked from the gallery
            Uri selectedImageUri = data.getData();
            Bitmap imageBitmap = null;
            try {
                // Convert the selected image URI to a Bitmap and set it to the ImageView
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imageViewTv.setImageBitmap(imageBitmap);

                imageBitmap = Bitmap.createScaledBitmap(imageBitmap,imageSize,imageSize,false);
                getPredictions(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
    }

    private void getPredictions(Bitmap bitmap){
        TensorImage image = TensorImage.fromBitmap(bitmap);
        image = imageProcessor.process(image);

        // Runs model inference and gets result.
        AutoModel1.Outputs outputs = model.process(image);

        float[] locations = outputs.getLocationsAsTensorBuffer().getFloatArray();
        float[] classes = outputs.getClassesAsTensorBuffer().getFloatArray();
        float[] scores = outputs.getScoresAsTensorBuffer().getFloatArray();
        float[] numberOfDetections = outputs.getNumberOfDetectionsAsTensorBuffer().getFloatArray();

        Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        int h = mutable.getHeight();
        int w = mutable.getWidth();

        paint.setTextSize(h / 15f);
        paint.setStrokeWidth(h / 85f);

        for (int index = 0; index < scores.length; index++) {
            if (scores[index] > 0.5) {
                int x = index * 4;
                paint.setColor(colors.get(index));
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(new RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(labels.get((int) classes[index]) + " " + scores[index], locations[x + 1] * w, locations[x] * h, paint);
            }
        }

        imageViewTv.setImageBitmap(mutable);
    }
}