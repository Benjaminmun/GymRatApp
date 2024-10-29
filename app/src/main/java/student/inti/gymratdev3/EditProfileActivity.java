package student.inti.gymratdev3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int IMAGE_PICK_CODE = 100;

    private EditText editUsername, editEmail, editBirthday, editHeight, editWeight;
    private Button saveChangesButton;
    private ImageButton uploadPictureButton;
    private ImageView profileImageView;
    private ProgressBar progressBar;
    private Calendar calendar;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private StorageReference storageReference;
    private FirebaseFirestore db;
    private Uri imageUri;

    private boolean isDatePickerOpen = false;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase and Storage
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");
        db = FirebaseFirestore.getInstance();

        // Change status bar color to match button color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Set up UI components
        setupUI();

        // Load user data from Firestore
        loadUserData();

        // Disable email editing
        if (currentUser != null) {
            editEmail.setText(currentUser.getEmail());
            editEmail.setEnabled(false);
        }

        // Handle DatePicker opening
        editBirthday.setFocusable(false);
        editBirthday.setOnClickListener(v -> {
            if (!isDatePickerOpen) showDatePickerDialog();
        });

        // Handle upload picture button click
        uploadPictureButton.setOnClickListener(v -> requestStoragePermission());

        // Handle save changes button click
        saveChangesButton.setOnClickListener(v -> saveChanges());
    }

    private void setupUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editBirthday = findViewById(R.id.edit_birthday);
        editHeight = findViewById(R.id.edit_height);
        editWeight = findViewById(R.id.edit_weight);
        profileImageView = findViewById(R.id.profile_image_view);
        saveChangesButton = findViewById(R.id.btn_save_changes);
        uploadPictureButton = findViewById(R.id.btn_upload_picture);
        progressBar = findViewById(R.id.progressBar);

        calendar = Calendar.getInstance();
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
            } else {
                selectImage();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                selectImage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Storage permission granted");
                selectImage();
            } else {
                Log.d("Permissions", "Storage permission denied");
                Toast.makeText(this, "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Log.d("Image URI", "Selected URI: " + (imageUri != null ? imageUri.toString() : "null"));
            if (imageUri != null) {
                // Load image with Glide and apply circle crop
                Glide.with(this)
                        .load(imageUri)
                        .circleCrop() // Apply circle crop transformation
                        .into(profileImageView);
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editUsername.setText(documentSnapshot.getString("username"));
                        editBirthday.setText(documentSnapshot.getString("birthday"));
                        editHeight.setText(documentSnapshot.getString("height"));
                        editWeight.setText(documentSnapshot.getString("weight"));

                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            // Load profile picture with Glide and apply circle crop
                            Glide.with(this)
                                    .load(profilePictureUrl)
                                    .circleCrop() // Apply circle crop transformation
                                    .into(profileImageView);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePickerDialog() {
        isDatePickerOpen = true;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    editBirthday.setText(selectedDate);
                    isDatePickerOpen = false;
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.setOnDismissListener(dialog -> isDatePickerOpen = false);
        datePickerDialog.show();
    }

    private void saveChanges() {
        String username = editUsername.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String height = editHeight.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        saveChangesButton.setEnabled(false);
        uploadPictureButton.setEnabled(false);

        if (imageUri != null) {
            uploadProfilePicture(username, birthday, height, weight);
        } else {
            saveToDatabase(username, birthday, height, weight, null);
        }
    }

    private void uploadProfilePicture(String username, String birthday, String height, String weight) {
        StorageReference fileReference = storageReference.child(currentUser.getUid() + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveToDatabase(username, birthday, height, weight, imageUrl);
                }))
                .addOnFailureListener(e -> handleFailure("Error uploading image: " + e.getMessage()));
    }

    private void saveToDatabase(String username, String birthday, String height, String weight, String imageUrl) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("username", username);
        userUpdates.put("birthday", birthday);
        userUpdates.put("height", height);
        userUpdates.put("weight", weight);
        if (imageUrl != null) userUpdates.put("profilePictureUrl", imageUrl);

        db.collection("users").document(currentUser.getUid())
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> handleSuccess(imageUrl))
                .addOnFailureListener(e -> handleFailure("Error updating profile: " + e.getMessage()));
    }

    private void handleSuccess(String imageUrl) {
        progressBar.setVisibility(View.INVISIBLE);
        saveChangesButton.setEnabled(true);
        uploadPictureButton.setEnabled(true);

        if (imageUrl != null) Glide.with(this).load(imageUrl).circleCrop().into(profileImageView);
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void handleFailure(String message) {
        progressBar.setVisibility(View.INVISIBLE);
        saveChangesButton.setEnabled(true);
        uploadPictureButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
