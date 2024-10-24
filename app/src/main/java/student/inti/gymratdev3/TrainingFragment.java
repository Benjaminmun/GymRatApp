package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
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

import android.text.Editable;
import android.text.TextWatcher;

public class TrainingFragment extends Fragment {

    private LinearLayout trainingContainer;
    private Button addTrainingButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_training, container, false);

        // Initialize UI elements
        trainingContainer = rootView.findViewById(R.id.training_container);
        addTrainingButton = rootView.findViewById(R.id.add_training_button);
        EditText searchEditText = rootView.findViewById(R.id.search_edit_text);

        // Initialize Firestore and get current user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Load user-specific trainings from Firestore
            loadUserTrainings(currentUser.getUid());
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }

        // Add listener to the "Add Training" button
        addTrainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditUserTrainingActivity.class);
            intent.putExtra("TRAINING_NAME", "New User Training");
            startActivity(intent);
        });

        // Implement search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter trainings based on the input search query
                filterTrainings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        return rootView;
    }

    // Method to load user-specific trainings from Firestore
    private void loadUserTrainings(String userId) {
        db.collection("trainings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        trainingContainer.removeAllViews();  // Clear previous trainings
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String trainingName = document.getString("trainingName");
                            addTrainingButtonToContainer(trainingName);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading trainings", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to add a training button to the container
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
        trainingContainer.addView(trainingButton);

        // Set onClickListener to open the TrainingDetailActivity when clicked
        trainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrainingDetailActivity.class);
            intent.putExtra("TRAINING_NAME", trainingName);
            startActivity(intent);
        });
    }

    // Method to filter displayed trainings based on search input
    private void filterTrainings(String query) {
        for (int i = 0; i < trainingContainer.getChildCount(); i++) {
            View view = trainingContainer.getChildAt(i);
            if (view instanceof Button) {
                Button trainingButton = (Button) view;
                String trainingName = trainingButton.getText().toString().toLowerCase();
                if (trainingName.contains(query.toLowerCase())) {
                    trainingButton.setVisibility(View.VISIBLE);
                } else {
                    trainingButton.setVisibility(View.GONE);
                }
            }
        }
    }
}
