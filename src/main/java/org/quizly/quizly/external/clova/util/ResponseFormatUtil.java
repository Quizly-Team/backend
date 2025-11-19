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

    public static ResponseFormat createSelectionQuizResponseFormat(int quizCount) {
        OptionsProperty selectionOptions = buildOptions("문제 선택지 목록 - 4개 선택지 존재", 4);
        return createMockExamResponseFormat(quizCount, quizCount, selectionOptions);
    }

    public static ResponseFormat createDescriptiveQuizResponseFormat(int quizCount) {
        OptionsProperty descriptiveOptions = buildOptions("문제 선택지 목록 - 빈 배열",0);
        return createMockExamResponseFormat(quizCount, quizCount, descriptiveOptions);
    }

    private static OptionsProperty buildOptions(String description, int OptionCount) {
        return new OptionsProperty(
            "array",
            description,
            new ItemsInfo("string"),
            OptionCount,
            OptionCount
        );
    }


    private static ResponseFormat createMockExamResponseFormat(int minItems, int maxItems, OptionsProperty optionsProperty) {
        Properties properties = new Properties(
            new Property("string", "문제 내용"),
            new TypeProperty(
                "string",
                "문제의 유형",
                List.of("FIND_CORRECT", "FIND_INCORRECT", "FIND_MATCH", "ESSAY", "SHORT_ANSWER", "TRUE_FALSE", "MULTIPLE_CHOICE")
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