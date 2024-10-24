
package student.inti.gymratdev3;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class TrainingDetailActivity extends AppCompatActivity {

    private Button addExerciseButton;
    private Button removeExerciseButton;
    private Button startWorkoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_detail);
        String trainingName = getIntent().getStringExtra("TRAINING_NAME");

        // Initialize buttons
        addExerciseButton = findViewById(R.id.add_exercise_button);
        removeExerciseButton = findViewById(R.id.remove_exercise_button);
        startWorkoutButton = findViewById(R.id.start_workout_button);

        // Set up button click listeners
        addExerciseButton.setOnClickListener(v -> {
            // Implement adding exercises logic here
        });

        removeExerciseButton.setOnClickListener(v -> {
            // Implement removing exercises logic here
        });

        startWorkoutButton.setOnClickListener(v -> {
            // Implement starting workout logic here
        });
    }
}
