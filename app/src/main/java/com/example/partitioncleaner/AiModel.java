/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import java.util.ArrayList;
import java.util.List;

/** 支持的在线模型清单（均兼容 OpenAI Chat Completions 接口）。 */
public class AiModel {

    public final String id;
    public final String name;
    public final String baseUrl;
    public final String modelId;

    public AiModel(String id, String name, String baseUrl, String modelId) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.modelId = modelId;
    }

    /** 默认模型清单（baseUrl / modelId 均可在 App 内覆盖）。 */
    public static List<AiModel> defaults() {
        List<AiModel> list = new ArrayList<>();
        list.add(new AiModel("hunyuan", "腾讯混元",
                "https://api.hunyuan.cloud.tencent.com/v1", "hunyuan-lite"));
        list.add(new AiModel("deepseek", "DeepSeek",
                "https://api.deepseek.com/v1", "deepseek-chat"));
        list.add(new AiModel("kimi", "Kimi (Moonshot)",
                "https://api.moonshot.cn/v1", "moonshot-v1-8k"));
        list.add(new AiModel("qwen", "通义千问",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"));
        list.add(new AiModel("doubao", "豆包 (火山方舟)",
                "https://ark.cn-beijing.volces.com/api/v3", "doubao-pro-32k"));
        return list;
    }
}
