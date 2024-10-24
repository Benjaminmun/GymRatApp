package student.inti.gymratdev3;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingDetailActivity extends AppCompatActivity {

    private Button addExerciseButton;
    private Button removeExerciseButton;
    private Button startWorkoutButton;
    private LinearLayout exercisesLayout;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String trainingName;
    private DocumentReference trainingRef; // Reference to the Firestore document

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);

        // Retrieve training name from the intent
        trainingName = getIntent().getStringExtra("TRAINING_NAME");

        // Error handling if training name is null
        if (trainingName == null || trainingName.isEmpty()) {
            showError("Error: Training name is missing.");
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        trainingRef = db.collection("trainings").document(trainingName);

        // Initialize buttons, layout, and progress bar
        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        exercisesLayout = findViewById(R.id.exercises_layout);
        progressBar = findViewById(R.id.progress_bar);

        // Set up button click listeners
        addExerciseButton.setOnClickListener(v -> openAddExerciseDialog());
        removeExerciseButton.setOnClickListener(v -> openRemoveExerciseDialog());
        startWorkoutButton.setOnClickListener(v -> startWorkout());

        // Fetch and display the exercises
        loadExercises();
    }

    // Fetch exercises from Firestore and display them in the layout
    private void loadExercises() {
        showLoading(true);
        trainingRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
                        if (exercises != null && !exercises.isEmpty()) {
                            displayExercises(exercises);
                        } else {
                            showError("No exercises found for this training plan.");
                        }
                    } else {
                        showError("No matching training found.");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Error loading exercises: " + e.getMessage());
                });
    }

    // Display the exercises in the LinearLayout
    private void displayExercises(List<Map<String, Object>> exercises) {
        exercisesLayout.removeAllViews(); // Clear previous entries

        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.get("name");
            Long reps = (Long) exercise.get("reps");
            Long sets = (Long) exercise.get("sets");

            TextView textView = new TextView(this);
            textView.setText(String.format("%s - %d sets, %d reps", name, sets, reps));
            textView.setTextSize(18);
            textView.setPadding(10, 10, 10, 10);

            // Allow editing of reps/sets on click
            textView.setOnClickListener(v -> openEditExerciseDialog(name, reps, sets));

            // Add each exercise to the layout
            exercisesLayout.addView(textView);
        }
    }

    // Open a dialog to add a new exercise
    private void openAddExerciseDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_exercise, null);

        EditText exerciseNameEditText = dialogView.findViewById(R.id.exercise_name);
        EditText repsEditText = dialogView.findViewById(R.id.exercise_reps);
        EditText setsEditText = dialogView.findViewById(R.id.exercise_sets);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Exercise")
                .setView(dialogView)
                .setPositiveButton("Add", (dialogInterface, i) -> {
                    String exerciseName = exerciseNameEditText.getText().toString();
                    int reps = Integer.parseInt(repsEditText.getText().toString());
                    int sets = Integer.parseInt(setsEditText.getText().toString());

                    addExercise(exerciseName, reps, sets);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    // Open a dialog to edit an existing exercise
    private void openEditExerciseDialog(String name, long reps, long sets) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_exercise, null);

        EditText exerciseNameEditText = dialogView.findViewById(R.id.exercise_name);
        EditText repsEditText = dialogView.findViewById(R.id.exercise_reps);
        EditText setsEditText = dialogView.findViewById(R.id.exercise_sets);

        // Pre-fill with existing values
        exerciseNameEditText.setText(name);
        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Exercise")
                .setView(dialogView)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String exerciseName = exerciseNameEditText.getText().toString();
                    int newReps = Integer.parseInt(repsEditText.getText().toString());
                    int newSets = Integer.parseInt(setsEditText.getText().toString());

                    updateExercise(exerciseName, newReps, newSets);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    // Method to add a new exercise to Firestore
    private void addExercise(String name, int reps, int sets) {
        showLoading(true);
        trainingRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
                    if (exercises != null) {
                        Map<String, Object> newExercise = new HashMap<>();
                        newExercise.put("name", name);
                        newExercise.put("reps", reps);
                        newExercise.put("sets", sets);

                        exercises.add(newExercise);

                        trainingRef.update("exercises", exercises)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    loadExercises(); // Refresh the list
                                    Toast.makeText(this, "Exercise added!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    showError("Error adding exercise: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Error fetching exercises: " + e.getMessage());
                });
    }

    // Method to update an existing exercise
    private void updateExercise(String name, int reps, int sets) {
        showLoading(true);
        trainingRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
                    if (exercises != null) {
                        for (Map<String, Object> exercise : exercises) {
                            if (exercise.get("name").equals(name)) {
                                exercise.put("reps", reps);
                                exercise.put("sets", sets);
                                break;
                            }
                        }

                        trainingRef.update("exercises", exercises)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    loadExercises(); // Refresh the list
                                    Toast.makeText(this, "Exercise updated!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    showError("Error updating exercise: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Error fetching exercises: " + e.getMessage());
                });
    }

    // Open a dialog to remove an exercise (to be implemented)
    private void openRemoveExerciseDialog() {
        // Implementation of exercise removal logic
        Snackbar.make(exercisesLayout, "Exercise removal not implemented yet.", Snackbar.LENGTH_SHORT).show();
    }

    // Start workout logic
    private void startWorkout() {
        Toast.makeText(this, "Workout started!", Toast.LENGTH_SHORT).show();
    }

    // Show or hide loading indicator
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Show error message in a Snackbar
    private void showError(String message) {
        Snackbar.make(exercisesLayout, message, Snackbar.LENGTH_LONG).show();
    }
}
