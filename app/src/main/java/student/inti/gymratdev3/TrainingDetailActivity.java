package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class TrainingDetailActivity extends AppCompatActivity {

    private static final String TAG = "TrainingDetailActivity";

    private LinearLayout exercisesLayout;
    private Button addExerciseButton, removeExerciseButton, startWorkoutButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String trainingId;
    private final List<View> exerciseViews = new ArrayList<>();
    private final List<String> availableExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);

        initializeUI();
        setupFirestore();
        loadAvailableExercises();
        loadExercises();

        // Set listeners
        addExerciseButton.setOnClickListener(v -> addExerciseField());
        removeExerciseButton.setOnClickListener(v -> removeLastExercise());
        startWorkoutButton.setOnClickListener(v -> saveAndNavigateToWorkout());
    }

    // Initialize UI components
    private void initializeUI() {
        exercisesLayout = findViewById(R.id.exercises_layout);
        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        progressBar = findViewById(R.id.progress_bar);
    }

    // Setup Firestore and current user
    private void setupFirestore() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        trainingId = getIntent().getStringExtra("TRAINING_ID");

        if (trainingId == null) {
            showError("Invalid training ID.");
            finish();
        }
    }

    // Load existing exercises from Firestore
    private void loadExercises() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("trainings").document(trainingId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    List<Map<String, Object>> exercises =
                            (List<Map<String, Object>>) snapshot.get("exercises");

                    if (exercises != null) displayExercises(exercises);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Failed to load exercises.");
                });
    }

    // Display all exercises from Firestore
    private void displayExercises(List<Map<String, Object>> exercises) {
        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.get("exercise");
            int reps = ((Number) exercise.get("reps")).intValue();
            int sets = ((Number) exercise.get("sets")).intValue();
            addExerciseView(name, reps, sets);
        }
    }

    // Dynamically add an existing exercise view
    private void addExerciseView(String name, int reps, int sets) {
        View exerciseView = LayoutInflater.from(this)
                .inflate(R.layout.exercise_input_training_details, exercisesLayout, false);

        TextView exerciseNameText = exerciseView.findViewById(R.id.exercise_name_text_view1);
        EditText repsEditText = exerciseView.findViewById(R.id.reps_edit_text1);
        EditText setsEditText = exerciseView.findViewById(R.id.sets_edit_text1);

        exerciseNameText.setText(name);
        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        exercisesLayout.addView(exerciseView);
        exerciseViews.add(exerciseView);
    }

    // Add a new exercise field dynamically
    private void addExerciseField() {
        View exerciseLayout = getLayoutInflater()
                .inflate(R.layout.exercise_input_training_details_added, exercisesLayout, false);

        Spinner exerciseSpinner = exerciseLayout.findViewById(R.id.exercise_spinner);
        setupExerciseSpinner(exerciseSpinner);

        // Set up the exercise spinner with the available exercises
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableExercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exerciseSpinner.setAdapter(adapter);

        exercisesLayout.addView(exerciseLayout);
        exerciseViews.add(exerciseLayout);

        Log.d(TAG, "Added new exercise field.");
    }

    // Remove the last added exercise
    private void removeLastExercise() {
        if (!exerciseViews.isEmpty()) {
            View lastExercise = exerciseViews.remove(exerciseViews.size() - 1);
            exercisesLayout.removeView(lastExercise);
        } else {
            showError("No exercises to remove.");
        }
    }

    // Setup the exercise spinner with available exercises
    private void setupExerciseSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, availableExercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Load available exercises from Firestore
    private void loadAvailableExercises() {
        db.collection("default_workouts").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String exerciseName = doc.getString("exercise_name");
                            if (exerciseName != null) availableExercises.add(exerciseName);
                        }
                        if (availableExercises.isEmpty()) showError("No available exercises found.");
                    } else {
                        showError("Failed to load available exercises.");
                    }
                });
    }

    // Save exercises and navigate to StartWorkoutActivity
    private void saveAndNavigateToWorkout() {
        List<Map<String, Object>> exercises = new ArrayList<>();

        for (View view : exerciseViews) {
            Spinner spinner = view.findViewById(R.id.exercise_spinner);
            EditText repsEditText = view.findViewById(R.id.reps_edit_text);
            EditText setsEditText = view.findViewById(R.id.sets_edit_text);

            Map<String, Object> exerciseData = new HashMap<>();
            exerciseData.put("exercise", spinner.getSelectedItem().toString());
            exerciseData.put("reps", Integer.parseInt(repsEditText.getText().toString()));
            exerciseData.put("sets", Integer.parseInt(setsEditText.getText().toString()));

            exercises.add(exerciseData);
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("trainings").document(trainingId)
                .update("exercises", exercises)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    navigateToWorkout();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Failed to save exercises.");
                });
    }

    // Navigate to the workout activity
    private void navigateToWorkout() {
        Intent intent = new Intent(this, StartWorkoutActivity.class);
        startActivity(intent);
        intent.putExtra("TRAINING_ID", trainingId);
        finish();
    }

    // Show error message
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
