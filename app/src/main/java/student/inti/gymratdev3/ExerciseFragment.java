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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExerciseFragment extends Fragment {

    private Button filterArmsButton, filterBackButton, filterOlympicButton, filterAllButton, addExerciseButton;
    private LinearLayout workoutContainer, filterButtonsContainer;
    private FirebaseFirestore db;
    private String userId;

    // Maps to store default workouts for different categories
    private final Map<String, Workout> defaultArmsWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultBackWorkouts = new HashMap<>();
    private final Map<String, Workout> defaultOlympicWorkouts = new HashMap<>();


    public ExerciseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise, container, false);

        // Initialize Firestore and User ID
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeUI(view); // Initialize all UI elements
        initializeDefaultWorkouts(); // Initialize default workout data
        setupFilterListeners(); // Setup filter button listeners
        setButtonListeners(); // Set click listeners for the buttons

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload all workouts when returning to this fragment
        workoutContainer.removeAllViews();
        loadAllWorkouts();
    }


    private void initializeUI(View view) {
        // Bind all UI elements
        filterArmsButton = view.findViewById(R.id.arms_category_button);
        filterBackButton = view.findViewById(R.id.back_category_button);
        filterOlympicButton = view.findViewById(R.id.olympic_category_button);
        filterAllButton = view.findViewById(R.id.filter_all_button);
        workoutContainer = view.findViewById(R.id.workout_container);
        filterButtonsContainer = view.findViewById(R.id.filter_buttons_container);
        ImageButton filterIconButton = view.findViewById(R.id.filter_icon_button);
        addExerciseButton = view.findViewById(R.id.add_exercise_button);
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
                "Stand with feet shoulder width apart. Hold a dumbbell in each hand in an underhand grip...",
                "1. Curl the dumbbells up until your forearms touch your chest...\n" +
                        "2. Pause for a few seconds and squeeze your bicep...\n" +
                        "3. Lower the dumbbells to the starting position and repeat."
        ));

        defaultArmsWorkouts.put("Diamond Push-Up", new Workout(
                "Chest, Triceps",
                "Bodyweight",
                "Lie prone on the floor with feet together and arms extended...",
                "1. Inhale and lower your body until your chest almost touches the floor...\n" +
                        "2. Exhale and raise your body back to the starting position and repeat."
        ));

        defaultArmsWorkouts.put("Hammer Curl with Cable", new Workout(
                "Biceps, Forearms",
                "Cable",
                "Stand with feet shoulder width apart...",
                "1. Squeeze your biceps to bend your elbows and curl the rope up...\n" +
                        "2. Return to the starting position slowly and repeat."
        ));


        // Back Workouts
        defaultBackWorkouts.put("Bent-over Row (Reverse Grip) Barbell", new Workout(
                "Back, Traps, Biceps",
                "Barbell",
                "Stand with feet shoulder width apart...",
                "1. Exhale and squeeze your lats muscle to lift the barbell...\n" +
                        "2. Pause, slowly lower the barbell, and repeat."
        ));

        // Olympic Workouts
        defaultOlympicWorkouts.put("Clean and Press with Barbell", new Workout(
                "Full Body",
                "Barbell",
                "Squat down with feet shoulder width apart...",
                "1. Drive your hips up and straighten your legs to pull the barbell...\n" +
                        "2. Press the bar overhead until your arms are straight."
        ));

        defaultOlympicWorkouts.put("Clean with Barbell", new Workout(
                "Full Body",
                "Barbell",
                "Squat down with feet shoulder width apart...",
                "1. Drive your hips up and straighten your legs to pull the barbell...\n" +
                        "2. Once the bar passes your mid-thigh, jump slightly and drop your body under the bar."
        ));

        defaultOlympicWorkouts.put("Snatch with Barbell", new Workout(
                "Full Body",
                "Barbell",
                "Put your feet under the bar. Bend down to grip the bar...",
                "1. Drive your hips up and straighten your legs to pull the barbell...\n" +
                        "2. As you lift the barbell, lower your hips to a front squat position..."
        ));
    }

    private void setupFilterListeners() {
        // Setup filter button listeners for different workout categories
        filterAllButton.setOnClickListener(v -> loadAllWorkouts());

        filterArmsButton.setOnClickListener(v -> {
            Log.d("Filter", "Default Arms Workouts size: " + defaultArmsWorkouts.size());
            filterWorkoutsByCategory("Arms", defaultArmsWorkouts);
        });

        filterBackButton.setOnClickListener(v -> {
            filterWorkoutsByCategory("Back", defaultBackWorkouts); // Filter Back workouts (both default and user-created)
        });

        filterOlympicButton.setOnClickListener(v -> {
            filterWorkoutsByCategory("Olympic", defaultOlympicWorkouts); // Filter Olympic workouts (both default and user-created)
        });
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
        addWorkoutCategory("Olympic", defaultOlympicWorkouts);
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
        TextView categoryLabel = addCategoryLabel(category.toUpperCase() + " EXERCISES");
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
                        TextView userCategoryLabel = addCategoryLabel("USER " + category.toUpperCase() + " EXERCISES");
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

//    private void fetchAndAddUserWorkouts(String category) {
//        // Remove existing user workouts and labels before adding new ones
//        removeExistingUserWorkouts();
//
//        // Fetch user workouts filtered by category from Firestore
//        db.collection("users").document(userId).collection("custom_exercises")
//                .whereEqualTo("category", category).get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
//                        // Add new category label for user workouts
//                        TextView userCategoryLabel = addCategoryLabel("USER " + category.toUpperCase() + " WORKOUTS");
//                        userCategoryLabel.setTextColor(Color.parseColor("#FF4500"));
//                        userCategoryLabel.setTypeface(null, Typeface.BOLD);
//                        userCategoryLabel.setGravity(Gravity.CENTER);
//
//                        // Add each workout button under the corresponding category
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            addUserWorkoutButton(document.getString("exercise_name"),
//                                    new String[]{document.getString("execution"),
//                                            document.getString("focus_area"),
//                                            document.getString("equipment"),
//                                            document.getString("preparation")});
//                        }
//                    } else if (task.getException() != null) {
//                        Log.e("Firestore", "Failed to fetch user exercises", task.getException());
//                        Toast.makeText(getActivity(), "Error loading exercises.", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//    }


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
            startActivity(intent);
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

        // Add filtered default workouts directly
        addDefaultWorkouts(filteredArmsWorkouts);
        addDefaultWorkouts(filteredBackWorkouts);
        addDefaultWorkouts(filteredOlympicWorkouts);

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




//    private void removeExistingUserWorkouts() {
//        // Loop through the workout container and remove user workout buttons
//        int childCount = workoutContainer.getChildCount();
//        for (int i = childCount - 1; i >= 0; i--) {
//            View view = workoutContainer.getChildAt(i);
//            // Assuming user workouts have a specific background (button_gradient_alternate_orange), remove only those views
//            if (view instanceof Button && ((Button) view).getBackground().getConstantState().equals(getResources().getDrawable(R.drawable.button_gradient_alternate_orange).getConstantState())) {
//                workoutContainer.removeView(view);
//            }
//        }
//    }
//
//
//    private boolean containsSearchQuery(String query, Map<String, Workout> workouts) {
//        for (String workoutName : workouts.keySet()) {
//            if (workoutName.toLowerCase().contains(query.toLowerCase())) {
//                return true;
//            }
//        }
//        return false;
//    }

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