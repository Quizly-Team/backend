package org.quizly.quizly.experiment;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiResponse;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.PreviousQuizContext;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiResponse;
import org.quizly.quizly.external.openai.util.OpenAiResponseFormatUtil;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 기획 요청 검증용 1회성 실험: 500자 청크 하나에서 "직전 문제 기반 체인" 방식으로 꼬리 문제를
 * 몇 개까지 정상적으로 생성할 수 있는지 실제 OpenAI 호출로 확인한다.
 *
 * 실제 OpenAI API를 호출해 비용이 발생하므로 기본적으로 비활성화되어 있다.
 * 실행하려면 클래스의 @Disabled 어노테이션을 주석 처리한 뒤
 * ./gradlew test --tests "*FollowUpQuizChainExperiment*" 로 실행하고 콘솔 로그를 확인한다.
 */
@Disabled("수동 실행 전용 - 실제 OpenAI API 호출 및 비용 발생. 실행 시에만 이 어노테이션을 주석 처리할 것")
@SpringBootTest
class FollowUpQuizChainExperiment {

    private static final Logger log = LogManager.getLogger(FollowUpQuizChainExperiment.class);
    private static final Quiz.QuizType QUIZ_TYPE = Quiz.QuizType.MULTIPLE_CHOICE;
    private static final int FOLLOW_UP_STEPS = 3;

    private static final String SAMPLE_CONCEPT = """
        광합성은 식물, 조류, 일부 세균이 빛 에너지를 이용해 이산화탄소와 물로부터 포도당과 산소를 만드는 과정이다. \
        이 과정은 엽록체 안에서 일어나며 크게 명반응과 캘빈 회로(암반응) 두 단계로 나뉜다. 명반응은 틸라코이드 막에서 \
        일어나며, 엽록소가 빛 에너지를 흡수해 물 분자를 분해하고 이때 산소가 부산물로 방출된다. 동시에 ATP와 NADPH라는 \
        에너지 운반 물질이 생성된다. 캘빈 회로는 스트로마에서 일어나며, 명반응에서 만들어진 ATP와 NADPH를 이용해 \
        이산화탄소를 고정하고 포도당을 합성한다. 이 반응에는 빛이 직접 필요하지 않지만 명반응의 산물에 의존하기 때문에 \
        결국 빛에 의존적인 과정이다. 광합성 속도는 빛의 세기, 이산화탄소 농도, 온도 세 가지 요인에 의해 제한되며, \
        이 중 가장 부족한 요인이 전체 속도를 결정한다. 이를 리비히의 최소량의 법칙이라 부른다.
        """.replaceAll("\\s+", " ").trim();

    private static final String SAMPLE_HISTORY = """
        임진왜란은 1592년 일본의 도요토미 히데요시가 조선을 침공하면서 시작된 전쟁이다. 일본군은 부산포에 상륙한 지 \
        20일 만에 한양을 점령했고, 선조는 의주까지 피난했다. 전쟁 초기 조선군은 육상에서 연이어 패배했지만, 이순신이 \
        이끄는 조선 수군은 옥포·사천·한산도 해전에서 연승을 거두며 일본군의 수륙병진 전략을 무너뜨렸다. 특히 1592년 \
        한산도 대첩에서는 학익진 전법으로 일본 수군에 큰 타격을 입혔다. 육상에서는 곽재우 등 의병이 각지에서 봉기해 \
        일본군의 보급로를 위협했고, 명나라 원군이 참전하면서 전세가 조금씩 조선에 유리해졌다. 1597년 일본은 정유재란을 \
        일으켜 다시 침공했으나, 명량 해전에서 이순신이 단 13척의 배로 일본 수군 133척을 상대해 대승을 거두었다. \
        1598년 도요토미 히데요시가 사망하자 일본군은 철수를 시작했고, 이순신은 노량 해전에서 철수하는 일본군을 \
        추격하다 전사했다.
        """.replaceAll("\\s+", " ").trim();

    private static final String SAMPLE_STATISTICS = """
        통계청 발표에 따르면 2023년 대한민국의 합계출산율은 0.72명으로 전년(0.78명) 대비 0.06명 감소하며 \
        역대 최저치를 기록했다. 같은 해 출생아 수는 23만 명으로 10년 전인 2013년의 43만 6천 명에 비해 절반 \
        수준으로 줄어들었다. 시도별로는 세종시의 합계출산율이 0.97명으로 전국에서 가장 높았고, 서울은 0.55명으로 \
        가장 낮았다. 평균 출산 연령은 33.6세로 2013년의 31.8세보다 1.8세 높아져 만혼·고령 출산 경향이 뚜렷해졌다. \
        65세 이상 고령인구 비율은 2023년 18.4%로 2013년의 12.2%에서 크게 늘었으며, 통계청은 2025년경 이 \
        비율이 20%를 넘어 초고령사회에 진입할 것으로 전망했다. 생산연령인구(15~64세) 비중은 2013년 73.4%에서 \
        2023년 70.6%로 줄어드는 추세이며, 이는 향후 노동력 부족과 사회보장 재정 부담 증가로 이어질 수 있다는 \
        분석이 나온다.
        """.replaceAll("\\s+", " ").trim();

    @Autowired
    private CreateQuizOpenAiService createQuizOpenAiService;

    @Autowired
    private CreateFollowUpQuizOpenAiService createFollowUpQuizOpenAiService;

    @Test
    void runFollowUpChainExperiment() {
        runChainForSample("SAMPLE_1_개념설명형(광합성)", SAMPLE_CONCEPT);
        runChainForSample("SAMPLE_2_역사서술형(임진왜란)", SAMPLE_HISTORY);
        runChainForSample("SAMPLE_3_수치통계형(출산율)", SAMPLE_STATISTICS);
    }

    private void runChainForSample(String sampleName, String sourceText) {
        log.info("===== {} (source length={}) =====", sampleName, sourceText.length());

        ResponseFormat responseFormat = OpenAiResponseFormatUtil
            .createSelectionQuizResponseFormat(1, QUIZ_TYPE.name());

        CreateQuizOpenAiResponse initialResponse = createQuizOpenAiService.execute(
            CreateQuizOpenAiRequest.builder()
                .plainText(sourceText)
                .promptPath(QUIZ_TYPE.getPromptPath())
                .responseFormat(responseFormat)
                .build());

        if (initialResponse == null || !initialResponse.isSuccess()
            || initialResponse.getGeneratedQuizResponseList().isEmpty()) {
            log.warn("[{}] STEP 0(원본 문제) 생성 실패: {}", sampleName, initialResponse);
            return;
        }

        GeneratedQuizResponse initialQuiz = initialResponse.getGeneratedQuizResponseList().get(0);
        List<PreviousQuizContext> chain = new ArrayList<>();
        chain.add(new PreviousQuizContext(initialQuiz.getQuiz(), initialQuiz.getAnswer()));
        logQuiz(sampleName, "STEP 0 (원본 문제)", initialQuiz);

        for (int step = 1; step <= FOLLOW_UP_STEPS; step++) {
            CreateFollowUpQuizOpenAiResponse followUpResponse = createFollowUpQuizOpenAiService.execute(
                CreateFollowUpQuizOpenAiRequest.builder()
                    .sourceText(sourceText)
                    .previousQuizzes(chain)
                    .quizType(QUIZ_TYPE)
                    .responseFormat(responseFormat)
                    .build());

            if (followUpResponse == null || !followUpResponse.isSuccess()) {
                log.warn("[{}] STEP {}(꼬리 문제) 생성 실패: {}", sampleName, step, followUpResponse);
                return;
            }

            GeneratedQuizResponse followUpQuiz = followUpResponse.getGeneratedQuiz();
            logQuiz(sampleName, "STEP " + step + " (꼬리 문제 " + step + ")", followUpQuiz);
            chain.add(new PreviousQuizContext(followUpQuiz.getQuiz(), followUpQuiz.getAnswer()));
        }
    }

    private void logQuiz(String sampleName, String stepLabel, GeneratedQuizResponse quiz) {
        log.info("[{}] {}\n  문제: {}\n  선택지: {}\n  정답: {}\n  해설: {}",
            sampleName, stepLabel, quiz.getQuiz(), quiz.getOptions(), quiz.getAnswer(),
            quiz.getExplanation());
    }
}
