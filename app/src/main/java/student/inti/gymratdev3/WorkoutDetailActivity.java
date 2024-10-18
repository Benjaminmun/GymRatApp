package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class WorkoutDetailActivity extends AppCompatActivity {

    private TextView workoutNameTextView, workoutDetailsTextView, focusAreaTextView, equipmentTextView, preparationTextView;
    private Button editButton, deleteButton;
    private FirebaseFirestore db;
    private String exerciseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        workoutNameTextView = findViewById(R.id.workout_name);
        workoutDetailsTextView = findViewById(R.id.workout_details);
        focusAreaTextView = findViewById(R.id.focus_area);
        equipmentTextView = findViewById(R.id.equipment);
        preparationTextView = findViewById(R.id.preparation);
        editButton = findViewById(R.id.edit_button);
        deleteButton = findViewById(R.id.delete_button);

        // Get workout data from intent
        Intent intent = getIntent();
        String workoutName = intent.getStringExtra("workoutName");
        String[] workoutDetails = intent.getStringArrayExtra("workoutDetails");
        String focusArea = intent.getStringExtra("focusArea");
        String equipment = intent.getStringExtra("equipment");
        String preparation = intent.getStringExtra("preparation");
        exerciseId = intent.getStringExtra("exerciseId"); // Get exerciseId to differentiate between user-created and default

        // Set workout data to views
        workoutNameTextView.setText(workoutName);
        if (workoutDetails != null) {
            StringBuilder details = new StringBuilder();
            for (String detail : workoutDetails) {
                details.append(detail).append("\n");
            }
            workoutDetailsTextView.setText(details.toString());
        }
        focusAreaTextView.setText(focusArea);
        equipmentTextView.setText(equipment);
        preparationTextView.setText(preparation);

        // Edit button functionality
        editButton.setOnClickListener(v -> {
            if (exerciseId != null) {
                Intent editIntent = new Intent(WorkoutDetailActivity.this, EditExerciseActivity.class);
                editIntent.putExtra("exerciseId", exerciseId);
                editIntent.putExtra("exerciseName", workoutName);
                // Pass new attributes to edit activity
                editIntent.putExtra("focusArea", focusArea);
                editIntent.putExtra("equipment", equipment);
                editIntent.putExtra("preparation", preparation);
                if (workoutDetails != null && workoutDetails.length > 1) {
                    editIntent.putExtra("exerciseExecution", workoutDetails[1].replace("Execution: ", ""));
                } else {
                    editIntent.putExtra("exerciseExecution", "");
                }
                startActivity(editIntent);
            } else {
                Toast.makeText(WorkoutDetailActivity.this, "Default workouts can't be edited", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete button functionality
        deleteButton.setOnClickListener(v -> {
            if (exerciseId != null) {
                db.collection("exercises").document(exerciseId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(WorkoutDetailActivity.this, "Workout deleted", Toast.LENGTH_SHORT).show();
                            finish();  // Close activity after deletion
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(WorkoutDetailActivity.this, "Error deleting workout", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(WorkoutDetailActivity.this, "Default workouts can't be deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
