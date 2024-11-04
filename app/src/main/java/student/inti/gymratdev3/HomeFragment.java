package student.inti.gymratdev3;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;




public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView weeklyExerciseCountTextView, weeklyMinutesTextView;
    private TextView averageDurationTextView; // Declare as instance variables
    private BarChart barChart;
    private PieChart pieChart;
    private static final String TAG = "HomeFragment";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        weeklyExerciseCountTextView = view.findViewById(R.id.weekly_exercise_count_text_view);
        weeklyMinutesTextView = view.findViewById(R.id.weekly_minutes_text_view);
        averageDurationTextView = view.findViewById(R.id.average_duration_text_view); // Initialize here
        barChart = view.findViewById(R.id.bar_chart);

        setupBarChart();
        loadWeeklyData();
        loadBarchartData();

        return view;
    }

    private void setupBarChart() {
        // BarChart style setup
        Description description = new Description();
        description.setText("");
        barChart.setDescription(description);

        barChart.setDrawGridBackground(false);
        barChart.setTouchEnabled(true);
        barChart.setPinchZoom(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setEnabled(false); // Disable the x-axis labels and grid lines

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(Color.DKGRAY);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadBarchartData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("workout_history")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<BarEntry> currentWeekEntries = new ArrayList<>();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Calendar calendar = Calendar.getInstance();
                        int currentWeekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
                        int index = 0; // X-axis index for each session

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Map<String, Object> data = doc.getData();
                            String dateStr = (String) data.get("date");

                            try {
                                calendar.setTime(dateFormat.parse(dateStr));
                                int sessionWeekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

                                // Only include data for the current week
                                if (sessionWeekOfYear == currentWeekOfYear) {
                                    String totalTimeStr = (String) data.get("totalTime");
                                    if (totalTimeStr != null) {
                                        String[] timeParts = totalTimeStr.split(":");
                                        int minutes = Integer.parseInt(timeParts[0]);
                                        int seconds = Integer.parseInt(timeParts[1]);

                                        // Convert total time into a float value (minutes + seconds as fractional part)
                                        float sessionTime = minutes + seconds / 60f;
                                        currentWeekEntries.add(new BarEntry(index++, sessionTime));
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing date", e);
                            }
                        }

                        // Set up BarChart Data
                        BarDataSet barDataSet = new BarDataSet(currentWeekEntries, "Training Time per Session (min:sec)");
                        barDataSet.setColor(ColorTemplate.MATERIAL_COLORS[0]);
                        barDataSet.setValueTextColor(Color.DKGRAY);
                        barDataSet.setValueTextSize(12f);
                        barDataSet.setValueFormatter(new TimeValueFormatter());

                        BarData barData = new BarData(barDataSet);
                        barData.setBarWidth(0.5f);
                        barChart.setData(barData);
                        barChart.animateY(1000);
                        barChart.invalidate();
                    } else {
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading weekly data", task.getException());
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadWeeklyData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("workout_history")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalMinutes = 0;
                        int totalSeconds = 0;
                        int cardioCount = 0;
                        int strengthCount = 0;

                        Map<Integer, Integer> weeklyMinutesMap = new HashMap<>();
                        Map<Integer, Integer> weeklyExerciseCountMap = new HashMap<>();
                        Map<Integer, Integer> weeklySessionCountMap = new HashMap<>();
                        Map<String, Integer> exerciseTypeCountMap = new HashMap<>(); // Track occurrences of each exercise type

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Map<String, Object> data = doc.getData();

                            // Calculate total exercises
                            List<?> exercises = (List<?>) data.get("exercises");
                            int exerciseCount = exercises != null ? exercises.size() : 0;

                            // Track each exercise type
                            if (exercises != null) {
                                for (Object exercise : exercises) {
                                    String exerciseType = (String) ((Map<String, Object>) exercise).get("type"); // Assuming exercise type is under "type"
                                    exerciseTypeCountMap.put(exerciseType, exerciseTypeCountMap.getOrDefault(exerciseType, 0) + 1);
                                }
                            }

                            // Get the date of the session and determine the week of the year
                            String dateStr = (String) data.get("date"); // Assuming "date" field is in "yyyy-MM-dd" format
                            try {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(dateFormat.parse(dateStr));
                                int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

                                // Calculate total time in minutes and seconds
                                String totalTimeStr = (String) data.get("totalTime");
                                if (totalTimeStr != null) {
                                    String[] timeParts = totalTimeStr.split(":");
                                    if (timeParts.length >= 2) {
                                        int minutes = Integer.parseInt(timeParts[0]);
                                        int seconds = Integer.parseInt(timeParts[1]);

                                        int totalMinutesForSession = minutes + (seconds / 60);
                                        totalMinutes += minutes;
                                        totalSeconds += seconds;

                                        // Add the session's minutes to the correct week in the map
                                        weeklyMinutesMap.put(weekOfYear, weeklyMinutesMap.getOrDefault(weekOfYear, 0) + totalMinutesForSession);
                                    }
                                }

                                // Track total exercises and sessions for the week
                                weeklyExerciseCountMap.put(weekOfYear, weeklyExerciseCountMap.getOrDefault(weekOfYear, 0) + exerciseCount);
                                weeklySessionCountMap.put(weekOfYear, weeklySessionCountMap.getOrDefault(weekOfYear, 0) + 1);

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing date", e);
                            }
                        }

                        // Get the most recent week's data
                        Integer latestWeek = weeklyExerciseCountMap.keySet().stream().max(Integer::compare).orElse(null);
                        int totalExercisesThisWeek = latestWeek != null ? weeklyExerciseCountMap.getOrDefault(latestWeek, 0) : 0;
                        int totalTrainingsThisWeek = latestWeek != null ? weeklySessionCountMap.getOrDefault(latestWeek, 0) : 0;

                        // Determine the most frequent exercise type by name
                        String mostFrequentExerciseType = exerciseTypeCountMap.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse("N/A");

                        // Display calculated values for this week
                        weeklyExerciseCountTextView.setText(String.format(Locale.getDefault(), "Exercises Completed This Week: %d", totalExercisesThisWeek));
                        weeklyMinutesTextView.setText(String.format(Locale.getDefault(), "Total Training Time This Week: %d min", totalMinutes));

                        // Display average duration and most frequent exercise type name
                        int totalWorkoutMinutes = totalMinutes + (totalSeconds / 60);
                        int averageMinutesPerSession = totalTrainingsThisWeek > 0 ? totalWorkoutMinutes / totalTrainingsThisWeek : 0;
                        int remainingSeconds = totalTrainingsThisWeek > 0 ? (totalSeconds % 60) / totalTrainingsThisWeek : 0;

                        averageDurationTextView.setText(String.format(Locale.getDefault(),
                                "Average Duration: %d:%02d", averageMinutesPerSession, remainingSeconds));

                    } else {
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading weekly data", task.getException());
                    }
                });
    }


    public class TimeValueFormatter extends ValueFormatter {
        @Override
        public String getBarLabel(BarEntry barEntry) {
            float value = barEntry.getY();
            int minutes = (int) value;
            int seconds = (int) ((value - minutes) * 60);
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }

}
