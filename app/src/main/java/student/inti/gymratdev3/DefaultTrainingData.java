package student.inti.gymratdev3;

import android.os.Build;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultTrainingData {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static List<Map<String, Object>> getFullBodyWorkout() {
        List<Map<String, Object>> fullBodyWorkout = new ArrayList<>();
        fullBodyWorkout.add(createExercise("Warm-up", 1, 1));
        fullBodyWorkout.add(createExercise("Bodyweight Squats", 3, 15));
        fullBodyWorkout.add(createExercise("Push-Ups", 3, 12));
        fullBodyWorkout.add(createExercise("Dumbbell Rows", 3, 12));
        fullBodyWorkout.add(createExercise("Lunges", 3, 10));
        fullBodyWorkout.add(createExercise("Plank", 3, 30)); // duration in seconds
        fullBodyWorkout.add(createExercise("Mountain Climbers", 3, 20));
        fullBodyWorkout.add(createExercise("Cool-down", 1, 1));

        saveWorkoutToFirebase("FullBodyWorkout", fullBodyWorkout);
        return fullBodyWorkout;
    }

    public static List<Map<String, Object>> getCardioBurn() {
        List<Map<String, Object>> cardioBurn = new ArrayList<>();
        cardioBurn.add(createExercise("Warm-up", 1, 1));
        cardioBurn.add(createExercise("Jump Rope", 3, 1)); // duration in minutes
        cardioBurn.add(createExercise("High Knees", 3, 45)); // duration in seconds
        cardioBurn.add(createExercise("Burpees", 3, 15));
        cardioBurn.add(createExercise("Jump Squats", 3, 15));
        cardioBurn.add(createExercise("Bicycle Crunches", 3, 20));
        cardioBurn.add(createExercise("Box Jumps or Step-Ups", 3, 12));
        cardioBurn.add(createExercise("Cool-down", 1, 1));

        saveWorkoutToFirebase("CardioBurn", cardioBurn);
        return cardioBurn;
    }

    public static List<Map<String, Object>> getStrengthTraining() {
        List<Map<String, Object>> strengthTraining = new ArrayList<>();
        strengthTraining.add(createExercise("Warm-up", 1, 1));
        strengthTraining.add(createExercise("Deadlift", 4, 8));
        strengthTraining.add(createExercise("Bench Press", 4, 8));
        strengthTraining.add(createExercise("Barbell Squats", 4, 8));
        strengthTraining.add(createExercise("Overhead Shoulder Press", 3, 10));
        strengthTraining.add(createExercise("Bicep Curls", 3, 12));
        strengthTraining.add(createExercise("Tricep Dips", 3, 12));
        strengthTraining.add(createExercise("Russian Twists", 3, 20));
        strengthTraining.add(createExercise("Cool-down", 1, 1));

        saveWorkoutToFirebase("StrengthTraining", strengthTraining);
        return strengthTraining;
    }

    private static Map<String, Object> createExercise(String name, int sets, int reps) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("name", name);
        exercise.put("sets", sets);
        exercise.put("reps", reps);
        return exercise;
    }

    private static void saveWorkoutToFirebase(String workoutType, List<Map<String, Object>> exercises) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            db.collection("default_training_plan").document(workoutType)
                    .set(Map.of("exercises", exercises))
                    .addOnSuccessListener(aVoid -> System.out.println(workoutType + " successfully written to Firebase!"))
                    .addOnFailureListener(e -> System.err.println("Error writing " + workoutType + " to Firebase: " + e));
        }
    }
}
