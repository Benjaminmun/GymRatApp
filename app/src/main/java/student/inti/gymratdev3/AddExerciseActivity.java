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

public class AddExerciseActivity extends AppCompatActivity {

    private EditText editExerciseName, editFocusArea, editEquipment, editPreparation, editExecution;
    private Button saveExerciseButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exercise);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

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

        // Set click listener for the Save Exercise button
        saveExerciseButton.setOnClickListener(v -> saveExercise());
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

        // Save exercise data to Firestore
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

        db.collection("exercises").document(name)
                .set(exerciseData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddExerciseActivity.this, "Exercise added successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close activity after saving
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddExerciseActivity.this, "Failed to add exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
