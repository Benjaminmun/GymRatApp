package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailTextView, passwordTextView;
    private Button Btn, registerBtn, resetPasswordBtn;
    private ProgressBar progressbar;
    private FirebaseAuth mAuth;
    private ImageView passwordToggle;  // Password toggle

    private boolean isPasswordVisible = false; // Track password visibility

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Change status bar color to match button color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailTextView = findViewById(R.id.email);
        passwordTextView = findViewById(R.id.password);
        Btn = findViewById(R.id.login);
        progressbar = findViewById(R.id.progressBar);
        registerBtn = findViewById(R.id.register);
        passwordToggle = findViewById(R.id.passwordToggle); // Password toggle image (eye icon)
        resetPasswordBtn = findViewById(R.id.resetPassword); // Reset password button

        // Set OnClickListener for Sign-in button
        Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUserAccount();
            }
        });

        // Set OnClickListener for Register button
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        // Set OnClickListener for Show/Hide Password toggle
        passwordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        // Set OnClickListener for Reset Password button
        resetPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
    }

    private void loginUserAccount() {
        progressbar.setVisibility(View.VISIBLE);

        String email = emailTextView.getText().toString();
        String password = passwordTextView.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Please enter email!!", Toast.LENGTH_LONG).show();
            progressbar.setVisibility(View.GONE);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Please enter password!!", Toast.LENGTH_LONG).show();
            progressbar.setVisibility(View.GONE);
            return;
        }

        // Sign-in existing user
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressbar.setVisibility(View.GONE); // Hide progress bar

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Check if email is verified
                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(getApplicationContext(), "Login successful!!", Toast.LENGTH_LONG).show();
                                // Navigate to MainActivity
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), "Please verify your email before logging in!", Toast.LENGTH_LONG).show();
                                FirebaseAuth.getInstance().signOut(); // Sign out if the email is not verified
                            }

                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(getApplicationContext(), "Invalid credentials. Please check your email and password.", Toast.LENGTH_LONG).show();
                            } catch (FirebaseAuthInvalidUserException e) {
                                Toast.makeText(getApplicationContext(), "No account found with this email. Please register.", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "Login failed!! Please try again later.", Toast.LENGTH_LONG).show();
                                Log.e("LoginActivity", "Error: ", e);
                            }
                        }
                    }
                });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordTextView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordToggle.setImageResource(R.drawable.ic_eye_closed); // Set to closed eye icon
        } else {
            passwordTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            passwordToggle.setImageResource(R.drawable.ic_eye_open); // Set to open eye icon
        }
        isPasswordVisible = !isPasswordVisible;
        passwordTextView.setSelection(passwordTextView.getText().length()); // Move cursor to the end
    }

    // Reset password method
    private void resetPassword() {
        String email = emailTextView.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Please enter your registered email!", Toast.LENGTH_LONG).show();
            return;
        }

        // Disable the button to prevent multiple clicks
        resetPasswordBtn.setEnabled(false);

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        resetPasswordBtn.setEnabled(true); // Re-enable the button

                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Password reset email sent! Check your inbox.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Error in sending password reset email. Please try again.", Toast.LENGTH_LONG).show();
                            Log.e("LoginActivity", "Error in sending password reset email", task.getException());
                        }
                    }
                });
    }
}
