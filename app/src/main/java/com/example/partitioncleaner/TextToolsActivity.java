package com.example.partitioncleaner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class TextToolsActivity extends AppCompatActivity {

    private TextInputEditText etInput;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_tools);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("文本工具");

        etInput = findViewById(R.id.et_input);
        tvOutput = findViewById(R.id.tv_output);

        findViewById(R.id.btn_upper).setOnClickListener(v -> upper());
        findViewById(R.id.btn_lower).setOnClickListener(v -> lower());
        findViewById(R.id.btn_reverse).setOnClickListener(v -> reverse());
        findViewById(R.id.btn_trim).setOnClickListener(v -> trim());
        findViewById(R.id.btn_count).setOnClickListener(v -> count());
        findViewById(R.id.btn_b64enc).setOnClickListener(v -> b64Encode());
        findViewById(R.id.btn_b64dec).setOnClickListener(v -> b64Decode());
        findViewById(R.id.btn_md5).setOnClickListener(v -> md5());
        findViewById(R.id.btn_copy).setOnClickListener(v -> copy());
    }

    private String input() {
        CharSequence c = etInput.getText();
        return c == null ? "" : c.toString();
    }

    private void setOut(String s) {
        if (tvOutput != null) {
            tvOutput.setText(s);
        }
    }

    private void upper() {
        setOut(input().toUpperCase(Locale.getDefault()));
    }

    private void lower() {
        setOut(input().toLowerCase(Locale.getDefault()));
    }

    private void reverse() {
        setOut(new StringBuilder(input()).reverse().toString());
    }

    private void trim() {
        setOut(input().replaceAll("\\s+", ""));
    }

    private void count() {
        String s = input();
        int chars = s.length();
        int lines = s.isEmpty() ? 0 : s.split("\n", -1).length;
        setOut("字符数：" + chars + "，行数：" + lines);
    }

    private void b64Encode() {
        try {
            byte[] data = input().getBytes("UTF-8");
            setOut(Base64.encodeToString(data, Base64.NO_WRAP));
        } catch (UnsupportedEncodingException e) {
            setOut("编码失败");
        }
    }

    private void b64Decode() {
        try {
            byte[] data = Base64.decode(input().trim(), Base64.NO_WRAP);
            setOut(new String(data, "UTF-8"));
        } catch (Exception e) {
            setOut("解码失败");
        }
    }

    private void md5() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(input().getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            setOut(sb.toString());
        } catch (NoSuchAlgorithmException e) {
            setOut("MD5 不可用");
        } catch (UnsupportedEncodingException e) {
            setOut("编码失败");
        }
    }

    private void copy() {
        CharSequence out = tvOutput.getText();
        if (out == null || out.length() == 0) {
            Toast.makeText(this, "没有可复制的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("text", out.toString()));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
