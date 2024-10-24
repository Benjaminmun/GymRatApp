package student.inti.gymratdev3;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditUserTrainingActivity extends AppCompatActivity {

    private EditText trainingNameEditText;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton, saveTrainingButton;
    private int exerciseCount = 0;
    private static final int MAX_EXERCISES = 10;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_training);

        // Initialize UI elements
        trainingNameEditText = findViewById(R.id.training_name_edit_text);
        exercisesContainer = findViewById(R.id.exercises_container);
        addExerciseButton = findViewById(R.id.add_exercise_button);
        saveTrainingButton = findViewById(R.id.save_training_button);

        // Initialize Firestore and get current user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Retrieve the initial training name if passed from the previous activity
        String initialTrainingName = getIntent().getStringExtra("TRAINING_NAME");
        if (initialTrainingName != null) {
            trainingNameEditText.setText(initialTrainingName);
        }

        // Add listener to the Add Exercise button
        addExerciseButton.setOnClickListener(v -> {
            if (exerciseCount < MAX_EXERCISES) {
                addExerciseField();
            } else {
                Toast.makeText(this, "You can only add up to 10 exercises.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button listener
        saveTrainingButton.setOnClickListener(v -> saveTrainingToFirestore());
    }

    // Method to add a new exercise input field
    private void addExerciseField() {
        EditText exerciseEditText = new EditText(this);
        exerciseEditText.setHint("Exercise " + (exerciseCount + 1));
        exercisesContainer.addView(exerciseEditText);
        exerciseCount++;
    }

    // Method to save training and exercises to Firestore
    private void saveTrainingToFirestore() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String trainingName = trainingNameEditText.getText().toString();
        if (trainingName.isEmpty()) {
            Toast.makeText(this, "Training name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect all exercises entered by the user
        ArrayList<String> exercises = new ArrayList<>();
        for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
            View view = exercisesContainer.getChildAt(i);
            if (view instanceof EditText) {
                String exercise = ((EditText) view).getText().toString();
                if (!exercise.isEmpty()) {
                    exercises.add(exercise);
                }
            }
        }

        // Prepare training data
        Map<String, Object> trainingData = new HashMap<>();
        trainingData.put("userId", currentUser.getUid());
        trainingData.put("trainingName", trainingName);
        trainingData.put("exercises", exercises);

        // Save training to Firestore
        db.collection("trainings").add(trainingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Training saved successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Go back to the previous activity
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving training", Toast.LENGTH_SHORT).show());
    }
}
