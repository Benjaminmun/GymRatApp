package student.inti.gymratdev3;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;


public class TrainingFragment extends Fragment {

    private LinearLayout trainingContainer;
    private Button addTrainingButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_training, container, false);

        // Initialize UI elements
        trainingContainer = rootView.findViewById(R.id.training_container);
        addTrainingButton = rootView.findViewById(R.id.add_training_button);
        EditText searchEditText = rootView.findViewById(R.id.search_edit_text);

        // Example to add default trainings (this can be modified to retrieve from Firebase later)
        addDefaultTrainings();

        // Add listener to the Add Training button
        addTrainingButton.setOnClickListener(v -> {
            // Add a new user-created training (this can be improved with a dialog or new activity)
            addUserCreatedTraining("New User Training");
        });

        // Implement search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // You can leave this method empty or handle any needed logic before the text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This is where the filtering happens
                filterTrainings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // You can leave this method empty or handle any logic needed after the text changes
            }
        });
        return rootView;
    }

    // Method to add default trainings
    private void addDefaultTrainings() {
        addTrainingButtonToContainer("Full Body Workout");
        addTrainingButtonToContainer("Cardio Burn");
        addTrainingButtonToContainer("Strength Training");
    }

    // Method to add a user-created training
    private void addUserCreatedTraining(String trainingName) {
    }

    // Method to create a training button and add it to the container
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
