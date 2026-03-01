package org.quizly.quizly.admin.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.InquiryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.email.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class AdminReplyInquiryService implements BaseService<AdminReplyInquiryService.AdminReplyInquiryRequest, AdminReplyInquiryService.AdminReplyInquiryResponse> {

    private final InquiryRepository inquiryRepository;
    private final EmailService emailService;

    @Override
    public AdminReplyInquiryResponse execute(AdminReplyInquiryRequest request) {

        if(request == null || !request.isValid()){
            return AdminReplyInquiryResponse.builder()
                .success(false)
                .errorCode(AdminReplyInquiryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        Inquiry inquiry = inquiryRepository.findById(request.getInquiryId())
            .orElseThrow(AdminReplyInquiryErrorCode.NOT_FOUND_INQUIRY::toException);
        inquiry.reply(request.getReply());

        User user = inquiry.getUser();


        if(user != null && user.getEmail() != null){
            Map<String,Object> variables = new HashMap<>();
            String nickname = user.getNickName() != null ? user.getNickName() : "고객";
            String defaultTitle = String.format("%s님, 문의 하신 내용에 답변이 완료되었습니다.",nickname);
            variables.put("title", defaultTitle);
            variables.put("replyContent", inquiry.getReply());
            variables.put("inquiryTitle", inquiry.getTitle());
            variables.put("inquiryContent", inquiry.getContent());

            EmailService.EmailRequest emailRequest = EmailService.EmailRequest
                .builder()
                .to(user.getEmail())
                .subject("[Quizly]: 문의 답변 알림")
                .templatePath("email/inquiry-reply")
                .variables(variables)
                .build();

            EmailService.EmailResponse emailResponse = emailService.execute(emailRequest);
            if (emailResponse == null || !emailResponse.isSuccess()) {
                log.error("Failed to send reply notification email to: {}", user.getEmail());

                return AdminReplyInquiryResponse.builder()
                    .success(false)
                    .errorCode(AdminReplyInquiryErrorCode.FAILED_TO_SEND_EMAIL)
                    .build();
            }
        }

        return AdminReplyInquiryResponse.builder()
            .success(true)
            .inquiryId(inquiry.getId())
            .title(inquiry.getTitle())
            .reply(inquiry.getReply())
            .repliedAt(inquiry.getRepliedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .build();

    }

    @Getter
    @RequiredArgsConstructor
    public enum AdminReplyInquiryErrorCode implements BaseErrorCode<DomainException> {
        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_FOUND_INQUIRY(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
        FAILED_TO_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, "답변 알림 메일 발송에 실패했습니다.");

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
    public static class AdminReplyInquiryRequest implements BaseRequest {

        private Long inquiryId;
        private String reply;

        @Override
        public boolean isValid() {
            return inquiryId != null && reply != null && !reply.isBlank();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AdminReplyInquiryResponse extends BaseResponse<AdminReplyInquiryService.AdminReplyInquiryErrorCode> {
        private Long inquiryId;
        private String title;
        private String reply;
        private String repliedAt;
    }
}
