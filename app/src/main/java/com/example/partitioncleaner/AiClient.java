package com.example.partitioncleaner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** OpenAI 兼容的 Chat Completions 客户端（非流式，使用系统自带 org.json）。 */
public class AiClient {

    public static class Msg {
        public final String role;
        public final String content;

        public Msg(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /**
     * 调用远程模型。失败抛出异常，由调用方决定回退策略。
     *
     * @param baseUrl 形如 https://xxx/v1 （无需带 /chat/completions）
     * @param apiKey  用户自行配置的 API Key
     * @param modelId 模型标识
     * @param history 完整对话历史（含 system / user / assistant）
     */
    public static String chat(String baseUrl, String apiKey, String modelId, List<Msg> history)
            throws Exception {
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        JSONObject body = new JSONObject();
        body.put("model", modelId);
        body.put("stream", false);
        JSONArray msgs = new JSONArray();
        for (Msg m : history) {
            JSONObject o = new JSONObject();
            o.put("role", m.role);
            o.put("content", m.content);
            msgs.put(o);
        }
        body.put("messages", msgs);

        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.close();

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code + ": " + sb);
        }
        JSONObject resp = new JSONObject(sb.toString());
        JSONArray choices = resp.getJSONArray("choices");
        if (choices.length() == 0) throw new Exception("empty choices");
        return choices.getJSONObject(0).getJSONObject("message").getString("content");
    }
}
