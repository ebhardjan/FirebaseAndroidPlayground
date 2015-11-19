package whatever.firebaseandroidplayground;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LocationTrackerService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public String TAG = "###LocationTrackerService";

    private int notificationID = 666;

    private Timer timer;
    private Handler handler = new Handler();
    private final IBinder binder = new LocalBinder();

    // the ID of the user
    public String userID = "asdfasdf";

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    // last location sent to the firebase
    protected Location lastSentLocation;


    public LocationTrackerService() {}

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        LocationTrackerService getService() {
            return LocationTrackerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // todo: get the userID!!!!
        userID = "t"+System.currentTimeMillis();

        // set firebase android context
        Firebase.setAndroidContext(this);

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        showNotification();

        // timer initialization and run tasks periodically
        if(timer == null)
            timer = new Timer();
        else
            timer.cancel();
        timer.scheduleAtFixedRate(new PeriodicalTask(), 0, Config.LOCATION_UPDATE_PERIOD);

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    // note: if ID stays unchanged it just updates the notification...
    private void showNotification() {
        // initialize the notification
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Location Tracker Service")
                        .setContentText("running: "+currentDateandTime);

        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //  allows you to update the notification later on.
        mNotificationManager.notify(notificationID, mBuilder.build());
    }



    // does things periodically
    class PeriodicalTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread -> safety first
            handler.post(new Runnable() {

                @Override
                public void run() {
                    showNotification();
                }
            });
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Location tempLocation, loggedLocation;
        tempLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (tempLocation != null) {
            loggedLocation = tempLocation;
            if (lastSentLocation == null)
                sendLocation(loggedLocation);
            else if (loggedLocation.distanceTo(lastSentLocation) > Config.LOCATION_TRACKER_SEND_DISTANCE_TRASHHOLD)
                //send current position
                sendLocation(loggedLocation);
        }
    }

    private void sendLocation(Location loc){
        Firebase firebaseRef = new Firebase(Config.FIREBASE_LOCATION_TRACKING);
        GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
        GeoFire geoFire = new GeoFire(firebaseRef);
        geoFire.setLocation(userID, gLoc);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection to Google Play services suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection to Google Play services failed");
        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
