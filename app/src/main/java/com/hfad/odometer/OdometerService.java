package com.hfad.odometer;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.content.ContextCompat;

public class OdometerService extends Service {

    public static final String PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION;

    private final IBinder binder = new OdometerBinder();
    private LocationManager locManager;
    private LocationListener listener;

    private Location previous = null;      // para acumular la distancia
    private Location lastLocation = null;  // última ubicación conocida
    private double distanceInMeters = 0;

    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (previous == null) {
                    previous = location;
                }
                distanceInMeters += location.distanceTo(previous);
                previous = location;
                lastLocation = location;
            }

            @Override
            public void onProviderDisabled(String provider) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
        };
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Inicia (o reinicia) las actualizaciones con la configuración capturada en la UI. */
    public boolean startTracking(int intervaloSegundos, float precisionMetros) {
        if (!hasPermission() || locManager == null) return false;
        stopTracking(); // evita listeners duplicados
        String provider = locManager.getBestProvider(new Criteria(), true);
        if (provider == null) return false; // GPS desactivado
        locManager.requestLocationUpdates(provider,
                intervaloSegundos * 1000L, precisionMetros, listener);
        return true;
    }

    public void stopTracking() {
        if (locManager != null && listener != null && hasPermission()) {
            locManager.removeUpdates(listener);
        }
    }

    public void resetDistance() {
        distanceInMeters = 0;
        previous = null;
    }

    /** Distancia recorrida en metros. */
    public double getDistance() {
        return distanceInMeters;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
        locManager = null;
        listener = null;
    }
}