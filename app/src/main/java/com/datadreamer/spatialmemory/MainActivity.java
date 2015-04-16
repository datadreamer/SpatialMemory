package com.datadreamer.spatialmemory;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener, HttpTaskListener, ResultCallback<Status> {

    // location variables
    private static final String TAG = "SpatialMemory";
    protected GoogleApiClient googleApiClient;
    protected ArrayList<Geofence> geofenceList;
    protected ArrayList<Photo> photoList;
    private boolean mGeofencesAdded;
    private PendingIntent mGeofencePendingIntent;
    protected Location loc;
    protected LocationRequest locRequest;
    protected String lastUpdate;
    protected int updateCount;
    protected boolean requestingLocationUpdates;

    // api/data variables
    protected boolean requestedLocalData;
    protected HttpTask localDataTask;
    private String apiUrl = "http://www.spatialmemory.com/api.py";

    // ui variables
    protected TextView textElement;





    /** ACTIVITY LIFECYCLE FUNCTIONS **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textElement = (TextView) findViewById(R.id.geocoordinates);
        geofenceList = new ArrayList<Geofence>();
        photoList = new ArrayList<Photo>();
        mGeofencePendingIntent = null;
        requestingLocationUpdates = true;
        requestedLocalData = false;
        lastUpdate = "";
        updateCount = 0;
        buildGoogleApiClient();
        googleApiClient.connect();
        if (googleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (googleApiClient.isConnected() && requestingLocationUpdates) {
            stopLocationUpdates();
            googleApiClient.disconnect();
        }
    }





    /** MENU FUNCTIONS **/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }





    /** LOCATION FUNCTIONS **/

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        locRequest = new LocationRequest();
        locRequest.setInterval(20000);
        locRequest.setFastestInterval(15000);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locRequest, this);
        Log.d(TAG, "Starting location updates.");
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        Log.d(TAG, "Ending location updates.");
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        lastUpdate = DateFormat.getTimeInstance().format(new Date());
        if(!requestedLocalData){
            localDataTask = new HttpTask(this);
            localDataTask.execute(apiUrl + "?action=local&lat=" + String.valueOf(loc.getLatitude()) + "&lon=" + String.valueOf(loc.getLongitude()));
            requestedLocalData = true;
        }
        Log.d(TAG, String.valueOf(loc.getLatitude()) +", "+ String.valueOf(loc.getLongitude()));
        updateCount++;
        updateUI();
    }

    private void updateUI() {
        String history = textElement.getText().toString();
        textElement.setText(String.valueOf(updateCount) + ": "+ lastUpdate +" = "+ String.valueOf(loc.getLatitude()) +", "+ String.valueOf(loc.getLongitude()) +"\n"+ history);
    }




    /** DATA HANDLING FUNCTIONS **/

    @Override
    public void httpDataDownloaded(String result) {
        Log.d(TAG, result);
        try {
            JSONArray list = new JSONArray(result);
            for(int i=0; i<list.length(); i++){         // for each photo entry
                JSONObject p = list.getJSONObject(i);
                //Log.d(TAG, p.getString("id") +" "+ p.getString("dist") +" "+ p.getString("lat") +" "+ p.getString("lon"));
                // update text element for debugging purposes
                String history = textElement.getText().toString();
                textElement.setText("id: " + p.getString("id") +", dist: "+ p.getString("dist") +", title: " +p.getString("title") +"\n"+ history);
                int id = p.getInt("id");
                int item_id = p.getInt("item_id");
                int page_id = p.getInt("page_id");
                String collection_id = p.getString("collection_id");
                String circa = p.getString("circa");
                String title = p.getString("title");
                float lat = Float.parseFloat(p.getString("lat"));
                float lon = Float.parseFloat(p.getString("lon"));
                Photo photo = new Photo(id, item_id, page_id, collection_id, title, circa, lat, lon);
                photoList.add(photo);
                // TODO: check for repeat entries before creating
                geofenceList.add(new Geofence.Builder()
                        .setRequestId(p.getString("id"))
                        .setCircularRegion(p.getDouble("lat"), p.getDouble("lon"), 100)
                        .setExpirationDuration(300000)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build());
            }
            // add geofences to activate callbacks
            LocationServices.GeofencingApi.addGeofences(googleApiClient, getGeofencingRequest(), getGeofencePendingIntent());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void httpImageDownloaded(Bitmap img, String[] args) {
        // this never gets used, but must be implemented
        Log.d(TAG, "Image downloaded!");
    }

    /**
     * Creates a pending intent to be activated when a geofence is entered.
     * @return PendingIntent to handle geofence events.
     */
    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        //intent.putParcelableArrayListExtra("photoList", photoList);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Creates a geofence request using list of locations returned from server.
     * @return GeofencingRequest
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    /**
     * Called when geofences have been added.
     * @param status from ResultCallback
     */
    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {   // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
        } else {    // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }


}
