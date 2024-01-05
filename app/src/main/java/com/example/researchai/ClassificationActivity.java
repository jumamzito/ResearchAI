package com.example.researchai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ClassificationActivity extends AppCompatActivity {

    private CardView arabiensisCardTv,gambieCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classification);

        arabiensisCardTv = findViewById(R.id.arabiensisCardTv);
        gambieCard = findViewById(R.id.gambieCard);

        clickListener();
    }

    private void clickListener(){

        arabiensisCardTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ClassificationActivity.this,AnophelesClassificationActivity.class));
                finish();
            }
        });

        gambieCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ClassificationActivity.this,GambieActivity.class));
                finish();
            }
        });


    }
}