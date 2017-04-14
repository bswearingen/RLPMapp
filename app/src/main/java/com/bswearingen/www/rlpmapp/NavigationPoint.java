package com.bswearingen.www.rlpmapp;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Created by Ben on 3/15/2017.
 */

public class NavigationPoint implements Serializable{

    public Coordinates Coordinates;
    public Maneuvers Maneuver;

    public NavigationPoint(LatLng coords, Maneuvers maneuver){
        Coordinates = new Coordinates(coords);
        Maneuver = maneuver;
    }

    public NavigationPoint(double lat, double lng, Maneuvers maneuver){
        Coordinates.latitude = lat;
        Coordinates.longitude = lng;
        Maneuver = maneuver;
    }

    public enum Maneuvers{
        LEFT,
        RIGHT,
        UTURN
    }
    public class Coordinates implements Serializable{
        double latitude;
        double longitude;

        public Coordinates(LatLng coords){
            latitude = coords.latitude;
            longitude = coords.longitude;
        }
    }
}
