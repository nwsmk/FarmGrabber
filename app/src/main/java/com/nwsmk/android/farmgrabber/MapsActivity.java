package com.nwsmk.android.farmgrabber;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.sqrt;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    /** System variables */
    private int totalRows = 0;
    private int totalCols = 0;
    private int currRow = 0;
    private int currCol = 0;
    private boolean finishJob = false;

    private LatLng[][] topLeftLatLngArray;

    /** Global Screen variables **/
    // screen dimension in pixels
    private int screenHeightPx = 0;
    private int screenWidthPx  = 0;

    // pixels per meter
    private int ppm            = 5;

    // screen dimension in meters
    private int screenHeightM  = 0;
    private int screenWidthM   = 0;

    // Google logo dimension in meters
    private float googleLogoPadding = 0;

    /** Global Map variables */
    private int landNum = 11;
    ArrayList<LatLng> boundary;
    private TreeMap<String, Point> mapImgMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set map type to satellite
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


        boundary = new ArrayList<>();

        /**
        boundary.add(new LatLng(16.4268129901041, 102.8380009059));
        boundary.add(new LatLng(16.4266819930293, 102.8379568936));
        boundary.add(new LatLng(16.426704769546, 102.8378494011));
        boundary.add(new LatLng(16.4268502721458, 102.8378330329));
        boundary.add(new LatLng(16.4268937418855, 102.8378020293)); **/

        boundary.add(new LatLng(14.078044, 100.599676));
        boundary.add(new LatLng(14.076957, 100.630704));
        boundary.add(new LatLng(14.062424, 100.627641));
        boundary.add(new LatLng(14.061009, 100.592892));

        drawOnMap(boundary);

        /** Main Process */
        // initialization
        getScreenPixels();
        getScreenMeters();

        float googleLogoScale = 0.2f;
        getGoogleLogoPadding(googleLogoScale);

        // bound box in lat/lng
        // 0) top left
        // 1) top right
        // 2) bottom right
        // 3) bottom left
        ArrayList<LatLng> boundLatLng = getBoundLatLng(boundary);
        //drawOnMap(boundLatLng);

        // bound box in meters (include padding for Google logo)
        // boundBoxM[0] = height
        // boundBoxM[1] = width;
        float[] boundBoxM = getBoundBoxM(boundLatLng);

        // calculate number of pictures to represent map in rows and columns
        int[] imageDimension = getImageDimension(boundBoxM);
        totalRows = imageDimension[0];
        totalCols = imageDimension[1];

        topLeftLatLngArray = getTopLeftLatLngArray(boundLatLng.get(0));
        fitMap();

        /**
        try {
            fitMap(topLeftLatLngArray);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    }

    /** Initializations ***************************************************************************/

    /** Get screen dimension in pixels */
    private void getScreenPixels() {
        // get screen resource
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // get screen dimensions
        screenWidthPx  = size.x;
        screenHeightPx = size.y;
    }

    /** Get screen dimension in meters */
    private void getScreenMeters () {
        screenWidthM  = (int) floor(screenWidthPx/ppm);
        screenHeightM = (int) floor(screenHeightPx/ppm);
    }

    /**
     * Find necessary padding for Google logo
     * @param scale                 ratio of Google logo to the total screen height
     * @return                      Google logo height in meters
     */
    private void getGoogleLogoPadding(float scale) {
        googleLogoPadding = (float) ceil(scale*screenHeightPx/ppm);
    }

    /** Process ***********************************************************************************/

    /**
     * Draw specified polygon on Google Maps
     */
    private void drawOnMap(ArrayList<LatLng> polygon) {

        PolygonOptions polygonOptions = new PolygonOptions();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < polygon.size(); i++) {
            polygonOptions.add(polygon.get(i));
            builder.include(polygon.get(i));
        }

        mMap.addPolygon(polygonOptions);

        LatLngBounds bounds = builder.build();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        mMap.animateCamera(cu, 2000, null);
    }

    /**
     * Get bounding box
     * @param polygon
     * @return
     */
    private ArrayList<LatLng> getBoundLatLng(ArrayList<LatLng> polygon) {
        ArrayList<LatLng> boundlatLng = new ArrayList<LatLng>();
        double[][] boundDouble  = new double[polygon.size()][2];

        for (int i = 0; i < polygon.size(); i++) {
            LatLng tmpLatLng = polygon.get(i);
            double lat       = tmpLatLng.latitude;
            double lng       = tmpLatLng.longitude;

            // convert lat/lng array to double array
            boundDouble[i][0] = lat;
            boundDouble[i][1] = lng;
        }

        // find 4 corners of the bound box
        double[] latArray = com.nwsmk.android.farmgrabber.utils.Maths.getCol(boundDouble, 0);
        double[] lngArray = com.nwsmk.android.farmgrabber.utils.Maths.getCol(boundDouble, 1);
        double minLat = com.nwsmk.android.farmgrabber.utils.Maths.getMin(latArray);
        double maxLat = com.nwsmk.android.farmgrabber.utils.Maths.getMax(latArray);
        double minLng = com.nwsmk.android.farmgrabber.utils.Maths.getMin(lngArray);
        double maxLng = com.nwsmk.android.farmgrabber.utils.Maths.getMax(lngArray);

        LatLng topLeft = new LatLng(maxLat, minLng);
        LatLng topRight = new LatLng(maxLat, maxLng);
        LatLng bottomRight = new LatLng(minLat, maxLng);
        LatLng bottomLeft = new LatLng(minLat, minLng);

        boundlatLng.add(topLeft);
        boundlatLng.add(topRight);
        boundlatLng.add(bottomRight);
        boundlatLng.add(bottomLeft);

        return boundlatLng;
    }

    /**
     * Get box dimension in meters
     * @param boundLatLng
     * @return
     */
    private float[] getBoundBoxM(ArrayList<LatLng> boundLatLng) {

        float[] boundBoxM = new float[2];

        // Read 4 corners in lat/lng
        LatLng topLeft     = boundLatLng.get(0);
        LatLng topRight    = boundLatLng.get(1);
        LatLng bottomRight = boundLatLng.get(2);
        LatLng bottomLeft  = boundLatLng.get(3);

        // find height in meters
        float[] height = new float[1];
        Location.distanceBetween(topLeft.latitude, topLeft.longitude,
                bottomLeft.latitude, bottomLeft.longitude, height);
        float boxHeight = height[0];

        // find width in meters
        float[] width = new float[1];
        Location.distanceBetween(topLeft.latitude, topLeft.longitude,
                topRight.latitude, topRight.longitude, width);
        float boxWidth = width[0];

        boundBoxM[0] = boxHeight + (2*googleLogoPadding);
        boundBoxM[1] = boxWidth + (2*googleLogoPadding);

        return boundBoxM;
    }

    private int[] getImageDimension(float[] boundBoxM) {
        int[] imageDimension = new int[2];
        int rows = 0;
        int cols = 0;

        rows = (int) ceil(boundBoxM[0]/screenHeightM);
        cols = (int) ceil(boundBoxM[1]/screenWidthM);

        imageDimension[0] = rows;
        imageDimension[1] = cols;

        return imageDimension;
    }


    private LatLng[][] getTopLeftLatLngArray(LatLng topLeftLatLng) {
        LatLng[][] topLeftLatLngArray = new LatLng[totalRows][totalCols];

        LatLng startTopLeftLatLng = getDestinationPoint(topLeftLatLng, -45, sqrt(2*googleLogoPadding));
        LatLng currTopLeftLatLng;

        double dWidth  = 0;
        double dHeight = 0;

        for (int i = 0; i < totalRows; i++) {
            dHeight = i*screenHeightM;
            currTopLeftLatLng = getDestinationPoint(startTopLeftLatLng, 180, dHeight);

            for (int j = 0; j < totalCols; j++) {
                dWidth = j*screenWidthM;
                topLeftLatLngArray[i][j] = getDestinationPoint(currTopLeftLatLng, 90, dWidth);
            }
        }

        return topLeftLatLngArray;
    }


    private LatLng getDestinationPoint(LatLng source, double brng, double dist) {
        dist = dist / (6371.0*1000);
        brng = Math.toRadians(brng);

        double lat1 = Math.toRadians(source.latitude), lon1 = Math.toRadians(source.longitude);
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) +
                Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));
        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(dist) *
                        Math.cos(lat1),
                Math.cos(dist) - Math.sin(lat1) *
                        Math.sin(lat2));
        if (Double.isNaN(lat2) || Double.isNaN(lon2)) {
            return null;
        }
        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }


    //private void fitMap(LatLng[][] topLeftLatLngArray) {
    private void fitMap() {

        finishJob = false;

        final String filename = Integer.toString(landNum) + "_" + Integer.toString(currRow) + "_" + Integer.toString(currCol) + ".png";

        LatLng topLeft = topLeftLatLngArray[currRow][currCol];
        LatLng bottomLeft = getDestinationPoint(topLeft, 90, screenWidthM);
        LatLng topRight = getDestinationPoint(topLeft, 180, screenHeightM);

        // current image lat/lng bounds
        LatLngBounds latLngBounds = new LatLngBounds(topRight, bottomLeft);

        // check if any boundary point is in this image
        getMapImgMap(filename, latLngBounds);


        int padding = 0; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(latLngBounds, screenWidthPx, screenHeightPx, padding);
        mMap.animateCamera(cu);

        GoogleMap.OnMapLoadedCallback mapLoadedCallback = new GoogleMap.OnMapLoadedCallback() {

            GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {

                Bitmap bitmap;

                @Override
                public void onSnapshotReady(Bitmap snapshot) {
                    bitmap = snapshot;
                    try {

                        String filepath = "/mnt/sdcard/Pictures/" + filename;
                        FileOutputStream out = new FileOutputStream(filepath);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);

                        // get screen bounds
                        LatLngBounds mLatLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                        LatLng mLatLngNE = mLatLngBounds.northeast;
                        LatLng mLatLngSW = mLatLngBounds.southwest;
                        Log.d("LOCATION", "NE: " + mLatLngNE + " SW: " + mLatLngSW);
                        Log.d("FINISH", "SNAPSHOTTED");
                        finishJob = true;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            @Override
            public void onMapLoaded() {
                mMap.snapshot(callback);
            }
        };

        mMap.setOnMapLoadedCallback(mapLoadedCallback);

        new MapCature().execute();

    }

    private void getMapImgMap(String key, LatLngBounds currBound) {

        for (int i = 0; i < boundary.size(); i++) {
            LatLng currLatlng = boundary.get(i);
            if (currBound.contains(currLatlng)) {
                mapImgMap.put(key, getPixelFromLatLng(currLatlng));
            } else {
                mapImgMap.put(key, null);
            }
        }
    }

    // get pixel coordinates from lat/lng input
    private Point getPixelFromLatLng(LatLng latLng) {
        android.graphics.Point pixelPoint = mMap.getProjection().toScreenLocation(latLng);
        return pixelPoint;
    }

    // get lat/lng coordinates from point input
    private LatLng getLatLngFromPixel(Point point) {
        android.graphics.Point pixelPoint = new android.graphics.Point((int)point.x, (int)point.y);
        LatLng latLng = mMap.getProjection().fromScreenLocation(pixelPoint);
        return latLng;
    }


    protected class MapCature extends AsyncTask<Void, Void, Void> {

        public MapCature() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            while (!finishJob);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Log.d("FINISH", "FINISH ONE ROUND SHOT");
            currCol++;
            if (currCol>=totalCols) {
                currRow++;
                currCol = 0;
                if (currRow>=totalRows) {
                    // do nothing
                    Log.d("CURR ROW & COL", "CURR ROW: " + currRow + " CURR COL: " + currCol);
                    Log.d("FINISH", "FINISH ALL SHOTS");
                } else {
                    Log.d("CURR ROW & COL", "CURR ROW: " + currRow + " CURR COL: " + currCol);
                    fitMap();
                }
            } else {
                Log.d("CURR ROW & COL", "CURR ROW: " + currRow + " CURR COL: " + currCol);
                fitMap();
            }

        }
    }
}
