package com.example.partitioncleaner;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SourceActivity extends BaseActivity {

    private static final String REPO_URL = "https://github.com/Johnny520/partition-cleaner";
    private static final String RELEASES_URL = "https://github.com/Johnny520/partition-cleaner/releases";
    private static final String JDK_URL = "https://adoptium.net/temurin17";
    private static final String GRADLE_URL = "https://gradle.org/releases/";
    private static final String SDK_URL = "https://developer.android.com/studio";
    private static final String CLONE_CMD = "git clone https://github.com/Johnny520/partition-cleaner.git";

    static class SrcItem {
        String emoji;
        String title;
        String sub;
        Runnable action;

        SrcItem(String emoji, String title, String sub, Runnable action) {
            this.emoji = emoji;
            this.title = title;
            this.sub = sub;
            this.action = action;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.source_title);

        List<SrcItem> items = new ArrayList<>();
        items.add(new SrcItem("📦", getString(R.string.src_zip), getString(R.string.src_zip_sub),
                () -> openUrl(REPO_URL + "/archive/refs/heads/main.zip")));
        items.add(new SrcItem("🔗", getString(R.string.src_repo), getString(R.string.src_repo_sub),
                () -> openUrl(REPO_URL)));
        items.add(new SrcItem("📥", getString(R.string.src_release), getString(R.string.src_release_sub),
                () -> openUrl(RELEASES_URL)));
        items.add(new SrcItem("☕", getString(R.string.src_jdk), getString(R.string.src_jdk_sub),
                () -> openUrl(JDK_URL)));
        items.add(new SrcItem("⚙️", getString(R.string.src_gradle), getString(R.string.src_gradle_sub),
                () -> openUrl(GRADLE_URL)));
        items.add(new SrcItem("🤖", getString(R.string.src_sdk), getString(R.string.src_sdk_sub),
                () -> openUrl(SDK_URL)));
        items.add(new SrcItem("📋", getString(R.string.src_clone), getString(R.string.src_clone_sub),
                this::copyClone));
        items.add(new SrcItem("🛠️", getString(R.string.src_build), getString(R.string.src_build_sub),
                this::showBuild));

        RecyclerView rv = findViewById(R.id.rv_source);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SrcAdapter(items));
    }

    private void copyClone() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("clone", CLONE_CMD));
            }
            Toast.makeText(this, R.string.src_copied, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, CLONE_CMD, Toast.LENGTH_LONG).show();
        }
    }

    private void showBuild() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.src_build)
                .setMessage(R.string.src_build_info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开：" + url, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class SrcAdapter extends RecyclerView.Adapter<SrcAdapter.VH> {
        private final List<SrcItem> list;

        SrcAdapter(List<SrcItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_source, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SrcItem it = list.get(position);
            holder.emoji.setText(it.emoji);
            holder.title.setText(it.title);
            holder.sub.setText(it.sub);
            holder.itemView.setOnClickListener(v -> it.action.run());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView emoji;
            TextView title;
            TextView sub;

            VH(View v) {
                super(v);
                emoji = v.findViewById(R.id.tv_src_emoji);
                title = v.findViewById(R.id.tv_src_title);
                sub = v.findViewById(R.id.tv_src_sub);
            }
        }
    }
}
