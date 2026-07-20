package com.example.partitioncleaner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class AltitudeActivity extends BaseActivity implements SensorEventListener, LocationListener {

    private static final int REQ_LOC = 4001;
    private static final float SEA_LEVEL_PRESSURE = 1013.25f;

    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private LocationManager locationManager;

    private TextView tvPressureAlt;
    private TextView tvPressure;
    private TextView tvGpsAlt;
    private TextView tvGpsStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_altitude);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("海拔测量");

        tvPressureAlt = findViewById(R.id.tv_pressure_alt);
        tvPressure = findViewById(R.id.tv_pressure);
        tvGpsAlt = findViewById(R.id.tv_gps_alt);
        tvGpsStatus = findViewById(R.id.tv_gps_status);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        pressureSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) : null;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (pressureSensor == null) {
            tvPressure.setText("气压传感器不可用");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pressureSensor != null) {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        requestGps();
    }

    private void requestGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return;
        }
        startGps();
    }

    @SuppressWarnings("MissingPermission")
    private void startGps() {
        if (locationManager == null) return;
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps && !net) {
            tvGpsStatus.setText("请开启定位");
            return;
        }
        tvGpsStatus.setText("定位中…");
        if (gps) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        if (net) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            float p = event.values[0];
            float alt = SensorManager.getAltitude(SEA_LEVEL_PRESSURE, p);
            tvPressure.setText(String.format(Locale.CHINA, "气压：%.1f hPa", p));
            tvPressureAlt.setText(String.format(Locale.CHINA, "%.0f 米", alt));
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        tvGpsAlt.setText(String.format(Locale.CHINA, "%.0f 米", location.getAltitude()));
        tvGpsStatus.setText("定位成功（精度约 " + (int) location.getAccuracy() + " 米）");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGps();
        } else {
            tvGpsStatus.setText("未授权定位，仅显示气压海拔");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (locationManager != null) locationManager.removeUpdates(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
