package com.example.researchai;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import com.example.researchai.ml.AutoModel1;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.List;

public class ArabiensisObjectedetectionActivity extends AppCompatActivity {

    private Paint paint = new Paint();
    private ImageView imageViewTv;
    private Button btnCapture,btnLoadFromGallery;
    private Bitmap bitmap;
    private AutoModel1 model;
    private List<String> labels;
    private ImageProcessor imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();

    private List<Integer> colors = List.of(Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arabiensis_objectedetection);

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
        imageViewTv = findViewById(R.id.imageViewTv);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        btnLoadFromGallery.setOnClickListener(v -> startActivityForResult(intent, 101));



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==101 && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                getPredictions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
    }

    private void getPredictions(){
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