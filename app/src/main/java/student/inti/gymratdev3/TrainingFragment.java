package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class TrainingFragment extends Fragment {

    private LinearLayout trainingContainer;
    private Button addTrainingButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // List to store all training views for filtering
    private final List<View> allTrainingViews = new ArrayList<>();

    // ListenerRegistration to manage Firestore listener
    private ListenerRegistration trainingsListenerRegistration;

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firestore listener to prevent memory leaks and crashes
        if (trainingsListenerRegistration != null) {
            trainingsListenerRegistration.remove();
            trainingsListenerRegistration = null;
        }
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrainings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });
    }

    private void addDefaultTrainings() {
        for (String training : defaultTrainings) {
            addDefaultTrainingButtonToContainer(training);
        }
    }

    private void loadUserTrainings() {
        if (currentUser == null) {
            showSnackbar("User not authenticated");
            return;
        }

        trainingsListenerRegistration = db.collection("trainings")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        if (isAdded()) {
                            showSnackbar("Failed to load trainings");
                        }
                        Log.e("TrainingFragment", "Firestore error: ", e);
                        return;
                    }

                    if (!isAdded()) {
                        Log.w("TrainingFragment", "Fragment not attached. Skipping UI updates.");
                        return;
                    }

                    // Remove existing user training views
                    removeUserTrainingViews();

                    // Add each training from Firestore with the document ID
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String trainingName = document.getString("trainingName");
                        String trainingId = document.getId(); // Get the unique document ID

                        if (trainingName != null) {
                            addUserTrainingButtonToContainer(trainingName, trainingId); // Add with document ID
                        }
                    }
                });
    }

    private void removeUserTrainingViews() {
        List<View> viewsToRemove = new ArrayList<>();
        for (View trainingView : allTrainingViews) {
            Button trainingButton = trainingView.findViewById(R.id.user_training_button);
            if (trainingButton != null) {
                trainingContainer.removeView(trainingView);
                viewsToRemove.add(trainingView);
            }
        }
        allTrainingViews.removeAll(viewsToRemove);
    }

    private void addDefaultTrainingButtonToContainer(String trainingName) {
        if (!isAdded()) {
            Log.w("TrainingFragment", "Fragment not attached. Skipping adding default training button.");
            return;
        }

        View trainingView = LayoutInflater.from(getContext()).inflate(R.layout.default_training_button_layout, null);
        Button trainingButton = trainingView.findViewById(R.id.training_button);
        ImageButton removeButton = trainingView.findViewById(R.id.remove_button);

        trainingButton.setText(trainingName);
        trainingButton.setBackgroundResource(R.drawable.button_gradient_alternate);
        trainingButton.setTextColor(getResources().getColor(android.R.color.white));
        trainingButton.setPadding(16, 16, 16, 16);

        // Navigate to the detail activity without trainingId
        trainingButton.setOnClickListener(v -> navigateToTrainingDetail(null));

        removeButton.setVisibility(View.GONE);

        trainingContainer.addView(trainingView);
        allTrainingViews.add(trainingView);
    }

    private void addUserTrainingButtonToContainer(String trainingName, String trainingId) {
        if (!isAdded()) {
            Log.w("TrainingFragment", "Fragment not attached. Skipping adding user training button.");
            return;
        }

        View trainingView = LayoutInflater.from(getContext()).inflate(R.layout.user_training_button_layout, null);
        Button trainingButton = trainingView.findViewById(R.id.user_training_button);
        ImageButton removeButton = trainingView.findViewById(R.id.remove_button);

        trainingButton.setText(trainingName);
        trainingButton.setBackgroundResource(R.drawable.button_gradient_alternate_orange);
        trainingButton.setTextColor(getResources().getColor(android.R.color.white));
        trainingButton.setPadding(16, 16, 16, 16);

        // Navigate to the detail activity with the trainingId
        trainingButton.setOnClickListener(v -> navigateToTrainingDetail(trainingId));

        removeButton.setVisibility(View.VISIBLE);
        removeButton.setOnClickListener(v -> removeTrainingPlan(trainingId, trainingView));

        trainingContainer.addView(trainingView);
        allTrainingViews.add(trainingView);
    }

    private void navigateToTrainingDetail(String trainingId) {
        Intent intent = new Intent(getContext(), TrainingDetailActivity.class);
        intent.putExtra("TRAINING_ID", trainingId); // Pass the document ID
        startActivity(intent);
    }

    private void removeTrainingPlan(String trainingId, View trainingView) {
        if (!isAdded()) {
            Log.w("TrainingFragment", "Fragment not attached. Cannot remove training plan.");
            return;
        }

        // Create a MaterialAlertDialogBuilder
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        // Set title and message for the dialog
        builder.setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to remove this training?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete the training by its document ID
                    db.collection("trainings").document(trainingId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                showSnackbar("Training removed");
                                trainingContainer.removeView(trainingView);
                                allTrainingViews.remove(trainingView);
                            })
                            .addOnFailureListener(e -> {
                                showSnackbar("Failed to remove training");
                                Log.e("TrainingFragment", "Error removing training: ", e);
                            });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show(); // Show the dialog
    }

    private void filterTrainings(String query) {
        query = query.toLowerCase();
        for (View trainingView : allTrainingViews) {
            Button trainingButton = trainingView.findViewById(R.id.training_button);
            if (trainingButton == null) {
                trainingButton = trainingView.findViewById(R.id.user_training_button);
            }
            String trainingName = trainingButton.getText().toString().toLowerCase();
            trainingView.setVisibility(trainingName.contains(query) ? View.VISIBLE : View.GONE);
        }
    }

    private void showSnackbar(String message) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Log.w("TrainingFragment", "Cannot show Snackbar. Fragment not attached or view is null.");
        }
    }
}