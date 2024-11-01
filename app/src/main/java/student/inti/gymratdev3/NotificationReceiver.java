package student.inti.gymratdev3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case StartWorkoutActivity.ACTION_PAUSE_WORKOUT:
                    // Handle pause action
                    Log.d(TAG, "Pause workout from notification");
                    // You can call a method to pause the workout
                    break;

                case StartWorkoutActivity.ACTION_RESUME_WORKOUT:
                    // Handle resume action
                    Log.d(TAG, "Resume workout from notification");
                    // You can call a method to resume the workout
                    break;
            }
        }
    }
}
