package com.example.researchai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import com.example.researchai.ml.AutoModel1;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.List;

public class GambieRealtimeOBActivity extends AppCompatActivity {

    private List<Integer> colors = List.of(
            Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED);
    private Paint paint = new Paint();

    private List<String> labels;

    private TextureView textureViewTv;
    private ImageView imageViewTv;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private Handler handler;
    private Bitmap bitmap;
    private AutoModel1 model;
    private ImageProcessor imageProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gambie_realtime_obactivity);

        getPermission();

        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try {
            model = AutoModel1.newInstance(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();
        HandlerThread handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());


        textureViewTv = findViewById(R.id.textureViewTv);
        imageViewTv = findViewById(R.id.imageViewTv);

        textureViewTv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                bitmap = textureViewTv.getBitmap();



                // Creates inputs for reference.
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
                int x = 0;
                for (int index = 0; index < scores.length; index++) {
                    x = index * 4;
                    if (scores[index] > 0.5) {
                        paint.setColor(colors.get(index));
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawRect(new RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint);
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawText(labels.get((int) classes[index]) + " " + scores[index], locations[x + 1] * w, locations[x] * h, paint);
                    }
                }

                imageViewTv.setImageBitmap(mutable);


            }
        });

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
    }

    @SuppressLint("MissingPermission")
    private void openCamera(){

        try {
            cameraManager.openCamera(cameraManager.getCameraIdList()[0],new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            cameraDevice = camera;

                            SurfaceTexture surfaceTexture = textureViewTv.getSurfaceTexture();
                            Surface surface = new Surface(surfaceTexture);

                            CaptureRequest.Builder captureRequestBuilder = null;
                            try {
                                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            } catch (CameraAccessException e) {
                                throw new RuntimeException(e);
                            }
                            captureRequestBuilder.addTarget(surface);

                            CaptureRequest.Builder finalCaptureRequestBuilder = captureRequestBuilder;
                            try {
                                cameraDevice.createCaptureSession(List.of(surface), new CameraCaptureSession.StateCallback() {
                                            @Override
                                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                                try {
                                                    session.setRepeatingRequest(finalCaptureRequestBuilder.build(),null,null);
                                                } catch (CameraAccessException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }

                                            @Override
                                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                                            }
                                        },handler
                                );
                            } catch (CameraAccessException e) {
                                throw new RuntimeException(e);
                            }


                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {

                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {

                        }
                    },
                    handler);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission();
        }
    }
}