package student.inti.gymratdev3;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingDetailActivity extends AppCompatActivity {

    private static final String TAG = "TrainingDetailActivity";

    private Button addExerciseButton, removeExerciseButton, startWorkoutButton;
    private LinearLayout exercisesLayout;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private DocumentReference trainingRef;
    private ListenerRegistration trainingListener;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);

        // Retrieve the training ID from the intent
        String trainingId = getIntent().getStringExtra("TRAINING_ID");
        if (trainingId == null || trainingId.isEmpty()) {
            showError("Error: Training ID is missing.");
            return;
        }

        // Initialize Firestore and create a reference to the training document using the ID
        db = FirebaseFirestore.getInstance();
        trainingRef = db.collection("trainings").document(trainingId);

        // Initialize UI elements
        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        exercisesLayout = findViewById(R.id.exercises_layout);
        progressBar = findViewById(R.id.progress_bar);

        // Set button click listeners
        addExerciseButton.setOnClickListener(v -> openAddExerciseDialog());
        startWorkoutButton.setOnClickListener(v -> startWorkout());

        // Attach a snapshot listener for real-time updates
        attachTrainingListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trainingListener != null) {
            trainingListener.remove();
        }
    }

    private void attachTrainingListener() {
        showLoading(true);
        Log.d(TAG, "Attempting to attach listener for: " + trainingRef.getPath());

        trainingListener = trainingRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                showLoading(false);
                showError("Error listening for updates: " + e.getMessage());
                Log.e(TAG, "Error listening for updates", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Log.d(TAG, "Document data: " + documentSnapshot.getData());

                Object exercisesObj = documentSnapshot.get("exercises");
                if (exercisesObj instanceof List) {
                    List<Map<String, Object>> exercises = (List<Map<String, Object>>) exercisesObj;
                    if (exercises.isEmpty()) {
                        exercisesLayout.removeAllViews();
                        showError("No exercises found for this training plan.");
                        showLoading(false);
                        return;
                    }
                    displayExercises(exercises);
                } else {
                    showError("Invalid data format for exercises.");
                    exercisesLayout.removeAllViews();
                }
            } else {
                showError("No matching training found.");
                exercisesLayout.removeAllViews();
            }
            showLoading(false);
        });
    }

    private void displayExercises(List<Map<String, Object>> exercises) {
        exercisesLayout.removeAllViews(); // Clear previous views

        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.get("exercise");
            Number repsNumber = (Number) exercise.get("reps"); // Can be Integer or Long
            Number setsNumber = (Number) exercise.get("sets"); // Can be Integer or Long

            if (name == null || repsNumber == null || setsNumber == null) {
                Log.w(TAG, "Skipping exercise with missing information: " + exercise);
                continue;
            }

            int reps = repsNumber.intValue(); // Convert to int
            int sets = setsNumber.intValue(); // Convert to int

            TextView textView = new TextView(this);
            textView.setText(String.format("%s - %d sets, %d reps", name, sets, reps));
            textView.setTextSize(18);
            textView.setPadding(10, 10, 10, 10);
            textView.setOnClickListener(v ->
                    openEditExerciseDialog(name, (long) reps, (long) sets)
            );

            exercisesLayout.addView(textView); // Add the TextView to the layout
        }
    }


    private void openEditExerciseDialog(String oldName, Long oldReps, Long oldSets) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_exercise, null);

        EditText nameEditText = dialogView.findViewById(R.id.exercise_name);
        EditText repsEditText = dialogView.findViewById(R.id.exercise_reps);
        EditText setsEditText = dialogView.findViewById(R.id.exercise_sets);

        // Set old values
        nameEditText.setText(oldName);
        repsEditText.setText(String.valueOf(oldReps));
        setsEditText.setText(String.valueOf(oldSets));

        // Disable name field to prevent editing
        nameEditText.setEnabled(false);
        nameEditText.setFocusable(false);

        new AlertDialog.Builder(this)
                .setTitle("Edit Exercise")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    int newReps = Integer.parseInt(repsEditText.getText().toString());
                    int newSets = Integer.parseInt(setsEditText.getText().toString());

                    // Call the function to update the existing exercise
                    updateExercise(oldName, newReps, newSets);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateExercise(String name, int newReps, int newSets) {
        showLoading(true);

        trainingRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
            if (exercises != null) {
                // Find the exercise by name and update reps/sets
                for (Map<String, Object> exercise : exercises) {
                    if (name.equals(exercise.get("exercise"))) {
                        exercise.put("reps", newReps);
                        exercise.put("sets", newSets);
                        break;
                    }
                }

                // Update Firestore with the modified exercises list
                trainingRef.update("exercises", exercises)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Exercise updated!", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        })
                        .addOnFailureListener(e -> {
                            showError("Error updating exercise: " + e.getMessage());
                            showLoading(false);
                        });
            } else {
                showError("No exercises found.");
                showLoading(false);
            }
        }).addOnFailureListener(e -> {
            showError("Error fetching exercises: " + e.getMessage());
            showLoading(false);
        });
    }


    private void openAddExerciseDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_exercise, null);

        EditText nameEditText = dialogView.findViewById(R.id.exercise_name);
        EditText repsEditText = dialogView.findViewById(R.id.exercise_reps);
        EditText setsEditText = dialogView.findViewById(R.id.exercise_sets);

        new AlertDialog.Builder(this)
                .setTitle("Add Exercise")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = nameEditText.getText().toString();
                    int reps = Integer.parseInt(repsEditText.getText().toString());
                    int sets = Integer.parseInt(setsEditText.getText().toString());
                    addExercise(name, reps, sets);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addExercise(String name, int reps, int sets) {
        showLoading(true);
        trainingRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
            if (exercises == null) exercises = new ArrayList<>();

            Map<String, Object> newExercise = new HashMap<>();
            newExercise.put("name", name);
            newExercise.put("reps", reps);
            newExercise.put("sets", sets);
            exercises.add(newExercise);

            trainingRef.update("exercises", exercises)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Exercise added!", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    })
                    .addOnFailureListener(e -> {
                        showError("Error adding exercise: " + e.getMessage());
                        showLoading(false);
                    });
        }).addOnFailureListener(e -> {
            showError("Error fetching exercises: " + e.getMessage());
            showLoading(false);
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Snackbar.make(exercisesLayout, message, Snackbar.LENGTH_LONG).show();
    }

    private void startWorkout() {
        Toast.makeText(this, "Workout started!", Toast.LENGTH_SHORT).show();
    }
}
