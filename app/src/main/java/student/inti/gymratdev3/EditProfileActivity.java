package student.inti.gymratdev3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editUsername, editEmail, editBirthday, editHeight, editWeight;
    private Button saveChangesButton;
    private ImageButton uploadPictureButton;
    private ImageView profileImageView;
    private Calendar calendar;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private StorageReference storageReference;
    private Uri imageUri;
    private static final int STORAGE_PERMISSION_CODE = 101;  // Request code for storage permission

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Change status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Initialize Firebase Auth and Storage reference
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back); // Custom back icon
        }

        // Set click listener for the back button
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Find views by ID
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editBirthday = findViewById(R.id.edit_birthday);
        editHeight = findViewById(R.id.edit_height);
        editWeight = findViewById(R.id.edit_weight);
        profileImageView = findViewById(R.id.profile_image_view);
        saveChangesButton = findViewById(R.id.btn_save_changes);
        uploadPictureButton = findViewById(R.id.btn_upload_picture);
        progressBar = findViewById(R.id.progressBar);

        // Initialize calendar for the DatePicker
        calendar = Calendar.getInstance();

        // Load existing user data
        loadUserData();

        // Set email as non-editable
        if (currentUser != null) {
            editEmail.setText(currentUser.getEmail());
            editEmail.setEnabled(false);  // Make the email field non-editable
        }

        // Set the birthday field to open the DatePickerDialog when clicked
        editBirthday.setFocusable(false);
        editBirthday.setOnClickListener(v -> showDatePickerDialog());

        // Set click listener for the Upload Picture button
        uploadPictureButton.setOnClickListener(v -> requestStoragePermission());

        // Set click listener for the Save Changes button
        saveChangesButton.setOnClickListener(v -> saveChanges());
    }

    // Method to request storage permission
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Storage permission is required to select a profile picture.", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            selectImage();
        }
    }

    // Handle the result of permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();  // Permission granted
            } else {
                Toast.makeText(this, "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Method to load user data from Firestore
    private void loadUserData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set data to the fields
                        String username = documentSnapshot.getString("username");
                        String birthday = documentSnapshot.getString("birthday");
                        String height = documentSnapshot.getString("height");
                        String weight = documentSnapshot.getString("weight");
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");

                        editUsername.setText(username);
                        editBirthday.setText(birthday);
                        editHeight.setText(height);
                        editWeight.setText(weight);

                        // If profile picture exists, load it
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(this).load(profilePictureUrl).into(profileImageView);
                        }
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error loading profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to open the gallery and allow the user to select an image
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                profileImageView.setImageURI(imageUri);  // Display selected image
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to save changes, including height, weight, and optionally a profile picture
    private void saveChanges() {
        String username = editUsername.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String height = editHeight.getText().toString().trim();
        String weight = editWeight.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            editUsername.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(birthday)) {
            editBirthday.setError("Birthday is required");
            return;
        }

        if (TextUtils.isEmpty(height)) {
            editHeight.setError("Height is required");
            return;
        }

        if (TextUtils.isEmpty(weight)) {
            editWeight.setError("Weight is required");
            return;
        }

        // Show ProgressBar while saving
        progressBar.setVisibility(View.VISIBLE);

        // Upload profile picture if selected, then save user data
        if (imageUri != null) {
            StorageReference fileReference = storageReference.child(currentUser.getUid() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveToDatabase(username, birthday, height, weight, imageUrl);
                    }))
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(EditProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveToDatabase(username, birthday, height, weight, null);
        }
    }

    // Save user data to Firebase Firestore
    private void saveToDatabase(String username, String birthday, String height, String weight, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("birthday", birthday);
        userData.put("height", height);
        userData.put("weight", weight);
        if (imageUrl != null) {
            userData.put("profilePictureUrl", imageUrl);
        }

        db.collection("users").document(currentUser.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Load updated user data
                    loadUserData();  // Fetch and display the latest data
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Show DatePickerDialog to select the birthday
    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(EditProfileActivity.this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    editBirthday.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
}
