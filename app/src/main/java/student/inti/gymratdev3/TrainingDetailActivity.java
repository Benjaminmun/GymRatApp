package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

    private Handler saveHandler = new Handler();
    private Runnable saveRunnable;

    private final List<View> exerciseViews = new ArrayList<>();
    private final List<String> availableExercises = new ArrayList<>();

    private boolean isDataChanged = false; // Track if user has made changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);

        initializeUI();
        setupFirestore();
        loadAvailableExercises();
        loadExercises();

        // Set listeners
        addExerciseButton.setOnClickListener(v -> {
            addExerciseField();
            isDataChanged = true; // Mark as changed when user adds an exercise
        });

        removeExerciseButton.setOnClickListener(v -> {
            removeLastExercise();
            isDataChanged = true; // Mark as changed when user removes an exercise
        });

        startWorkoutButton.setOnClickListener(v -> {
            if (isDataChanged) {
                Log.d(TAG, "Changes detected. Saving and starting workout...");
                saveAndNavigateToWorkout();
            } else {
                Log.d(TAG, "No changes detected. Navigating to workout...");
                navigateToWorkout(); // No changes, navigate directly
            }
        });
    }



    private void initializeUI() {
        exercisesLayout = findViewById(R.id.exercises_layout);
        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        progressBar = findViewById(R.id.progress_bar);

        startWorkoutButton.setEnabled(true);
        startWorkoutButton.setVisibility(View.VISIBLE);
    }

    private void setupFirestore() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        trainingId = getIntent().getStringExtra("TRAINING_ID");

        if (trainingId == null) {
            showError("Invalid training ID.");
            finish();
        }
    }

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

    private void displayExercises(List<Map<String, Object>> exercises) {
        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.get("exercise");
            int reps = ((Number) exercise.get("reps")).intValue();
            int sets = ((Number) exercise.get("sets")).intValue();
            addExerciseView(name, reps, sets);
        }
    }

    private void addExerciseView(String name, int reps, int sets) {
        View exerciseView = LayoutInflater.from(this)
                .inflate(R.layout.exercise_input_training_details, exercisesLayout, false);

        TextView exerciseNameText = exerciseView.findViewById(R.id.exercise_name_text_view1);
        EditText repsEditText = exerciseView.findViewById(R.id.reps_edit_text1);
        EditText setsEditText = exerciseView.findViewById(R.id.sets_edit_text1);

        exerciseNameText.setText(name);
        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        // Monitor changes in reps or sets to mark the data as changed
        repsEditText.addTextChangedListener(createTextWatcher());
        setsEditText.addTextChangedListener(createTextWatcher());

        exercisesLayout.addView(exerciseView);
        exerciseViews.add(exerciseView);
    }

    private void addExerciseField() {
        View exerciseLayout = getLayoutInflater()
                .inflate(R.layout.exercise_input_training_details_added, exercisesLayout, false);

        Spinner exerciseSpinner = exerciseLayout.findViewById(R.id.exercise_spinner);
        EditText repsEditText = exerciseLayout.findViewById(R.id.reps_edit_text);
        EditText setsEditText = exerciseLayout.findViewById(R.id.sets_edit_text);

        setupExerciseSpinner(exerciseSpinner);

        // Monitor changes in reps or sets to mark the data as changed
        repsEditText.addTextChangedListener(createTextWatcher());
        setsEditText.addTextChangedListener(createTextWatcher());

        exercisesLayout.addView(exerciseLayout);
        exerciseViews.add(exerciseLayout);

        isDataChanged = true; // Mark data as changed when user adds an exercise
        scheduleSave();
        Log.d(TAG, "Added new exercise field.");
    }


    private void removeLastExercise() {
        if (!exerciseViews.isEmpty()) {
            View lastExercise = exerciseViews.remove(exerciseViews.size() - 1);
            exercisesLayout.removeView(lastExercise);
            isDataChanged = true; // Mark data as changed when user removes an exercise
            scheduleSave();
        } else {
            showError("No exercises to remove.");
        }
    }


    private void setupExerciseSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, availableExercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadAvailableExercises() {
        db.collection("default_workouts").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String exerciseName = doc.getString("exercise_name");
                            if (exerciseName != null) availableExercises.add(exerciseName);
                        }
                    } else {
                        showError("Failed to load available exercises.");
                    }
                });
    }

    private void saveAndNavigateToWorkout() {
        saveExercises(() -> navigateToWorkout());
    }

    private void saveExercises(Runnable onSuccess) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        for (View view : exerciseViews) {
            Spinner spinner = view.findViewById(R.id.exercise_spinner);
            TextView exerciseNameText = view.findViewById(R.id.exercise_name_text_view1);
            EditText repsEditText = view.findViewById(R.id.reps_edit_text);
            if (repsEditText == null) {
                repsEditText = view.findViewById(R.id.reps_edit_text1);
            }
            EditText setsEditText = view.findViewById(R.id.sets_edit_text);
            if (setsEditText == null) {
                setsEditText = view.findViewById(R.id.sets_edit_text1);
            }

            Map<String, Object> exerciseData = new HashMap<>();
            if (spinner != null) {
                // New exercise added
                exerciseData.put("exercise", spinner.getSelectedItem().toString());
            } else if (exerciseNameText != null) {
                // Existing exercise
                exerciseData.put("exercise", exerciseNameText.getText().toString());
            } else {
                continue; // Skip if neither spinner nor text view is found
            }

            try {
                int reps = Integer.parseInt(repsEditText.getText().toString());
                int sets = Integer.parseInt(setsEditText.getText().toString());
                exerciseData.put("reps", reps);
                exerciseData.put("sets", sets);
            } catch (NumberFormatException e) {
                showError("Invalid number format in reps or sets.");
                continue;
            }

            exercises.add(exerciseData);
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("trainings").document(trainingId)
                .update("exercises", exercises)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    isDataChanged = false; // Reset the flag
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Failed to save exercises.");
                });
    }


    private void navigateToWorkout() {
        Intent intent = new Intent(this, StartWorkoutActivity.class);
        intent.putExtra("TRAINING_ID", trainingId);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Helper method to track changes in EditText fields
    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isDataChanged = true; // Mark data as changed if user edits text
                // Remove any pending saves
                saveHandler.removeCallbacks(saveRunnable);
                // Schedule a new save after a delay
                saveRunnable = () -> saveExercises(null);
                saveHandler.postDelayed(saveRunnable, 2000); // Save after 2 seconds of inactivity
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }

    private void scheduleSave() {
        // Remove any pending saves
        saveHandler.removeCallbacks(saveRunnable);
        // Schedule a new save after a delay
        saveRunnable = () -> saveExercises(null);
        saveHandler.postDelayed(saveRunnable, 2000); // Save after 2 seconds
    }
}