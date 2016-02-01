package fr.membrives.dispotrains.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import fr.membrives.dispotrains.R;
import fr.membrives.dispotrains.StationDetailActivity;
import fr.membrives.dispotrains.data.Elevator;
import fr.membrives.dispotrains.data.Station;

/**
 * Manages notifications for non-working stations
 */
public class StationNotificationManager {
    private final Context mContext;

    public StationNotificationManager(Context context) {
        mContext = context;
    }

    public void changedWorkingState(Station station) {
        emitNotification(station, Notification.PRIORITY_LOW);
    }

    public void notWorking(Station station) {
        emitNotification(station, Notification.PRIORITY_LOW);
    }

    private void emitNotification(Station station, int priority) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext).setSmallIcon(R.drawable.ic_launcher);
        if (station.getWorking()) {
            mBuilder.setContentTitle(station.getDisplay() + " est réparée").setContentText(
                    Integer.toString(station.getElevators().size()) + " ascenseurs en " +
                            "fonctionnement.");
        } else {
            int brokenElevators =
                    Collections2.filter(station.getElevators(), new Predicate<Elevator>() {
                        @Override
                        public boolean apply(Elevator input) {
                            return !input.isWorking();
                        }
                    }).size();
            mBuilder.setContentTitle(station.getDisplay() + " en panne")
                    .setContentText(Integer.toString(brokenElevators) + " ascenseurs en " +
                            "panne.");

        }

        mBuilder.setPriority(priority);

        Intent resultIntent = new Intent(mContext, StationDetailActivity.class);
        resultIntent.putExtra("station", station.getName());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack
        stackBuilder.addParentStack(StationDetailActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(station.getName().hashCode(), mBuilder.build());
    }
}
