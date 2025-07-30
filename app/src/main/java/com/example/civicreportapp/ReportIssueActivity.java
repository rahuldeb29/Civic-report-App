package com.example.civicreportapp;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Arrays;

public class ReportIssueActivity extends Activity {
    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 200;
    private static final int REQUEST_LOCATION_PERMISSION = 300;
    private static final int MAX_IMAGES = 5;

    ArrayList<Uri> imageUris = new ArrayList<>();
    TextView photoCounter;
    FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button cameraBtn = findViewById(R.id.buttonCamera);
        Button galleryBtn = findViewById(R.id.buttonGallery);
        Button locationBtn = findViewById(R.id.buttonCurrentLocation);
        Button searchLocationBtn = findViewById(R.id.buttonSearchLocation);
        AutoCompleteTextView mlaAutoComplete = findViewById(R.id.autoCompleteMLA);
        photoCounter = findViewById(R.id.photoCounter);

        String[] mlas = getResources().getStringArray(R.array.tripura_mlas);
        ArrayAdapter<String> mlaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mlas);
        mlaAutoComplete.setAdapter(mlaAdapter);

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
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), REQUEST_GALLERY);
        });

        locationBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Toast.makeText(this, "Location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();
                }
            });
        });

        searchLocationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
            startActivity(intent);
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
                    updatePhotoCounter();
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
                updatePhotoCounter();
            }
        }
    }

    private void updatePhotoCounter() {
        if (photoCounter != null) {
            photoCounter.setText("Upload Issue Photos (" + imageUris.size() + "/5)");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            findViewById(R.id.buttonCurrentLocation).performClick();
        }
    }
}