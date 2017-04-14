package com.bswearingen.www.rlpmapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayDeque;

import layout.PathFinderNotification;

public class DirectionProviderService extends Service {
    public DirectionProviderService() {
    }
    public static final String ACTION_STOP = "com.bswearingen.www.rlpmapp.action.Stop";
    public static final String KEY_NAVARRAY = "com.bswearingen.www.rlpmapp.key.NavArray";
    private static final String TAG = "DIRECTIONPROVIDERSRVICE";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 200;
    private static final float LOCATION_DISTANCE = 0f;

    private static RLPBluetoothManager mRLPBluetoothManager;
    private static ArrayDeque<NavigationPoint> mNavArray;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            if(mNavArray.isEmpty()) {
                stopSelf();
                PathFinderNotification.cancel(DirectionProviderService.this);
                return;
            }
            NavigationPoint current = mNavArray.peekFirst();
            Location destination = new Location(location);
            destination.setLatitude(current.Coordinates.latitude);
            destination.setLongitude(current.Coordinates.longitude);

            Log.e(TAG, "onLocationChanged: " + location);
            if(location.distanceTo(destination) < MainActivity.ALERT_RADIUS){
                Log.e(TAG, "LESS THAN 10");
                notifyBluetooth(mNavArray.peekFirst());
            }
        }

        private void notifyBluetooth(NavigationPoint mCurrentPoint)
        {
            boolean result = false;
            if(mCurrentPoint.Maneuver == NavigationPoint.Maneuvers.LEFT)
                result = mRLPBluetoothManager.sendMsg("L");
            else if(mCurrentPoint.Maneuver == NavigationPoint.Maneuvers.RIGHT)
                result = mRLPBluetoothManager.sendMsg("R");
            else
                result = mRLPBluetoothManager.sendMsg("U");
            if(result)
                mNavArray.poll();
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(ACTION_STOP.equals(intent.getAction())) {
            mNavArray.clear();
            stopSelf();
            PathFinderNotification.cancel(this);
            return START_NOT_STICKY;
        }
        Log.e(TAG, "onStartCommand");
        mNavArray = (ArrayDeque)intent.getExtras().getSerializable(KEY_NAVARRAY);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        mRLPBluetoothManager = RLPBluetoothManager.getInstance();
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (SecurityException ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}
