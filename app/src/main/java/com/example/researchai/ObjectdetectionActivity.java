package com.example.researchai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ObjectdetectionActivity extends AppCompatActivity {

    private CardView arabiensisObjectDetection,gambieObjectDetection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objectdetection);

        arabiensisObjectDetection = findViewById(R.id.arabiensisObjectDetection);
        gambieObjectDetection = findViewById(R.id.gambieObjectDetection);

        clickListener();
    }

    private void clickListener(){

        arabiensisObjectDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ObjectdetectionActivity.this, ArabiensisObjectedetectionActivity.class));
//                finish();
            }
        });

        gambieObjectDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ObjectdetectionActivity.this,GambieActivity.class));
//                finish();
            }
        });


    }
}