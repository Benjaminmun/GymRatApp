package student.inti.gymratdev3;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class StartWorkoutActivity extends AppCompatActivity {

    private TextView timerTextView;
    private Button startButton, pauseButton, resumeButton, finishButton;
    private CheckBox workoutDoneCheckBox;

    private Handler handler = new Handler();
    private long startTime = 0L, timeInMillis = 0L, pauseOffset = 0L;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        timerTextView = findViewById(R.id.timerTextView);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        finishButton = findViewById(R.id.finishButton);
        workoutDoneCheckBox = findViewById(R.id.workoutDoneCheckBox);

        startButton.setOnClickListener(v -> startWorkout());
        pauseButton.setOnClickListener(v -> pauseWorkout());
        resumeButton.setOnClickListener(v -> resumeWorkout());
        finishButton.setOnClickListener(v -> finishWorkout());
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
        if (isRunning) {
            pauseWorkout();
        }

        if (workoutDoneCheckBox.isChecked()) {
            long totalTime = pauseOffset;
            saveWorkoutTimeToDatabase(totalTime);
            Toast.makeText(this, "Workout finished!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Please complete the workout before finishing.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveWorkoutTimeToDatabase(long totalTime) {
        // Add your logic here to save the totalTime into your database
        // Example: SQLite, Firebase, etc.
        // Convert milliseconds to seconds/minutes for better readability
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
