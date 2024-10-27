package student.inti.gymratdev3;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditUserTrainingActivity extends AppCompatActivity {

    private EditText trainingNameEditText;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton, saveTrainingButton;
    private int exerciseCount = 0;
    private static final int MAX_EXERCISES = 10;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialog;

    // Store default and custom exercises
    private ArrayList<String> availableExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_training);

        // Initialize UI elements
        trainingNameEditText = findViewById(R.id.training_name_edit_text);
        exercisesContainer = findViewById(R.id.exercises_container);
        addExerciseButton = findViewById(R.id.add_exercise_button);
        saveTrainingButton = findViewById(R.id.save_training_button);

        // Initialize Firestore and user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving training...");
        progressDialog.setCancelable(false);

        // Load available exercises (both default and custom)
        loadAvailableExercises();

        // Handle Add Exercise button click
        addExerciseButton.setOnClickListener(v -> {
            if (exerciseCount < MAX_EXERCISES) {
                addExerciseField();
            } else {
                Toast.makeText(this, "You can only add up to 10 exercises.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button listener with delayed trigger to capture latest input
        saveTrainingButton.setOnClickListener(v -> {
            trainingNameEditText.clearFocus();  // Ensure input is captured
            saveTrainingButton.postDelayed(this::saveTrainingToFirestore, 50);
        });
    }

    // Load available exercises from Firestore (default workouts + user custom exercises)
    private void loadAvailableExercises() {
        availableExercises.clear(); // Clear previous data to avoid duplication

        // Load default workouts from Firestore's `default_workouts` collection
        db.collection("default_workouts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String exerciseName = doc.getString("exercise_name");
                            if (exerciseName != null) {
                                availableExercises.add(exerciseName);
                            }
                        }
                        // After loading default workouts, load custom exercises
                        loadCustomExercises();
                    } else {
                        Toast.makeText(this, "Failed to load default workouts.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Load user custom exercises from Firestore
    private void loadCustomExercises() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).collection("custom_exercises")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String exerciseName = doc.getString("exercise_name");
                                if (exerciseName != null) {
                                    availableExercises.add(exerciseName);
                                }
                            }
                        } else {
                            Toast.makeText(this, "Failed to load custom exercises.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Add a new exercise input field with a dropdown Spinner for exercise selection
    private void addExerciseField() {
        LinearLayout exerciseLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.exercise_input_layout, null);

        Spinner exerciseSpinner = exerciseLayout.findViewById(R.id.exercise_spinner);
        EditText repsEditText = exerciseLayout.findViewById(R.id.reps_edit_text);
        EditText setsEditText = exerciseLayout.findViewById(R.id.sets_edit_text);

        // Set up the exercise spinner with the available exercises
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableExercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exerciseSpinner.setAdapter(adapter);

        // Add validation for reps and sets input fields
        validateRepsAndSets(repsEditText, setsEditText);

        // Add the new exercise layout to the container
        exercisesContainer.addView(exerciseLayout);
        exerciseCount++;
    }

    // Real-time validation for reps and sets input
    private void validateRepsAndSets(EditText repsEditText, EditText setsEditText) {
        repsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    repsEditText.setError("Reps cannot be empty");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    setsEditText.setError("Sets cannot be empty");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Save training and exercises to Firestore
    private void saveTrainingToFirestore() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Trim the input to avoid issues with whitespace
        String trainingName = trainingNameEditText.getText().toString().trim();
        if (trainingName.isEmpty()) {
            Toast.makeText(this, "Training name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Map<String, Object>> exercises = new ArrayList<>();
        for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
            View view = exercisesContainer.getChildAt(i);
            Spinner exerciseSpinner = view.findViewById(R.id.exercise_spinner);
            EditText repsEditText = view.findViewById(R.id.reps_edit_text);
            EditText setsEditText = view.findViewById(R.id.sets_edit_text);

            String exercise = exerciseSpinner.getSelectedItem().toString();
            String reps = repsEditText.getText().toString();
            String sets = setsEditText.getText().toString();

            if (reps.isEmpty() || sets.isEmpty()) {
                Toast.makeText(this, "Reps and sets cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> exerciseData = new HashMap<>();
            exerciseData.put("exercise", exercise);
            exerciseData.put("reps", Integer.parseInt(reps));
            exerciseData.put("sets", Integer.parseInt(sets));

            exercises.add(exerciseData);
        }

        if (exercises.isEmpty()) {
            Toast.makeText(this, "At least one exercise must be added.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> trainingData = new HashMap<>();
        trainingData.put("userId", currentUser.getUid());
        trainingData.put("trainingName", trainingName);
        trainingData.put("exercises", exercises);

        progressDialog.show();
        db.collection("trainings").add(trainingData)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Training saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error saving training", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Do you want to save changes?")
                .setPositiveButton("Yes", (dialog, which) -> saveTrainingToFirestore())
                .setNegativeButton("No", (dialog, which) -> super.onBackPressed())
                .show();
    }
}