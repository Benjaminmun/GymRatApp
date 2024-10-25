package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class TrainingFragment extends Fragment {

    private LinearLayout trainingContainer;
    private Button addTrainingButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // List to store all training buttons for filtering
    private final List<View> allTrainingViews = new ArrayList<>();
    private final List<String> defaultTrainings = List.of("Full Body Workout", "Cardio Burn", "Strength Training");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_training, container, false);

        // Initialize views
        initializeViews(rootView);

        // Initialize Firestore and get the current user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Load user-specific trainings with real-time updates
        loadUserTrainings();

        // Set up search functionality
        setupSearchFunctionality(rootView);

        // Add default trainings
        addDefaultTrainings();

        // Add listener to the "Add Training" button
        addTrainingButton.setOnClickListener(v -> navigateToEditUserTraining());

        return rootView;
    }

    private void initializeViews(View rootView) {
        trainingContainer = rootView.findViewById(R.id.training_container);
        addTrainingButton = rootView.findViewById(R.id.add_training_button);
    }

    private void navigateToEditUserTraining() {
        Intent intent = new Intent(getContext(), EditUserTrainingActivity.class);
        intent.putExtra("TRAINING_NAME", "New User Training");
        startActivity(intent);
    }

    private void setupSearchFunctionality(View rootView) {
        EditText searchEditText = rootView.findViewById(R.id.search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrainings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void addDefaultTrainings() {
        for (String training : defaultTrainings) {
            addTrainingButtonToContainer(training, null, false);
        }
    }

    private void loadUserTrainings() {
        if (currentUser == null) {
            showSnackbar("User not authenticated");
            return;
        }

        db.collection("trainings")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        showSnackbar("Failed to load trainings");
                        return;
                    }

                    // Clear user-specific trainings only (keep default ones)
                    for (View trainingView : allTrainingViews) {
                        Button trainingButton = trainingView.findViewById(R.id.training_button);
                        String trainingName = trainingButton.getText().toString();
                        if (!defaultTrainings.contains(trainingName)) {
                            trainingContainer.removeView(trainingView);
                        }
                    }
                    allTrainingViews.clear(); // Clear all stored views

                    // Add each training from Firestore with the document ID
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String trainingName = document.getString("trainingName");
                        String trainingId = document.getId(); // Get the unique document ID

                        if (trainingName != null) {
                            addTrainingButtonToContainer(trainingName, trainingId, true); // Add with document ID
                        }
                    }
                });
    }

    private void addTrainingButtonToContainer(String trainingName, String trainingId, boolean isRemovable) {
        View trainingView = LayoutInflater.from(getContext()).inflate(R.layout.training_button_layout, null);
        Button trainingButton = trainingView.findViewById(R.id.training_button);
        Button removeButton = trainingView.findViewById(R.id.remove_button);

        trainingButton.setText(trainingName);
        trainingButton.setBackgroundResource(R.drawable.button_gradient_alternate);
        trainingButton.setTextColor(getResources().getColor(android.R.color.white));
        trainingButton.setPadding(16, 16, 16, 16);

        // Navigate to the detail activity with the trainingId
        trainingButton.setOnClickListener(v -> navigateToTrainingDetail(trainingId));

        if (isRemovable) {
            removeButton.setVisibility(View.VISIBLE);
            removeButton.setOnClickListener(v -> removeTrainingPlan(trainingId, trainingView));
        } else {
            removeButton.setVisibility(View.GONE);
        }

        trainingContainer.addView(trainingView);
        allTrainingViews.add(trainingView);
    }

    private void navigateToTrainingDetail(String trainingId) {
        Intent intent = new Intent(getContext(), TrainingDetailActivity.class);
        intent.putExtra("TRAINING_ID", trainingId); // Pass the document ID
        startActivity(intent);
    }

    private void removeTrainingPlan(String trainingId, View trainingView) {
        // Delete the training by its document ID
        db.collection("trainings").document(trainingId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showSnackbar("Training removed");
                    trainingContainer.removeView(trainingView);
                    allTrainingViews.remove(trainingView);
                })
                .addOnFailureListener(e -> showSnackbar("Failed to remove training"));
    }

    private void filterTrainings(String query) {
        query = query.toLowerCase();
        for (View trainingView : allTrainingViews) {
            Button trainingButton = trainingView.findViewById(R.id.training_button);
            String trainingName = trainingButton.getText().toString().toLowerCase();
            trainingView.setVisibility(trainingName.contains(query) ? View.VISIBLE : View.GONE);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }
}
