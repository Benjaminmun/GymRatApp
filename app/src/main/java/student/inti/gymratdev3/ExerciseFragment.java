package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ExerciseFragment extends Fragment {

    private Button addExerciseButton, filterArmsButton, filterBackButton, filterOlympicButton, filterAllButton;
    private LinearLayout workoutContainer, filterButtonsContainer;
    private FirebaseFirestore db;
    private String userId;
    private Map<String, String[]> defaultArmsWorkouts, defaultBackWorkouts, defaultOlympicWorkouts;
    private ImageButton filterIconButton;

    public ExerciseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise, container, false);

        // Initialize Firebase Firestore and get the current user ID
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize UI elements
        addExerciseButton = view.findViewById(R.id.add_exercise_button);
        workoutContainer = view.findViewById(R.id.workout_container);
        filterButtonsContainer = view.findViewById(R.id.filter_buttons_container);

        // Initialize filter buttons
        filterArmsButton = view.findViewById(R.id.arms_category_button);
        filterBackButton = view.findViewById(R.id.back_category_button);
        filterOlympicButton = view.findViewById(R.id.olympic_category_button);
        filterAllButton = view.findViewById(R.id.filter_all_button);

        // Initialize filter icon button
        filterIconButton = view.findViewById(R.id.filter_icon_button);

        // Initialize default workouts
        initializeDefaultWorkouts();

        // Load all workouts (both default and user-added) when the fragment is created
        displayAllExercises();

        // Add new exercise button listener
        addExerciseButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddExerciseActivity.class);
            startActivity(intent);
        });

        // Filter buttons listeners
        filterAllButton.setOnClickListener(v -> displayAllExercises());
        filterArmsButton.setOnClickListener(v -> filterExercises("arms"));
        filterBackButton.setOnClickListener(v -> filterExercises("back"));
        filterOlympicButton.setOnClickListener(v -> filterExercises("olympic"));

        // Filter icon button click listener
        filterIconButton.setOnClickListener(v -> {
            if (filterButtonsContainer.getVisibility() == View.VISIBLE) {
                filterButtonsContainer.setVisibility(View.GONE);
            } else {
                filterButtonsContainer.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    // New method to handle searching for workouts
    private void searchExercises() {
        Fragment searchWorkoutEditText = null;
        String searchText = searchWorkoutEditText.getText().toString().trim();
        if (TextUtils.isEmpty(searchText)) {
            Toast.makeText(getActivity(), "Please enter a workout name to search.", Toast.LENGTH_SHORT).show();
            return;
        }

        workoutContainer.removeAllViews();  // Clear previous results

        // Search in user exercises
        db.collection("exercises")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String workoutName = document.getString("name");
                            if (workoutName != null && workoutName.toLowerCase().contains(searchText.toLowerCase())) {
                                String[] workoutDetails = new String[]{
                                        document.getString("description"),
                                        "Reps: " + document.getLong("reps")
                                };

                                LinearLayout workoutLayout = new LinearLayout(getActivity());
                                workoutLayout.setOrientation(LinearLayout.HORIZONTAL);

                                Button workoutButton = new Button(getActivity());
                                workoutButton.setText(workoutName);
                                workoutButton.setOnClickListener(v -> showWorkoutDetails(workoutName, workoutDetails));

                                workoutLayout.addView(workoutButton);
                                workoutContainer.addView(workoutLayout);
                            }
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error loading exercises", Toast.LENGTH_SHORT).show();
                    }
                });

        // Optionally, you can also search in default workouts if needed
    }

    private void initializeDefaultWorkouts() {
        defaultArmsWorkouts = new HashMap<>();
        defaultBackWorkouts = new HashMap<>();
        defaultOlympicWorkouts = new HashMap<>();

        // Arms Workouts
        defaultArmsWorkouts.put("Bicep Curl with Dumbbell", new String[]{
                "Focus Area - Biceps, Forearms", "Equipment - Dumbbell",
                "Preparation: Stand with feet shoulder width apart. Hold a dumbbell in each hand in an underhand grip...",
                "Execution: 1. Curl the dumbbells up until your forearms touch your chest...",
                "2. Pause for a few seconds and squeeze your bicep...",
                "3. Lower the dumbbells to the starting position and repeat."});

        defaultArmsWorkouts.put("Diamond Push-Up", new String[]{
                "Focus Area - Chest, Triceps", "Equipment - Bodyweight",
                "Preparation: Lie prone on the floor with feet together and arms extended...",
                "Execution: 1. Inhale and lower your body until your chest almost touches the floor...",
                "2. Exhale and raise your body back to the starting position and repeat."});

        defaultArmsWorkouts.put("Hammer Curl with Cable", new String[]{
                "Focus Area - Biceps, Forearms", "Equipment - Cable",
                "Preparation: Stand with feet shoulder width apart...",
                "Execution: 1. Squeeze your biceps to bend your elbows and curl the rope up...",
                "2. Return to the starting position slowly and repeat."});

        // Back Workouts
        defaultBackWorkouts.put("Bent-over Row (Reverse Grip) Barbell", new String[]{
                "Focus Area - Back, Traps, Biceps", "Equipment - Barbell",
                "Preparation: Stand with feet shoulder width apart...",
                "Execution: 1. Exhale and squeeze your lats muscle to lift the barbell...",
                "2. Pause, slowly lower the barbell, and repeat."});

        // Olympic Workouts
        defaultOlympicWorkouts.put("Clean and Press with Barbell", new String[]{
                "Focus Area - Full Body", "Equipment - Barbell",
                "Preparation: Squat down with feet shoulder width apart...",
                "Execution: 1. Drive your hips up and straighten your legs to pull the barbell...",
                "2. Press the bar overhead until your arms are straight..."});

        defaultOlympicWorkouts.put("Clean with Barbell", new String[]{
                "Focus Area - Full Body", "Equipment - Barbell",
                "Preparation: Squat down with feet shoulder width apart...",
                "Execution: 1. Drive your hips up and straighten your legs to pull the barbell...",
                "2. Once the bar passes your mid-thigh, jump slightly and drop your body under the bar..."});

        defaultOlympicWorkouts.put("Snatch with Barbell", new String[]{
                "Focus Area - Full Body", "Equipment - Barbell",
                "Preparation: Put your feet under the bar. Bend down to grip the bar...",
                "Execution: 1. Drive your hips up and straighten your legs to pull the barbell...",
                "2. As you lift the barbell, lower your hips to a front squat position..."});
    }


    private void displayAllExercises() {
        workoutContainer.removeAllViews();  // Clear previous buttons

        displayDefaultWorkouts("arms", defaultArmsWorkouts);
        displayDefaultWorkouts("back", defaultBackWorkouts);
        displayDefaultWorkouts("olympic", defaultOlympicWorkouts);

        displayUserExercises();
    }

    private void displayDefaultWorkouts(String category, Map<String, String[]> defaultWorkouts) {
        for (Map.Entry<String, String[]> entry : defaultWorkouts.entrySet()) {
            LinearLayout workoutLayout = getWorkoutLayout(entry);

            workoutContainer.addView(workoutLayout);
        }
    }

    private void displayUserExercises() {
        db.collection("exercises")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String workoutName = document.getString("name");
                            String[] workoutDetails = new String[]{
                                    document.getString("description"),
                                    "Reps: " + document.getLong("reps")
                            };

                            LinearLayout workoutLayout = new LinearLayout(getActivity());
                            workoutLayout.setOrientation(LinearLayout.HORIZONTAL);

                            Button workoutButton = new Button(getActivity());
                            workoutButton.setText(workoutName);
                            workoutButton.setOnClickListener(v -> showWorkoutDetails(workoutName, workoutDetails));

                            workoutLayout.addView(workoutButton);
                            workoutContainer.addView(workoutLayout);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error loading exercises", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterExercises(String category) {
        workoutContainer.removeAllViews();

        switch (category) {
            case "arms":
                displayDefaultWorkouts(category, defaultArmsWorkouts);
                break;
            case "back":
                displayDefaultWorkouts(category, defaultBackWorkouts);
                break;
            case "olympic":
                displayDefaultWorkouts(category, defaultOlympicWorkouts);
                break;
        }

        db.collection("exercises")
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String workoutName = document.getString("name");
                            String[] workoutDetails = new String[]{
                                    document.getString("description"),
                                    "Reps: " + document.getLong("reps")
                            };

                            LinearLayout workoutLayout = new LinearLayout(getActivity());
                            workoutLayout.setOrientation(LinearLayout.HORIZONTAL);

                            Button workoutButton = new Button(getActivity());
                            workoutButton.setText(workoutName);
                            workoutButton.setOnClickListener(v -> showWorkoutDetails(workoutName, workoutDetails));

                            workoutLayout.addView(workoutButton);
                            workoutContainer.addView(workoutLayout);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error loading exercises", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showWorkoutDetails(String workoutName, String[] workoutDetails) {
        Intent intent = new Intent(getActivity(), WorkoutDetailActivity.class);

        intent.putExtra("workoutName", workoutName);
        intent.putExtra("workoutDetails", workoutDetails);
        intent.putExtra("focusArea", workoutDetails[0]);
        intent.putExtra("equipment", workoutDetails[1]);
        intent.putExtra("preparation", workoutDetails[2]);

        startActivity(intent);
    }

    private LinearLayout getWorkoutLayout(Map.Entry<String, String[]> entry) {
        LinearLayout workoutLayout = new LinearLayout(getActivity());
        workoutLayout.setOrientation(LinearLayout.VERTICAL);

        Button workoutButton = new Button(getActivity());
        workoutButton.setText(entry.getKey());
        workoutButton.setOnClickListener(v -> showWorkoutDetails(entry.getKey(), entry.getValue()));

        workoutLayout.addView(workoutButton);
        return workoutLayout;
    }
}
