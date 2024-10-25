package student.inti.gymratdev3;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AddExerciseActivity extends AppCompatActivity {

    private EditText editExerciseName, editPreparation, editExecution;
    private Spinner editFocusArea, editEquipment, editExerciseCategory;
    private Button saveExerciseButton, cancelButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exercise);

        // Change status bar color to match button color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Initialize Views
        editExerciseName = findViewById(R.id.edit_exercise_name);
        editPreparation = findViewById(R.id.edit_preparation);
        editExecution = findViewById(R.id.edit_execution);
        editFocusArea = findViewById(R.id.spinner_focus_area);
        editEquipment = findViewById(R.id.spinner_equipment);
        editExerciseCategory = findViewById(R.id.spinner_exercise_category);
        saveExerciseButton = findViewById(R.id.btn_save_exercise);
        cancelButton = findViewById(R.id.cancel_button);
        progressBar = findViewById(R.id.progressBar);

        // Set up Spinner with exercise categories
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.exercise_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editExerciseCategory.setAdapter(categoryAdapter);

        // Set up Spinner for focus areas
        ArrayAdapter<CharSequence> focusAdapter = ArrayAdapter.createFromResource(
                this, R.array.focus_areas, android.R.layout.simple_spinner_item);
        focusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editFocusArea.setAdapter(focusAdapter);

        // Set up Spinner for equipment
        ArrayAdapter<CharSequence> equipmentAdapter = ArrayAdapter.createFromResource(
                this, R.array.equipment_types, android.R.layout.simple_spinner_item);
        equipmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editEquipment.setAdapter(equipmentAdapter);

        // Set listeners for buttons
        saveExerciseButton.setOnClickListener(v -> saveExercise());
        cancelButton.setOnClickListener(v -> finish());  // Close the activity on cancel
    }

    // Save exercise details to Firestore
    private void saveExercise() {
        String name = editExerciseName.getText().toString().trim();  // Trim spaces
        String preparation = editPreparation.getText().toString().trim();  // Trim spaces
        String execution = editExecution.getText().toString().trim();  // Trim spaces
        String focusArea = editFocusArea.getSelectedItem() != null
                ? editFocusArea.getSelectedItem().toString().trim()  // Trim spaces
                : "";
        String equipment = editEquipment.getSelectedItem() != null
                ? editEquipment.getSelectedItem().toString().trim()  // Trim spaces
                : "";
        String category = editExerciseCategory.getSelectedItem() != null
                ? editExerciseCategory.getSelectedItem().toString().trim()  // Trim spaces
                : "";

        // Validate input
        if (TextUtils.isEmpty(name)) {
            editExerciseName.setError("Exercise name is required");
            return;
        }
        if (TextUtils.isEmpty(execution)) {
            editExecution.setError("Execution steps are required");
            return;
        }
        if (TextUtils.isEmpty(focusArea)) {
            Toast.makeText(this, "Please select a focus area", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(equipment)) {
            Toast.makeText(this, "Please select an equipment type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(preparation)) {
            editPreparation.setError("Preparation steps are required");
            return;
        }
        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar while checking and saving
        progressBar.setVisibility(View.VISIBLE);

        // Check for duplicate exercise name before saving
        checkForDuplicateExercise(name, focusArea, equipment, preparation, execution, category);
    }

    // Check if an exercise with the same name (case-insensitive) already exists
    private void checkForDuplicateExercise(String name, String focusArea, String equipment,
                                           String preparation, String execution, String category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = currentUser.getUid();

        // Query Firestore for any document where the name matches case-insensitively
        db.collection("users").document(userId)
                .collection("custom_exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean exists = queryDocumentSnapshots.getDocuments().stream()
                            .anyMatch(document -> document.getString("exercise_name").equalsIgnoreCase(name));  // Check case-insensitive match

                    if (exists) {
                        // Exercise name already exists, show error
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(AddExerciseActivity.this, "Exercise with this name already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        // Exercise name does not exist, proceed to save
                        saveToDatabase(name, focusArea, equipment, preparation, execution, category);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddExerciseActivity.this, "Error checking exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Save data to Firestore
    private void saveToDatabase(String name, String focusArea, String equipment,
                                String preparation, String execution, String category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = currentUser.getUid();

        // Prepare exercise data
        Map<String, Object> exerciseData = new HashMap<>();
        exerciseData.put("exercise_name", name);  // Save with original casing
        exerciseData.put("focus_area", focusArea);
        exerciseData.put("equipment", equipment);
        exerciseData.put("preparation", preparation);
        exerciseData.put("execution", execution);
        exerciseData.put("category", category);

        // Save to Firestore with merge option
        db.collection("users").document(userId)
                .collection("custom_exercises").document(name)
                .set(exerciseData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddExerciseActivity.this, "Exercise added successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddExerciseActivity.this, "Failed to add exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
