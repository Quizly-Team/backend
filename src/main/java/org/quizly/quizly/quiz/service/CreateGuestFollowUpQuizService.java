package org.quizly.quizly.quiz.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.core.domain.repository.QuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiResponse;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.PreviousQuizContext;
import org.quizly.quizly.external.openai.util.OpenAiResponseFormatUtil;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.quizly.quizly.quiz.service.CreateGuestFollowUpQuizService.CreateGuestFollowUpQuizRequest;
import org.quizly.quizly.quiz.service.CreateGuestFollowUpQuizService.CreateGuestFollowUpQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CreateGuestFollowUpQuizService implements
    BaseService<CreateGuestFollowUpQuizRequest, CreateGuestFollowUpQuizResponse> {

    private final QuizRepository quizRepository;
    private final CreateFollowUpQuizOpenAiService createFollowUpQuizOpenAiService;

    @Override
    public CreateGuestFollowUpQuizResponse execute(CreateGuestFollowUpQuizRequest request) {
        if (request == null || !request.isValid()) {
            return CreateGuestFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateGuestFollowUpQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        Optional<Quiz> optionalQuiz = quizRepository.findById(request.getQuizId());
        if (optionalQuiz.isEmpty()) {
            return CreateGuestFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateGuestFollowUpQuizErrorCode.QUIZ_NOT_FOUND)
                .build();
        }
        Quiz quiz = optionalQuiz.get();

        if (!Boolean.TRUE.equals(quiz.getGuest())) {
            log.warn("[CreateGuestFollowUpQuizService] Not a guest quiz. quizId: {}", quiz.getId());
            return CreateGuestFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateGuestFollowUpQuizErrorCode.NOT_A_GUEST_QUIZ)
                .build();
        }

        if (quiz.getSourceText() == null || quiz.getSourceText().isBlank()) {
            return CreateGuestFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateGuestFollowUpQuizErrorCode.SOURCE_TEXT_NOT_FOUND)
                .build();
        }

        List<PreviousQuizContext> chain = buildChain(quiz);
        ResponseFormat responseFormat = createResponseFormat(quiz.getQuizType());

        CreateFollowUpQuizOpenAiResponse openAiResponse = createFollowUpQuizOpenAiService.execute(
            CreateFollowUpQuizOpenAiRequest.builder()
                .sourceText(quiz.getSourceText())
                .previousQuizzes(chain)
                .quizType(quiz.getQuizType())
                .responseFormat(responseFormat)
                .build());

        if (openAiResponse == null || !openAiResponse.isSuccess()) {
            log.warn("[CreateGuestFollowUpQuizService] Failed to create follow-up quiz from OpenAi.");
            return CreateGuestFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateGuestFollowUpQuizErrorCode.OPENAI_QUIZ_GENERATION_FAILED)
                .build();
        }

        Quiz followUpQuiz = saveFollowUpQuiz(openAiResponse.getGeneratedQuiz(), quiz);

        return CreateGuestFollowUpQuizResponse.builder()
            .quiz(followUpQuiz)
            .build();
    }

    private Quiz saveFollowUpQuiz(GeneratedQuizResponse generatedQuiz, Quiz parentQuiz) {
        Quiz followUpQuiz = Quiz.builder()
            .quizText(generatedQuiz.getQuiz())
            .answer(generatedQuiz.getAnswer())
            .quizType(generatedQuiz.getType())
            .explanation(generatedQuiz.getExplanation())
            .options(generatedQuiz.getOptions())
            .topic(parentQuiz.getTopic())
            .sourceText(parentQuiz.getSourceText())
            .parentQuiz(parentQuiz)
            .user(null)
            .guest(true)
            .build();

        return quizRepository.save(followUpQuiz);
    }

    private List<PreviousQuizContext> buildChain(Quiz quiz) {
        LinkedList<PreviousQuizContext> chain = new LinkedList<>();
        chain.addFirst(new PreviousQuizContext(quiz.getQuizText(), quiz.getAnswer(),
            quiz.getOptions(), quiz.getExplanation()));
        for (Quiz current = quiz.getParentQuiz(); current != null;
            current = current.getParentQuiz()) {
            chain.addFirst(PreviousQuizContext.questionOnly(current.getQuizText()));
        }
        return chain;
    }

    private ResponseFormat createResponseFormat(Quiz.QuizType quizType) {
        String quizTypeName = quizType.name();
        if (quizType == Quiz.QuizType.TRUE_FALSE) {
            return OpenAiResponseFormatUtil.createDescriptiveQuizResponseFormat(1, quizTypeName);
        }
        return OpenAiResponseFormatUtil.createSelectionQuizResponseFormat(1, quizTypeName);
    }

    @Getter
    @RequiredArgsConstructor
    public enum CreateGuestFollowUpQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
        NOT_A_GUEST_QUIZ(HttpStatus.FORBIDDEN, "게스트 퀴즈가 아니면 꼬리 문제를 생성할 수 없습니다."),
        SOURCE_TEXT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY,
            "원본 텍스트가 없는 퀴즈라 꼬리 문제를 생성할 수 없습니다."),
        OPENAI_QUIZ_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "OPENAI 서버에서 꼬리 문제 생성에 실패하였습니다.");

        private final HttpStatus httpStatus;

        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateGuestFollowUpQuizRequest implements BaseRequest {

        private Long quizId;

        @Override
        public boolean isValid() {
            return quizId != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateGuestFollowUpQuizResponse extends
        BaseResponse<CreateGuestFollowUpQuizErrorCode> {

        private Quiz quiz;
    }
}
