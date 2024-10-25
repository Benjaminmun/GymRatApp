package student.inti.gymratdev3;

import android.widget.ArrayAdapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditUserTrainingActivity extends AppCompatActivity {

    private EditText trainingNameEditText;
    private LinearLayout exercisesContainer;
    private Button addExerciseButton, saveTrainingButton;
    private int exerciseCount = 0;
    private static final int MAX_EXERCISES = 10;
    private static final int MAX_EXERCISE_NAME_LENGTH = 30;
    private ArrayList<String> exerciseNames = new ArrayList<>();


    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialog;

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

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving training...");
        progressDialog.setCancelable(false);

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
        loadExerciseNames();

    }

    // Method to query exercise names from Firestore and populate exerciseNames list
    private void loadExerciseNames() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("custom_exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    exerciseNames.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String exerciseName = document.getString("exerciseName");
                        if (exerciseName != null) {
                            exerciseNames.add(exerciseName);
                        }
                    }
                    // Call loadTrainingState only after exerciseNames is populated
                    loadTrainingState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load exercises", Toast.LENGTH_SHORT).show();
                });
    }

    // Method to add a new exercise input field with the Spinner for exercise selection
    private void addExerciseField() {
        LinearLayout exerciseLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.exercise_input_layout, null);
        Spinner exerciseSpinner = exerciseLayout.findViewById(R.id.exercise_edit_text);
        EditText repsEditText = exerciseLayout.findViewById(R.id.reps_edit_text);
        EditText setsEditText = exerciseLayout.findViewById(R.id.sets_edit_text);



        // Set the ArrayAdapter to populate the exercise spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exerciseNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exerciseSpinner.setAdapter(adapter);


        // Add real-time validation for reps and sets
        repsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    repsEditText.setError("Reps cannot be empty");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    setsEditText.setError("Sets cannot be empty");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        exercisesContainer.addView(exerciseLayout);
        exerciseCount++;
    }


    // Overriding the back button to ask whether the user wants to save the changes
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Do you want to save changes?")
                .setPositiveButton("Yes", (dialog, which) -> saveTrainingToFirestore())
                .setNegativeButton("No", (dialog, which) -> {
                    clearTrainingState();  // Clear saved state if user chooses not to save
                    super.onBackPressed();
                })
                .show();
    }

    // Inside saveTrainingToFirestore, update to retrieve selected exercise names from the Spinner
    private void saveTrainingToFirestore() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String trainingName = trainingNameEditText.getText().toString().trim();
        if (trainingName.isEmpty()) {
            Toast.makeText(this, "Training name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Map<String, Object>> exercises = new ArrayList<>();
        for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
            View view = exercisesContainer.getChildAt(i);
            Spinner exerciseSpinner = view.findViewById(R.id.exercise_edit_text);
            EditText repsEditText = view.findViewById(R.id.reps_edit_text);
            EditText setsEditText = view.findViewById(R.id.sets_edit_text);

            String exercise = exerciseSpinner.getSelectedItem().toString();
            String reps = repsEditText.getText().toString().trim();
            String sets = setsEditText.getText().toString().trim();

            if (exercise.isEmpty() || reps.isEmpty() || sets.isEmpty()) {
                Toast.makeText(this, "Exercise, reps, and sets cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> exerciseData = new HashMap<>();
            exerciseData.put("exercise", exercise);
            exerciseData.put("reps", Integer.parseInt(reps));
            exerciseData.put("sets", Integer.parseInt(sets));

            exercises.add(exerciseData);
        }

        // Save to Firestore as before
    }

    // Save the current state of the activity in SharedPreferences
    private void saveTrainingState() {
        SharedPreferences prefs = getSharedPreferences("UserTraining", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("trainingName", trainingNameEditText.getText().toString());

        // Save exercises, reps, and sets
        editor.putInt("exerciseCount", exerciseCount);
        for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
            View view = exercisesContainer.getChildAt(i);
            EditText exerciseEditText = view.findViewById(R.id.exercise_edit_text);
            EditText repsEditText = view.findViewById(R.id.reps_edit_text);
            EditText setsEditText = view.findViewById(R.id.sets_edit_text);

            editor.putString("exercise_" + i, exerciseEditText.getText().toString());
            editor.putString("reps_" + i, repsEditText.getText().toString());
            editor.putString("sets_" + i, setsEditText.getText().toString());
        }

        editor.apply();
    }

    // Load the saved state when the activity resumes
    private void loadTrainingState() {
        SharedPreferences prefs = getSharedPreferences("UserTraining", MODE_PRIVATE);
        String savedTrainingName = prefs.getString("trainingName", "");
        trainingNameEditText.setText(savedTrainingName);

        int savedExerciseCount = prefs.getInt("exerciseCount", 0);
        for (int i = 0; i < savedExerciseCount; i++) {
            String exerciseName = prefs.getString("exercise_" + i, "");
            String reps = prefs.getString("reps_" + i, "");
            String sets = prefs.getString("sets_" + i, "");

            if (!exerciseName.isEmpty() && !reps.isEmpty() && !sets.isEmpty()) {
                // Add exercise field and set up the adapter for Spinner
                addExerciseField();

                View lastExerciseView = exercisesContainer.getChildAt(i);
                Spinner lastExerciseSpinner = lastExerciseView.findViewById(R.id.exercise_edit_text);
                EditText lastRepsEditText = lastExerciseView.findViewById(R.id.reps_edit_text);
                EditText lastSetsEditText = lastExerciseView.findViewById(R.id.sets_edit_text);

                // Set ArrayAdapter for the Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exerciseNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                lastExerciseSpinner.setAdapter(adapter);

                // Set the saved values
                lastRepsEditText.setText(reps);
                lastSetsEditText.setText(sets);

                // Set the selected exercise name if it exists in the adapter
                int position = adapter.getPosition(exerciseName);
                if (position >= 0) {
                    lastExerciseSpinner.setSelection(position);
                }
            }
        }
    }


    // Clear the saved state
    private void clearTrainingState() {
        SharedPreferences prefs = getSharedPreferences("UserTraining", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
