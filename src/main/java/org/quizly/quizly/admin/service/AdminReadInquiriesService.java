package org.quizly.quizly.admin.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.repository.InquiryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminReadInquiriesService implements BaseService<AdminReadInquiriesService.AdminReadInquiriesRequest, AdminReadInquiriesService.AdminReadInquiriesResponse> {

    private final InquiryRepository inquiryRepository;

    @Override
    public AdminReadInquiriesResponse execute(AdminReadInquiriesRequest request) {

        if(request == null || !request.isValid()){
            return AdminReadInquiriesService.AdminReadInquiriesResponse.builder()
                .success(false)
                .errorCode(AdminReadInquiriesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        Sort sort = Sort.by(Sort.Direction.DESC,"createdAt");

        List<Inquiry>inquiryList;

        if(request.getStatus() == null){
            inquiryList = inquiryRepository.findAllWithUser(sort);
        }else{
            inquiryList = inquiryRepository.findAllByStatusWithUser(request.getStatus(),sort);
        }

        return AdminReadInquiriesResponse.builder()
            .success(true)
            .inquiryList(inquiryList)
            .build();

    }


    @Getter
    @RequiredArgsConstructor
    public enum AdminReadInquiriesErrorCode implements BaseErrorCode<DomainException> {
        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다.");

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
    public static class AdminReadInquiriesRequest implements BaseRequest {

        private Inquiry.Status status;


        @Override
        public boolean isValid() {
            return true;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AdminReadInquiriesResponse extends BaseResponse<AdminReadInquiriesService.AdminReadInquiriesErrorCode> {

        private List<Inquiry> inquiryList;

    }
}
