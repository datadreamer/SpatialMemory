package com.datadreamer.spatialmemory;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService implements HttpTaskListener {

    protected static final String TAG = "Geofence";
    protected HttpTask dataTask;
    protected HttpImageTask imageTask;
    private String apiUrl = "http://www.spatialmemory.com/api.py";
    private String id, circa, title;

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            //String geofenceTransitionDetails = getGeofenceTransitionDetails(this, geofenceTransition, triggeringGeofences);
            //sendNotification(geofenceTransitionDetails);
            //Log.i(TAG, geofenceTransitionDetails);
            for (Geofence geofence : triggeringGeofences) {
                Log.d(TAG, geofence.getRequestId());
                id = geofence.getRequestId();
                // FIXME: do something with multiple ID's returned
            }

            // TODO: grab data from photoList
            //ArrayList photoList = intent.getParcelableArrayListExtra("photoList");

            // grab data from server since this isn't working
            dataTask = new HttpTask(this);
            dataTask.execute(apiUrl + "?action=info&id=" + id);
            // TODO: issue notification with thumb and
            // photo data
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the PhotoActivity.
     */
    private void sendNotification(String notificationDetails) {
        //Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);    // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), PhotoActivity.class);
        //notificationIntent.putExtra("photo", )
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);  // Construct a task stack.
        stackBuilder.addParentStack(MainActivity.class);                // Add the main Activity to the task stack as the parent.
        stackBuilder.addNextIntent(notificationIntent);                 // Push the content Intent onto the stack.

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        // TODO: use image that was just downloaded for icon.
        // TODO: grab title and date of image and display in notification.
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


    private String getGeofenceTransitionDetails(Context context, int geofenceTransition, List<Geofence> triggeringGeofences) {
        String geofenceTransitionString = getTransitionString(geofenceTransition);
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);
        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

    @Override
    public void httpDataDownloaded(String result) {
        // get photo data to display in notification
        Log.d(TAG, "Photo info downloaded.");
        try {
            JSONObject p = new JSONObject(result);
            id = p.getString("id");
            title = p.getString("title");
            circa = p.getString("circa");
            // get thumbnail
            imageTask = new HttpImageTask(this);
            imageTask.execute(apiUrl + "?action=photo&id=" + id + "&sw=96&sh=96");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void httpImageDownloaded(Bitmap img) {
        // get thumbnail to display in notification
        Log.d(TAG, "Thumbnail downloaded.");
        Intent notificationIntent = new Intent(getApplicationContext(), PhotoActivity.class);
        notificationIntent.putExtra("id", id);
        notificationIntent.putExtra("title", title);
        notificationIntent.putExtra("circa", circa);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(img)
                .setContentTitle(title)
                .setContentText(circa)
                .setContentIntent(notificationPendingIntent);
        builder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
        Log.d(TAG, "Notification issued.");
    }
}
