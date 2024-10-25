package student.inti.gymratdev3;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.*;
import java.util.*;

public class TrainingDetailActivity extends AppCompatActivity {

    private Button addExerciseButton, removeExerciseButton, startWorkoutButton;
    private LinearLayout exercisesLayout;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private DocumentReference trainingRef;
    private ListenerRegistration trainingListener;
    private List<String> availableExercises = new ArrayList<>();

    private static final String TAG = "TrainingDetailActivity";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);

        String trainingId = getIntent().getStringExtra("TRAINING_ID");
        if (isInvalidId(trainingId)) return;

        db = FirebaseFirestore.getInstance();
        trainingRef = db.collection("trainings").document(trainingId);

        initializeUI();
        loadAvailableExercises();
        attachTrainingListener();
    }

    private boolean isInvalidId(String id) {
        if (id == null || id.isEmpty()) {
            showError("Error: Training ID is missing.");
            finish();
            return true;
        }
        return false;
    }

    private void initializeUI() {
        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        exercisesLayout = findViewById(R.id.exercises_layout);
        progressBar = findViewById(R.id.progress_bar);

        addExerciseButton.setOnClickListener(v -> addExerciseField());
        removeExerciseButton.setOnClickListener(v -> removeLastExercise());
        startWorkoutButton.setOnClickListener(v -> startWorkout());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trainingListener != null) trainingListener.remove();
    }

    private void attachTrainingListener() {
        showLoading(true);
        Log.d(TAG, "Attaching listener to: " + trainingRef.getPath());

        trainingListener = trainingRef.addSnapshotListener((snapshot, e) -> {
            showLoading(false);
            if (e != null) {
                handleFirestoreError(e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                displayExercises((List<Map<String, Object>>) snapshot.get("exercises"));
            } else {
                showError("No matching training found.");
                exercisesLayout.removeAllViews();
            }
        });
    }

    private void handleFirestoreError(FirebaseFirestoreException e) {
        Log.e(TAG, "Firestore Error", e);
        showError("An error occurred: " + e.getMessage());
    }

    private void displayExercises(List<Map<String, Object>> exercises) {
        exercisesLayout.removeAllViews();
        if (exercises == null) return;

        for (Map<String, Object> exercise : exercises) {
            try {
                String name = Objects.requireNonNull((String) exercise.get("exercise"));
                int reps = ((Number) Objects.requireNonNull(exercise.get("reps"))).intValue();
                int sets = ((Number) Objects.requireNonNull(exercise.get("sets"))).intValue();
                addExerciseView(name, reps, sets);
            } catch (Exception e) {
                Log.w(TAG, "Invalid exercise data: " + exercise, e);
            }
        }
    }

    private void addExerciseView(String name, int reps, int sets) {
        View exerciseView = LayoutInflater.from(this)
                .inflate(R.layout.exercise_input_training_details, exercisesLayout, false);

        ((TextView) exerciseView.findViewById(R.id.exercise_name_text_view1)).setText(name);
        EditText repsEditText = exerciseView.findViewById(R.id.reps_edit_text1);
        EditText setsEditText = exerciseView.findViewById(R.id.sets_edit_text1);

        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        repsEditText.addTextChangedListener(new InputValidator(repsEditText));
        setsEditText.addTextChangedListener(new InputValidator(setsEditText));

        exercisesLayout.addView(exerciseView);
    }

    private void addExerciseField() {
        View exerciseLayout = getLayoutInflater()
                .inflate(R.layout.exercise_input_training_details_added, exercisesLayout, false);

        Spinner exerciseSpinner = exerciseLayout.findViewById(R.id.exercise_spinner);
        setupExerciseSpinner(exerciseSpinner);
        exercisesLayout.addView(exerciseLayout);
        Log.d(TAG, "Added new exercise field.");
    }

    private void removeLastExercise() {
        int childCount = exercisesLayout.getChildCount();
        if (childCount > 0) {
            exercisesLayout.removeViewAt(childCount - 1);
            Log.d(TAG, "Removed last exercise field.");
        } else {
            showSnackbar("No exercises to remove.");
        }
    }

    private void setupExerciseSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, availableExercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadAvailableExercises() {
        db.collection("default_workouts").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String exerciseName = doc.getString("exercise_name");
                    if (exerciseName != null) availableExercises.add(exerciseName);
                }
                if (availableExercises.isEmpty()) showSnackbar("No available exercises found.");
            } else {
                showError("Failed to load exercises.");
            }
        });
    }

    private List<Map<String, Object>> collectExercisesData() {
        List<Map<String, Object>> exercises = new ArrayList<>();

        for (int i = 0; i < exercisesLayout.getChildCount(); i++) {
            View exerciseView = exercisesLayout.getChildAt(i);
            Spinner exerciseSpinner = exerciseView.findViewById(R.id.exercise_spinner);
            EditText repsEditText = exerciseView.findViewById(R.id.reps_edit_text1);
            EditText setsEditText = exerciseView.findViewById(R.id.sets_edit_text1);

            String exerciseName = (String) exerciseSpinner.getSelectedItem();
            String repsText = repsEditText.getText().toString().trim();
            String setsText = setsEditText.getText().toString().trim();

            if (exerciseName == null || repsText.isEmpty() || setsText.isEmpty()) {
                showSnackbar("Please fill in all exercise fields.");
                return Collections.emptyList();
            }

            try {
                int reps = Integer.parseInt(repsText);
                int sets = Integer.parseInt(setsText);

                Map<String, Object> exercise = new HashMap<>();
                exercise.put("exercise", exerciseName);
                exercise.put("reps", reps);
                exercise.put("sets", sets);
                exercises.add(exercise);
            } catch (NumberFormatException e) {
                showSnackbar("Invalid input.");
                return Collections.emptyList();
            }
        }
        return exercises;
    }

    private void startWorkout() {
        List<Map<String, Object>> exercises = collectExercisesData();
        if (exercises.isEmpty()) return;

        showLoading(true);
        trainingRef.update("exercises", exercises)
                .addOnSuccessListener(aVoid -> {
                    showSnackbar("Workout started!");
                    Intent intent = new Intent(this, StartWorkoutActivity.class);
                    intent.putExtra("TRAINING_ID", trainingRef.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to update exercises", e);
                    showError("Failed to start workout: " + e.getMessage());
                });
    }

    private class InputValidator implements TextWatcher {
        private final EditText editText;

        public InputValidator(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            validateInput();
        }

        @Override
        public void afterTextChanged(Editable s) {}

        private void validateInput() {
            String input = editText.getText().toString().trim();
            if (input.isEmpty() || !input.matches("\\d+") || Integer.parseInt(input) <= 0) {
                editText.setError("Enter a valid number.");
                startWorkoutButton.setEnabled(false);
            } else {
                editText.setError(null);
                checkAllInputsValid();
            }
        }
    }

    private void checkAllInputsValid() {
        boolean allValid = true;
        for (int i = 0; i < exercisesLayout.getChildCount(); i++) {
            View view = exercisesLayout.getChildAt(i);
            EditText repsEditText = view.findViewById(R.id.reps_edit_text1);
            EditText setsEditText = view.findViewById(R.id.sets_edit_text1);

            if (repsEditText.getError() != null || setsEditText.getError() != null) {
                allValid = false;
                break;
            }
        }
        startWorkoutButton.setEnabled(allValid);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }
}
