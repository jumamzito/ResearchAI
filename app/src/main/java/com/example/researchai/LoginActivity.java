package com.example.researchai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEdit, passwordEdit;
    private Button loginBtn;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private TextView signupTv;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

        auth = FirebaseAuth.getInstance();

        clickListener();

    }

    private void init(){
        emailEdit = findViewById(R.id.emailET);
        passwordEdit = findViewById(R.id.passwordET);
        progressBar = findViewById(R.id.progressBar);
        signupTv = findViewById(R.id.signup_tv);
        loginBtn = findViewById(R.id.loginBtn);
    }

    private void clickListener(){
        signupTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
                finish();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEdit.getText().toString();
                String password = passwordEdit.getText().toString();

                if (TextUtils.isEmpty(email)){
                    emailEdit.setError("Input valid email");
                    return;
                }

                if(TextUtils.isEmpty(password)){
                    passwordEdit.setError("Required");
                    return;
                }

                signin(email,password);
            }
        });

    }

    private void signin(String email,String password){
        progressBar.setVisibility(View.VISIBLE);
        auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            // Login Successful
                            progressBar.setVisibility(View.GONE);
                            startActivity(new Intent(LoginActivity.this,MainActivity.class));
                            finish();

                        }else{
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,"Error: "+
                                    Objects.requireNonNull(task.getException()).getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
}