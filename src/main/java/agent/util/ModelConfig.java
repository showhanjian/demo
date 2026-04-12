package agent.util;

import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.GenerateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型配置统一类
 * 根据 MODEL_TYPE 环境变量决定创建哪种模型
 */
public class ModelConfig {

    private static final Logger logger = LoggerFactory.getLogger(ModelConfig.class);

    public enum ModelType {
        DEEPSEEK("deepseek"),
        MINMAX("minmax");

        private final String value;

        ModelType(String value) {
            this.value = value;
        }

        public static ModelType fromEnv(String envValue) {
            if (envValue == null) {
                return DEEPSEEK;
            }
            for (ModelType type : values()) {
                if (type.value.equalsIgnoreCase(envValue.trim())) {
                    return type;
                }
            }
            return DEEPSEEK;
        }
    }

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final ModelType modelType;

    private ModelConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.modelName = builder.modelName;
        this.modelType = builder.modelType;
        logger.info("[ModelConfig] 创建配置: type={}, modelName={}, baseUrl={}", modelType, modelName, baseUrl);
    }

    // Getters
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getModelName() { return modelName; }
    public ModelType getModelType() { return modelType; }

    /**
     * 创建模型实例（统一使用 OpenAI 格式）
     * MiniMax API 要求 user 参数一致，通过 additionalBodyParams 设置
     */
    public Model createModel() {
        logger.info("[ModelConfig] 创建模型, type={}, modelName={}", modelType, modelName);

        // MiniMax API 要求 user 参数一致，通过 additionalBodyParams 设置
        java.util.Map<String, Object> additionalBody = new java.util.HashMap<>();
        additionalBody.put("user", "user01");

        Model model = OpenAIChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .baseUrl(baseUrl)
            .formatter(new MergedSystemFormatter())
            .generateOptions(GenerateOptions.builder()
                .additionalBodyParams(additionalBody)
                .build())
            .build();

        logger.info("[ModelConfig] 模型创建成功");
        return model;
    }

    /**
     * 从环境变量构建 ModelConfig
     *
     * 环境变量:
     * - A_MODEL_TYPE: "deepseek" 或 "minmax" (默认 deepseek)
     *
     * DeepSeek 环境变量:
     * - A_DEEPSEEK_AUTH_TOKEN
     * - A_DEEPSEEK_BASE_URL
     * - A_DEEPSEEK_MODEL
     *
     * MiniMax 环境变量:
     * - A_MINMAX_AUTH_TOKEN
     * - A_MINMAX_BASE_URL
     * - A_MINMAX_MODEL
     */
    public static ModelConfig fromEnv() {
        String modelTypeStr = System.getenv("A_MODEL_TYPE");
        ModelType type = ModelType.fromEnv(modelTypeStr);

        String apiKey;
        String baseUrl;
        String modelName;

        if (type == ModelType.MINMAX) {
            apiKey = System.getenv("A_MINMAX_AUTH_TOKEN");
            baseUrl = System.getenv("A_MINMAX_BASE_URL");
            modelName = System.getenv("A_MINMAX_MODEL");
        } else {
            apiKey = System.getenv("A_DEEPSEEK_AUTH_TOKEN");
            baseUrl = System.getenv("A_DEEPSEEK_BASE_URL");
            modelName = System.getenv("A_DEEPSEEK_MODEL");
        }

        logger.info("[ModelConfig] 从环境变量构建, A_MODEL_TYPE={}", modelTypeStr);

        return builder()
            .modelType(type)
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private ModelType modelType = ModelType.DEEPSEEK;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder modelType(ModelType modelType) {
            this.modelType = modelType;
            return this;
        }

        public ModelConfig build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey cannot be null or empty");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("baseUrl cannot be null or empty");
            }
            if (modelName == null || modelName.isEmpty()) {
                throw new IllegalArgumentException("modelName cannot be null or empty");
            }
            return new ModelConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ModelConfig{" +
            "modelType=" + modelType +
            ", modelName='" + modelName + '\'' +
            ", baseUrl='" + baseUrl + '\'' +
            '}';
    }
}
