package com.datadreamer.spatialmemory;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONObject;

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
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
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
            imageTask = new HttpImageTask(this, id);
            imageTask.execute(apiUrl + "?action=photo&id=" + id + "&sw=96&sh=96");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void httpImageDownloaded(Bitmap img, String id) {
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
                .setContentTitle("Spatial Memory")
                .setContentText(title +" ("+circa+")")
                .setContentIntent(notificationPendingIntent);
        builder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
        Log.d(TAG, "Notification issued.");
    }
}
