package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CountdownActivity extends AppCompatActivity {

    private TextView countdownTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        countdownTextView = findViewById(R.id.countdownTextView);

        startCountdown(); // Start countdown when this activity loads
    }

    private void startCountdown() {
        final int[] countdown = {3}; // Adjust as needed
        Handler countdownHandler = new Handler();
        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    countdownTextView.setText(String.valueOf(countdown[0]--));
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // After countdown finishes, move to StartWorkoutActivity
                    Intent intent = new Intent(CountdownActivity.this, StartWorkoutActivity.class);
                    startActivity(intent);
                    finish(); // Close the countdown activity
                }
            }
        });
    }
}
