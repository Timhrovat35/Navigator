package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import java.text.DecimalFormat;

public class NavigationActivity extends AppCompatActivity implements SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "NavigationActivity";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView distanceTextView;
    private Location startLocation;
    private ProgressBar loadingProgressBar;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private float bearing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        distanceTextView = findViewById(R.id.distanceTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingProgressBar.setVisibility(View.VISIBLE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("startLocation")) {
            startLocation = intent.getParcelableExtra("startLocation");
        }

        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopLocationUpdates();
                navigateToMainActivity();
            }
        });

        createLocationCallback();
        requestLocationUpdates();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            // Handle the received location here
                            handleLocationUpdate(location);
                        }
                    }
                }
            }
        };
    }

    private void requestLocationUpdates() {
        // Check if the location permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(4000); // Request location updates every 4 seconds
            locationRequest.setFastestInterval(2000); // Fastest interval for location updates
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            Log.d(TAG, "Requesting location updates");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void handleLocationUpdate(Location location) {
        // Handle the received location here
        if (startLocation != null) {
            double startLatitude = startLocation.getLatitude();
            double startLongitude = startLocation.getLongitude();
            double currentLatitude = location.getLatitude();
            double currentLongitude = location.getLongitude();

            bearing = calculateBearing(startLatitude, startLongitude, currentLatitude, currentLongitude);

            double distance = calculateDistance(currentLatitude, currentLongitude, startLatitude, startLongitude);
            if (distance <= 1) {
                // Distance is 1 or less, navigate to SuccessActivity
                stopLocationUpdates();
                navigateToSuccessActivity();
            }
            DecimalFormat decimalFormat = new DecimalFormat("#0.00");
            String formattedDistance = decimalFormat.format(distance);
            distanceTextView.setText("Distance: " + formattedDistance + " meters");
            Log.d(TAG, "Location Update: Lat " + location.getLatitude() +
                    ", Lng " + location.getLongitude() + " Starting location: Lat: " + startLocation.getLatitude() + " , Lng: " + startLocation.getLongitude() + " DISTANCE (m): " + distance + " BEARING: " + bearing);
        }

        loadingProgressBar.setVisibility(View.GONE);
    }

    private void rotateCompassImageView(float azimuth) {
        ImageView compassImageView = findViewById(R.id.compassImageView);
        compassImageView.setRotation(((-bearing) - azimuth));
    }

    private static float calculateBearing(double startLat, double startLon, double currentLat, double currentLon) {
        double startLatRad = Math.toRadians(startLat);
        double startLonRad = Math.toRadians(startLon);
        double currentLatRad = Math.toRadians(currentLat);
        double currentLonRad = Math.toRadians(currentLon);

        double deltaLon = startLonRad - currentLonRad;

        double y = Math.sin(deltaLon) * Math.cos(startLatRad);
        double x = Math.cos(currentLatRad) * Math.sin(startLatRad) - Math.sin(currentLatRad) * Math.cos(startLatRad) * Math.cos(deltaLon);

        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);

        return (float) (-bearingDeg);
    }

    private double calculateDistance(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        GeodesicData geodesicData = Geodesic.WGS84.Inverse(startLatitude, startLongitude, endLatitude, endLongitude);
        double distanceInMeters = geodesicData.s12;
        return distanceInMeters;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(NavigationActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void navigateToSuccessActivity() {
        Intent intent = new Intent(NavigationActivity.this, SuccessActivity.class);
        startActivity(intent);
    }

    // Handle location permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                // Location permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the rotation sensor listener
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the rotation sensor listener
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            float[] orientationAngles = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float azimuthRad = orientationAngles[0];
            float azimuthDeg = (float) Math.toDegrees(azimuthRad);
            rotateCompassImageView(azimuthDeg);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}
