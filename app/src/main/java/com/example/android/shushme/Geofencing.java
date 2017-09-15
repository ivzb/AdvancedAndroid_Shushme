package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.shushme.provider.GeofenceBroadcastReceiver;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class Geofencing implements ResultCallback {

    public static final String TAG = Geofencing.class.getSimpleName();
    private static final float GEOFENCE_RADIUS = 50; // 50 meters
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    public Geofencing(Context context, GoogleApiClient googleApiClient) {
        mContext = context;
        mGoogleApiClient = googleApiClient;
        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;
    }

    public void registerAllGeofences() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected() ||
                mGeofenceList == null || mGeofenceList.size() == 0) {
            return;
        }

        try {
            LocationServices.GeofencingApi
                .addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent())
                .setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, securityException.getMessage());
        }
    }

    public void unRegisterAllGeofences() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) return;

        try {
            LocationServices.GeofencingApi
                .removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent())
                .setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, securityException.getMessage());
        }
    }

    public void updateGeofencesList(PlaceBuffer places) {
        mGeofenceList = new ArrayList<>();

        if (places == null || places.getCount() == 0) return;

        for (Place place: places) {
            String placeId = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLng = place.getLatLng().longitude;

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeId)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLng, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            mGeofenceList.add(geofence);
        }
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG, String.format("Error adding/removing geofence : %s",
                result.getStatus().toString()));
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent == null) {
            Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
            mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return mGeofencePendingIntent;
    }
}
