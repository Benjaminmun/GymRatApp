package student.inti.gymratdev3;

import android.app.AlertDialog;
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

    private Button addExerciseButton, removeExerciseButton, startWorkoutButton;
    private LinearLayout exercisesLayout;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String trainingName;
    private DocumentReference trainingRef;
    private ListenerRegistration trainingListener;

    private static final String TAG = "TrainingDetailActivity";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);

        // Retrieve training name from the intent
        trainingName = getIntent().getStringExtra("TRAINING_NAME");
        if (trainingName == null || trainingName.isEmpty()) {
            showError("Error: Training name is missing.");
            return;
        }

        // Initialize Firestore and UI elements
        db = FirebaseFirestore.getInstance();
        trainingRef = db.collection("trainings").document(trainingName);

        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        exercisesLayout = findViewById(R.id.exercises_layout);
        progressBar = findViewById(R.id.progress_bar);

        // Set button click listeners
        addExerciseButton.setOnClickListener(v -> openAddExerciseDialog());
//        removeExerciseButton.setOnClickListener(v -> openRemoveExerciseDialog());
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
        trainingListener = trainingRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                showLoading(false);
                showError("Error listening for exercise updates: " + e.getMessage());
                Log.e(TAG, "Error listening for updates", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                List<Map<String, Object>> exercises = (List<Map<String, Object>>) documentSnapshot.get("exercises");
                if (exercises == null || exercises.isEmpty()) {
                    exercisesLayout.removeAllViews(); // Clear if no exercises
                    showLoading(false);
                    showError("No exercises found for this training plan.");
                    return;
                }

                Log.d(TAG, "Exercises loaded successfully: " + exercises.toString());
                displayExercises(exercises);
                showLoading(false);
            } else {
                showLoading(false);
                showError("No matching training found.");
                exercisesLayout.removeAllViews();
            }
        });
    }

    private void displayExercises(List<Map<String, Object>> exercises) {
        exercisesLayout.removeAllViews(); // Clear previous views
        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.get("name");
            Long reps = (Long) exercise.get("reps");
            Long sets = (Long) exercise.get("sets");

            if (name == null || reps == null || sets == null) {
                Log.w(TAG, "Skipping exercise with missing information: " + exercise);
                continue; // Skip incomplete exercises
            }

            TextView textView = new TextView(this);
            textView.setText(String.format("%s - %d sets, %d reps", name, sets, reps));
            textView.setTextSize(18);
            textView.setPadding(10, 10, 10, 10);
            textView.setOnClickListener(v -> openEditExerciseDialog(name, reps, sets));

            exercisesLayout.addView(textView);
        }
    }

    private void openEditExerciseDialog(String oldName, Long oldReps, Long oldSets) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_exercise, null);

        EditText nameEditText = dialogView.findViewById(R.id.exercise_name);
        EditText repsEditText = dialogView.findViewById(R.id.exercise_reps);
        EditText setsEditText = dialogView.findViewById(R.id.exercise_sets);

        nameEditText.setText(oldName);
        repsEditText.setText(String.valueOf(oldReps));
        setsEditText.setText(String.valueOf(oldSets));

        new AlertDialog.Builder(this)
                .setTitle("Edit Exercise")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameEditText.getText().toString();
                    int newReps = Integer.parseInt(repsEditText.getText().toString());
                    int newSets = Integer.parseInt(setsEditText.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                        Log.d(TAG, "Exercise added successfully");
                        Toast.makeText(this, "Exercise added!", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        showError("Error adding exercise: " + e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            showLoading(false);
            showError("Error fetching exercises: " + e.getMessage());
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
