package student.inti.gymratdev3;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StartWorkoutActivity extends AppCompatActivity {

    private TextView timerTextView;
    private LinearLayout exercisesContainer;
    private Button startButton, pauseButton, resumeButton, finishButton;

    private Handler handler = new Handler();
    private long startTime = 0L, timeInMillis = 0L, pauseOffset = 0L;
    private boolean isRunning = false;

    private FirebaseFirestore db;
    private DocumentReference trainingRef;
    private List<Map<String, Object>> exercises;
    private static final String TAG = "StartWorkoutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        timerTextView = findViewById(R.id.timerTextView);
        exercisesContainer = findViewById(R.id.exercisesContainer);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        finishButton = findViewById(R.id.finishButton);

        db = FirebaseFirestore.getInstance();
        String trainingId = getIntent().getStringExtra("TRAINING_ID");
        trainingRef = db.collection("trainings").document(trainingId);

        loadExercises();

        startButton.setOnClickListener(v -> startWorkout());
        pauseButton.setOnClickListener(v -> pauseWorkout());
        resumeButton.setOnClickListener(v -> resumeWorkout());
        finishButton.setOnClickListener(v -> finishWorkout());
    }

    private void loadExercises() {
        trainingRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                exercises = (List<Map<String, Object>>) snapshot.get("exercises");
                if (exercises != null && !exercises.isEmpty()) {
                    displayExercises();
                }
            } else {
                Toast.makeText(this, "No exercises found.", Toast.LENGTH_SHORT).show();
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
        CheckBox doneCheckBox = exerciseRow.findViewById(R.id.doneCheckBox);

        String name = (String) exercise.get("exercise");
        int reps = ((Number) exercise.get("reps")).intValue();
        int sets = ((Number) exercise.get("sets")).intValue();

        exerciseNameText.setText(name);
        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        doneCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                repsEditText.setEnabled(false);
                setsEditText.setEnabled(false);
                moveToNextExercise(index + 1);
            }
        });

        exercisesContainer.addView(exerciseRow);
    }

    private void moveToNextExercise(int nextIndex) {
        if (nextIndex < exercises.size()) {
            View nextExerciseRow = exercisesContainer.getChildAt(nextIndex);
            nextExerciseRow.requestFocus();
        } else {
            Toast.makeText(this, "All exercises completed!", Toast.LENGTH_SHORT).show();
        }
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

    private void resumeWorkout() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            handler.post(timerRunnable);
            isRunning = true;
            Toast.makeText(this, "Workout resumed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishWorkout() {
        if (isRunning) pauseWorkout();
        saveWorkoutHistory();
        Toast.makeText(this, "Workout finished!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveWorkoutHistory() {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        Map<String, Object> workoutHistory = new HashMap<>();
        workoutHistory.put("date", date);
        workoutHistory.put("exercises", exercises);

        db.collection("workout_history").add(workoutHistory)
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
}

