package student.inti.gymratdev3;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ExerciseFragment extends Fragment {

    private Button filterArmsButton, filterBackButton, filterOlympicButton, filterLegButton,
            filterChestButton, filterCoreButton, filterGlutesButton, filterShouldersButton,
            filterFullBodyButton, filterCardioButton, filterAllButton, addExerciseButton;
    private LinearLayout workoutContainer, filterButtonsContainer;
    private FirebaseFirestore db;
    private String userId;
    private ImageButton filterIconButton;
    private HorizontalScrollView filterButtonsScrollView;
    private boolean isFilterVisible = false; // Tracks the visibility of the filter buttons

    // Maps to store default workouts for different categories
    private final Map<String, Workout> defaultArmsWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultBackWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultOlympicWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultLegWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultChestWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultCoreWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultGlutesWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultShouldersWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultFullBodyWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultCardioWorkouts = new HashMap<>();

    public ExerciseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise, container, false);

        // Initialize Firestore and User ID
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        initializeUI(view); // Initialize all UI elements
        initializeDefaultWorkouts(); // Initialize default workout data
        saveDefaultWorkoutsToGlobalCollection(); // Save default workouts to global collection
        setupFilterListeners(); // Setup filter button listeners
        setButtonListeners(); // Set click listeners for the buttons

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == getActivity().RESULT_OK) {
            // Refresh the workouts after adding a new exercise
            loadAllWorkouts();
            Toast.makeText(getActivity(), "Exercise added successfully", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // Reload all workouts when returning to this fragment
        workoutContainer.removeAllViews();
        loadAllWorkouts();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        filterIconButton = view.findViewById(R.id.filter_icon_button);


        // Set click listener for filter button
        filterIconButton.setOnClickListener(v -> {
            // Toggle visibility and update state
            isFilterVisible = !isFilterVisible;
            filterButtonsScrollView.setVisibility(isFilterVisible ? View.VISIBLE : View.GONE);
        });
    }

    private void initializeUI(View view) {
        // Bind all UI elements
        filterArmsButton = view.findViewById(R.id.arms_category_button);
        filterBackButton = view.findViewById(R.id.back_category_button);
        filterOlympicButton = view.findViewById(R.id.olympic_category_button);
        filterLegButton = view.findViewById(R.id.leg_category_button);
        filterChestButton = view.findViewById(R.id.chest_category_button);
        filterCoreButton = view.findViewById(R.id.core_category_button);
        filterGlutesButton = view.findViewById(R.id.glutes_category_button);
        filterShouldersButton = view.findViewById(R.id.shoulder_category_button);
        filterFullBodyButton = view.findViewById(R.id.fullbody_category_button);
        filterCardioButton = view.findViewById(R.id.cardio_category_button);
        filterAllButton = view.findViewById(R.id.filter_all_button);
        workoutContainer = view.findViewById(R.id.workout_container);
        filterButtonsContainer = view.findViewById(R.id.filter_buttons_container);
        ImageButton filterIconButton = view.findViewById(R.id.filter_icon_button);
        addExerciseButton = view.findViewById(R.id.add_exercise_button);
        filterButtonsScrollView = view.findViewById(R.id.filter_buttons_scrollview);
        EditText searchEditText = view.findViewById(R.id.search_edit_text);

        // Show/Hide filter buttons on clicking the filter icon
        filterIconButton.setOnClickListener(v -> toggleFilterVisibility());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Perform search while typing
                filterWorkoutsBySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });

    }

    private void initializeDefaultWorkouts() {
        // Populate default workouts for Arms, Back, Olympic categories

        // Arms Workouts
        defaultArmsWorkouts.put("Bicep Curl with Dumbbell", new Workout(
                "Biceps, Forearms",
                "Dumbbell",
                "Stand with feet shoulder width apart. Hold a dumbbell in each hand in an underhand grip.",
                "1. Curl the dumbbells up until your forearms touch your chest.\n" +
                        "2. Pause for a few seconds and squeeze your bicep.\n" +
                        "3. Lower the dumbbells to the starting position and repeat."
        ));

        defaultArmsWorkouts.put("Diamond Push-Up", new Workout(
                "Chest, Triceps",
                "Bodyweight",
                "Lie prone on the floor with feet together and arms extended.",
                "1. Inhale and lower your body until your chest almost touches the floor.\n" +
                        "2. Exhale and raise your body back to the starting position and repeat."
        ));

        defaultArmsWorkouts.put("Hammer Curl with Cable", new Workout(
                "Biceps, Forearms",
                "Cable",
                "Stand with feet shoulder width apart.",
                "1. Squeeze your biceps to bend your elbows and curl the rope up.\n" +
                        "2. Return to the starting position slowly and repeat."
        ));

        // Back Workouts
        defaultBackWorkouts.put("Bent-over Row (Reverse Grip) Barbell", new Workout(
                "Back, Traps, Biceps",
                "Barbell",
                "Stand with feet shoulder width apart.",
                "1. Exhale and squeeze your lats muscle to lift the barbell.\n" +
                        "2. Pause, slowly lower the barbell, and repeat."
        ));

        // Cardio Workouts
        defaultCardioWorkouts.put("Jumping Jacks", new Workout(
                "Full Body, Cardiovascular",
                "Bodyweight",
                "Stand upright with your legs together and arms at your sides.",
                "1. Jump while spreading your legs and raising your arms overhead.\n" +
                        "2. Return to the starting position and repeat."
        ));

        // Chest Workouts
        defaultChestWorkouts.put("Bench Press", new Workout(
                "Chest, Triceps",
                "Barbell",
                "Lie back on a bench with a barbell in an overhand grip.",
                "1. Lower the barbell until it touches your chest.\n" +
                        "2. Press the bar back up to the starting position."
        ));

        defaultChestWorkouts.put("Chest Fly", new Workout(
                "Chest, Shoulders",
                "Dumbbell",
                "Lie back on a bench with dumbbells in each hand.",
                "1. Lower the dumbbells to the side with a slight bend in your elbows.\n" +
                        "2. Squeeze your chest and bring the dumbbells back up."
        ));

        // Core Workouts
        defaultCoreWorkouts.put("Plank", new Workout(
                "Core",
                "Bodyweight",
                "Lie face down on the floor, supporting your body with your forearms and toes.",
                "1. Keep your core tight and hold the position.\n" +
                        "2. Hold for as long as possible."
        ));

        // Full Body Workouts
        defaultFullBodyWorkouts.put("Burpees", new Workout(
                "Full Body",
                "Bodyweight",
                "Stand with feet shoulder width apart...",
                "1. Drop into a squat position and kick your legs back into a push-up position.\n" +
                        "2. Jump back to your feet and leap up with arms overhead."
        ));

        // Glutes Workouts
        defaultGlutesWorkouts.put("Glute Bridge", new Workout(
                "Glutes, Core",
                "Bodyweight",
                "Lie on your back with knees bent and feet flat on the floor.",
                "1. Squeeze your glutes and lift your hips towards the ceiling.\n" +
                        "2. Lower back down and repeat."
        ));

        // Legs Workouts
        defaultLegWorkouts.put("Squats", new Workout(
                "Legs, Glutes",
                "Bodyweight",
                "Stand with feet shoulder width apart...",
                "1. Lower your body into a squat position until your thighs are parallel to the ground.\n" +
                        "2. Push back up to the starting position."
        ));

        // Olympic Workouts
        defaultOlympicWorkouts.put("Clean and Press with Barbell", new Workout(
                "Full Body",
                "Barbell",
                "Squat down with feet shoulder width apart.",
                "1. Drive your hips up and straighten your legs to pull the barbell.\n" +
                        "2. Press the bar overhead until your arms are straight."
        ));

        defaultOlympicWorkouts.put("Clean with Barbell", new Workout(
                "Full Body",
                "Barbell",
                "Squat down with feet shoulder width apart...",
                "1. Drive your hips up and straighten your legs to pull the barbell.\n" +
                        "2. Once the bar passes your mid-thigh, jump slightly and drop your body under the bar."
        ));

        defaultOlympicWorkouts.put("Snatch with Barbell", new Workout(
                "Full Body",
                "Barbell",
                "Put your feet under the bar. Bend down to grip the bar...",
                "1. Drive your hips up and straighten your legs to pull the barbell.\n" +
                        "2. As you lift the barbell, lower your hips to a front squat position."
        ));

        // Shoulders Workouts
        defaultShouldersWorkouts.put("Shoulder Press", new Workout(
                "Shoulders",
                "Dumbbell",
                "Sit on a bench with dumbbells in each hand at shoulder height...",
                "1. Press the dumbbells overhead until your arms are straight.\n" +
                        "2. Lower back to the starting position."
        ));

    }


    private void setupFilterListeners() {
        // Setup filter button listeners to filter workouts by category
        filterArmsButton.setOnClickListener(v -> filterWorkoutsByCategory("Arms", defaultArmsWorkouts));
        filterBackButton.setOnClickListener(v -> filterWorkoutsByCategory("Back", defaultBackWorkouts));
        filterCardioButton.setOnClickListener(v -> filterWorkoutsByCategory("Cardio", defaultCardioWorkouts));
        filterChestButton.setOnClickListener(v -> filterWorkoutsByCategory("Chest", defaultChestWorkouts));
        filterCoreButton.setOnClickListener(v -> filterWorkoutsByCategory("Core", defaultCoreWorkouts));
        filterFullBodyButton.setOnClickListener(v -> filterWorkoutsByCategory("Full Body", defaultFullBodyWorkouts));
        filterGlutesButton.setOnClickListener(v -> filterWorkoutsByCategory("Glutes", defaultGlutesWorkouts));
        filterLegButton.setOnClickListener(v -> filterWorkoutsByCategory("Legs", defaultLegWorkouts));
        filterOlympicButton.setOnClickListener(v -> filterWorkoutsByCategory("Olympic", defaultOlympicWorkouts));
        filterShouldersButton.setOnClickListener(v -> filterWorkoutsByCategory("Shoulders", defaultShouldersWorkouts));
        filterAllButton.setOnClickListener(v -> loadAllWorkouts());
    }


    private void toggleFilterVisibility() {
        // Toggle visibility of filter buttons
        if (filterButtonsContainer.getVisibility() == View.VISIBLE) {
            filterButtonsContainer.setVisibility(View.GONE);
        } else {
            filterButtonsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void loadAllWorkouts() {
        workoutContainer.removeAllViews();
        addWorkoutCategory("Arms", defaultArmsWorkouts);
        addWorkoutCategory("Back", defaultBackWorkouts);
        addWorkoutCategory("Cardio", defaultCardioWorkouts);
        addWorkoutCategory("Chest", defaultChestWorkouts);
        addWorkoutCategory("Core", defaultCoreWorkouts);
        addWorkoutCategory("Full Body", defaultFullBodyWorkouts);
        addWorkoutCategory("Glutes", defaultGlutesWorkouts);
        addWorkoutCategory("Legs", defaultLegWorkouts);
        addWorkoutCategory("Olympic", defaultOlympicWorkouts);
        addWorkoutCategory("Shoulders", defaultShouldersWorkouts);
    }

    private void saveDefaultWorkoutsToGlobalCollection() {
        Log.d("Firestore", "Saving default workouts to global collection...");

        // Save all workout categories to Firestore (overwrite or merge if they exist)
        saveWorkoutCategoryToFirestore("Arms", defaultArmsWorkouts);
        saveWorkoutCategoryToFirestore("Back", defaultBackWorkouts);
        saveWorkoutCategoryToFirestore("Cardio", defaultCardioWorkouts);
        saveWorkoutCategoryToFirestore("Chest", defaultChestWorkouts);
        saveWorkoutCategoryToFirestore("Core", defaultCoreWorkouts);
        saveWorkoutCategoryToFirestore("Full Body", defaultFullBodyWorkouts);
        saveWorkoutCategoryToFirestore("Glutes", defaultGlutesWorkouts);
        saveWorkoutCategoryToFirestore("Legs", defaultLegWorkouts);
        saveWorkoutCategoryToFirestore("Olympic", defaultOlympicWorkouts);
        saveWorkoutCategoryToFirestore("Shoulders", defaultShouldersWorkouts);
    }

    private void saveWorkoutCategoryToFirestore(String category, Map<String, Workout> workouts) {
        for (Map.Entry<String, Workout> entry : workouts.entrySet()) {
            String workoutName = entry.getKey(); // Use workout name as document ID

            // Create a map to store workout data
            Map<String, Object> workoutData = new HashMap<>();
            workoutData.put("exercise_name", workoutName);
            workoutData.put("category", category);
            workoutData.put("focus_area", entry.getValue().getFocusArea());
            workoutData.put("equipment", entry.getValue().getEquipment());
            workoutData.put("preparation", entry.getValue().getPreparation());
            workoutData.put("execution", entry.getValue().getExecution());

            // Save to Firestore, using SetOptions.merge() to update or add if it exists
            db.collection("default_workouts").document(workoutName)
                    .set(workoutData, SetOptions.merge())
                    .addOnSuccessListener(aVoid ->
                            Log.d("Firestore", "Workout saved or updated: " + workoutName))
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Error saving workout: " + workoutName, e));
        }
    }





    private void filterWorkoutsByCategory(String category, Map<String, Workout> defaultWorkouts) {
        workoutContainer.removeAllViews(); // Clear the container

        // Add default workouts filtered by category
        addDefaultWorkoutsToCategory1(category, defaultWorkouts);

        // Fetch and filter user workouts filtered by category
        fetchAndAddUserWorkoutsToCategory(category); // New method to handle filtering user workouts
    }


    private void fetchAndAddUserWorkoutsToCategory(String category) {
        Log.d("Firestore", "Fetching user workouts for category: " + category); // Add log here
        db.collection("users").document(userId).collection("custom_exercises")
                .whereEqualTo("category", category).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firestore", "Query returned: " + task.getResult().size() + " documents"); // Log result size
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("Firestore", "Adding user workout: " + document.getString("exercise_name")); // Log each document
                                addUserWorkoutButton(document.getString("exercise_name"),
                                        new String[]{document.getString("execution"),
                                                document.getString("focus_area"),
                                                document.getString("equipment"),
                                                document.getString("preparation")});
                            }
                        } else {
                            Log.d("Firestore", "No user workouts found for category: " + category);
                        }
                    } else {
                        Log.e("Firestore", "Error fetching user exercises: ", task.getException());
                        Toast.makeText(getActivity(), "Failed to load user exercises.", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void addDefaultWorkoutsToCategory(String category, Map<String, Workout> defaultWorkouts) {
        // Add a label for the category
        TextView categoryLabel = addCategoryLabel(category.toUpperCase() + " WORKOUTS");
        categoryLabel.setTextColor(Color.parseColor("#3100d4")); // Hardcoded text color
        categoryLabel.setTypeface(null, Typeface.BOLD); // Make text bold
        categoryLabel.setGravity(Gravity.CENTER); // Center the text

        // Add default workouts to the view
        addDefaultWorkouts(defaultWorkouts);
    }

    // This function passively removes the display of the exercise category label
    private void addDefaultWorkoutsToCategory1(String category, Map<String, Workout> defaultWorkouts) {
        // Add default workouts to the view
        addDefaultWorkouts(defaultWorkouts);
    }

    private void fetchAndAddUserWorkoutsToCategoryAll(String category) {
        db.collection("users").document(userId).collection("custom_exercises")
                .whereEqualTo("category", category).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Log.d("Firestore", "Query returned: " + task.getResult().size() + " documents"); // Log result size

                        // Add a label for user workouts if any are found
                        TextView userCategoryLabel = addCategoryLabel("USER " + category.toUpperCase() + " WORKOUTS");
                        userCategoryLabel.setTextColor(Color.parseColor("#FF4500"));
                        userCategoryLabel.setTypeface(null, Typeface.BOLD);
                        userCategoryLabel.setGravity(Gravity.CENTER);

                        // Add each user workout to the UI
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            addUserWorkoutButton(document.getString("exercise_name"),
                                    new String[]{document.getString("execution"),
                                            document.getString("focus_area"),
                                            document.getString("equipment"),
                                            document.getString("preparation")});
                        }
                    } else if (task.getException() != null) {
                        Log.e("Firestore", "Error fetching user exercises: ", task.getException());
                        Toast.makeText(getActivity(), "Failed to load user exercises.", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void addWorkoutCategory(String category, Map<String, Workout> defaultWorkouts) {
        addDefaultWorkoutsToCategory(category, defaultWorkouts); // Add default workouts
        fetchAndAddUserWorkoutsToCategoryAll(category); // Fetch and add user workouts
    }

    private void addDefaultWorkouts(Map<String, Workout> workouts) {
        // Add each default workout to the container
        for (Map.Entry<String, Workout> entry : workouts.entrySet()) {
            addWorkoutButton(entry.getKey(), entry.getValue());
        }
    }


    private TextView addCategoryLabel(String text) {
        // Add a label to separate workout categories
        TextView label = new TextView(getActivity());
        label.setText(text);
        label.setTextSize(18);
        label.setPadding(0, 10, 0, 10);
        workoutContainer.addView(label);
        return label;
    }

    private void addWorkoutButton(String name, Workout workout) {
        addWorkoutButton(name, new String[]{workout.getExecution(), workout.getFocusArea(), workout.getEquipment(), workout.getPreparation()});
    }

    private void addWorkoutButton(String name, String[] details) {
        Button button = new Button(new ContextThemeWrapper(getActivity(), R.style.MyButton), null, 0);
        button.setText(name);

        button.setBackgroundResource(R.drawable.button_gradient_alternate);

        button.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        button.setLayoutParams(params);

        button.setPadding(24, 24, 24, 24);

        button.setOnClickListener(v -> openWorkoutDetails(name, details));

        workoutContainer.addView(button);
    }

    private void addUserWorkoutButton(String name, String[] details) {
        removeExistingWorkoutButton(name); // First check if a button with the same workout name already exists

        Button button = new Button(new ContextThemeWrapper(getActivity(), R.style.MyButton), null, 0);
        button.setText(name);

        button.setBackgroundResource(R.drawable.button_gradient_alternate_orange);

        button.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16); // Add margins around the button
        button.setLayoutParams(params);

        button.setPadding(24, 24, 24, 24); // Increase padding for a more spacious look

        button.setOnClickListener(v -> openWorkoutDetails(name, details));

        workoutContainer.addView(button);
    }

    // Function to remove an existing workout button by its name
    private void removeExistingWorkoutButton(String workoutName) {
        for (int i = 0; i < workoutContainer.getChildCount(); i++) {
            View view = workoutContainer.getChildAt(i);
            if (view instanceof Button) {
                Button button = (Button) view;
                if (button.getText().toString().equalsIgnoreCase(workoutName)) {
                    workoutContainer.removeView(view);
                    break; // Exit loop after removing the first matching button
                }
            }
        }
    }


    private void openWorkoutDetails(String workoutName, String[] workoutDetails) {
        // Start the WorkoutDetailActivity with workout details
        Intent intent = new Intent(getActivity(), WorkoutDetailActivity.class);
        intent.putExtra("workoutName", workoutName);
        intent.putExtra("execution", workoutDetails[0]);
        intent.putExtra("focusArea", workoutDetails[1]);
        intent.putExtra("equipment", workoutDetails[2]);
        intent.putExtra("preparation", workoutDetails[3]);
        intent.putExtra("isCustomExercise", true);
        startActivity(intent);
    }

    private void setButtonListeners() {
        addExerciseButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddExerciseActivity.class);
            startActivityForResult(intent, 1); // Request code 1 for adding a new exercise
        });
    }


    private void filterWorkoutsBySearch(String query) {
        // Clear the workout container first
        workoutContainer.removeAllViews();

        // If the search field is empty, reload all workouts with category labels
        if (query.isEmpty()) {
            loadAllWorkouts(); // Reload all workouts and categories when search is cleared
            return;
        }

        // Filter and add default workouts for Arms, Back, and Olympic without category labels
        Map<String, Workout> filteredArmsWorkouts = filterWorkouts(defaultArmsWorkouts, query);
        Map<String, Workout> filteredBackWorkouts = filterWorkouts(defaultBackWorkouts, query);
        Map<String, Workout> filteredOlympicWorkouts = filterWorkouts(defaultOlympicWorkouts, query);
        Map<String, Workout> filteredLegWorkouts = filterWorkouts(defaultLegWorkouts, query);
        Map<String, Workout> filteredChestWorkouts = filterWorkouts(defaultChestWorkouts, query);
        Map<String, Workout> filteredCoreWorkouts = filterWorkouts(defaultCoreWorkouts, query);
        Map<String, Workout> filteredGlutesWorkouts = filterWorkouts(defaultGlutesWorkouts, query);
        Map<String, Workout> filteredShouldersWorkouts = filterWorkouts(defaultShouldersWorkouts, query);
        Map<String, Workout> filteredFullBodyWorkouts = filterWorkouts(defaultFullBodyWorkouts, query);
        Map<String, Workout> filteredCardioWorkouts = filterWorkouts(defaultCardioWorkouts, query);



        // Add filtered default workouts directly
        addDefaultWorkouts(filteredArmsWorkouts);
        addDefaultWorkouts(filteredBackWorkouts);
        addDefaultWorkouts(filteredOlympicWorkouts);
        addDefaultWorkouts(filteredLegWorkouts);
        addDefaultWorkouts(filteredChestWorkouts);
        addDefaultWorkouts(filteredCoreWorkouts);
        addDefaultWorkouts(filteredGlutesWorkouts);
        addDefaultWorkouts(filteredShouldersWorkouts);
        addDefaultWorkouts(filteredFullBodyWorkouts);
        addDefaultWorkouts(filteredCardioWorkouts);

        // Fetch and add filtered user-created workouts directly
        fetchAndFilterUserWorkouts(query);
    }


    private void fetchAndFilterUserWorkouts(String query) {
        // Fetch user workouts filtered by the search query from Firestore
        db.collection("users").document(userId).collection("custom_exercises")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Log.d("Firestore", "Query returned: " + task.getResult().size() + " documents"); // Log result size
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String exerciseName = document.getString("exercise_name");

                            // Check if the exercise name contains the search query
                            if (exerciseName != null && exerciseName.toLowerCase().contains(query.toLowerCase())) {
                                String[] details = {
                                        document.getString("execution"),
                                        document.getString("focus_area"),
                                        document.getString("equipment"),
                                        document.getString("preparation")
                                };

                                // Add the user workout button without category labels
                                addUserWorkoutButton(exerciseName, details);
                            }
                        }
                    } else if (task.getException() != null) {
                        Log.e("Firestore", "Failed to fetch user exercises", task.getException());
                        Toast.makeText(getActivity(), "Error loading exercises.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Map<String, Workout> filterWorkouts(Map<String, Workout> workouts, String query) {
        Map<String, Workout> filteredWorkouts = new HashMap<>();
        for (Map.Entry<String, Workout> entry : workouts.entrySet()) {
            if (entry.getKey().toLowerCase().contains(query.toLowerCase())) {
                filteredWorkouts.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredWorkouts;
    }



}