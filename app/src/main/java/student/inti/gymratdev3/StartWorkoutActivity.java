package student.inti.gymratdev3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class StartWorkoutActivity extends AppCompatActivity {

    private TextView timerTextView, countdownTextView, trainingNameTextView;
    private LinearLayout exercisesContainer;
    private Button pauseButton, finishButton;

    private Handler handler = new Handler();
    private long startTime = 0L, timeInMillis = 0L, pauseOffset = 0L;
    private boolean isRunning = false;

    private FirebaseFirestore db;
    private DocumentReference trainingRef;
    private List<Map<String, Object>> exercises;
    private List<Map<String, Object>> completedExercises = new ArrayList<>();
    private static final String TAG = "StartWorkoutActivity";

    private final int[] countdown = {3}; // Countdown start value
    private Handler countdownHandler = new Handler();

    // Notification Channel ID and Notification ID
    private static final String CHANNEL_ID = "workout_channel";
    private static final int WORKOUT_NOTIFICATION_ID = 1001;

    // ActivityResultLauncher for Notification Permission (Android 13+)
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        // Initialize Notification Permission Launcher
        initializePermissionLauncher();

        // Change status bar color to match button color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#3100d4"));
        }

        // Handle actions from the notification
        if (getIntent().getAction() != null) {
            handleNotificationAction(getIntent().getAction());
        }

        // Initialize views
        initializeViews();

        // Initialize Firestore
        initializeFirestore();

        // Load exercises from Firestore
        loadExercises();

        // Button listeners
        setupButtonListeners();

        // Create Notification Channel
        createNotificationChannel();

        // Request Notification Permission if needed
        requestNotificationPermission();

        // Start countdown before the workout begins
        startCountdown();
    }

    /**
     * Initialize the ActivityResultLauncher for requesting notification permissions.
     */
    private void initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted.");
                        // Optionally, send a notification to confirm permission
                        sendNotification("Notification Permission Granted", "You will now receive workout notifications.");
                    } else {
                        Toast.makeText(this, "Notification permission denied. Notifications won't appear.", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Notification permission denied.");
                    }
                }
        );
    }

    /**
     * Initialize UI components.
     */
    private void initializeViews() {
        timerTextView = findViewById(R.id.timerTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        exercisesContainer = findViewById(R.id.exercisesContainer);
        pauseButton = findViewById(R.id.pauseButton);
        finishButton = findViewById(R.id.finishButton);
        trainingNameTextView = findViewById(R.id.trainingNameTextView);

        // Ensure only countdown is shown initially
        countdownTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.GONE);
        exercisesContainer.setVisibility(View.GONE);
        trainingNameTextView.setVisibility(View.GONE);
        pauseButton.setVisibility(View.GONE);
        finishButton.setVisibility(View.GONE);
    }

    /**
     * Initialize Firestore and get the training reference.
     */
    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
        String trainingId = getIntent().getStringExtra("TRAINING_ID");
        if (trainingId == null || trainingId.isEmpty()) {
            Toast.makeText(this, "Invalid Training ID.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Training ID is null or empty.");
            finish();
            return;
        }
        trainingRef = db.collection("trainings").document(trainingId);
    }

    /**
     * Set up button click listeners.
     */
    private void setupButtonListeners() {
        pauseButton.setOnClickListener(v -> {
            pauseWorkout();
            showPauseDialog();
        });

        finishButton.setOnClickListener(v -> finishWorkout());
    }

    /**
     * Handle notification actions.
     *
     * @param action The action string from the intent.
     */
    private void handleNotificationAction(String action) {
        Log.d(TAG, "Handling notification action: " + action);
        switch (action) {
            case "ACTION_PAUSE_WORKOUT":
                pauseWorkout();
                showPauseDialog();
                break;
            case "ACTION_FINISH_WORKOUT":
                finishWorkout();
                break;
            default:
                Log.w(TAG, "Unknown action: " + action);
                break;
        }
    }

    /**
     * Request Notification Permission for Android 13+.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting notification permission.");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Notification permission already granted.");
            }
        }
    }

    /**
     * Rewritten sendNotification method with main content intent.
     *
     * @param title   The title of the notification.
     * @param content The content text of the notification.
     */
    private void sendNotification(String title, String content) {
        Log.d(TAG, "sendNotification called with title: " + title + " and content: " + content);

        // Check if notification permission is granted (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot send notification. POST_NOTIFICATIONS permission not granted.");
                return;
            }
        }

        try {
            // Intent to launch StartWorkoutActivity when the notification is tapped
            Intent mainIntent = new Intent(this, StartWorkoutActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                    this,
                    2, // Use a unique request code for this intent
                    mainIntent,
                    getPendingIntentFlags()
            );

            // Intent for the Pause action in the notification
            Intent pauseIntent = new Intent(this, StartWorkoutActivity.class);
            pauseIntent.setAction("ACTION_PAUSE_WORKOUT");
            pauseIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pausePendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    pauseIntent,
                    getPendingIntentFlags()
            );

            // Intent for the Finish action in the notification
            Intent finishIntent = new Intent(this, StartWorkoutActivity.class);
            finishIntent.setAction("ACTION_FINISH_WORKOUT");
            finishIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent finishPendingIntent = PendingIntent.getActivity(
                    this,
                    1, // Use different request codes to distinguish
                    finishIntent,
                    getPendingIntentFlags()
            );

            // Custom notification sound
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.custom_notification_sound);

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "workout_channel_with_sound")
                    .setSmallIcon(R.drawable.ic_workout_notification)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true) // Non-dismissable
                    .setAutoCancel(false) // Prevent auto-cancel on tap
                    .setSound(soundUri)
                    .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
                    .addAction(R.drawable.ic_finish, "Finish", finishPendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(mainPendingIntent);  // Set main content intent

            // Display or update the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(WORKOUT_NOTIFICATION_ID, builder.build());

            Log.d(TAG, "Notification sent/updated with ID: " + WORKOUT_NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification.", e);
        }
    }

    /**
     * Get the appropriate PendingIntent flags based on Android version.
     *
     * @return The flags to use for PendingIntent.
     */
    private int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    /**
     * Rewritten createNotificationChannel method with two channels: one with sound and one silent.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Notification Channels are only available in API 26+
            // Channel with sound
            CharSequence nameWithSound = "Workout Notifications with Sound";
            String descriptionWithSound = "Notifications for workout start with sound";
            int importanceWithSound = NotificationManager.IMPORTANCE_DEFAULT;

            // Custom notification sound
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.custom_notification_sound);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channelWithSound = new NotificationChannel("workout_channel_with_sound", nameWithSound, importanceWithSound);
            channelWithSound.setDescription(descriptionWithSound);
            channelWithSound.setSound(soundUri, audioAttributes);

            // Channel without sound (silent)
            CharSequence nameSilent = "Workout Notifications Silent";
            String descriptionSilent = "Silent notifications for workout updates";
            int importanceSilent = NotificationManager.IMPORTANCE_LOW; // Low importance to avoid sound

            NotificationChannel channelSilent = new NotificationChannel("workout_channel_silent", nameSilent, importanceSilent);
            channelSilent.setDescription(descriptionSilent);
            channelSilent.setSound(null, null); // No sound

            // Register the channels with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channelWithSound);
                notificationManager.createNotificationChannel(channelSilent);
                Log.d(TAG, "Notification channels created or updated.");
            } else {
                Log.e(TAG, "NotificationManager is null. Cannot create notification channels.");
            }
        }
    }


    /**
     * Rewritten playTickSound method with error handling.
     */
    private void playTickSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.tick);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            Log.d(TAG, "Tick sound played.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to play tick sound.", e);
        }
    }

    /**
     * Rewritten onBackPressed to ensure proper handling.
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Pause the countdown timer
        countdownHandler.removeCallbacksAndMessages(null);
        showPauseDialog();
    }

    /**
     * Rewritten onNewIntent to handle notification actions properly.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getAction() != null) {
            handleNotificationAction(intent.getAction());
        }
    }

    /**
     * Save workout history to Firestore.
     *
     * @param totalTime The total time of the workout.
     */
    private void saveWorkoutHistory(String totalTime) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User not authenticated.");
            return;
        }
        String userId = auth.getCurrentUser().getUid();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String date = dateFormat.format(new Date());

        Map<String, Object> workoutHistory = new HashMap<>();
        workoutHistory.put("date", date);
        workoutHistory.put("totalTime", totalTime);
        workoutHistory.put("exercises", completedExercises);

        db.collection("users")
                .document(userId)
                .collection("workout_history")
                .add(workoutHistory)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Workout history saved."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save workout history", e));
    }

    /**
     * Load exercises from Firestore.
     */
    private void loadExercises() {
        trainingRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String trainingName = snapshot.getString("trainingName");
                trainingNameTextView.setText(trainingName != null ? trainingName : "Unnamed Training");
                exercises = (List<Map<String, Object>>) snapshot.get("exercises");
                if (exercises != null && !exercises.isEmpty()) {
                    displayExercises();
                } else {
                    Toast.makeText(this, "No exercises found.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "No exercises found in training.");
                }
            } else {
                Toast.makeText(this, "Training data not found.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Training data not found.");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load exercises.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to load exercises", e);
        });
    }

    /**
     * Display exercises in the UI.
     */
    private void displayExercises() {
        for (int i = 0; i < exercises.size(); i++) {
            addExerciseRow(exercises.get(i), i);
        }
    }

    /**
     * Add an exercise row to the exercises container.
     *
     * @param exercise The exercise data.
     * @param index    The index of the exercise.
     */
    private void addExerciseRow(Map<String, Object> exercise, int index) {
        View exerciseRow = getLayoutInflater().inflate(R.layout.exercise_row, exercisesContainer, false);

        TextView exerciseNameText = exerciseRow.findViewById(R.id.exerciseNameText);
        EditText repsEditText = exerciseRow.findViewById(R.id.repsEditText);
        EditText setsEditText = exerciseRow.findViewById(R.id.setsEditText);
        EditText kgEditText = exerciseRow.findViewById(R.id.kgEditText);
        CheckBox doneCheckBox = exerciseRow.findViewById(R.id.doneCheckBox);

        String name = (String) exercise.get("exercise");
        int reps = ((Number) exercise.get("reps")).intValue();
        int sets = ((Number) exercise.get("sets")).intValue();

        exerciseNameText.setText(name);
        repsEditText.setText(String.valueOf(reps));
        setsEditText.setText(String.valueOf(sets));

        doneCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                try {
                    int updatedReps = Integer.parseInt(repsEditText.getText().toString());
                    int updatedSets = Integer.parseInt(setsEditText.getText().toString());
                    float updatedKg = Float.parseFloat(kgEditText.getText().toString());

                    Map<String, Object> updatedExercise = new HashMap<>();
                    updatedExercise.put("exercise", name);
                    updatedExercise.put("reps", updatedReps);
                    updatedExercise.put("sets", updatedSets);
                    updatedExercise.put("kg", updatedKg);

                    repsEditText.setEnabled(false);
                    setsEditText.setEnabled(false);
                    kgEditText.setEnabled(false);
                    completedExercises.add(updatedExercise);

                    Log.d(TAG, "Exercise completed: " + name);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter valid numbers.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Invalid number format for exercise: " + name, e);
                    doneCheckBox.setChecked(false);
                }
            } else {
                repsEditText.setEnabled(true);
                setsEditText.setEnabled(true);
                kgEditText.setEnabled(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    completedExercises.removeIf(e -> e.get("exercise").equals(name));
                    Log.d(TAG, "Exercise uncompleted: " + name);
                }
            }
        });

        exercisesContainer.addView(exerciseRow);
    }

    /**
     * Runnable for updating the timer.
     */
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timeInMillis = System.currentTimeMillis() - startTime + pauseOffset;
            int seconds = (int) (timeInMillis / 1000) % 60;
            int minutes = (int) (timeInMillis / 1000) / 60;

            String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            timerTextView.setText(time);
            handler.postDelayed(this, 1000);

            // Update the notification with the current timer
            updateWorkoutNotification(time);
        }
    };

    /**
     * Update the workout notification with the current timer.
     *
     * @param time The current time of the workout.
     */
    private void updateWorkoutNotification(String time) {
        String title = "Workout In Progress";
        String content = "Time: " + time;

        Log.d(TAG, "Updating notification with time: " + time);

        // Check if notification permission is granted (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot update notification. POST_NOTIFICATIONS permission not granted.");
                return;
            }
        }

        try {
            // Intent to launch StartWorkoutActivity when the notification is tapped
            Intent mainIntent = new Intent(this, StartWorkoutActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                    this,
                    2, // Use a unique request code for this intent
                    mainIntent,
                    getPendingIntentFlags()
            );

            // Intent for the Pause action in the notification
            Intent pauseIntent = new Intent(this, StartWorkoutActivity.class);
            pauseIntent.setAction("ACTION_PAUSE_WORKOUT");
            pauseIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pausePendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    pauseIntent,
                    getPendingIntentFlags()
            );

            // Intent for the Finish action in the notification
            Intent finishIntent = new Intent(this, StartWorkoutActivity.class);
            finishIntent.setAction("ACTION_FINISH_WORKOUT");
            finishIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent finishPendingIntent = PendingIntent.getActivity(
                    this,
                    1, // Use different request codes to distinguish
                    finishIntent,
                    getPendingIntentFlags()
            );

            // Build the notification using the silent channel
            // Build the notification using the same channel as sendNotification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_workout_notification) // Ensure this drawable exists and is white-only
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Use default priority to match the channel
                    .setOngoing(true) // Make the notification non-dismissible
                    .setAutoCancel(false)
                    .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
                    .addAction(R.drawable.ic_finish, "Finish", finishPendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(mainPendingIntent) // Set main content intent
                    .setOnlyAlertOnce(true); // Prevents alerts on updates


            // Display or update the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(WORKOUT_NOTIFICATION_ID, builder.build());

            Log.d(TAG, "Notification updated with timer: " + time);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update notification.", e);
        }
    }


    /**
     * Show the pause dialog with options to resume, restart, or cancel.
     */
    public void showPauseDialog() {
        if (isRunning) pauseWorkout();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_workout_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnResume = dialogView.findViewById(R.id.btnResume);
        Button btnRestart = dialogView.findViewById(R.id.btnRestart);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnResume.setOnClickListener(v -> {
            if (countdown[0] > 0) {
                resumeCountdown();
                dialog.dismiss();
            } else {
                startWorkout();
                dialog.dismiss();
            }
        });

        btnRestart.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Restart Workout")
                    .setMessage("Are you sure you want to restart the workout?")
                    .setPositiveButton("Yes", (confirmDialog, which) -> {
                        restartWorkout();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (confirmDialog, which) -> confirmDialog.dismiss())
                    .show();
        });

        btnCancel.setOnClickListener(v -> cancelWorkout());

        dialog.show();
    }

    /**
     * Restart the workout by resetting all relevant variables and UI components.
     */
    private void restartWorkout() {
        pauseOffset = 0;
        handler.removeCallbacks(timerRunnable);
        timerTextView.setText("00:00");
        completedExercises.clear();
        exercisesContainer.removeAllViews();
        loadExercises();
        startCountdown();
        Log.d(TAG, "Workout restarted.");

        // Explicitly cancel the notification when workout is restarted
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(WORKOUT_NOTIFICATION_ID);
        Log.d(TAG, "Workout notification canceled.");
    }
    /**
     * Start the workout timer and send a notification.
     */
    private void startWorkout() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            handler.post(timerRunnable);
            isRunning = true;
            Toast.makeText(this, "Workout started!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Workout started.");
            sendNotification("Workout Started", "Your workout timer has started!");
        }
    }

    /**
     * Pause the workout timer and send a notification.
     */
    private void pauseWorkout() {
        if (isRunning) {
            pauseOffset += System.currentTimeMillis() - startTime;
            handler.removeCallbacks(timerRunnable);
            isRunning = false;
            Toast.makeText(this, "Workout paused!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Workout paused.");
            sendNotification("Workout Paused", "Your workout is currently paused.");
        }
    }

    /**
     * Finish the workout, save history, and send a notification.
     */
    private void finishWorkout() {
        if (completedExercises.isEmpty()) {
            Toast.makeText(this, "Please complete at least one exercise to finish the workout.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted to finish workout without completing any exercises.");
            return;
        }

        if (isRunning) pauseWorkout();
        String totalTime = timerTextView.getText().toString();
        saveWorkoutHistory(totalTime);
        Toast.makeText(this, "Workout finished and saved!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Workout finished.");

        // Explicitly cancel the notification when workout is finished
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(WORKOUT_NOTIFICATION_ID);
        Log.d(TAG, "Workout notification canceled.");

        finish();
    }

    private void cancelWorkout() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(WORKOUT_NOTIFICATION_ID);
        Log.d(TAG, "Workout notification canceled.");

        finish();
    }

    /**
     * Start the countdown before the workout begins.
     */
    private void startCountdown() {
        // Reset countdown value
        countdown[0] = 3;

        countdownTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.GONE);
        exercisesContainer.setVisibility(View.GONE);
        trainingNameTextView.setVisibility(View.GONE);
        pauseButton.setVisibility(View.GONE);
        finishButton.setVisibility(View.GONE);

        countdownTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        countdownTextView.setGravity(Gravity.CENTER);

        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    playTickSound();
                    countdownTextView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                            StartWorkoutActivity.this, R.anim.scale_animation));
                    countdownTextView.setText(String.valueOf(countdown[0]--));
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    countdownTextView.setVisibility(View.GONE);
                    timerTextView.setVisibility(View.VISIBLE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    trainingNameTextView.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.VISIBLE);
                    finishButton.setVisibility(View.VISIBLE);
                    startWorkout();
                }
            }
        });

        Log.d(TAG, "Countdown started.");
    }

    /**
     * Resume the countdown after a pause.
     */
    private void resumeCountdown() {
        countdownHandler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    playTickSound(); // Play tick sound
                    countdownTextView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                            StartWorkoutActivity.this, R.anim.scale_animation));
                    countdownTextView.setText(String.valueOf(countdown[0]--));
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    countdownTextView.setVisibility(View.GONE);
                    timerTextView.setVisibility(View.VISIBLE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    trainingNameTextView.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.VISIBLE);
                    finishButton.setVisibility(View.VISIBLE);
                    startWorkout();
                }
            }
        });

        Log.d(TAG, "Countdown resumed.");
    }
}
