package org.quizly.quizly.admin.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.repository.InquiryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminReadInquiriesService implements
    BaseService<AdminReadInquiriesService.AdminReadInquiriesRequest, AdminReadInquiriesService.AdminReadInquiriesResponse> {

    private final InquiryRepository inquiryRepository;
    private static final String SORT_BY_LATEST = "createdAt";

    @Override
    public AdminReadInquiriesResponse execute(AdminReadInquiriesRequest request) {

        if (request == null || !request.isValid()) {
            return AdminReadInquiriesService.AdminReadInquiriesResponse.builder()
                .success(false)
                .errorCode(AdminReadInquiriesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }
        Pageable pageRequest = request.getPageRequest()
            .withSort(Sort.by(Sort.Direction.DESC, SORT_BY_LATEST));

        Page<Inquiry> inquiryPage;

        if (request.getStatus() == null) {
            inquiryPage = inquiryRepository.findAllWithUser(pageRequest);
        } else {
            inquiryPage = inquiryRepository.findAllByStatusWithUser(request.getStatus(),
                pageRequest);
        }

        return AdminReadInquiriesResponse.builder()
            .success(true)
            .inquiryList(inquiryPage.getContent())
            .pagination(Pagination.getPaginationFromPage(inquiryPage))
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
        private PageRequest pageRequest;


        @Override
        public boolean isValid() {
            return pageRequest != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AdminReadInquiriesResponse extends
        BaseResponse<AdminReadInquiriesService.AdminReadInquiriesErrorCode> {

        private List<Inquiry> inquiryList;
        private Pagination pagination;

    }
}
