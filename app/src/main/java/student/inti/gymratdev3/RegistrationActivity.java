package student.inti.gymratdev3;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationActivity extends AppCompatActivity {

    private EditText emailTextView, passwordTextView, confirmPasswordTextView;
    private Button Btn, backToLoginBtn;
    private ProgressBar progressbar;
    private FirebaseAuth mAuth;
    private ImageView passwordToggle;
    private ImageView confirmPasswordToggle;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private TextView passwordStrengthTextView;
    private TextView passwordConfirmTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Change status bar color to match button color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Taking FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        // Initializing views through id defined in layout
        emailTextView = findViewById(R.id.email);
        passwordTextView = findViewById(R.id.passwd);
        confirmPasswordTextView = findViewById(R.id.confirmPassword);
        Btn = findViewById(R.id.btnregister);
        backToLoginBtn = findViewById(R.id.backToLogin);
        progressbar = findViewById(R.id.progressbar);
        passwordToggle = findViewById(R.id.passwordToggle);
        passwordStrengthTextView = findViewById(R.id.passwordStrengthTextView);
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle); // Use correct ID for confirm password toggle
        passwordConfirmTextView = findViewById(R.id.passwordConfirmMatch);

        // Set onClickListener on Registration button
        Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerNewUser();
            }
        });

        // Handle show/hide password
        passwordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        // Handle show/hide confirm password
        confirmPasswordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleConfirmPasswordVisibility();
            }
        });

        // Add TextWatcher to password input field
        passwordTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Call the method to check password strength
                checkPasswordStrength(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        confirmPasswordTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Call the method to check if passwords match
                checkConfirmPassword(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        // Set onClickListener for Back to Login button
        backToLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finish this activity to remove it from the back stack
            }
        });
    }

    private void registerNewUser() {
        // Show the visibility of progress bar to show loading
        progressbar.setVisibility(View.VISIBLE);

        String email = emailTextView.getText().toString();
        String password = passwordTextView.getText().toString();
        String confirmPassword = confirmPasswordTextView.getText().toString();

        // Validations for input email and password
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter email!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter password!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getApplicationContext(),
                            "Please confirm your password!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getApplicationContext(),
                            "Passwords do not match!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        }

        // Create new user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Send verification email
                            if (user != null) {
                                user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(),
                                                    "Registration successful! Please check your email for verification.",
                                                    Toast.LENGTH_LONG).show();
                                            progressbar.setVisibility(View.GONE);

                                            // Sign out the user to prevent them from using the app before verifying email
                                            FirebaseAuth.getInstance().signOut();

                                            // Redirect to Login Activity
                                            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(getApplicationContext(),
                                                    "Failed to send verification email.",
                                                    Toast.LENGTH_LONG).show();
                                            progressbar.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }
                        } else {
                            progressbar.setVisibility(View.GONE);

                            // Handle errors
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                Toast.makeText(getApplicationContext(),
                                        "Password is too weak! Please use a stronger password.",
                                        Toast.LENGTH_LONG).show();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(getApplicationContext(),
                                        "Invalid email format. Please use a correct email address.",
                                        Toast.LENGTH_LONG).show();
                            } catch (FirebaseAuthUserCollisionException e) {
                                Toast.makeText(getApplicationContext(),
                                        "An account with this email already exists. Please use a different email.",
                                        Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(),
                                        "Registration failed!! Please try again later.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    // Toggle the visibility of the password
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordTextView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordToggle.setImageResource(R.drawable.ic_eye_closed); // Set closed eye icon
        } else {
            passwordTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            passwordToggle.setImageResource(R.drawable.ic_eye_open); // Set open eye icon
        }
        isPasswordVisible = !isPasswordVisible;
        passwordTextView.setSelection(passwordTextView.getText().length()); // Move cursor to the end
    }

    // Toggle the visibility of the confirm password
    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            confirmPasswordTextView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            confirmPasswordToggle.setImageResource(R.drawable.ic_eye_closed); // Set closed eye icon
        } else {
            confirmPasswordTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            confirmPasswordToggle.setImageResource(R.drawable.ic_eye_open); // Set open eye icon
        }
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        confirmPasswordTextView.setSelection(confirmPasswordTextView.getText().length()); // Move cursor to the end
    }

    // Method to display password strength
    private void checkPasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) {
            passwordStrengthTextView.setVisibility(View.GONE);
            return;
        }

        // Set the TextView visible
        passwordStrengthTextView.setVisibility(View.VISIBLE);

        if (password.length() < 6) {
            passwordStrengthTextView.setText("Password too short");
            passwordStrengthTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (isPasswordStrong(password)) {
            passwordStrengthTextView.setText("Strong Password");
            passwordStrengthTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (password.length() >= 6 && password.length() < 10) {
            passwordStrengthTextView.setText("Medium Strength");
            passwordStrengthTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            passwordStrengthTextView.setText("Weak Password");
            passwordStrengthTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    // Method to display whether passwords match
    private void checkConfirmPassword(String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            passwordConfirmTextView.setVisibility(View.GONE);
            return;
        }

        // Set the TextView visible
        passwordConfirmTextView.setVisibility(View.VISIBLE);

        // Compare the confirmPassword with the actual password
        if (!passwordTextView.getText().toString().equals(confirmPassword)) {
            passwordConfirmTextView.setText("Password does not match.");
            passwordConfirmTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            passwordConfirmTextView.setVisibility(View.GONE);
        }
    }


    // Method to check if the password is strong (at least 1 uppercase, 1 digit, and 1 special character)
    private boolean isPasswordStrong(String password) {
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+].*");

        return password.length() >= 10 && hasUppercase && hasDigit && hasSpecialChar;
    }
}
