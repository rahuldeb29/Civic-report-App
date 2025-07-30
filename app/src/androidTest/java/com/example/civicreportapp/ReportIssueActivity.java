package com.example.civicreportapp;

/*
 * This is a single-page UI layout for the civic issue reporting app in Java using Android XML + Java Activity
 * Functionality:
 * - Select image via Camera or Gallery (max 5 images allowed)
 */



// src/java/com/yourpackage/ReportIssueActivity.java
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ReportIssueActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 200;
    private static final int MAX_IMAGES = 5;

    ArrayList<Uri> imageUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        Button cameraBtn = findViewById(R.id.buttonCamera);
        Button galleryBtn = findViewById(R.id.buttonGallery);

        cameraBtn.setOnClickListener(v -> {
            if (imageUris.size() >= MAX_IMAGES) {
                Toast.makeText(this, "You can upload max 5 images", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        });

        galleryBtn.setOnClickListener(v -> {
            if (imageUris.size() >= MAX_IMAGES) {
                Toast.makeText(this, "You can upload max 5 images", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), REQUEST_GALLERY);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null && data.getExtras() != null) {
                Uri imageUri = (Uri) data.getExtras().get("data");
                if (imageUri != null && imageUris.size() < MAX_IMAGES) {
                    imageUris.add(imageUri);
                }
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count && imageUris.size() < MAX_IMAGES; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imageUris.add(imageUri);
                    }
                } else if (data.getData() != null && imageUris.size() < MAX_IMAGES) {
                    imageUris.add(data.getData());
                }
            }
        }
    }
}

