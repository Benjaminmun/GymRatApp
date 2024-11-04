package student.inti.gymratdev3;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProperEditUserTrainingActivity extends AppCompatActivity {

    private EditText trainingNameEditText;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton, saveTrainingButton;
    private int exerciseCount = 0;
    private static final int MAX_EXERCISES = 10;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialog;

    private ArrayList<String> availableExercises = new ArrayList<>();
    private String trainingId;  // Store the training ID for editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_training);

        // Status bar color and initialization
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

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
        loadAvailableExercises(() -> {
            trainingId = getIntent().getStringExtra("TRAINING_ID");
            if (trainingId != null) {
                loadTrainingData(trainingId);  // Load data for editing only after exercises are loaded
            } else {
                Toast.makeText(this, "Training ID not provided", Toast.LENGTH_SHORT).show();
                finish();  // End activity if no ID is provided
            }
        });

        // Handle Add Exercise button click
        addExerciseButton.setOnClickListener(v -> {
            if (exerciseCount < MAX_EXERCISES) {
                addExerciseField();
            } else {
                Toast.makeText(this, "You can only add up to 10 exercises.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button listener
        saveTrainingButton.setOnClickListener(v -> {
            trainingNameEditText.clearFocus();  // Ensure input is captured
            saveTrainingButton.postDelayed(this::saveTrainingToFirestore, 50);
        });

    }

    private void addExerciseField() {
        // Define this method as required, for example, by calling an overloaded version or adding blank inputs.
        addExerciseField("", 0, 0);  // This adds a blank exercise with default values
    }

    // Load data for editing from Firestore using training ID
    private void loadTrainingData(String trainingId) {
        db.collection("trainings").document(trainingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String trainingName = documentSnapshot.getString("trainingName");
                        trainingNameEditText.setText(trainingName);

                        List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
                        if (exercises != null) {
                            for (Map<String, Object> exerciseData : exercises) {
                                String name = (String) exerciseData.get("exercise");
                                int reps = ((Number) exerciseData.get("reps")).intValue();
                                int sets = ((Number) exerciseData.get("sets")).intValue();
                                addExerciseField(name, reps, sets);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Training not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load training data", Toast.LENGTH_SHORT).show());
    }

    // Load available exercises and then execute a callback when done
    private void loadAvailableExercises(Runnable onComplete) {
        availableExercises.clear();

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
                        loadCustomExercises(onComplete); // After default, load custom exercises
                    } else {
                        Toast.makeText(this, "Failed to load default workouts.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Load user custom exercises and then call the callback
    private void loadCustomExercises(Runnable onComplete) {
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
                            if (onComplete != null) onComplete.run(); // Run callback after loading
                        } else {
                            Toast.makeText(this, "Failed to load custom exercises.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Overloaded method to add an exercise field with data
    private void addExerciseField(String name, int reps, int sets) {
        LinearLayout exerciseLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.exercise_input_layout, null);
        Spinner exerciseSpinner = exerciseLayout.findViewById(R.id.exercise_spinner);
        EditText repsEditText = exerciseLayout.findViewById(R.id.reps_edit_text);
        EditText setsEditText = exerciseLayout.findViewById(R.id.sets_edit_text);
        ImageButton deleteExerciseButton = exerciseLayout.findViewById(R.id.delete_exercise_button);

        // Set up the spinner adapter
        setupExerciseSpinner(exerciseSpinner);

        // Set the selected exercise after adapter is set
        int exerciseIndex = availableExercises.indexOf(name);
        if (exerciseIndex >= 0) {
            exerciseSpinner.setSelection(exerciseIndex);
        }

        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        // Real-time validation directly for repsEditText
        repsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    repsEditText.setError("Reps cannot be empty");
                } else if (s.toString().equals("0")) {
                    repsEditText.setError("Reps cannot be 0");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Real-time validation directly for setsEditText
        setsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    setsEditText.setError("Sets cannot be empty");
                } else if (s.toString().equals("0")) {
                    setsEditText.setError("Sets cannot be 0");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up delete button to remove exercise layout
        deleteExerciseButton.setOnClickListener(v -> {
            exercisesContainer.removeView(exerciseLayout);
            exerciseCount--;
        });

        exercisesContainer.addView(exerciseLayout);
        exerciseCount++;
    }


    // Set up exercise spinner for loading exercise options
    private void setupExerciseSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableExercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
                } else if (s.toString().equals("0")) {
                    repsEditText.setError("Reps cannot be 0");
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
                } else if (s.toString().equals("0")) {
                    setsEditText.setError("Sets cannot be 0");
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

        // Trim input to avoid whitespace issues
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

        // Prepare the training data for saving
        Map<String, Object> trainingData = new HashMap<>();
        trainingData.put("userId", currentUser.getUid());
        trainingData.put("trainingName", trainingName);
        trainingData.put("exercises", exercises);

        progressDialog.show();

        // Update the existing document with the trainingId rather than adding a new one
        db.collection("trainings").document(trainingId)
                .set(trainingData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Training saved successfully", Toast.LENGTH_SHORT).show();

                    // Set result before finishing activity
                    setResult(RESULT_OK);
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