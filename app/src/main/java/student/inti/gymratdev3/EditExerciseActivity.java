package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditExerciseActivity extends AppCompatActivity {

    private EditText editExerciseName, editFocusArea, editEquipment, editPreparation, editExecution;
    private Button saveExerciseButton, cancelButton;
    private ProgressBar progressBar;
    private String workoutName;
    private String exerciseCategory;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_exercise);

        // Change status bar color to match button color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Initialize views
        initializeViews();

        // Get workout details from intent
        workoutName = getIntent().getStringExtra("exerciseName");

        // Load existing exercise data
        loadExerciseData();

        // Set click listener for the Save Exercise button
        saveExerciseButton.setOnClickListener(v -> saveExercise());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        editExerciseName = findViewById(R.id.edit_exercise_name);
        editFocusArea = findViewById(R.id.edit_focus_area);
        editEquipment = findViewById(R.id.edit_equipment);
        editPreparation = findViewById(R.id.edit_preparation);
        editExecution = findViewById(R.id.edit_execution);
        saveExerciseButton = findViewById(R.id.btn_save_exercise);
        progressBar = findViewById(R.id.progressBar);
        cancelButton = findViewById(R.id.cancel_button);
    }

    private void loadExerciseData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("custom_exercises")
                .document(workoutName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> exerciseData = documentSnapshot.getData();
                        editExerciseName.setText((String) exerciseData.get("exercise_name"));
                        editFocusArea.setText((String) exerciseData.get("focus_area"));
                        editEquipment.setText((String) exerciseData.get("equipment"));
                        editPreparation.setText((String) exerciseData.get("preparation"));
                        editExecution.setText((String) exerciseData.get("execution"));
                        exerciseCategory = (String) exerciseData.get("category");  // Store the category for later use
                    } else {
                        Toast.makeText(EditExerciseActivity.this, "Failed to load custom exercise data.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditExerciseActivity.this, "Error loading exercise data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void saveExercise() {
        String name = editExerciseName.getText().toString().trim();
        String focusArea = editFocusArea.getText().toString().trim();
        String equipment = editEquipment.getText().toString().trim();
        String preparation = editPreparation.getText().toString().trim();
        String execution = editExecution.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editExerciseName.setError("Exercise name is required");
            return;
        }
        if (TextUtils.isEmpty(execution)) {
            editExecution.setError("Execution steps are required");
            return;
        }
        if (TextUtils.isEmpty(focusArea)) {
            editFocusArea.setError("Focus area is required");
            return;
        }
        if (TextUtils.isEmpty(equipment)) {
            editEquipment.setError("Equipment is required");
            return;
        }
        if (TextUtils.isEmpty(preparation)) {
            editPreparation.setError("Preparation steps are required");
            return;
        }

        // Show progress bar while checking for duplicates and saving
        progressBar.setVisibility(View.VISIBLE);

        // Check for duplicate exercise name before saving
        checkForDuplicateExercise(name, focusArea, equipment, preparation, execution);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkForDuplicateExercise(String name, String focusArea, String equipment,
                                           String preparation, String execution) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String lowerCaseName = name.toLowerCase();  // Convert input name to lowercase for checking

        // Query Firestore for any document where the name matches case-insensitively
        db.collection("users").document(userId)
                .collection("custom_exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean exists = queryDocumentSnapshots.getDocuments().stream()
                            .filter(document -> !document.getId().equals(workoutName)) // Exclude the current exercise being edited
                            .anyMatch(document -> document.getString("exercise_name").equalsIgnoreCase(name));  // Check case-insensitive match

                    if (exists) {
                        // Exercise name already exists, show error
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(EditExerciseActivity.this, "Exercise with this name already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        // Exercise name does not exist, proceed to save
                        updateExerciseInDatabase(name, focusArea, equipment, preparation, execution);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditExerciseActivity.this, "Error checking exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExerciseInDatabase(String name, String focusArea, String equipment, String preparation, String execution) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Prepare exercise data
        Map<String, Object> exerciseData = new HashMap<>();
        exerciseData.put("exercise_name", name);
        exerciseData.put("focus_area", focusArea);
        exerciseData.put("equipment", equipment);
        exerciseData.put("preparation", preparation);
        exerciseData.put("execution", execution);
        exerciseData.put("category", exerciseCategory);  // Keep the same category as originally set

        // Step 1: Delete the old document if the exercise name has changed
        if (!name.equals(workoutName)) {
            db.collection("users")
                    .document(userId)
                    .collection("custom_exercises")
                    .document(workoutName)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // After deleting the old document, save the new one with the updated name
                        saveNewExerciseData(db, userId, name, exerciseData);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(EditExerciseActivity.this, "Failed to update exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If the name has not changed, just update the existing document
            saveNewExerciseData(db, userId, name, exerciseData);
        }
    }

    private void saveNewExerciseData(FirebaseFirestore db, String userId, String newName, Map<String, Object> exerciseData) {
        db.collection("users")
                .document(userId)
                .collection("custom_exercises")
                .document(newName)
                .set(exerciseData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditExerciseActivity.this, "Exercise updated successfully", Toast.LENGTH_SHORT).show();

                    // Set result to notify WorkoutDetailActivity
                    setResult(RESULT_OK);

                    finish(); // Close the current activity (EditExerciseActivity)
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditExerciseActivity.this, "Failed to update exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
