package whatever.firebaseandroidplayground;

import android.location.Location;

import com.firebase.client.Firebase;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

/**
 * Created by jan on 18.11.15.
 */
public class LocationSaver {

    public static String TAG = "###LocationSaver";
    public static void saveLocation(Location loc, long timestamp, String userID){
        Firebase firebaseRef = new Firebase(Config.FIREBASE_LOCATION_TRACKING);
        firebaseRef.child("last_update").child(userID).setValue(timestamp);
        GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
        GeoFire geoFire = new GeoFire(firebaseRef.child("location"));
        geoFire.setLocation(userID, gLoc);
    }

}
