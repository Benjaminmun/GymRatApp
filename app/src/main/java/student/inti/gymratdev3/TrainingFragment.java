package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TrainingFragment extends Fragment {

    private LinearLayout trainingContainer;
    private Button addTrainingButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // List to store all training buttons for filtering
    private List<Button> allTrainingButtons = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_training, container, false);

        trainingContainer = rootView.findViewById(R.id.training_container);
        addTrainingButton = rootView.findViewById(R.id.add_training_button);
        EditText searchEditText = rootView.findViewById(R.id.search_edit_text);

        // Initialize Firestore and get current user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Add default trainings
        addDefaultTrainings();

        // Add listener to the Add Training button
        addTrainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditUserTrainingActivity.class);
            intent.putExtra("TRAINING_NAME", "New User Training");
            startActivity(intent);
        });

        // Load user-specific trainings from Firestore
        loadUserTrainings();

        // Set up the search functionality
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

        return rootView;
    }

    // Method to add default trainings
    private void addDefaultTrainings() {
        addTrainingButtonToContainer("Full Body Workout");
        addTrainingButtonToContainer("Cardio Burn");
        addTrainingButtonToContainer("Strength Training");
    }

    // Method to load user-specific trainings from Firestore
    private void loadUserTrainings() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("trainings")
                .whereEqualTo("userId", currentUser.getUid())  // Filter trainings by user ID
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String trainingName = document.getString("trainingName");
                        if (trainingName != null) {
                            addTrainingButtonToContainer(trainingName);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load trainings", Toast.LENGTH_SHORT).show());
    }

    // Method to add a training button to the container and list
    private void addTrainingButtonToContainer(String trainingName) {
        Button trainingButton = new Button(getContext());
        trainingButton.setText(trainingName);
        trainingButton.setBackgroundResource(R.drawable.button_gradient_alternate);
        trainingButton.setTextColor(getResources().getColor(android.R.color.white));
        trainingButton.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        trainingButton.setLayoutParams(params);
        trainingButton.setTextSize(16);

        // Add to the layout and the list of buttons for filtering
        trainingContainer.addView(trainingButton);
        allTrainingButtons.add(trainingButton);

        // Set the button's click listener
        trainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrainingDetailActivity.class);
            intent.putExtra("TRAINING_NAME", trainingName);
            startActivity(intent);
        });
    }

    // Method to filter displayed trainings based on search input
    private void filterTrainings(String query) {
        query = query.toLowerCase();
        for (Button trainingButton : allTrainingButtons) {
            String trainingName = trainingButton.getText().toString().toLowerCase();
            if (trainingName.contains(query)) {
                trainingButton.setVisibility(View.VISIBLE);
            } else {
                trainingButton.setVisibility(View.GONE);
            }
        }
    }
}
