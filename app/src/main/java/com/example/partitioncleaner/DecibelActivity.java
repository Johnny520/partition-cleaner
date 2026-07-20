package com.example.partitioncleaner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

public class DecibelActivity extends BaseActivity {

    private static final int REQ_AUDIO = 3001;
    private static final int SAMPLE_RATE = 44100;

    private TextView tvDb;
    private LinearProgressIndicator pbLevel;
    private Button btnToggle;

    private AudioRecord record;
    private Thread thread;
    private volatile boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decibel);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("分贝仪");

        tvDb = findViewById(R.id.tv_db);
        pbLevel = findViewById(R.id.pb_level);
        btnToggle = findViewById(R.id.btn_toggle);

        btnToggle.setOnClickListener(v -> {
            if (running) stopMeasure();
            else startMeasure();
        });
    }

    private void startMeasure() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        beginRecord();
    }

    private void beginRecord() {
        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuf <= 0) {
            Toast.makeText(this, "不支持音频采集", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2);
        } catch (SecurityException e) {
            Toast.makeText(this, "无录音权限", Toast.LENGTH_SHORT).show();
            return;
        }
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "录音初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }
        running = true;
        btnToggle.setText("停止测量");
        record.startRecording();
        final int bufLen = minBuf;
        thread = new Thread(() -> {
            short[] buf = new short[bufLen];
            while (running) {
                int read = record.read(buf, 0, buf.length);
                if (read <= 0) continue;
                long sum = 0;
                for (int i = 0; i < read; i++) sum += (long) buf[i] * buf[i];
                double rms = Math.sqrt(sum / (double) read);
                double dbFs = 20 * Math.log10(rms / 32767.0 + 1e-9);
                int dbSpl = (int) Math.max(0, Math.min(120, dbFs + 90));
                runOnUiThread(() -> {
                    tvDb.setText(dbSpl + " dB");
                    pbLevel.setProgress(dbSpl);
                });
                try {
                    Thread.sleep(120);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        });
        thread.start();
    }

    private void stopMeasure() {
        running = false;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
        if (record != null) {
            try {
                record.stop();
                record.release();
            } catch (Exception ignored) {
            }
            record = null;
        }
        btnToggle.setText("开始测量");
        tvDb.setText("-- dB");
        pbLevel.setProgress(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            beginRecord();
        } else {
            Toast.makeText(this, "需要录音权限才能测量分贝", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (running) stopMeasure();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
