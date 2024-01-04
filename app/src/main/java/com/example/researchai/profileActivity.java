package com.example.researchai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.researchai.model.ProfileModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class profileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView nameTv, emailTv,shareTv,logoutTv;
    private ImageButton ImageEditBtn;
    private Button updateBtn;

    private FirebaseAuth auth;
    private FirebaseUser user;

    private DatabaseReference reference;
    private static final int IMAGE_PICKER = 1;
    private Uri photoUri;
    private String imageUrl;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        init();

        loadDataFromDatabase();
        clickListener();
    }

    private void init(){
        profileImage = findViewById(R.id.profileImage);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        shareTv = findViewById(R.id.shareTv);
        logoutTv = findViewById(R.id.logoutTv);
        ImageEditBtn = findViewById(R.id.imageEditBtn);
        updateBtn = findViewById(R.id.updateBtn);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        reference = FirebaseDatabase.getInstance().getReference().child("Users");


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCancelable(false);

    }

    private void loadDataFromDatabase(){
        reference.child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
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
                        Toast.makeText(profileActivity.this, "Error: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void clickListener(){
        logoutTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                startActivity(new Intent(profileActivity.this,LoginActivity.class));
                finish();
            }
        });

        shareTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sharebody = "Check out the research ai app. Download "+getString(R.string.app_name)+
                        "from play store\n"+
                        "https://play.google.com/store/apps/details?id="+
                        getPackageName();

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT,sharebody);
                intent.setType("text/plain");
                startActivity(intent);
            }
        });

        ImageEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withContext(profileActivity.this)
                        .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                                if(multiplePermissionsReport.areAllPermissionsGranted()){
                                    Intent intent = new Intent(Intent.ACTION_PICK);
                                    intent.setType("image/*");
                                    startActivityForResult(intent, IMAGE_PICKER);


                                    
                                }else{
                                    Toast.makeText(profileActivity.this, "Please allow permission", Toast.LENGTH_SHORT).show();
                                }

                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                            }
                        }).check();
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_PICKER && resultCode == RESULT_OK){
            if(data != null){

                photoUri = data.getData();
                updateBtn.setVisibility(View.VISIBLE);

            }
        }
    }

    private void uploadImage(){
        if(photoUri == null){
            return;
        }

        String fileName = user.getUid()+".jpg";
        FirebaseStorage storage = FirebaseStorage.getInstance();
        final  StorageReference storageReference = storage.getReference().child("Images/"+fileName);

        progressDialog.show();
        storageReference.putFile(photoUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                imageUrl=uri.toString();

                                uploadImageUrlToDatabase();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(profileActivity.this, "Error : "+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        long totalS = taskSnapshot.getTotalByteCount();
                        long transferS = taskSnapshot.getBytesTransferred();

                        long totalSize = (totalS / 1024);
                        long transferSize = transferS/1024;


                        progressDialog.setMessage("Uploaded "+((int) transferSize)+ "KB / "+((int) totalSize)+ "KB");

                    }
                });
    }

    private void uploadImageUrlToDatabase(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("image",imageUrl);

        reference.child(user.getUid())
                .updateChildren(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateBtn.setVisibility(View.GONE);
                        progressDialog.dismiss();
                    }
                });

    }
}