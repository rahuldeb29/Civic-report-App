package com.example.civicreportapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class ReportIssueActivity extends Activity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 200;
    private static final int REQUEST_LOCATION_PERMISSION = 300;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 400;
    private static final int MAX_IMAGES = 5;

    private LinearLayout imagePreviewContainer;
    private TextView photoCounter;
    private TextView locationResultTextView;
    private EditText editTitle, editDescription;
    private AutoCompleteTextView mlaAutoComplete;
    private Button submitButton;

    private ArrayList<Uri> imageUris = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        photoCounter = findViewById(R.id.photoCounter);
        locationResultTextView = findViewById(R.id.locationResultTextView);
        mlaAutoComplete = findViewById(R.id.autoCompleteMLA);
        editTitle = findViewById(R.id.editTextTitle);
        editDescription = findViewById(R.id.editTextDescription);
        submitButton = findViewById(R.id.buttonSubmit);

        Button currentLocationBtn = findViewById(R.id.buttonCurrentLocation);
        Button cameraBtn = findViewById(R.id.buttonCamera);
        Button galleryBtn = findViewById(R.id.buttonGallery);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseApp.initializeApp(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY_HERE", Locale.ENGLISH);
        }

        String[] mlas = getResources().getStringArray(R.array.tripura_mlas);
        ArrayAdapter<String> mlaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mlas);
        mlaAutoComplete.setAdapter(mlaAdapter);

        currentLocationBtn.setOnClickListener(v -> fetchCurrentLocation());

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

        submitButton.setOnClickListener(v -> {
            submitButton.setEnabled(false);
            uploadReportToFirebase();
        });
    }

    private void addImageToPreview(Uri imageUri) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(imageUri);
        imagePreviewContainer.addView(imageView);
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (!addresses.isEmpty()) {
                        String fullAddress = addresses.get(0).getAddressLine(0);
                        locationResultTextView.setText("Current Location:\n" + fullAddress);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void uploadReportToFirebase() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String mla = mlaAutoComplete.getText().toString().trim();
        String location = locationResultTextView.getText().toString().replace("Current Location:\n", "").trim();

        if (title.isEmpty() || description.isEmpty() || mla.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            submitButton.setEnabled(true);
            return;
        }

        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        String reportId = reportsRef.push().getKey();

        List<String> uploadedImageUrls = new ArrayList<>();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("report_images").child(reportId);

        if (imageUris.isEmpty()) {
            saveReportData(reportId, title, description, mla, location, uploadedImageUrls);
            return;
        }

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            StorageReference imageRef = storageRef.child("image_" + i + ".jpg");

            int finalI = i;
            imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadedImageUrls.add(uri.toString());
                        if (uploadedImageUrls.size() == imageUris.size()) {
                            saveReportData(reportId, title, description, mla, location, uploadedImageUrls);
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
                        submitButton.setEnabled(true);
                    })
            ).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                submitButton.setEnabled(true);
            });
        }
    }

    private void saveReportData(String reportId, String title, String description, String mla, String location, List<String> imageUrls) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("title", title);
        reportData.put("description", description);
        reportData.put("mla", mla);
        reportData.put("location", location);
        reportData.put("imageUrls", imageUrls);

        FirebaseDatabase.getInstance().getReference("reports").child(reportId).setValue(reportData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            if (imageBitmap != null && imageUris.size() < MAX_IMAGES) {
                Uri imageUri = getImageUri(imageBitmap);
                imageUris.add(imageUri);
                addImageToPreview(imageUri);
                updatePhotoCounter();
            }
        } else if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count && imageUris.size() < MAX_IMAGES; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    if (imageUri != null) {
                        imageUris.add(imageUri);
                        addImageToPreview(imageUri);
                        Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (data.getData() != null && imageUris.size() < MAX_IMAGES) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    imageUris.add(imageUri);
                    addImageToPreview(imageUri);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                }
            }
            updatePhotoCounter();
        } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            locationResultTextView.setText("Selected Location:\n" + place.getAddress());
        }
    }

    private void updatePhotoCounter() {
        if (photoCounter != null) {
            photoCounter.setText("Upload Issue Photos (" + imageUris.size() + "/5)");
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "CapturedImage_" + System.currentTimeMillis(), null);
        return Uri.parse(path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        }
    }
}
