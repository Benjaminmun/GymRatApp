package student.inti.gymratdev3;

import android.media.MediaPlayer;
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

    private final int[] countdown = {3}; // Countdown start value
    private Handler countdownHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        // Change status bar color to match button color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

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
            pauseWorkout();
            showPauseDialog();
        });
        finishButton.setOnClickListener(v -> finishWorkout());

        // Start countdown before the workout begins
        startCountdown();
    }

    @Override
    public void onBackPressed() {
        // Pause the countdown timer
        countdownHandler.removeCallbacksAndMessages(null);

        new AlertDialog.Builder(this)
                .setTitle("Cancel Workout")
                .setMessage("Are you sure you want to cancel the workout? Your progress will not be saved.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    super.onBackPressed(); // Cancel the workout and return to the previous screen
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Resume the countdown if user cancels the dialog
                    resumeCountdown();
                })
                .show();
    }

    private void resumeCountdown() {
        // Restart the countdown from where it left off
        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    playTickSound();  // Play tick sound

                    countdownTextView.startAnimation(
                            android.view.animation.AnimationUtils.loadAnimation(
                                    StartWorkoutActivity.this, R.anim.scale_animation));

                    countdownTextView.setText(String.valueOf(countdown[0]--));
                    countdownHandler.postDelayed(this, 1000); // 1-second delay
                } else {
                    // Switch views after countdown finishes
                    countdownTextView.setVisibility(View.GONE);
                    timerTextView.setVisibility(View.VISIBLE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    trainingNameTextView.setVisibility(View.VISIBLE);

                    pauseButton.setVisibility(View.VISIBLE);
                    finishButton.setVisibility(View.VISIBLE);

                    startWorkout(); // Start workout automatically
                }
            }
        });
    }



    private void startCountdown() {
        countdownTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.GONE);
        exercisesContainer.setVisibility(View.GONE);
        trainingNameTextView.setVisibility(View.GONE);
        pauseButton.setVisibility(View.GONE);
        finishButton.setVisibility(View.GONE);

        countdownTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        countdownTextView.setGravity(Gravity.CENTER);

        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    playTickSound();  // Play tick sound

                    countdownTextView.startAnimation(
                            android.view.animation.AnimationUtils.loadAnimation(
                                    StartWorkoutActivity.this, R.anim.scale_animation));

                    countdownTextView.setText(String.valueOf(countdown[0]--));
                    countdownHandler.postDelayed(this, 1000); // 1-second delay
                } else {
                    // Switch views after countdown finishes
                    countdownTextView.setVisibility(View.GONE);
                    timerTextView.setVisibility(View.VISIBLE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    trainingNameTextView.setVisibility(View.VISIBLE);

                    pauseButton.setVisibility(View.VISIBLE);
                    finishButton.setVisibility(View.VISIBLE);

                    startWorkout(); // Start workout automatically
                }
            }
        });
    }

    private void playTickSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.tick);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
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
        // Check if at least one exercise has been completed
        if (completedExercises.isEmpty()) {
            Toast.makeText(this, "Please complete at least one exercise to finish the workout.", Toast.LENGTH_SHORT).show();
            return; // Prevent finishing the workout
        }

        if (isRunning) pauseWorkout();
        // If exercises are completed, proceed with finishing
        String totalTime = timerTextView.getText().toString();
        saveWorkoutHistory(totalTime);
        Toast.makeText(this, "Workout finished and saved!", Toast.LENGTH_SHORT).show();
        finish();
    }


    private void saveWorkoutHistory(String totalTime) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
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

    private void loadExercises() {
        trainingRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String trainingName = snapshot.getString("trainingName");
                trainingNameTextView.setText(trainingName != null ? trainingName : "Unnamed Training");

                exercises = (List<Map<String, Object>>) snapshot.get("exercises");
                if (exercises != null && !exercises.isEmpty()) {
                    displayExercises();
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
            addExerciseRow(exercises.get(i), i);
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
                    updatedExercise.put("kg", updatedKg);

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
