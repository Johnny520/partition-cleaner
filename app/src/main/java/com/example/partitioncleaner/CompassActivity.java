package com.example.partitioncleaner;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CompassActivity extends BaseActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private float azimuth = 0f;

    private ImageView ivNeedle;
    private TextView tvDeg;
    private TextView tvDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("指南针");

        ivNeedle = findViewById(R.id.iv_needle);
        tvDeg = findViewById(R.id.tv_deg);
        tvDir = findViewById(R.id.tv_dir);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if (accelerometer == null || magnetometer == null) {
            Toast.makeText(this, "设备缺少方向传感器", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3);
        } else {
            return;
        }

        float[] r = new float[9];
        float[] i = new float[9];
        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(r, orientation);
            float newAzimuth = (float) Math.toDegrees(orientation[0]);
            if (newAzimuth < 0) newAzimuth += 360f;
            azimuth = newAzimuth;
            runOnUiThread(this::updateUi);
        }
    }

    private void updateUi() {
        tvDeg.setText(String.format(java.util.Locale.CHINA, "%.0f°", azimuth));
        tvDir.setText(toChineseDir(azimuth));
        ivNeedle.setRotation(-azimuth);
    }

    private static String toChineseDir(float az) {
        if (az >= 337.5 || az < 22.5) return "正北";
        if (az < 67.5) return "东北";
        if (az < 112.5) return "正东";
        if (az < 157.5) return "东南";
        if (az < 202.5) return "正南";
        if (az < 247.5) return "西南";
        if (az < 292.5) return "正西";
        return "西北";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
