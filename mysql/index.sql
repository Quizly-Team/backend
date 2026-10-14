# 실서버에 인덱스 변경 이력 (제거는 기존 CREATE 를 남겨 두고 DROP 문을 새로 추가한다)

-- solve_history

# [2026-02-19] 추가
# 사용처 : 제거됨 (아래 2026-10-14 참고)
CREATE INDEX idx_solve_history_user_quiz_created
    ON solve_history(user_id, quiz_id, created_at DESC);

# 사용처 : findWrongQuizDateGroupsByUser, findWrongQuizTopicGroupsByUser,
#         풀이 이력 존재 확인(EXISTS sh.user = ? AND sh.quiz = q) - findQuizDateGroupsByUser, findQuizTopicGroupsByUser,
#         findLatestSolveHistoriesByUserAndDateTimeRange/Topic (count)
CREATE INDEX idx_solve_history_user_correct_quiz_created
    ON solve_history(user_id, is_correct, quiz_id, created_at DESC);

# 사용처 : aggregationUserReader (USE INDEX 로 고정 - 인덱스 이름 변경 시 함께 수정)
CREATE INDEX idx_solve_history_submitted_user
    ON solve_history(submitted_at, user_id);

# 사용처 : findFirstAttemptsByQuizTypeAndDateTimeRange, findFirstAttemptsHourlySummaryByUserAndDateTimeRange
CREATE INDEX idx_solve_history_user_submitted
    ON solve_history(user_id, submitted_at);

# [2026-10-15] 제거
# idx_solve_history_user_correct_quiz_created 가 같은 쿼리를 커버링으로 대신하고,
# 인덱스를 쓰는 쿼리 17개에서 실행 시간 차이가 측정 오차 수준이라 제거한다
DROP INDEX idx_solve_history_user_quiz_created ON solve_history;

-- quiz

# [2026-10-15] 추가
# 사용처 : findQuizDateGroupsByUser, findLatest(Wrong)SolveHistoriesByUserAndDateTimeRange
#         (사용자의 문제를 생성일 범위로 찾고, 같은 순서로 정렬해 LIMIT 만큼만 읽음)
CREATE INDEX idx_quiz_user_created
    ON quiz(user_id, created_at);

# [2026-10-15] 추가
# 사용처 : findQuizTopicGroupsByUser, findLatest(Wrong)SolveHistoriesByUserAndTopic
CREATE INDEX idx_quiz_user_topic_created
    ON quiz(user_id, topic, created_at);
