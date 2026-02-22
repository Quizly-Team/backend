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
import org.quizly.quizly.inquiry.service.ReadInquiriesService.ReadInquiriesRequest;
import org.quizly.quizly.inquiry.service.ReadInquiriesService.ReadInquiriesResponse;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReadInquiriesService implements BaseService<ReadInquiriesRequest,ReadInquiriesResponse> {

    private final ReadUserService readUserService;
    private final InquiryRepository inquiryRepository;

    @Override
    public ReadInquiriesResponse execute(ReadInquiriesRequest request) {

        if(request == null || !request.isValid()){
            return ReadInquiriesResponse.builder()
                .success(false)
                .errorCode(ReadInquiriesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }
        ReadUserService.ReadUserResponse readUserResponse = readUserService.execute(
            ReadUserService.ReadUserRequest.builder()
                .userPrincipal(request.getUserPrincipal())
                .build()
        );

        if(!readUserResponse.isSuccess()){
            return ReadInquiriesResponse.builder()
                .success(false)
                .errorCode(ReadInquiriesErrorCode.NOT_FOUND_USER)
                .build();
        }
        User user = readUserResponse.getUser();

        List<Inquiry> inquiryList = inquiryRepository.findAllByUser(user);

        return ReadInquiriesResponse.builder()
            .success(true)
            .inquiryList(inquiryList)
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReadInquiriesErrorCode implements BaseErrorCode<DomainException>{
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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ReadInquiriesRequest implements BaseRequest{
        private UserPrincipal userPrincipal;

        @Override
        public boolean isValid() {
            return userPrincipal != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ReadInquiriesResponse extends BaseResponse<ReadInquiriesErrorCode>{

        private List<Inquiry> inquiryList;

    }
}
