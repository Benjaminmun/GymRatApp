package student.inti.gymratdev3;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText newPasswordField, confirmPasswordField, currentPasswordField;
    private Button changePasswordButton;
    private ProgressBar progressBar;
    private TextView passwordMismatchText;
    private FirebaseAuth firebaseAuth;

    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Change status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        newPasswordField = findViewById(R.id.new_password);
        confirmPasswordField = findViewById(R.id.confirm_password);
        currentPasswordField = findViewById(R.id.current_password);
        changePasswordButton = findViewById(R.id.btn_change_password);
        progressBar = findViewById(R.id.progressBar);
        passwordMismatchText = findViewById(R.id.passwordConfirmMatch);

        // Set button click listener
        changePasswordButton.setOnClickListener(v -> changePassword());

        // Password visibility toggles
        ImageButton toggleCurrentPassword = findViewById(R.id.toggle_current_password);
        toggleCurrentPassword.setOnClickListener(v -> togglePasswordVisibility(currentPasswordField, toggleCurrentPassword));

        ImageButton toggleNewPassword = findViewById(R.id.toggle_new_password);
        toggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(newPasswordField, toggleNewPassword));

        ImageButton toggleConfirmPassword = findViewById(R.id.toggle_confirm_password);
        toggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(confirmPasswordField, toggleConfirmPassword));
    }

    private void changePassword() {
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();
        String currentPassword = currentPasswordField.getText().toString().trim();

        if (!newPassword.equals(confirmPassword)) {
            passwordMismatchText.setVisibility(View.VISIBLE);
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            reAuthenticateUser(user, currentPassword, newPassword);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "User not logged in. Please log in again.", Toast.LENGTH_LONG).show();
        }
    }

    private void reAuthenticateUser(FirebaseUser user, String currentPassword, String newPassword) {
        // Get the user's credentials
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        // Re-authenticate the user
        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("ChangePassword", "Re-authentication successful.");
                    updatePassword(user, newPassword);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ChangePasswordActivity.this, "Re-authentication failed. Check your current password.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updatePassword(FirebaseUser user, String newPassword) {
        // Update the user's password
        user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Password updated successfully.", Toast.LENGTH_LONG).show();
                    finish(); // Close the activity
                } else {
                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to update password.";
                    Toast.makeText(ChangePasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void togglePasswordVisibility(EditText passwordField, ImageButton toggleButton) {
        boolean isVisible = (passwordField.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        if (isVisible) {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_eye_closed);
        } else {
            passwordField.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_eye_open);
        }
        passwordField.setSelection(passwordField.getText().length()); // Keep cursor at the end
    }
}
