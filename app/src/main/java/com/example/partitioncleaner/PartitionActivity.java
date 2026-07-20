/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PartitionActivity extends BaseActivity {

    private RecyclerView rv;
    private TextView summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partition);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.feat_partition);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rv = findViewById(R.id.rv_partition);
        rv.setLayoutManager(new LinearLayoutManager(this));
        summary = findViewById(R.id.tv_partition_summary);

        load();
    }

    private void load() {
        List<PartitionInfo> all = new ArrayList<>();
        all.addAll(PartitionScanner.scanFilesystem());
        all.addAll(PartitionScanner.scanMemory());

        long totalUsed = 0, totalSize = 0;
        for (PartitionInfo p : all) {
            totalUsed += p.getUsed();
            totalSize += p.total;
        }
        summary.setText(getString(R.string.partition_summary,
                all.size(), Util.formatSize(totalUsed), Util.formatSize(totalSize)));

        rv.setAdapter(new PartitionAdapter(all));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
