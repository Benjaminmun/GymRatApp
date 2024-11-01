package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {

    private Button editProfileButton, changePasswordButton, logoutButton;
    private TextView emailTextView, usernameTextView;
    private ImageView profileImageView;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;

    public ProfileFragment() {
        // Required empty public constructor
    }

    // Remove unused newInstance method if not required
    // public static ProfileFragment newInstance(String param1, String param2) { ... }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth
        firebaseStorage = FirebaseStorage.getInstance(); // Initialize Firebase Storage
        firebaseFirestore = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Find views by ID
        initializeViews(view);

        // Get the current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            emailTextView.setText(userEmail); // Display user email

            // Load username from Firestore
            loadUsernameFromFirestore(user.getUid());

            // Load profile image from Firebase Storage
            loadProfileImage();
        } else {
            // User is not logged in, redirect to login or show a message
            showSnackbar("User not authenticated");
            // Optionally, navigate to LoginActivity
            navigateToLogin();
        }

        // Set click listeners for the buttons
        setButtonListeners();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // If you have any listeners or bindings, clean them up here
    }

    // Initialize all UI elements
    private void initializeViews(View view) {
        editProfileButton = view.findViewById(R.id.edit_profile);
        changePasswordButton = view.findViewById(R.id.change_password);
        logoutButton = view.findViewById(R.id.logout);
        emailTextView = view.findViewById(R.id.email);
        usernameTextView = view.findViewById(R.id.username);
        profileImageView = view.findViewById(R.id.profile_picture);
    }

    // Method to load username from Firestore
    private void loadUsernameFromFirestore(String userId) {
        DocumentReference userRef = firebaseFirestore.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (!isAdded()) {
                // Fragment is not attached, skip UI updates
                return;
            }

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String username = document.getString("username");
                    if (username != null && !username.isEmpty()) {
                        usernameTextView.setText(username); // Set username
                    } else {
                        usernameTextView.setText("User"); // Placeholder if no username
                    }
                } else {
                    usernameTextView.setText("User"); // Document does not exist
                }
            } else {
                Toast.makeText(getContext(), "Failed to load username", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Set click listeners for the buttons
    private void setButtonListeners() {
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    // Method to load profile image from Firebase Storage
    private void loadProfileImage() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            // User is not authenticated, skip loading image
            return;
        }

        StorageReference storageReference = firebaseStorage.getReference("profile_pictures/" + user.getUid() + ".jpg");
        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
            if (!isAdded()) {
                // Fragment is not attached, skip UI updates
                return;
            }
            // Load image with Glide and apply circle crop
            Glide.with(this)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop() // Apply circle crop transformation
                    .placeholder(R.drawable.ic_profile_placeholder) // Placeholder image
                    .error(R.drawable.ic_profile_placeholder) // Error image
                    .into(profileImageView);
        }).addOnFailureListener(e -> {
            if (!isAdded()) {
                // Fragment is not attached, skip UI updates
                return;
            }
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Navigate to LoginActivity if user is not authenticated
    private void navigateToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    // Show a Snackbar message
    private void showSnackbar(String message) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        }
    }
}