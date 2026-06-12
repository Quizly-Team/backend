package org.quizly.quizly.external.openai.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.JsonSchema;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;

public class OpenAiResponseFormatUtil {

    private static final int SELECTION_OPTION_COUNT = 4;
    private static final int DESCRIPTIVE_OPTION_COUNT = 0;

    public static ResponseFormat createSelectionQuizResponseFormat(int quizCount, String quizType) {
        return buildQuizResponseFormat(
            "selection_quiz_list",
            quizType,
            optionsArrayProperty("문제 선택지 - 정확히 4개의 선택지", SELECTION_OPTION_COUNT),
            quizCount
        );
    }

    public static ResponseFormat createDescriptiveQuizResponseFormat(int quizCount, String quizType) {
        return buildQuizResponseFormat(
            "descriptive_quiz_list",
            quizType,
            optionsArrayProperty("문제 선택지 목록 - 빈 배열", DESCRIPTIVE_OPTION_COUNT),
            quizCount
        );
    }

    public static ResponseFormat createTopicResponseFormat() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("topic", stringProperty("텍스트 내용 전체를 관통하는 핵심 주제 (5단어 이내)"));

        Map<String, Object> schema = objectSchema(properties, List.of("topic"));

        return ResponseFormat.jsonSchema(JsonSchema.of("topic_response", schema));
    }

    private static ResponseFormat buildQuizResponseFormat(
        String schemaName,
        String quizType,
        Map<String, Object> optionsProperty,
        int quizCount
    ) {
        Map<String, Object> quizItemProperties = new LinkedHashMap<>();
        quizItemProperties.put("quiz", stringProperty("문제 내용"));
        quizItemProperties.put("type", enumStringProperty("문제의 유형", List.of(quizType)));
        quizItemProperties.put("options", optionsProperty);
        quizItemProperties.put("answer", stringProperty("퀴즈의 답안"));
        quizItemProperties.put("explanation", stringProperty("정답에 대한 해설"));

        Map<String, Object> quizItemSchema = objectSchema(
            quizItemProperties,
            List.of("quiz", "type", "options", "answer", "explanation")
        );

        Map<String, Object> quizzesArray = new LinkedHashMap<>();
        quizzesArray.put("type", "array");
        quizzesArray.put("description", String.format("정확히 %d개의 문제를 생성한다.", quizCount));
        quizzesArray.put("items", quizItemSchema);
        quizzesArray.put("minItems", quizCount);
        quizzesArray.put("maxItems", quizCount);

        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("quizzes", quizzesArray);

        Map<String, Object> rootSchema = objectSchema(rootProperties, List.of("quizzes"));

        return ResponseFormat.jsonSchema(JsonSchema.of(schemaName, rootSchema));
    }

    private static Map<String, Object> optionsArrayProperty(String description, int optionCount) {
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("type", "string");

        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "array");
        property.put("description", description);
        property.put("items", items);
        property.put("minItems", optionCount);
        property.put("maxItems", optionCount);
        return property;
    }

    private static Map<String, Object> stringProperty(String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        property.put("description", description);
        return property;
    }

    private static Map<String, Object> enumStringProperty(String description, List<String> enumValues) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        property.put("description", description);
        property.put("enum", enumValues);
        return property;
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }
}
