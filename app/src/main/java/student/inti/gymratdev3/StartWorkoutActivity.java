package student.inti.gymratdev3;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StartWorkoutActivity extends AppCompatActivity {

    private TextView timerTextView, countdownTextView, trainingNameTextView;
    private LinearLayout exercisesContainer;
    private Button pauseButton, finishButton;

    private Handler handler = new Handler();
    private long startTime = 0L, timeInMillis = 0L, pauseOffset = 0L;
    private boolean isRunning = false;

    private FirebaseFirestore db;
    private DocumentReference trainingRef;
    private List<Map<String, Object>> exercises;
    private List<Map<String, Object>> completedExercises = new ArrayList<>();
    private static final String TAG = "StartWorkoutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        // Initialize views
        timerTextView = findViewById(R.id.timerTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        exercisesContainer = findViewById(R.id.exercisesContainer);
        pauseButton = findViewById(R.id.pauseButton);
        finishButton = findViewById(R.id.finishButton);
        trainingNameTextView = findViewById(R.id.trainingNameTextView);

        // Ensure only countdown is shown initially
        countdownTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.GONE);
        exercisesContainer.setVisibility(View.GONE);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        String trainingId = getIntent().getStringExtra("TRAINING_ID");
        trainingRef = db.collection("trainings").document(trainingId);

        loadExercises();

        // Button listeners
        pauseButton.setOnClickListener(v -> {
            pauseWorkout();  // Pause the workout if it's running
            showPauseDialog(); // Display the workout options dialog
        });
        finishButton.setOnClickListener(v -> finishWorkout());

        // Start countdown before the workout begins
        startCountdown();
    }

    private void startCountdown() {
        // Show only the countdown and hide other views
        countdownTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.GONE);
        exercisesContainer.setVisibility(View.GONE);
        trainingNameTextView.setVisibility(View.GONE); // Hide the training name

        // Hide pause and finish buttons during the countdown
        pauseButton.setVisibility(View.GONE);
        finishButton.setVisibility(View.GONE);

        // Ensure the countdown is centered
        countdownTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        countdownTextView.setGravity(Gravity.CENTER);

        final int[] countdown = {3}; // Adjust starting value as needed
        Handler countdownHandler = new Handler();
        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    countdownTextView.setText(String.valueOf(countdown[0]--));
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // Switch views after countdown finishes
                    countdownTextView.setVisibility(View.GONE);
                    timerTextView.setVisibility(View.VISIBLE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    trainingNameTextView.setVisibility(View.VISIBLE); // Show training name

                    // Show pause and finish buttons once the workout starts
                    pauseButton.setVisibility(View.VISIBLE);
                    finishButton.setVisibility(View.VISIBLE);

                    startWorkout(); // Automatically start the workout after countdown
                }
            }
        });
    }


    private void loadExercises() {
        trainingRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Display the training name using the correct key from Firestore
                String trainingName = snapshot.getString("trainingName"); // Updated key to match your data structure
                if (trainingName != null) {
                    trainingNameTextView.setText(trainingName);
                } else {
                    trainingNameTextView.setText("Unnamed Training");
                }

                // Load exercises from the snapshot
                exercises = (List<Map<String, Object>>) snapshot.get("exercises");
                if (exercises != null && !exercises.isEmpty()) {
                    displayExercises(); // Call a method to display the exercises in the UI
                } else {
                    Toast.makeText(this, "No exercises found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Training data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to load exercises", e));
    }


    private void displayExercises() {
        for (int i = 0; i < exercises.size(); i++) {
            Map<String, Object> exercise = exercises.get(i);
            addExerciseRow(exercise, i);
        }
    }

    private void addExerciseRow(Map<String, Object> exercise, int index) {
        View exerciseRow = getLayoutInflater().inflate(R.layout.exercise_row, exercisesContainer, false);

        TextView exerciseNameText = exerciseRow.findViewById(R.id.exerciseNameText);
        EditText repsEditText = exerciseRow.findViewById(R.id.repsEditText);
        EditText setsEditText = exerciseRow.findViewById(R.id.setsEditText);
        EditText kgEditText = exerciseRow.findViewById(R.id.kgEditText);
        CheckBox doneCheckBox = exerciseRow.findViewById(R.id.doneCheckBox);

        String name = (String) exercise.get("exercise");
        int reps = ((Number) exercise.get("reps")).intValue();
        int sets = ((Number) exercise.get("sets")).intValue();

        exerciseNameText.setText(name);
        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        doneCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                try {
                    int updatedReps = Integer.parseInt(repsEditText.getText().toString());
                    int updatedSets = Integer.parseInt(setsEditText.getText().toString());
                    float updatedKg = Float.parseFloat(kgEditText.getText().toString());

                    Map<String, Object> updatedExercise = new HashMap<>();
                    updatedExercise.put("exercise", name);
                    updatedExercise.put("reps", updatedReps);
                    updatedExercise.put("sets", updatedSets);
                    updatedExercise.put("kg", updatedKg);  // Add weight (kg)

                    repsEditText.setEnabled(false);
                    setsEditText.setEnabled(false);
                    kgEditText.setEnabled(false);
                    completedExercises.add(updatedExercise);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter valid numbers.", Toast.LENGTH_SHORT).show();
                    doneCheckBox.setChecked(false);
                }
            } else {
                repsEditText.setEnabled(true);
                setsEditText.setEnabled(true);
                kgEditText.setEnabled(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    completedExercises.removeIf(e -> e.get("exercise").equals(name));
                }
            }
        });

        exercisesContainer.addView(exerciseRow);
    }


    private void startWorkout() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            handler.post(timerRunnable);
            isRunning = true;
            Toast.makeText(this, "Workout started!", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseWorkout() {
        if (isRunning) {
            pauseOffset += System.currentTimeMillis() - startTime;
            handler.removeCallbacks(timerRunnable);
            isRunning = false;
            Toast.makeText(this, "Workout paused!", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishWorkout() {
        if (isRunning) pauseWorkout();

        if (completedExercises.size() > 0) {
            String totalTime = timerTextView.getText().toString(); // Capture the final time
            saveWorkoutHistory(totalTime); // Pass the total time to be saved
            Toast.makeText(this, "Workout finished and saved!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No exercises completed. Nothing saved.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }


    private void saveWorkoutHistory(String totalTime) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm    ", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        String date = dateFormat.format(new Date());

        Map<String, Object> workoutHistory = new HashMap<>();
        workoutHistory.put("date", date);
        workoutHistory.put("totalTime", totalTime);
        workoutHistory.put("exercises", completedExercises);

        db.collection("users")
                .document(userId)
                .collection("workout_history")
                .add(workoutHistory)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Workout history saved."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save workout history", e));
    }



    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timeInMillis = System.currentTimeMillis() - startTime + pauseOffset;
            int seconds = (int) (timeInMillis / 1000) % 60;
            int minutes = (int) (timeInMillis / 1000) / 60;

            String time = String.format("%02d:%02d", minutes, seconds);
            timerTextView.setText(time);
            handler.postDelayed(this, 1000);
        }
    };

    public void showPauseDialog() {
        if (isRunning) pauseWorkout();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_workout_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnResume = dialogView.findViewById(R.id.btnResume);
        Button btnRestart = dialogView.findViewById(R.id.btnRestart);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnResume.setOnClickListener(v -> {
            startWorkout();
            dialog.dismiss();
        });

        btnRestart.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Restart Workout")
                    .setMessage("Are you sure you want to restart the workout?")
                    .setPositiveButton("Yes", (confirmDialog, which) -> {
                        restartWorkout();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (confirmDialog, which) -> confirmDialog.dismiss())
                    .show();
        });

        btnCancel.setOnClickListener(v -> finish());

        dialog.show();
    }

    private void restartWorkout() {
        pauseOffset = 0;
        handler.removeCallbacks(timerRunnable);
        timerTextView.setText("00:00");
        completedExercises.clear();
        exercisesContainer.removeAllViews();
        loadExercises();
        startCountdown();
    }
}