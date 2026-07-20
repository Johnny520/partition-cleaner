package com.example.partitioncleaner;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class UnitConverterActivity extends BaseActivity {

    private static final int CAT_LENGTH = 0;
    private static final int CAT_WEIGHT = 1;
    private static final int CAT_TEMP = 2;
    private static final int CAT_DATA = 3;

    private final String[][] UNITS = {
            {"千米(km)", "米(m)", "厘米(cm)", "毫米(mm)", "英里", "英尺", "英寸", "码"},
            {"吨", "千克(kg)", "克(g)", "毫克(mg)", "磅", "盎司"},
            {"摄氏度(°C)", "华氏度(°F)", "开尔文(K)"},
            {"B", "KB", "MB", "GB", "TB"}
    };
    private final double[][] FACTORS = {
            {1000, 1, 0.01, 0.001, 1609.344, 0.3048, 0.0254, 0.9144},
            {1000, 1, 0.001, 1e-6, 0.453592, 0.0283495},
            {1, 1, 1},
            {1, 1024, 1048576, 1073741824, 1099511627776.0}
    };

    private int currentCat = CAT_LENGTH;
    private TextInputEditText etValue;
    private Spinner spinnerFrom;
    private Spinner spinnerTo;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_converter);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("单位换算");

        etValue = findViewById(R.id.et_value);
        spinnerFrom = findViewById(R.id.spinner_from);
        spinnerTo = findViewById(R.id.spinner_to);
        tvResult = findViewById(R.id.tv_result);

        MaterialButtonToggleGroup group = findViewById(R.id.group_category);
        if (group.findViewById(R.id.btn_cat_len) != null) {
            group.check(R.id.btn_cat_len);
        }
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int cat;
            if (checkedId == R.id.btn_cat_len) {
                cat = CAT_LENGTH;
            } else if (checkedId == R.id.btn_cat_weight) {
                cat = CAT_WEIGHT;
            } else if (checkedId == R.id.btn_cat_temp) {
                cat = CAT_TEMP;
            } else {
                cat = CAT_DATA;
            }
            switchCategory(cat);
        });

        TextWatcher tw = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
                convert();
            }

            public void afterTextChanged(Editable s) {
            }
        };
        etValue.addTextChangedListener(tw);

        AdapterView.OnItemSelectedListener sel = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                convert();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerFrom.setOnItemSelectedListener(sel);
        spinnerTo.setOnItemSelectedListener(sel);

        findViewById(R.id.btn_convert).setOnClickListener(v -> convert());

        switchCategory(CAT_LENGTH);
    }

    private void switchCategory(int cat) {
        currentCat = cat;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, UNITS[cat]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        if (UNITS[cat].length > 1) {
            spinnerFrom.setSelection(0);
            spinnerTo.setSelection(1);
        } else {
            spinnerFrom.setSelection(0);
            spinnerTo.setSelection(0);
        }
        tvResult.setText("");
        convert();
    }

    private void convert() {
        String raw = etValue.getText() == null ? "" : etValue.getText().toString().trim();
        if (raw.isEmpty()) {
            tvResult.setText("");
            return;
        }
        double v;
        try {
            v = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            tvResult.setText("请输入数字");
            return;
        }
        int from = spinnerFrom.getSelectedItemPosition();
        int to = spinnerTo.getSelectedItemPosition();
        if (from < 0 || to < 0) {
            return;
        }
        double res;
        if (currentCat == CAT_TEMP) {
            res = tempConvert(v, from, to);
        } else {
            res = v * FACTORS[currentCat][from] / FACTORS[currentCat][to];
        }
        tvResult.setText(format(res));
    }

    private double tempConvert(double v, int from, int to) {
        double c;
        if (from == 0) {
            c = v;
        } else if (from == 1) {
            c = (v - 32) * 5.0 / 9.0;
        } else {
            c = v - 273.15;
        }
        if (to == 0) {
            return c;
        } else if (to == 1) {
            return c * 9.0 / 5.0 + 32.0;
        } else {
            return c + 273.15;
        }
    }

    private String format(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "错误";
        }
        String s = String.format(Locale.getDefault(), "%.6f", d);
        s = s.replaceAll("0*$", "").replaceAll("\\.$", "");
        return s;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
