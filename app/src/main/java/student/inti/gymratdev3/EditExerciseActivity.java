package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import student.inti.gymratdev3.databinding.ActivityEditExerciseBinding;

public class EditExerciseActivity extends AppCompatActivity {

    private ActivityEditExerciseBinding binding;
    private FirebaseFirestore db;
    private String userId;
    private String workoutName;
    private String exerciseCategory;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivityEditExerciseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase Initialization
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Change status bar color
        setStatusBarColor("#3100d4");

        // Get Intent Extras
        workoutName = getIntent().getStringExtra("exerciseName");

        // Load Exercise Data from Firestore
        loadExerciseData();

        // Set OnClickListeners
        binding.btnSaveExercise.setOnClickListener(v -> saveExercise());
        binding.cancelButton.setOnClickListener(v -> finish());
    }

    private void setStatusBarColor(String colorHex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor(colorHex));
        }
    }

    private void loadExerciseData() {
        showProgress();

        db.collection("users").document(userId)
                .collection("custom_exercises").document(workoutName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        populateExerciseData(documentSnapshot.getData());
                    } else {
                        showToast("Failed to load custom exercise data.");
                    }
                    hideProgress();
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    showToast("Error loading exercise data: " + e.getMessage());
                });
    }

    private void populateExerciseData(Map<String, Object> exerciseData) {
        binding.editExerciseName.setText((String) exerciseData.get("exercise_name"));
        binding.editFocusArea.setText((String) exerciseData.get("focus_area"));
        binding.editEquipment.setText((String) exerciseData.get("equipment"));
        binding.editPreparation.setText((String) exerciseData.get("preparation"));
        binding.editExecution.setText((String) exerciseData.get("execution"));
        exerciseCategory = (String) exerciseData.get("category");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void saveExercise() {
        String name = binding.editExerciseName.getText().toString().trim();
        String focusArea = binding.editFocusArea.getText().toString().trim();
        String equipment = binding.editEquipment.getText().toString().trim();
        String preparation = binding.editPreparation.getText().toString().trim();
        String execution = binding.editExecution.getText().toString().trim();

        if (isValidInput(name, focusArea, equipment, preparation, execution)) {
            showProgress();
            checkForDuplicateExercise(name, focusArea, equipment, preparation, execution);
        }
    }

    private boolean isValidInput(String name, String focusArea, String equipment, String preparation, String execution) {
        if (TextUtils.isEmpty(name)) {
            binding.editExerciseName.setError("Exercise name is required");
            return false;
        }
        if (TextUtils.isEmpty(execution)) {
            binding.editExecution.setError("Execution steps are required");
            return false;
        }
        if (TextUtils.isEmpty(focusArea)) {
            binding.editFocusArea.setError("Focus area is required");
            return false;
        }
        if (TextUtils.isEmpty(equipment)) {
            binding.editEquipment.setError("Equipment is required");
            return false;
        }
        if (TextUtils.isEmpty(preparation)) {
            binding.editPreparation.setError("Preparation steps are required");
            return false;
        }
        return true;
    }

    private void checkForDuplicateExercise(String name, String focusArea, String equipment, String preparation, String execution) {
        db.collection("users")
                .document(userId)
                .collection("custom_exercises")
                .whereEqualTo("exercise_name", name) // Query only by name to reduce reads
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean exists = querySnapshot.getDocuments().stream()
                            .anyMatch(doc -> !doc.getId().equals(workoutName)); // Ensure the same exercise isn't matched

                    if (exists) {
                        handleDuplicateExercise();
                    } else {
                        updateExerciseInDatabase(name, focusArea, equipment, preparation, execution);
                    }
                })
                .addOnFailureListener(e -> handleFirestoreError(e, "Error checking exercise"));
    }

    private void handleDuplicateExercise() {
        hideProgress();
        showToast("Exercise with this name already exists");
    }

    private void handleFirestoreError(Exception e, String message) {
        hideProgress();
        showToast(message + ": " + e.getMessage());
    }


    private void updateExerciseInDatabase(String name, String focusArea, String equipment, String preparation, String execution) {
        Map<String, Object> exerciseData = new HashMap<>();
        exerciseData.put("exercise_name", name);
        exerciseData.put("focus_area", focusArea);
        exerciseData.put("equipment", equipment);
        exerciseData.put("preparation", preparation);
        exerciseData.put("execution", execution);
        exerciseData.put("category", exerciseCategory);

        if (!name.equals(workoutName)) {
            db.collection("users").document(userId)
                    .collection("custom_exercises").document(workoutName)
                    .delete()
                    .addOnSuccessListener(aVoid -> saveNewExerciseData(name, exerciseData))
                    .addOnFailureListener(e -> {
                        hideProgress();
                        showToast("Failed to update exercise: " + e.getMessage());
                    });
        } else {
            saveNewExerciseData(name, exerciseData);
        }
    }

    private void saveNewExerciseData(String name, Map<String, Object> exerciseData) {
        db.collection("users").document(userId)
                .collection("custom_exercises").document(name)
                .set(exerciseData)
                .addOnSuccessListener(aVoid -> {
                    hideProgress();
                    showToast("Exercise updated successfully");
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    showToast("Failed to update exercise: " + e.getMessage());
                });
    }

    private void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        binding.progressBar.setVisibility(View.INVISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
