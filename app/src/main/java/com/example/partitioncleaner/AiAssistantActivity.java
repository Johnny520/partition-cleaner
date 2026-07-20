/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AiAssistantActivity extends BaseActivity {

    private static final String PREF_AI = "ai_keys";

    private RecyclerView rv;
    private EditText et;
    private ChatAdapter adapter;
    private SwitchMaterial swOnline;
    private TextView tvMode;
    private Spinner spModel;
    private boolean online = false;
    private final List<AiModel> models = AiModel.defaults();
    private AiModel currentModel = models.get(0);
    private android.content.SharedPreferences prefs;
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

        prefs = getSharedPreferences(PREF_AI, MODE_PRIVATE);

        swOnline = findViewById(R.id.sw_online);
        tvMode = findViewById(R.id.tv_mode);
        swOnline.setOnCheckedChangeListener((button, isChecked) -> {
            online = isChecked;
            tvMode.setText(isChecked ? R.string.ai_mode_online : R.string.ai_mode_offline);
        });

        spModel = findViewById(R.id.sp_model);
        List<String> names = new ArrayList<>();
        for (AiModel m : models) names.add(m.name);
        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spModel.setAdapter(spAdapter);
        spModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentModel = models.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.btn_key).setOnClickListener(v -> openKeyDialog());

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
            if (online) {
                askOnline(text, thinkIndex);
            } else {
                String reply = brain.answer(text);
                messages.set(thinkIndex, new ChatMessage(false, reply));
                adapter.notifyItemChanged(thinkIndex);
                scrollBottom();
            }
        }, online ? 700 : 450);
    }

    /** 调用在线模型；未配置 Key 或异常时回退到本地离线大脑。 */
    private void askOnline(String userText, int thinkIndex) {
        String key = prefs.getString("ai_key_" + currentModel.id, "");
        String base = prefs.getString("ai_base_" + currentModel.id, currentModel.baseUrl);

        if (key == null || key.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.ai_no_key, currentModel.name), Toast.LENGTH_LONG).show();
            String reply = brain.answer(userText);
            messages.set(thinkIndex, new ChatMessage(false, reply));
            adapter.notifyItemChanged(thinkIndex);
            scrollBottom();
            return;
        }

        List<AiClient.Msg> history = new ArrayList<>();
        history.add(new AiClient.Msg("system", getString(R.string.ai_system_prompt)));
        for (int i = 0; i < messages.size(); i++) {
            if (i == thinkIndex) break;
            ChatMessage m = messages.get(i);
            history.add(new AiClient.Msg(m.isUser ? "user" : "assistant", m.text));
        }
        history.add(new AiClient.Msg("user", userText));

        new Thread(() -> {
            try {
                final String reply = AiClient.chat(base, key, currentModel.modelId, history);
                runOnUiThread(() -> {
                    messages.set(thinkIndex, new ChatMessage(false, reply != null && !reply.isEmpty()
                            ? reply : brain.answer(userText)));
                    adapter.notifyItemChanged(thinkIndex);
                    scrollBottom();
                });
            } catch (final Exception e) {
                final String msg = "调用 " + currentModel.name + " 失败：" + e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    messages.set(thinkIndex,
                            new ChatMessage(false, getString(R.string.ai_fallback) + brain.answer(userText)));
                    adapter.notifyItemChanged(thinkIndex);
                    scrollBottom();
                });
            }
        }).start();
    }

    /** 弹出对话框填写当前模型的 API Key（及可选的接口地址）。 */
    private void openKeyDialog() {
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_api_key, null);
        TextInputEditText etKey = dialogView.findViewById(R.id.et_api_key);
        TextInputEditText etBase = dialogView.findViewById(R.id.et_api_base);
        etKey.setText(prefs.getString("ai_key_" + currentModel.id, ""));
        etBase.setText(prefs.getString("ai_base_" + currentModel.id, currentModel.baseUrl));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ai_key_title, currentModel.name))
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String k = etKey.getText() == null ? "" : etKey.getText().toString().trim();
                    String b = etBase.getText() == null ? "" : etBase.getText().toString().trim();
                    prefs.edit()
                            .putString("ai_key_" + currentModel.id, k)
                            .putString("ai_base_" + currentModel.id,
                                    b.isEmpty() ? currentModel.baseUrl : b)
                            .apply();
                    Toast.makeText(this, R.string.ai_key_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
