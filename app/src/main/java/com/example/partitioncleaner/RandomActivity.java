package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Random;

public class RandomActivity extends BaseActivity {

    private TextInputEditText etMin;
    private TextInputEditText etMax;
    private TextInputEditText etCount;
    private TextView tvResult;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("随机数");

        etMin = findViewById(R.id.et_min);
        etMax = findViewById(R.id.et_max);
        etCount = findViewById(R.id.et_count);
        tvResult = findViewById(R.id.tv_result);

        findViewById(R.id.btn_generate).setOnClickListener(v -> generate());
    }

    private String textOf(TextInputEditText et) {
        CharSequence c = et.getText();
        return c == null ? "" : c.toString().trim();
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void generate() {
        String minS = textOf(etMin);
        String maxS = textOf(etMax);

        if (minS.isEmpty() || maxS.isEmpty()) {
            Toast.makeText(this, "请输入最小值和最大值", Toast.LENGTH_SHORT).show();
            return;
        }

        int min;
        int max;
        try {
            min = Integer.parseInt(minS);
            max = Integer.parseInt(maxS);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = parseIntSafe(textOf(etCount), 1);

        if (min > max) {
            Toast.makeText(this, "最小值不能大于最大值", Toast.LENGTH_SHORT).show();
            return;
        }
        if (count < 1) {
            count = 1;
        }
        if (count > 200) {
            count = 200;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int v = random.nextInt(max - min + 1) + min;
            sb.append(v);
            if (i != count - 1) {
                sb.append("\n");
            }
        }
        tvResult.setText(sb.toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
