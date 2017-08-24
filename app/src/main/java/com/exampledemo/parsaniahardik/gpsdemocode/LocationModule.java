package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.util.GeoPoint;

/**
 * Created by igalk on 8/22/2017.
 */

public class LocationModule extends HandlerThread
        implements com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private final GoogleApiClient mGoogleApiClient;
    private final Handler mUiHandler;
    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private final long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private final long FASTEST_INTERVAL = 2000; /* 2 sec */

    private KalmanGPS m_KalmanGPS;

    public LocationModule(String name, Context context, Handler handlerUi) {
        super(name);

        mUiHandler = handlerUi;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    @Override
    public void onLooperPrepared() {
        super.onLooperPrepared();
        mGoogleApiClient.connect();
    }

    protected void initializeLocation() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        LocationServices
                .FusedLocationApi
                .requestLocationUpdates(
                        mGoogleApiClient,
                        mLocationRequest,
                        this);
    }

    private void initializeKalman() {
        try {
            m_KalmanGPS = new KalmanGPS();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint gPt = new GeoPoint(
                location.getLatitude(),
                location.getLongitude()
        );

        try {
            m_KalmanGPS.push(gPt.getLongitude(), gPt.getLatitude());
            double[] coordinate = m_KalmanGPS.getCoordinate();
            gPt = new GeoPoint(coordinate[1], coordinate[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle bundleContet = new Bundle();
        bundleContet.putDouble("longitude", location.getLongitude());
        bundleContet.putDouble("latitude", location.getLatitude());

        Message msg = mUiHandler.obtainMessage();
        msg.what = ConstMessages.MSG_NEW_GPS_POINT;
        msg.setData(bundleContet);

        mUiHandler.sendMessage(msg);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        initializeLocation();
        initializeKalman();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

}
