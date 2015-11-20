package whatever.firebaseandroidplayground;

/**
 * Created by jan on 18.11.15.
 */
public class Config {
    //firebase and gcm stuff
    public static String FIREBASE_BASE_ADDRESS = "https://savemya.firebaseio.com/";
    public static String FIREBASE_LOCATION_TRACKING = FIREBASE_BASE_ADDRESS+"locations/";
    public static String GCM_REGISTER = "https://save-my-ass.appspot.com/_ah/api/savemyass/v1/register";
    public static String GCM_START_ALARM = "https://save-my-ass.appspot.com/_ah/api/savemyass/v1/alarm";

    public static int LOCATION_UPDATE_PERIOD = 60*60*1000; //in ms -> maximum time between two location updates
    public static int LOCATION_UPDATE_PERIOD_MIN = 30*60*1000; //in ms -> we won't get more location updates than that
    public static float LOCATION_TRACKER_SEND_DISTANCE_THRESHOLD = 1000; //in m
}
