package student.inti.gymratdev3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder> {

    private final List<Map<String, Object>> workoutHistory;

    public WorkoutHistoryAdapter(List<Map<String, Object>> workoutHistory) {
        this.workoutHistory = workoutHistory;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> workout = workoutHistory.get(position);

        if (workout == null) {
            holder.dateTextView.setText("No data");
            holder.detailsTextView.setText("");
            holder.totalTimeTextView.setText("Total Time: N/A");
            return; // Handle null case
        }

        // Extract data from the workout map
        String date = (String) workout.getOrDefault("date", "Unknown Date");
        String totalTime = (String) workout.getOrDefault("totalTime", "00:00"); // Default to 00:00 if not available
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) workout.getOrDefault("exercises", new ArrayList<>());

        // Set the date and total time in the UI
        holder.dateTextView.setText(date);
        holder.totalTimeTextView.setText("Total Time: " + totalTime);

        // Build the exercises details string, including KG value
        StringBuilder details = new StringBuilder();
        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.getOrDefault("exercise", "Unnamed Exercise");
            int reps = ((Number) exercise.getOrDefault("reps", 0)).intValue();
            int sets = ((Number) exercise.getOrDefault("sets", 0)).intValue();
            int kg = ((Number) exercise.getOrDefault("kg", 0)).intValue();

            details.append(name)
                    .append(": ").append(reps).append(" reps x ").append(sets).append(" sets ")
                    .append("@ ").append(kg).append(" kg\n");
        }

        // Set the exercises details in the UI
        holder.detailsTextView.setText(details.toString());
    }


    @Override
    public int getItemCount() {
        return workoutHistory.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView, totalTimeTextView, detailsTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.date_text_view);
            totalTimeTextView = itemView.findViewById(R.id.total_time_text_view); // Add total time TextView
            detailsTextView = itemView.findViewById(R.id.details_text_view);
        }
    }
}
