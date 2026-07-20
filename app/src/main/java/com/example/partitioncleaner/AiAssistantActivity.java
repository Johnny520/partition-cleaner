package com.example.partitioncleaner;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class AiAssistantActivity extends BaseActivity {

    private RecyclerView rv;
    private EditText et;
    private ChatAdapter adapter;
    private SwitchMaterial swOnline;
    private TextView tvMode;
    private boolean online = false;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final HunyuanBrain brain = new HunyuanBrain();
    private final Handler handler = new Handler(Looper.getMainLooper());

    static class ChatMessage {
        final boolean isUser;
        final String text;
        ChatMessage(boolean isUser, String text) {
            this.isUser = isUser;
            this.text = text;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.feat_ai);
        toolbar.setSubtitle(R.string.ai_subtitle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rv = findViewById(R.id.rv_ai);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        rv.setAdapter(adapter);

        swOnline = findViewById(R.id.sw_online);
        tvMode = findViewById(R.id.tv_mode);
        swOnline.setOnCheckedChangeListener((button, isChecked) -> {
            online = isChecked;
            tvMode.setText(isChecked ? R.string.ai_mode_online : R.string.ai_mode_offline);
        });

        et = findViewById(R.id.et_ai);
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_ai_send).setOnClickListener(v -> send());

        setupChips();

        messages.add(new ChatMessage(false, getString(R.string.ai_welcome)));
        adapter.notifyItemInserted(0);
    }

    private void setupChips() {
        com.google.android.material.chip.ChipGroup group = findViewById(R.id.chip_group);
        int[] labels = {R.string.ai_q_clean, R.string.ai_q_root, R.string.ai_q_device,
                R.string.ai_q_calc, R.string.ai_q_tool, R.string.ai_q_theme};
        for (int id : labels) {
            Chip chip = new Chip(this);
            chip.setText(id);
            chip.setCheckable(false);
            chip.setClickable(true);
            final String text = getString(id);
            chip.setOnClickListener(v -> {
                et.setText(text);
                et.setSelection(et.getText().length());
                send();
            });
            group.addView(chip);
        }
    }

    private void send() {
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return;
        messages.add(new ChatMessage(true, text));
        adapter.notifyItemInserted(messages.size() - 1);
        et.setText("");
        scrollBottom();

        // 模拟思考
        messages.add(new ChatMessage(false, online ? getString(R.string.ai_online_thinking) : getString(R.string.ai_think)));
        int thinkIndex = messages.size() - 1;
        adapter.notifyItemInserted(thinkIndex);
        scrollBottom();

        handler.postDelayed(() -> {
            String reply;
            if (online) {
                // 在线模式：当前未配置云端接口，回退到本地离线应答并提示
                Toast.makeText(this, R.string.ai_online_unavailable, Toast.LENGTH_SHORT).show();
                reply = brain.answer(text);
            } else {
                reply = brain.answer(text);
            }
            messages.set(thinkIndex, new ChatMessage(false, reply));
            adapter.notifyItemChanged(thinkIndex);
            scrollBottom();
        }, online ? 700 : 450);
    }

    private void scrollBottom() {
        rv.post(() -> rv.scrollToPosition(messages.size() - 1));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        private static final int USER = 0, AI = 1;

        @Override
        public int getItemViewType(int pos) {
            return messages.get(pos).isUser ? USER : AI;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ChatMessage m = messages.get(pos);
            h.bubble.setText(m.text);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) h.bubble.getLayoutParams();
            if (m.isUser) {
                ((LinearLayout) h.itemView).setGravity(Gravity.END);
                h.bubble.setBackgroundResource(R.drawable.bubble_user);
            } else {
                ((LinearLayout) h.itemView).setGravity(Gravity.START);
                h.bubble.setBackgroundResource(R.drawable.bubble_ai);
            }
            h.bubble.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() { return messages.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView bubble;
            VH(View v) {
                super(v);
                bubble = v.findViewById(R.id.tv_bubble);
            }
        }
    }
}
