package com.example.researchai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.researchai.model.ProfileModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {


    private CardView classificationCard,objectDetectionCard, moreInfoCard;
    private CircleImageView profileImage;
    private TextView nameTv, emailTv;
    Toolbar toolbar;


    DatabaseReference reference;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        setSupportActionBar(toolbar);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        reference = FirebaseDatabase.getInstance().getReference().child("Users");

        getDataFromDatabase();

        clickListener();

    }

    private void clickListener(){

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,profileActivity.class));
            }
        });

        classificationCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,ClassificationActivity.class));
                finish();
            }
        });

        objectDetectionCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,ObjectdetectionActivity.class));
                finish();
            }
        });



    }



    private void init(){

        classificationCard = findViewById(R.id.classificationCard);
        objectDetectionCard = findViewById(R.id.objectDetectionCard);
        profileImage = findViewById(R.id.profileImage);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        toolbar = findViewById(R.id.toolbar);

    }

    private void getDataFromDatabase(){
        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProfileModel model = snapshot.getValue(ProfileModel.class);
                assert model != null;
                nameTv.setText(model.getName());
                emailTv.setText(model.getEmail());

                Glide.with(getApplicationContext())
                        .load(model.getImage())
                        .timeout(6000)
                        .placeholder(R.drawable.profile)
                        .into(profileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: "+error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }
}