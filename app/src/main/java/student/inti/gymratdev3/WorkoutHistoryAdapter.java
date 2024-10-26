package student.inti.gymratdev3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

        String date = (String) workout.get("date");
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) workout.get("exercises");

        holder.dateTextView.setText(date);

        StringBuilder details = new StringBuilder();
        for (Map<String, Object> exercise : exercises) {
            String name = (String) exercise.get("exercise");
            int reps = ((Number) exercise.get("reps")).intValue();
            int sets = ((Number) exercise.get("sets")).intValue();
            details.append(name).append(": ").append(reps)
                    .append(" reps x ").append(sets).append(" sets\n");
        }
        holder.detailsTextView.setText(details.toString());
    }

    @Override
    public int getItemCount() {
        return workoutHistory.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView, detailsTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.date_text_view);
            detailsTextView = itemView.findViewById(R.id.details_text_view);
        }
    }
}
