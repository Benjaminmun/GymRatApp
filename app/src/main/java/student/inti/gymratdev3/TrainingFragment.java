package student.inti.gymratdev3;

import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class TrainingFragment extends Fragment {

    private TextView timerTextView;
    private Button startButton, pauseButton, resumeButton, nextWorkoutButton;

    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeBuff = 0L;
    private long updatedTime = 0L;

    private Handler handler;
    private Runnable timerRunnable;
    private Runnable countdownRunnable;

    private boolean isRunning = false;
    private int countdownTime = 3; // 3 seconds countdown

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);

        timerTextView = view.findViewById(R.id.timerTextView);
        startButton = view.findViewById(R.id.startButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        resumeButton = view.findViewById(R.id.resumeButton);
        nextWorkoutButton = view.findViewById(R.id.nextWorkoutButton);

        handler = new Handler();

        // Timer Runnable
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeInMilliseconds = System.currentTimeMillis() - startTime;
                updatedTime = timeBuff + timeInMilliseconds;

                int secs = (int) (updatedTime / 1000);
                int mins = secs / 60;
                int hrs = mins / 60;
                secs = secs % 60;
                int deciSecs = (int) (updatedTime % 1000) / 100; // Calculate deciseconds

                // Update the timer text view to include deciseconds
                timerTextView.setText(String.format("%02d:%02d:%02d.%d", hrs, mins % 60, secs, deciSecs));

                handler.postDelayed(this, 100); // Update every 100ms for deciseconds
            }
        };

        // Countdown Runnable
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdownTime > 0) {
                    timerTextView.setText(String.valueOf(countdownTime));
                    countdownTime--;
                    handler.postDelayed(this, 1000); // Countdown every second
                } else {
                    // Start the actual timer after countdown
                    startTimer();
                }
            }
        };

        // Start Button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    countdownTime = 3; // Reset countdown time
                    handler.postDelayed(countdownRunnable, 0);
                    isRunning = true;
                }
            }
        });

        // Pause Button
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    timeBuff += timeInMilliseconds; // Accumulate elapsed time
                    handler.removeCallbacks(timerRunnable); // Stop timer updates
                    isRunning = false; // Mark timer as not running
                }
            }
        });

        // Resume Button
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    // Adjust start time for resuming
                    startTime = System.currentTimeMillis() - timeBuff; // Set start time
                    countdownTime = 3;
                    handler.postDelayed(timerRunnable, 0); // Start the timer again
                    isRunning = true; // Mark timer as running
                }
            }
        });

        // Next Workout Button
        nextWorkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset Timer and move to the next workout
                timeBuff = 0L;
                timeInMilliseconds = 0L;
                updatedTime = 0L;
                timerTextView.setText("00:00:00.0"); // Reset display to initial state
                handler.removeCallbacks(timerRunnable); // Stop the timer
                handler.removeCallbacks(countdownRunnable); // Stop countdown if running
                isRunning = false; // Reset running state
                countdownTime = 3; // Reset countdown time
            }
        });

        return view;
    }

    private void startTimer() {
        startTime = System.currentTimeMillis(); // Initialize start time
        handler.postDelayed(timerRunnable, 0); // Start updating the timer
        isRunning = true; // Mark timer as running
    }
}
