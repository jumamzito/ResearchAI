package com.example.researchai;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SmashActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smash);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (user != null){
                    startActivity(new Intent(SmashActivity.this,MainActivity.class));
                }else{
                    startActivity(new Intent(SmashActivity.this,LoginActivity.class));
                }
                finish();

            }
        },1500);
    }
}