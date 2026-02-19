-- solve_history

# 사용처 : findLatestSolveHistoriesByUser
CREATE INDEX idx_solve_history_user_quiz_created
    ON solve_history(user_id, quiz_id, created_at DESC);

# 사용처 : findLatestWrongSolveHistoriesByUser
CREATE INDEX idx_solve_history_user_correct_quiz_created
    ON solve_history(user_id, is_correct, quiz_id, created_at DESC);

# 사용처 : aggregationUserReader
CREATE INDEX idx_solve_history_submitted_user
    ON solve_history(submitted_at, user_id);

# 사용처 : findFirstAttemptsByQuizTypeAndDate, findHourlySummaryByUserAndDate
CREATE INDEX idx_solve_history_user_submitted
    ON solve_history(user_id, submitted_at);
