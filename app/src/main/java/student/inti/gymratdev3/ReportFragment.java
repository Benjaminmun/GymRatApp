package student.inti.gymratdev3;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportFragment extends Fragment {

    private RecyclerView workoutHistoryRecycler;
    private Button refreshButton;
    private FirebaseFirestore db;
    private WorkoutHistoryAdapter adapter;
    private List<Map<String, Object>> workoutHistoryList = new ArrayList<>();

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

    // Load workout history from Firestore
    private void loadWorkoutHistory() {
        db.collection("workout_history")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        workoutHistoryList.clear(); // Clear old data
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            workoutHistoryList.add(doc.getData()); // Add new data
                        }
                        adapter.notifyDataSetChanged(); // Refresh the RecyclerView
                        Log.d(TAG, "Workout history loaded.");
                    } else {
                        Toast.makeText(getContext(), "Failed to load history.",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading history", task.getException());
                    }
                });
    }
}
