package org.quizly.quizly.external.clova.util;

import java.util.List;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema.Items;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema.Items.Properties;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema.Items.Properties.OptionsProperty;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema.Items.Properties.OptionsProperty.ItemsInfo;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema.Items.Properties.Property;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat.Schema.Items.Properties.TypeProperty;

public class ResponseFormatUtil {

    public static ResponseFormat createSelectionMockExamResponseFormat(int minItems, int maxItems) {
        OptionsProperty selectionOptions = new OptionsProperty(
            "array",
            "문제 선택지 목록 - 4개 선택지 존재",
            new ItemsInfo("string"),
            4,
            4
        );
        return createMockExamResponseFormat(minItems, maxItems, selectionOptions);
    }

    public static ResponseFormat createDescriptiveMockExamResponseFormat(int minItems, int maxItems) {
        OptionsProperty descriptiveOptions = new OptionsProperty(
            "array",
            "문제 선택지 목록 - 빈 배열",
            new ItemsInfo("string"),
            0,
            0
        );
        return createMockExamResponseFormat(minItems, maxItems, descriptiveOptions);
    }

    private static ResponseFormat createMockExamResponseFormat(int minItems, int maxItems, OptionsProperty optionsProperty) {
        Properties properties = new Properties(
            new Property("string", "문제 내용"),
            new TypeProperty(
                "string",
                "문제의 유형",
                List.of("FIND_CORRECT", "FIND_INCORRECT", "FIND_MATCH", "ESSAY", "SHORT_ANSWER", "TRUE_FALSE")
            ),
            optionsProperty,
            new Property("string", "퀴즈의 답안"),
            new Property("string", "정답에 대한 해설")
        );

        Items items = new Items("object", List.of("quiz", "type", "options", "answer", "explanation"), properties);
        Schema schema = new Schema("array", items, minItems, maxItems);
        return new ResponseFormat("json", schema);
    }

}