package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

public class WorkoutDetailActivity extends AppCompatActivity {

    private TextView workoutNameTextView, focusAreaTextView, equipmentTextView, preparationTextView, executionTextView;
    private Button editButton, deleteButton;
    private FirebaseFirestore db;
    private boolean isCustomExercise;
    private String workoutName, focusArea, equipment, preparation, execution;
    private static final int EDIT_EXERCISE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        // Change status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Get workout details from Intent and populate the views
        retrieveWorkoutDetailsFromIntent();

        // Set up button functionalities
        checkIfCustomExerciseAndSetupButtons();

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back); // Custom back icon
        }

        // Set click listener for the back button
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_EXERCISE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Close WorkoutDetailActivity
            finish();
        }
    }

    private void initializeViews() {
        workoutNameTextView = findViewById(R.id.workout_name);
        executionTextView = findViewById(R.id.execution);
        focusAreaTextView = findViewById(R.id.focus_area);
        equipmentTextView = findViewById(R.id.equipment);
        preparationTextView = findViewById(R.id.preparation);
        editButton = findViewById(R.id.edit_button);
        deleteButton = findViewById(R.id.delete_button);
    }

    private void retrieveWorkoutDetailsFromIntent() {
        Intent intent = getIntent();
        workoutName = intent.getStringExtra("workoutName");
        execution = intent.getStringExtra("execution");
        focusArea = intent.getStringExtra("focusArea");
        equipment = intent.getStringExtra("equipment");
        preparation = intent.getStringExtra("preparation");

        // Set workout data in TextViews
        workoutNameTextView.setText(workoutName);
        executionTextView.setText(execution);
        focusAreaTextView.setText(focusArea);
        equipmentTextView.setText(equipment);
        preparationTextView.setText(preparation);
    }

    // Check if it's a custom exercise and configure the buttons
    private void checkIfCustomExerciseAndSetupButtons() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("custom_exercises")
                .whereEqualTo("exercise_name", workoutName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        isCustomExercise = true;
                        setupEditButton();
                        setupDeleteButton();
                    } else {
                        isCustomExercise = false;
                        disableEditAndDeleteForDefaultExercises();
                    }
                });
    }

    private void setupEditButton() {
        editButton.setOnClickListener(v -> {
            if (isCustomExercise) {
                Intent intent = new Intent(WorkoutDetailActivity.this, EditExerciseActivity.class);
                intent.putExtra("exerciseName", workoutName);  // Pass the correct workout name
                intent.putExtra("execution", execution);
                intent.putExtra("focusArea", focusArea);
                intent.putExtra("equipment", equipment);
                intent.putExtra("preparation", preparation);
                startActivityForResult(intent, EDIT_EXERCISE_REQUEST_CODE);  // Track the result
            }
        });
    }

    private void setupDeleteButton() {
        deleteButton.setOnClickListener(v -> {
            if (isCustomExercise) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Exercise")
                        .setMessage("Are you sure you want to delete this exercise?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteCustomExercise())
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void deleteCustomExercise() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("custom_exercises")
                .whereEqualTo("exercise_name", workoutName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        db.collection("users")
                                .document(userId)
                                .collection("custom_exercises")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(WorkoutDetailActivity.this, "Workout deleted", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(WorkoutDetailActivity.this, "Error deleting workout", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void disableEditAndDeleteForDefaultExercises() {
        editButton.setOnClickListener(v -> {
            Toast.makeText(WorkoutDetailActivity.this, "Default workouts can't be edited", Toast.LENGTH_SHORT).show();
        });

        deleteButton.setOnClickListener(v -> {
            Toast.makeText(WorkoutDetailActivity.this, "Default workouts can't be deleted", Toast.LENGTH_SHORT).show();
        });
    }
}
