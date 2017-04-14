package com.bswearingen.www.rlpmapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import layout.PathFinderNotification;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String PREFS_NAME = "RLPPrefsFile";
    public static final int ALERT_RADIUS = 60;

    private String TAG = "RLPMapp";
    private GoogleMap mMap;
    private LocationManager mLocationManager=null;
    private LocationListener mLocationListener=null;
    private Marker mStart = null;
    private Marker mDestMarker = null;
    private LatLng mDestination = null;
    private Polyline mPolyLine = null;
    private RequestQueue queue = null;
    private ArrayDeque<NavigationPoint> mNavigationQueue = new ArrayDeque<NavigationPoint>();
    private NotificationManager mNotificationManager;
    private RLPBluetoothManager mRLPBluetoothManager;
    private List<Integer> proximityAlerts = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        queue = Volley.newRequestQueue(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.settings) {
            // do something here
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
        } else if(id == R.id.demo){
            Intent i = new Intent(MainActivity.this, DemoActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
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
        mNotificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        mMap.setOnMapClickListener(makeMapOnClickListener());

        mMap.setOnMarkerClickListener(createMapOnClickListener());

        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission
                .ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    1 );
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, mLocationListener);
        Location lastKnown = null;
        try {
            lastKnown = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        catch(SecurityException e) {}
        if(lastKnown == null) {
            try {
                lastKnown = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            catch(SecurityException e) {}
        }

        if(lastKnown == null) return;
        LatLng lastKnownLatLong = new LatLng(lastKnown.getLatitude(), lastKnown.getLongitude());

        // Add at the current location
        MarkerOptions marker = new MarkerOptions();
        marker.position(lastKnownLatLong);
        marker.icon(defaultMarker(HUE_AZURE));

        mStart = mMap.addMarker(marker);

        // Move the camera there
        mMap.moveCamera(CameraUpdateFactory.newLatLng(lastKnownLatLong));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

        Button directionButton = (Button) findViewById(R.id.directionButton);
        directionButton.setOnClickListener(makeOnDirectionButtonClickListener());

        mRLPBluetoothManager = RLPBluetoothManager.getInstance(this);

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO - Start a service that runs through the queue
                mMap.setOnMapClickListener(null);
                //PathfinderIntentService.startActionPathfinding(MainActivity.this, mNavigationQueue, mRLPBluetoothManager);
                //RemoteViews contentView = new RemoteViews(MainActivity.this.getPackageName(), R.layout.path)
                Intent intent = new Intent(MainActivity.this, DirectionProviderService.class);
                Bundle extras = new Bundle();
                extras.putSerializable(DirectionProviderService.KEY_NAVARRAY, mNavigationQueue);
                intent.putExtras(extras);
                startService(intent);
                PathFinderNotification.notify(MainActivity.this, "engaged", 0);

            }
        });
    }

    public GoogleMap.OnMapClickListener makeMapOnClickListener(){
        return new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // TODO - This is too (noticeably) slow
                mMap.clear();

                mMap.addMarker(new MarkerOptions().position(mStart.getPosition()).draggable(false).icon(BitmapDescriptorFactory
                        .defaultMarker(HUE_AZURE)));
                mDestMarker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true).icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                mDestination = latLng;
                mNavigationQueue.clear();
            }
        };
    }

    public View.OnClickListener makeOnDirectionButtonClickListener()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDestination != null) {
                    // Get directions from google
                    String baseURL = "https://maps.googleapis.com/maps/api/directions/json?";
                    baseURL += "origin=" + mStart.getPosition().latitude + "," + mStart.getPosition().longitude;
                    baseURL += "&destination=" + mDestination.latitude + "," + mDestination.longitude;
                    baseURL += "&mode=bicycling";//&key=" + getString(R.string" +
                    //".google_directions_key);

                    JsonObjectRequest stringRequest = new JsonObjectRequest (
                            Request.Method.GET, baseURL, null, makeDirectionResponseListener(),
                            new Response.ErrorListener(){
                                @Override
                                public void onErrorResponse(VolleyError error)
                                {
                                    VolleyError err = error;
                                }
                            });
                    queue.add(stringRequest);
                }
                else
                    Toast.makeText(getApplicationContext(), "Add a destination", Toast.LENGTH_LONG).show();
            }
        };
    }

    public Response.Listener<JSONObject> makeDirectionResponseListener(){
        return new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject Response)
            {
                try {
                    JSONArray routeObject = Response.getJSONArray("routes");
                    JSONObject routes = routeObject.getJSONObject(0);
                    JSONObject overviewPolylines = routes
                            .getJSONObject("overview_polyline");

                    JSONArray legsObject = routes.getJSONArray("legs");
                    JSONObject legs = legsObject.getJSONObject(0);

                    JSONArray stepsObject = legs.getJSONArray("steps");

                    String encodedString = overviewPolylines.getString("points");

                    List<LatLng> list = decodePoly(encodedString);
                    PolylineOptions options = new PolylineOptions().width(10).color
                            (Color.BLUE).geodesic(false);

                    mNavigationQueue = new ArrayDeque<NavigationPoint>(stepsObject.length());
                    for(int z = 0; z < stepsObject.length(); z++)
                    {
                        JSONObject currentStep = stepsObject.getJSONObject(z);
                        if(currentStep.has("maneuver"))
                        {
                            JSONObject start = currentStep.getJSONObject
                                    ("start_location");
                            double lat = start.getDouble("lat");
                            double lng = start.getDouble("lng");
                            LatLng point = new LatLng(lat, lng);

                            if(currentStep.getString("maneuver").equals("turn-left")) {
                                mMap.addCircle(new CircleOptions().radius(ALERT_RADIUS).center(point));
                                mMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                mNavigationQueue.add(new NavigationPoint(point,
                                        NavigationPoint.Maneuvers.LEFT));
                            }
                            else if(currentStep.getString("maneuver").equals
                                    ("turn-right")) {
                                mMap.addCircle(new CircleOptions().radius(ALERT_RADIUS).center(point));
                                mMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                mNavigationQueue.add(new NavigationPoint(point,
                                        NavigationPoint.Maneuvers.RIGHT));
                            }
                        }
                    }
                    for (int z = 0; z < list.size(); z++) {
                        LatLng point = list.get(z);
                        options.add(point);
                    }
                    mPolyLine = mMap.addPolyline(options);

                    // TODO - Enable start button when directions are loaded
                    // TODO - Disable directions button when started
                    // TODO - Enable end button when started
                    // TODO - Disable end button when not started
                    // TODO - Start location/bluetooth service with start button
                    // TODO - Pass JSON object to service
                    // TODO - Service parses JSON steps array
                    // TODO - Service alerts with left/right (if maneuver contains
                    // left/right)
                    // TODO - End service with stop button
                }
                catch (JSONException e) {
                    Toast.makeText(MainActivity.this, Response.toString(), Toast
                            .LENGTH_LONG);
                }

            }
        };
    }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private GoogleMap.OnMarkerClickListener createMapOnClickListener(){
        return new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                CameraPosition currentPosition = mMap.getCameraPosition();
                Location cameraLocation = new Location("");
                cameraLocation.setLatitude(mMap.getCameraPosition().target.latitude);
                cameraLocation.setLongitude(mMap.getCameraPosition().target.longitude);

                Location markerLocation = new Location("");
                markerLocation.setLatitude(marker.getPosition().latitude);
                markerLocation.setLongitude(marker.getPosition().longitude);

                return false;
            }
        };
    }

    public void UpdateStartPosition(Location location){
        mStart.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            mStart.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
