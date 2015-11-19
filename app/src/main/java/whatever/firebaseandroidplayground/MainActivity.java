package whatever.firebaseandroidplayground;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected static String TAG = "###Main";

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    // last location logged inside the app
    protected Location mLastLocation;

    protected String userID;
    protected long timestamp;

    private TextView lastLocLat, lastLocLong, userIDTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set firebase android context
        Firebase.setAndroidContext(this);

        // build Google API client to get the last known location
        buildGoogleApiClient();

        Button sendLocation = (Button) findViewById(R.id.sendLocation);
        sendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLastLocation == null) {
                    Toast toast = Toast.makeText(getApplicationContext(), "not able to send location, permission granted?", Toast.LENGTH_LONG);
                    toast.show();
                }
                LocationSaver.saveLocation(mLastLocation, timestamp, userID);
            }
        });

        Button nearby = (Button) findViewById(R.id.nearbyButton);
        nearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), PeopleNearby.class);
                startActivity(i);
            }
        });

        // set the userID
        userID = "t"+(System.currentTimeMillis());

        lastLocLat = (TextView) findViewById(R.id.lastLocLat);
        lastLocLong = (TextView) findViewById(R.id.lastLocLong);
        userIDTV = (TextView) findViewById(R.id.userID);
        userIDTV.setText("userID: "+userID);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Toast toast = Toast.makeText(this, "please grant location permission in settings!", Toast.LENGTH_LONG);
            toast.show();
        }
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
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d(TAG, "lat:" + mLastLocation.getLatitude() +
                    " long:" + mLastLocation.getAltitude());
            lastLocLat.setText("lat: " + mLastLocation.getLatitude());
            lastLocLong.setText("long: "+mLastLocation.getLongitude());
            timestamp = System.currentTimeMillis();
        } else {
            Log.d(TAG, "location couldn't be updated...");
            lastLocLat.setText("lat: unavailable");
            lastLocLong.setText("long: unavailable");
        }
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
