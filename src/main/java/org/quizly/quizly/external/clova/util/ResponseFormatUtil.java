package org.quizly.quizly.external.clova.util;

import java.util.List;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ArraySchema;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.Items;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ObjectSchema;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.OptionsPropertyField;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.OptionsPropertyField.ItemsInfo;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.PropertyField;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.QuizProperties;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.TopicProperties;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.TypePropertyField;

public class ResponseFormatUtil {

    public static ResponseFormat createTopicResponseFormat() {
        TopicProperties properties = new TopicProperties(
            new PropertyField("string", "텍스트 내용 전체를 관통하는 핵심 주제 (5단어 이내)")
        );
        ObjectSchema schema = ObjectSchema.of(List.of("topic"), properties);
        return ResponseFormat.json(schema);
    }

    public static ResponseFormat createSelectionQuizResponseFormat(int quizCount) {
        OptionsPropertyField selectionOptions = buildOptions("문제 선택지 목록 - 4개 선택지 존재", 4);
        return createQuizResponseFormat(quizCount, quizCount, selectionOptions);
    }

    public static ResponseFormat createDescriptiveQuizResponseFormat(int quizCount) {
        OptionsPropertyField descriptiveOptions = buildOptions("문제 선택지 목록 - 빈 배열", 0);
        return createQuizResponseFormat(quizCount, quizCount, descriptiveOptions);
    }

    private static OptionsPropertyField buildOptions(String description, int optionCount) {
        return new OptionsPropertyField(
            "array",
            description,
            new ItemsInfo("string"),
            optionCount,
            optionCount
        );
    }

    private static ResponseFormat createQuizResponseFormat(int minItems, int maxItems, OptionsPropertyField optionsProperty) {
        QuizProperties properties = new QuizProperties(
            new PropertyField("string", "문제 내용"),
            new TypePropertyField(
                "string",
                "문제의 유형",
                List.of("FIND_CORRECT", "FIND_INCORRECT", "FIND_MATCH", "ESSAY", "SHORT_ANSWER", "TRUE_FALSE", "MULTIPLE_CHOICE")
            ),
            optionsProperty,
            new PropertyField("string", "퀴즈의 답안"),
            new PropertyField("string", "정답에 대한 해설")
        );

        Items items = Items.of(List.of("quiz", "type", "options", "answer", "explanation"), properties);
        ArraySchema schema = ArraySchema.of(items, minItems, maxItems);
        return ResponseFormat.json(schema);
    }

}