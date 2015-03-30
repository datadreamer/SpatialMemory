package com.datadreamer.spatialmemory;

import android.app.PendingIntent;
import android.content.Intent;
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
    protected GoogleApiClient mGoogleApiClient;     // entry point to Google Play services.
    protected ArrayList<Geofence> mGeofenceList;    // The list of geofences used in this sample.
    private boolean mGeofencesAdded;                // keep track if geofences were added.
    private PendingIntent mGeofencePendingIntent;   // requesting to add or remove geofences.
    protected Location mCurrentLocation;
    protected LocationRequest mLocationRequest;
    protected String mLastUpdateTime;
    protected int updateCount;
    protected boolean mRequestingLocationUpdates;

    // api/data variables
    protected String serverData;
    protected boolean requestedLocalData;
    protected HttpTask localDataTask;
    private String apiUrl = "http://www.spatialmemory.com/api.py";

    // ui variables
    protected TextView textElement;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textElement = (TextView) findViewById(R.id.geocoordinates);
        mGeofenceList = new ArrayList<Geofence>();
        mGeofencePendingIntent = null;
        mRequestingLocationUpdates = true;
        requestedLocalData = false;
        mLastUpdateTime = "";
        updateCount = 0;
        buildGoogleApiClient();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if(!requestedLocalData){
            localDataTask = new HttpTask(this);
            localDataTask.execute(apiUrl + "?action=local&lat=" + String.valueOf(mCurrentLocation.getLatitude()) + "&lon=" + String.valueOf(mCurrentLocation.getLongitude()));
            requestedLocalData = true;
        }
        updateCount++;
        updateUI();
    }

    private void updateUI() {
        Log.d(TAG, String.valueOf(mCurrentLocation.getLatitude()) +", "+ String.valueOf(mCurrentLocation.getLongitude()));
        String history = textElement.getText().toString();
        textElement.setText(String.valueOf(updateCount) + ": "+
                mLastUpdateTime +" = "+
                String.valueOf(mCurrentLocation.getLatitude()) +", "+
                String.valueOf(mCurrentLocation.getLongitude()) +"\n"+
                history);
    }

    @Override
    public void httpTaskComplete(String result) {
        Log.d(TAG, result);
        try {
            JSONArray list = new JSONArray(result);
            // for each photo entry
            for(int i=0; i<list.length(); i++){
                JSONObject p = list.getJSONObject(i);
                Log.d(TAG, p.getString("id") +" "+ p.getString("dist") +" "+ p.getString("lat") +" "+ p.getString("lon"));
                // build new geofence for entry
                // TODO: check for repeat entries before creating
                mGeofenceList.add(new Geofence.Builder()
                        .setRequestId(p.getString("id"))
                        .setCircularRegion(p.getDouble("lat"), p.getDouble("lon"), 50)
                        .setExpirationDuration(300000)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
            }
            // add geofences to activate callbacks
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(), getGeofencePendingIntent()).setResultCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }
}
