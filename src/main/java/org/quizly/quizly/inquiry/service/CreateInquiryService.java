package org.quizly.quizly.inquiry.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadUserService;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.InquiryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateInquiryService implements BaseService<CreateInquiryService.CreateInquiryRequest, CreateInquiryService.CreateInquiryResponse> {

    private final ReadUserService readUserService;
    private final InquiryRepository inquiryRepository;

    @Override
    public CreateInquiryResponse execute(CreateInquiryRequest request) {
        if(request == null || !request.isValid()){
            return CreateInquiryResponse.builder()
                .success(false)
                .errorCode(CreateInquiryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        ReadUserService.ReadUserResponse readUserResponse = readUserService.execute(
            ReadUserService.ReadUserRequest
                .builder()
                .userPrincipal(request.getUserPrincipal())
                .build()
        );

        if(!readUserResponse.isSuccess()){
            return CreateInquiryResponse.builder()
                .success(false)
                .errorCode(CreateInquiryErrorCode.NOT_FOUND_USER)
                .build();
        }

        User user = readUserResponse.getUser();

        Inquiry inquiry = Inquiry.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .status(Inquiry.Status.WAITING)
            .user(user)
            .build();


        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        return CreateInquiryResponse.builder()
            .success(true)
            .inquiryId(savedInquiry.getId())
            .status(savedInquiry.getStatus())
            .build();
    }



    @Getter
    @RequiredArgsConstructor
    public enum CreateInquiryErrorCode implements BaseErrorCode<DomainException> {
        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");


        private final HttpStatus httpStatus;

        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInquiryRequest implements BaseRequest{
        private UserPrincipal userPrincipal;
        private String title;
        private String content;

        @Override
        public boolean isValid(){
            return userPrincipal != null && title != null && !title.isBlank() && content !=null && !content.isBlank();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInquiryResponse extends BaseResponse<CreateInquiryErrorCode>{
        private Long inquiryId;
        private Inquiry.Status status;
    }
}



