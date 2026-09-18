package org.quizly.quizly.quiz.service;

import java.time.LocalDateTime;
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
import org.quizly.quizly.account.service.ReadUserService;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.core.domain.entity.SolveHistory;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.domain.repository.QuizRepository;
import org.quizly.quizly.core.domain.repository.SolveHistoryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiResponse;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.PreviousQuizContext;
import org.quizly.quizly.external.openai.util.OpenAiResponseFormatUtil;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.quizly.quizly.quiz.service.CreateMemberFollowUpQuizService.CreateMemberFollowUpQuizRequest;
import org.quizly.quizly.quiz.service.CreateMemberFollowUpQuizService.CreateMemberFollowUpQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CreateMemberFollowUpQuizService implements
    BaseService<CreateMemberFollowUpQuizRequest, CreateMemberFollowUpQuizResponse> {

    private final QuizRepository quizRepository;
    private final ReadUserService readUserService;
    private final SolveHistoryRepository solveHistoryRepository;
    private final CreateFollowUpQuizOpenAiService createFollowUpQuizOpenAiService;

    @Override
    public CreateMemberFollowUpQuizResponse execute(CreateMemberFollowUpQuizRequest request) {
        if (request == null || !request.isValid()) {
            return CreateMemberFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateMemberFollowUpQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        ReadUserResponse readUserResponse = readUserService.execute(
            ReadUserRequest.builder()
                .userPrincipal(request.getUserPrincipal())
                .build()
        );

        if (!readUserResponse.isSuccess()) {
            return CreateMemberFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateMemberFollowUpQuizErrorCode.NOT_FOUND_USER)
                .build();
        }
        User user = readUserResponse.getUser();

        Optional<Quiz> optionalQuiz = quizRepository.findById(request.getQuizId());
        if (optionalQuiz.isEmpty()) {
            return CreateMemberFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateMemberFollowUpQuizErrorCode.QUIZ_NOT_FOUND)
                .build();
        }
        Quiz quiz = optionalQuiz.get();

        if (quiz.getUser() == null || !quiz.getUser().equals(user)) {
            log.warn(
                "[CreateMemberFollowUpQuizService] Cannot access other quiz userId: {}, quizId: {}",
                user.getId(), quiz.getId());
            return CreateMemberFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateMemberFollowUpQuizErrorCode.CANNOT_ACCESS_OTHER_QUIZ)
                .build();
        }

        if (quiz.getSourceText() == null || quiz.getSourceText().isBlank()) {
            return CreateMemberFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateMemberFollowUpQuizErrorCode.SOURCE_TEXT_NOT_FOUND)
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
            log.warn("[CreateMemberFollowUpQuizService] Failed to create follow-up quiz from OpenAi.");
            return CreateMemberFollowUpQuizResponse.builder()
                .success(false)
                .errorCode(CreateMemberFollowUpQuizErrorCode.OPENAI_QUIZ_GENERATION_FAILED)
                .build();
        }

        Quiz followUpQuiz = saveFollowUpQuiz(openAiResponse.getGeneratedQuiz(), quiz, user);

        return CreateMemberFollowUpQuizResponse.builder()
            .quiz(followUpQuiz)
            .build();
    }

    private Quiz saveFollowUpQuiz(GeneratedQuizResponse generatedQuiz, Quiz parentQuiz, User user) {
        Quiz followUpQuiz = Quiz.builder()
            .quizText(generatedQuiz.getQuiz())
            .answer(generatedQuiz.getAnswer())
            .quizType(generatedQuiz.getType())
            .explanation(generatedQuiz.getExplanation())
            .options(generatedQuiz.getOptions())
            .topic(parentQuiz.getTopic())
            .sourceText(parentQuiz.getSourceText())
            .parentQuiz(parentQuiz)
            .user(user)
            .guest(false)
            .build();

        Quiz savedQuiz = quizRepository.save(followUpQuiz);

        solveHistoryRepository.save(SolveHistory.builder()
            .user(user)
            .quiz(savedQuiz)
            .isCorrect(false)
            .submittedAt(LocalDateTime.now())
            .build());

        return savedQuiz;
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
    public enum CreateMemberFollowUpQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
        QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
        CANNOT_ACCESS_OTHER_QUIZ(HttpStatus.FORBIDDEN, "다른 유저가 만든 퀴즈로는 꼬리 문제를 생성할 수 없습니다."),
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
    public static class CreateMemberFollowUpQuizRequest implements BaseRequest {

        private Long quizId;

        private UserPrincipal userPrincipal;

        @Override
        public boolean isValid() {
            return quizId != null && userPrincipal != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateMemberFollowUpQuizResponse extends
        BaseResponse<CreateMemberFollowUpQuizErrorCode> {

        private Quiz quiz;
    }
}
