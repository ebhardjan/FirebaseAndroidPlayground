package whatever.firebaseandroidplayground;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LocationTrackerService extends Service implements
        com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public String TAG = "###LocationTrackerService";

    private int notificationID = 666;

    private Timer timer;
    private Handler handler = new Handler();
    private final IBinder binder = new LocalBinder();
    private ConnectivityManager cm;
    private ArrayList<String> log;

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

        // todo: get the userID!!!! -> probably do this in the main activity and put it here...
        userID = "t"+System.currentTimeMillis();

        // set firebase android context
        Firebase.setAndroidContext(this);

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        log = new ArrayList<>();

        log("service created");

        // connectivity manager initialization, used for hasInternet
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        // timer initialization and run tasks periodically
        if(timer == null)
            timer = new Timer();
        else
            timer.cancel();
        timer.scheduleAtFixedRate(new PeriodicalTask(), 0, Config.LOCATION_UPDATE_PERIOD);

    }

    private String getTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date())+": ";
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Config.LOCATION_UPDATE_PERIOD);
        mLocationRequest.setFastestInterval(Config.LOCATION_UPDATE_PERIOD_MIN);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        return mLocationRequest;
    }

    /**
     * Displays notification for debugging and testing purposes...
     * note: if ID stays unchanged it just updates the notification...
     */
    private void showNotification(String text) {
        // initialize the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Location Tracker Service")
                        .setContentText(getTime() + ": " + text);
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
        log(text);
    }

    private void log(String text){
        log.add(getTime()+text);
    }

    public ArrayList<String> getLog(){
        return log;
    }

    // does things periodically

    /**
     * Task that gets executed periodically, currently does just nothing except showing the
     * notification...
     * Will this really be used later on? -> I think no!
     */
    class PeriodicalTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread -> safety first
            handler.post(new Runnable() {

                @Override
                public void run() {
                    log("PeriodicalTask run()");
                }
            });
        }
    }

    /**
     * Checks if the location is far enough away from the last one sent, and sends it in that case
     */
    private void checkLocationAndUpdate(Location lastLocation) {
        log("checking if updating location in firebase is needed");
        Location loggedLocation;
        if (lastLocation != null) {
            loggedLocation = lastLocation;
            if (lastSentLocation == null)
                sendLocation(loggedLocation);
            else if (loggedLocation.distanceTo(lastSentLocation) > Config.LOCATION_TRACKER_SEND_DISTANCE_TRESHOLD)
                //send current position
                sendLocation(loggedLocation);
        }
    }

    /**
     * Saves location in firebase
     */
    private void sendLocation(Location loc){
        if(hasInternet()) {
            log("updating location in firebase...");
            // save loc in firebase
            Firebase firebaseRef = new Firebase(Config.FIREBASE_LOCATION_TRACKING);
            GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
            GeoFire geoFire = new GeoFire(firebaseRef);
            geoFire.setLocation(userID, gLoc);
        }
        else {
            log("currently no internet connection, registering connectivity_change receiver");
            //register broadcast receiver and do this when internet is available again...
            IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            MyReceiver myReceiver = new MyReceiver(loc);
            registerReceiver(myReceiver, filter);
        }
    }

    /**
     * Broadcast receiver for the internet connectivity event...
     */
    public class MyReceiver extends BroadcastReceiver {
        private Location loc;
        public MyReceiver(Location log) {}

        @Override
        public void onReceive(Context context, Intent intent) {
            // This method is called when this BroadcastReceiver receives an Intent broadcast.
            if(hasInternet())
                log("internet available -> logging");
                // save loc in firebase
                Firebase firebaseRef = new Firebase(Config.FIREBASE_LOCATION_TRACKING);
                GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
                GeoFire geoFire = new GeoFire(firebaseRef);
                geoFire.setLocation(userID, gLoc);
        }
    }

    /**
     * Returns true if internet is available
     */
    protected boolean hasInternet(){
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), this);
        checkLocationAndUpdate(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
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
        Log.d(TAG, "Connection to Google Play services suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection to Google Play services failed");
        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onLocationChanged(Location location) {
        checkLocationAndUpdate(location);
    }
}
