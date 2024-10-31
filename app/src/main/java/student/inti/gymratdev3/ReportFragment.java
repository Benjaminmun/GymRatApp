package student.inti.gymratdev3;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private RecyclerView workoutHistoryRecycler;
    private Button refreshButton;
    private FirebaseFirestore db;
    private WorkoutHistoryAdapter adapter;
    private List<Map<String, Object>> workoutHistoryList = new ArrayList<>();

    private TextView totalMinutesTextView, totalExercisesTextView; // New Stats Views
    private static final String TAG = "ReportFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Initialize Firestore and UI elements
        db = FirebaseFirestore.getInstance();
        workoutHistoryRecycler = view.findViewById(R.id.workout_history_recycler);
        refreshButton = view.findViewById(R.id.refresh_button);
        totalMinutesTextView = view.findViewById(R.id.total_minutes_text_view); // New
        totalExercisesTextView = view.findViewById(R.id.total_exercises_text_view); // New

        // Set up RecyclerView
        workoutHistoryRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WorkoutHistoryAdapter(workoutHistoryList);
        workoutHistoryRecycler.setAdapter(adapter);

        // Load data on fragment start
        loadWorkoutHistory();

        // Refresh button click listener
        refreshButton.setOnClickListener(v -> loadWorkoutHistory());

        return view;
    }

    // Load workout history for the logged-in user
    private void loadWorkoutHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get user ID

        db.collection("users")
                .document(userId)
                .collection("workout_history")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        workoutHistoryList.clear(); // Clear old data
                        int totalMinutes = 0;
                        int totalSeconds = 0;
                        int totalExercises = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Map<String, Object> data = doc.getData();
                            workoutHistoryList.add(data); // Add new data

                            // Calculate total minutes and seconds
                            if (data.containsKey("totalTime")) {
                                String totalTimeStr = (String) data.get("totalTime");

                                try {
                                    // Split the string into minutes and seconds
                                    String[] timeParts = totalTimeStr.split(":");
                                    int minutes = Integer.parseInt(timeParts[0]);
                                    int seconds = Integer.parseInt(timeParts[1]);

                                    // Accumulate minutes and seconds separately
                                    totalMinutes += minutes;
                                    totalSeconds += seconds;
                                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                    Log.e(TAG, "Invalid totalTime format: " + totalTimeStr, e);
                                }
                            }

                            // Calculate total exercises
                            if (data.containsKey("exercises")) {
                                totalExercises += ((List<?>) data.get("exercises")).size(); // Exercise count
                            }
                        }

                        // Convert excess seconds to minutes
                        totalMinutes += totalSeconds / 60;
                        totalSeconds = totalSeconds % 60;

                        // Display the total time with correct pluralization
                        String totalTimeDisplay = String.format(
                                Locale.getDefault(), // Explicitly specify locale
                                "Total Training Time: %d min%s %d sec%s",
                                totalMinutes,
                                (totalMinutes != 1 ? "s" : ""),
                                totalSeconds,
                                (totalSeconds != 1 ? "s" : "")
                        );

                        // Update Stats Views
                        totalMinutesTextView.setText(totalTimeDisplay);
                        totalExercisesTextView.setText(String.format(Locale.getDefault(), "Total Exercises: %d", totalExercises));

                        adapter.notifyDataSetChanged(); // Refresh the RecyclerView
                        Log.d(TAG, "Workout history loaded.");
                    } else {
                        Toast.makeText(getContext(), "Failed to load history.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading history", task.getException());
                    }
                });
    }

}
