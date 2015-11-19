package whatever.firebaseandroidplayground;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class PeopleNearby extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected static String TAG = "###PeopleNearby";

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    // last location logged inside the app
    protected GeoQuery geoQuery;

    protected TextView log;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people_nearby);

        log = (TextView) findViewById(R.id.log);
        log.setText("log:");

        buildGoogleApiClient();

        GeoFire geoFire = new GeoFire((new Firebase(Config.FIREBASE_LOCATION_TRACKING)).child("location"));
        //GeoFire geoFire = new GeoFire(new Firebase("https://publicdata-transit.firebaseio.com/_geofire"));
        geoQuery = geoFire.queryAtLocation(new GeoLocation(0, 0), 100);
        //GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(33.7, -118.3), 100);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                log.setText(log.getText() + "\n" + "enter - key: " + key + " loc: " + location.latitude + "," + location.longitude);
            }

            @Override
            public void onKeyExited(String key) {
                log.setText(log.getText()+"\n"+"exit - key: "+key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                log.setText(log.getText() + "\n" + "move - key: " + key + " loc: " + location.latitude + "," + location.longitude);
            }

            @Override
            public void onGeoQueryReady() {
                Log.d("###", "All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                Log.d("###", "There was an error with this query: " + error);
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
        } else {
            mLastLocation = new Location("");
            mLastLocation.setLatitude(0);
            mLastLocation.setLongitude(0);
        }
        GeoLocation newLoc = new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        geoQuery.setCenter(newLoc);
        log.setText(log.getText()+"\nreset center to: "+newLoc.latitude+ ", "+newLoc.longitude);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
}
