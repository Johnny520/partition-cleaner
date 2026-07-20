package com.example.partitioncleaner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

/**
 * 隐私政策 / 用户协议展示页。通过 {@link #open(Context, int)} 传入模式跳转。
 */
public class PolicyActivity extends BaseActivity {

    public static final int MODE_PRIVACY = 0;
    public static final int MODE_AGREEMENT = 1;

    private static final String EXTRA_MODE = "mode";

    public static void open(Context context, int mode) {
        Intent i = new Intent(context, PolicyActivity.class);
        i.putExtra(EXTRA_MODE, mode);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tv = findViewById(R.id.tv_policy);
        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_PRIVACY);
        if (mode == MODE_AGREEMENT) {
            setTitle(R.string.policy_agreement_title);
            tv.setText(R.string.policy_agreement_text);
        } else {
            setTitle(R.string.policy_privacy_title);
            tv.setText(R.string.policy_privacy_text);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
