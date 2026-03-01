package org.quizly.quizly.external.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.external.email.error.EmailErrorCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailService implements BaseService<EmailService.EmailRequest, EmailService.EmailResponse> {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Override
    public EmailResponse execute(EmailRequest request) {
        if(request == null || !request.isValid()){
            return EmailResponse.builder()
                .success(false)
                .errorCode(EmailErrorCode.NOT_EXIST_EMAIL_REQUIRED_PARAMETER)
                .build();
        }
        MimeMessage message = javaMailSender.createMimeMessage();
        try{

            MimeMessageHelper helper = new MimeMessageHelper(message,true,"UTF-8");

            Context context = new Context();
            if (request.getVariables() != null) {
                request.getVariables().forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process(request.getTemplatePath(),context);

            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(htmlContent,true);


            javaMailSender.send(message);
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            return EmailResponse.builder()
                .success(true)
                .to(request.getTo())
                .sentAt(now)
                .build();

        }catch (MessagingException e){
            log.error("Email Sending failed to : {}",request.getTo(),e);
            return EmailResponse.builder()
                .success(false)
                .errorCode(EmailErrorCode.FAILED_TO_SEND)
                .build();
        }


    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailRequest implements BaseRequest {
        private String to;
        private String subject;
        private String templatePath;
        private Map<String,Object> variables;

        @Override
        public boolean isValid() {
            return to != null && subject != null && templatePath != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class EmailResponse extends BaseResponse<EmailErrorCode> {
        private String to;
        private String sentAt;
    }
}
