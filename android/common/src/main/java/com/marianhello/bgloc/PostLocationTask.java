package com.marianhello.bgloc;

import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.logging.LoggerManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Location task to post/sync locations from location providers
 *
 * All locations updates are recorded in local db at all times.
 * Also location is also send to all messenger clients.
 *
 * If option.url is defined, each location is also immediately posted.
 * If post is successful, the location is deleted from local db.
 * All failed to post locations are coalesced and send in some time later in one single batch.
 * Batch sync takes place only when number of failed to post locations reaches syncTreshold.
 *
 * If only option.syncUrl is defined, locations are send only in single batch,
 * when number of locations reaches syncTreshold.
 *
 */
public class PostLocationTask {
    private final LocationDAO mLocationDAO;
    private final PostLocationTaskListener mTaskListener;
    private final ConnectivityListener mConnectivityListener;

    private final ExecutorService mExecutor;

    private volatile boolean mHasConnectivity = true;
    private volatile Config mConfig;

    private org.slf4j.Logger logger;

    private DateFormat df = new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss.mmm");

    public interface PostLocationTaskListener
    {
        void onSyncRequested();
        void onRequestedAbortUpdates();
        void onHttpAuthorizationUpdates();
    }

    public PostLocationTask(LocationDAO dao, PostLocationTaskListener taskListener,
                            ConnectivityListener connectivityListener) {
        logger = LoggerManager.getLogger(PostLocationTask.class);
        logger.info("Creating PostLocationTask");

        mLocationDAO = dao;
        mTaskListener = taskListener;
        mConnectivityListener = connectivityListener;

        mExecutor = Executors.newSingleThreadExecutor();
    }

    public void setConfig(Config config) {
        mConfig = config;
    }

    public void setHasConnectivity(boolean hasConnectivity) {
        mHasConnectivity = hasConnectivity;
    }

    public void clearQueue() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mLocationDAO.deleteUnpostedLocations();
            }
        });
    }
/*
    private float calculateBearingAngle(double lat1, double lng1, double lat2, double lng2){
        double Phi1 = Math.toRadians(lat1);
        double Phi2 = Math.toRadians(lat2);
        double DeltaLambda = Math.toRadians(lng2 - lng1);
        double Theta = Math.atan2((Math.sin(DeltaLambda) * Math.cos(Phi2)),
          (Math.cos(Phi1) * Math.sin(Phi2) - Math.sin(Phi1) * Math.cos(Phi2) * Math.cos(DeltaLambda)));
        return (float)Math.toDegrees(Theta);
    }
*/
/*
    public double getEstimatedMiles(double lat1, double lng1, double lat2 , double lng2) {
        int R = 6371;
        double dLat = (lat2 - lat1) * Math.PI / 180;
        double dLon = (lng2 - lng1) * Math.PI / 180;
        double rLat1 = lat1 * Math.PI / 180;
        double rLat2 = lat2 * Math.PI / 180;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
          Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(rLat1) * Math.cos(rLat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R*c * 100) / 100;
    }
*/
    // 修改 add 不 post location 僅變更資料後儲存至DB
    public BackgroundLocation add(final BackgroundLocation location) {
        if (mConfig == null) {
            logger.warn("PostLocationTask has no config. Did you called setConfig? Skipping location.");
            return null;
        }

//        try {
//            BackgroundLocation prev = mLocationDAO.getLastLocation();
//            if (prev != null) {
//                // calculate bearing angle
//                float heading = calculateBearingAngle(
//                    prev.getLatitude(), prev.getLongitude(),
//                    location.getLatitude(), location.getLongitude());
//                location.setBearing(heading);
//                // calculate estimated miles
//                double miles = getEstimatedMiles(
//                    prev.getLatitude(), prev.getLongitude(),
//                    location.getLatitude(), location.getLongitude());
//                location.setEstimateMile(miles);
//            } else {
//                location.setBearing(0);
//                location.setEstimateMile(0);
//            }
//        } catch (Exception e) {
//            logger.warn("Error while get heading: {}", e.getMessage());
//        }

        // convert speed to km/h
//        if (location.hasSpeed()) {
//            location.setSpeed((float) Math.floor(location.getSpeed() * 3.6));
//        }

        long locationId = mLocationDAO.persistLocation(location);
        location.setLocationId(locationId);

//        try {
//            mExecutor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    post(location);
//                }
//            });
//        } catch (RejectedExecutionException ex) {
//            mLocationDAO.updateLocationForSync(locationId);
//        }

        return location;
    }

    public void shutdown() {
        shutdown(60);
    }

    public void shutdown(int waitSeconds) {
        mExecutor.shutdown();
        try {
            if (!mExecutor.awaitTermination(waitSeconds, TimeUnit.SECONDS)) {
                mExecutor.shutdownNow();
                mLocationDAO.deleteUnpostedLocations();
            }
        } catch (InterruptedException e) {
            mExecutor.shutdownNow();
        }
    }
/*
    private void post(final BackgroundLocation location) {
        long locationId = location.getLocationId();

        if (mHasConnectivity && mConfig.hasValidUrl()) {
            // get address position
//            String address = location.getPosition();
//            try {
//                if (address.isEmpty()) {
//                    location.setPosition("");
//                    if (mConfig.hasValidAddrUrl()) {
//                        String addrUrl = mConfig.getAddrUrl();
//                        addrUrl = addrUrl.replaceFirst("@latitude", Double.toString(location.getLatitude()));
//                        addrUrl = addrUrl.replaceFirst("@longitude", Double.toString(location.getLongitude()));
//                        JSONArray addrJson = HttpsPostService.getJSON(addrUrl);
//                        if (addrJson != null) {
//                          address = addrJson.getJSONObject(0).getString("Addr");
//                        }
//                        logger.debug("Posting address json \nto url: {} \nresponse: {}", addrUrl, addrJson == null ? "" : addrJson.toString());
//                    }
//                }
//            } catch (Exception e) {
//                logger.warn("Error while posting address: {}", e.getMessage());
//            } finally {
//                location.setPosition(address);
//            }

            if (postLocation(location)) {
                mLocationDAO.deleteLocationById(locationId);

                return; // if posted successfully do nothing more
            } else {
                mLocationDAO.updateLocationForSync(locationId);
            }
        } else {
            mLocationDAO.updateLocationForSync(locationId);
        }

        if (mConfig.hasValidSyncUrl()) {
            long syncLocationsCount = mLocationDAO.getLocationsForSyncCount(System.currentTimeMillis());
            if (syncLocationsCount >= mConfig.getSyncThreshold()) {
                logger.debug("Attempt to sync locations: {} threshold: {}", syncLocationsCount, mConfig.getSyncThreshold());
                mTaskListener.onSyncRequested();
            }
        }
    }
*/
    /**
     * 增加變更GPS位置至Sync作業，並通知可以進行同步作業
     * @param locationId
     */
    public void updateLocationForSync(long locationId, String position, String direction, double estimateMile, float speed) {
        mLocationDAO.updateLocationForSync(locationId, position, direction, estimateMile, speed);

        if (mConfig.hasValidSyncUrl()) {
            long syncLocationsCount = mLocationDAO.getLocationsForSyncCount(System.currentTimeMillis());
            if (syncLocationsCount >= mConfig.getSyncThreshold()) {
                logger.debug("Attempt to sync locations: {} threshold: {}", syncLocationsCount, mConfig.getSyncThreshold());
                mTaskListener.onSyncRequested();
            }
        }
    }
/*
    private String getHeadingDirection (double bearing) {
      String bearingWord = "";
      if (bearing >= 22 && bearing <= 67) bearingWord = "NE";
      else if (bearing >= 67 && bearing <= 112) bearingWord = "E";
      else if (bearing >= 112 && bearing <= 157) bearingWord = "SE";
      else if (bearing >= 157 && bearing <= 202) bearingWord = "S";
      else if (bearing >= 202 && bearing <= 247) bearingWord = "SW";
      else if (bearing >= 247 && bearing <= 292) bearingWord = "W";
      else if (bearing >= 292 && bearing <= 337) bearingWord = "NW";
      else if (bearing >= 337 || bearing <= 22) bearingWord = "N";
      return bearingWord;
    }
*/
/*
    private boolean postLocation(BackgroundLocation location) {
        logger.debug("Executing PostLocationTask#postLocation");
        JSONArray jsonLocations = new JSONArray();

        try {
            JSONObject json = (JSONObject) mConfig.getTemplate().locationToJson(location);
            // if bearing exist add direction attribute
//            if (json.has("bearing")) {
//                double bearing = json.getDouble("bearing");
//                json.put("direction", getHeadingDirection(bearing));
//            }
            // get estimate miles

            // convert timestamp to date format
//            if (json.has("gpsDate")) {
//              String d = df.format(new java.util.Date(Long.parseLong(json.getString("gpsDate"))));
//              json.put("gpsDate", d);
//              json.remove("gpsDate");
//              json.put("gpsDate", d);
//            }
            jsonLocations.put(json);
        } catch (JSONException e) {
            logger.warn("Location to json failed: {}", location.toString());
            return false;
        }

        String url = mConfig.getUrl();
        logger.debug("Posting json to url: {} headers: {}", url, mConfig.getHttpHeaders());
        int responseCode;

        try {
            logger.debug("Display posting locations: {}", jsonLocations.toString(2));
            responseCode = HttpPostService.postJSON(url, jsonLocations, mConfig.getHttpHeaders());
        } catch (Exception e) {
            mHasConnectivity = mConnectivityListener.hasConnectivity();
            logger.warn("Error while posting locations: {}", e.getMessage());
            return false;
        }

        if (responseCode == 285) {
            // Okay, but we don't need to continue sending these

            logger.debug("Location was sent to the server, and received an \"HTTP 285 Updates Not Required\"");

            if (mTaskListener != null)
                mTaskListener.onRequestedAbortUpdates();
        }

        if (responseCode == 401) {
            if (mTaskListener != null)
                mTaskListener.onHttpAuthorizationUpdates();
        }

        // All 2xx statuses are okay and 401 unauthorized pass
        boolean isStatusOkay = (responseCode >= 200 && responseCode < 300) || responseCode == 401;

        if (!isStatusOkay) {
            logger.warn("Server error while posting locations responseCode: {}", responseCode);
            return false;
        }

        return true;
    }
*/
}
