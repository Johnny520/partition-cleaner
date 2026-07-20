package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimestampActivity extends BaseActivity {

    private TextInputEditText etTs;
    private TextInputEditText etTime;
    private TextView tvTsResult;
    private TextView tvTimeResult;
    private TextView tvNowResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timestamp);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("时间戳");

        etTs = findViewById(R.id.et_ts);
        etTime = findViewById(R.id.et_time);
        tvTsResult = findViewById(R.id.tv_ts_result);
        tvTimeResult = findViewById(R.id.tv_time_result);
        tvNowResult = findViewById(R.id.tv_now_result);

        findViewById(R.id.btn_ts_to_time).setOnClickListener(v -> tsToTime());
        findViewById(R.id.btn_time_to_ts).setOnClickListener(v -> timeToTs());
        findViewById(R.id.btn_now).setOnClickListener(v -> nowTs());
    }

    private void tsToTime() {
        CharSequence c = etTs.getText();
        if (c == null || c.toString().trim().isEmpty()) {
            Toast.makeText(this, "请输入时间戳", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            long ts = Long.parseLong(c.toString().trim());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            tvTsResult.setText(sdf.format(new Date(ts * 1000L)));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "格式错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void timeToTs() {
        CharSequence c = etTime.getText();
        if (c == null || c.toString().trim().isEmpty()) {
            Toast.makeText(this, "请输入时间", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date d = sdf.parse(c.toString().trim());
            if (d == null) {
                Toast.makeText(this, "格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
            tvTimeResult.setText(String.valueOf(d.getTime() / 1000L));
        } catch (ParseException e) {
            Toast.makeText(this, "格式错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void nowTs() {
        tvNowResult.setText(String.valueOf(System.currentTimeMillis() / 1000L));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
