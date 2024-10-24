// EditUserTrainingActivity.java
package student.inti.gymratdev3;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditUserTrainingActivity extends AppCompatActivity {

    private EditText trainingNameEditText;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton, saveTrainingButton;
    private int exerciseCount = 0;
    private static final int MAX_EXERCISES = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_training);

        // Initialize UI elements
        trainingNameEditText = findViewById(R.id.training_name_edit_text);
        exercisesContainer = findViewById(R.id.exercises_container);
        addExerciseButton = findViewById(R.id.add_exercise_button);
        saveTrainingButton = findViewById(R.id.save_training_button);

        // Retrieve the initial training name if passed from the previous activity
        String initialTrainingName = getIntent().getStringExtra("TRAINING_NAME");
        if (initialTrainingName != null) {
            trainingNameEditText.setText(initialTrainingName);
        }

        // Add a listener to add exercises
        addExerciseButton.setOnClickListener(v -> {
            if (exerciseCount < MAX_EXERCISES) {
                addExerciseField();
            } else {
                Toast.makeText(this, "You can only add up to 10 exercises.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button listener
        saveTrainingButton.setOnClickListener(v -> {
            // Retrieve the updated training name
            String updatedTrainingName = trainingNameEditText.getText().toString();

            // Retrieve all exercise names
            StringBuilder exercises = new StringBuilder();
            for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
                View view = exercisesContainer.getChildAt(i);
                if (view instanceof EditText) {
                    exercises.append(((EditText) view).getText().toString()).append("\n");
                }
            }

            // Here, you can save the training data (name and exercises) to Firebase or SQLite
            Toast.makeText(this, "Training Saved: " + updatedTrainingName, Toast.LENGTH_SHORT).show();
        });
    }

    // Method to add a new exercise input field
    private void addExerciseField() {
        EditText exerciseEditText = new EditText(this);
        exerciseEditText.setHint("Exercise " + (exerciseCount + 1));
        exercisesContainer.addView(exerciseEditText);
        exerciseCount++;
    }
}
