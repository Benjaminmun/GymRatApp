package student.inti.gymratdev3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditExerciseActivity extends AppCompatActivity {

    private EditText editExerciseName, editFocusArea, editEquipment, editPreparation, editExecution;
    private Button saveExerciseButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private String exerciseId; // ID of the exercise to edit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_exercise);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Get exercise ID from intent
        exerciseId = getIntent().getStringExtra("EXERCISE_ID");

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
        }

        // Find views by ID
        editExerciseName = findViewById(R.id.edit_exercise_name);
        editFocusArea = findViewById(R.id.edit_focus_area);
        editEquipment = findViewById(R.id.edit_equipment);
        editPreparation = findViewById(R.id.edit_preparation);
        editExecution = findViewById(R.id.edit_execution);
        saveExerciseButton = findViewById(R.id.btn_save_exercise);
        progressBar = findViewById(R.id.progressBar);

        // Load existing exercise data
        loadExerciseData();

        // Set click listener for the Save Exercise button
        saveExerciseButton.setOnClickListener(v -> saveExercise());
    }

    // Load exercise data from Firestore
    private void loadExerciseData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("exercises").document(exerciseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set data to the fields
                        String name = documentSnapshot.getString("name");
                        String focusArea = documentSnapshot.getString("focusArea");
                        String equipment = documentSnapshot.getString("equipment");
                        String preparation = documentSnapshot.getString("preparation");
                        String execution = documentSnapshot.getString("execution");

                        editExerciseName.setText(name);
                        editFocusArea.setText(focusArea);
                        editEquipment.setText(equipment);
                        editPreparation.setText(preparation);
                        editExecution.setText(execution);
                    } else {
                        Toast.makeText(EditExerciseActivity.this, "Failed to load exercise data.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditExerciseActivity.this, "Error loading exercise data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Save exercise details
    private void saveExercise() {
        String name = editExerciseName.getText().toString().trim();
        String focusArea = editFocusArea.getText().toString().trim();
        String equipment = editEquipment.getText().toString().trim();
        String preparation = editPreparation.getText().toString().trim();
        String execution = editExecution.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(name)) {
            editExerciseName.setError("Exercise name is required");
            return;
        }

        // Show ProgressBar while saving
        progressBar.setVisibility(View.VISIBLE);

        saveToDatabase(name, focusArea, equipment, preparation, execution);
    }

    // Save exercise data to Firestore
    private void saveToDatabase(String name, String focusArea, String equipment, String preparation, String execution) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> exerciseData = new HashMap<>();
        exerciseData.put("name", name);
        exerciseData.put("focusArea", focusArea);
        exerciseData.put("equipment", equipment);
        exerciseData.put("preparation", preparation);
        exerciseData.put("execution", execution);

        db.collection("exercises").document(exerciseId)
                .set(exerciseData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditExerciseActivity.this, "Exercise updated successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close activity after saving
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditExerciseActivity.this, "Failed to update exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
