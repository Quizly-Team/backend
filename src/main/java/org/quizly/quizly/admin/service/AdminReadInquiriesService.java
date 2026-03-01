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
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.quizly.quizly.admin.dto.response.AdminReadInquiriesResponse.AdminInquiryDetail;
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

        java.util.Set<String> allowedSortBy = java.util.Set.of(
            "id", "status", "createdAt", "updatedAt", "repliedAt", "title");

        String sortBy = request.getSortBy();
        if(sortBy == null){
            sortBy = "createdAt";
        }

        if (!allowedSortBy.contains(sortBy)) {
            return AdminReadInquiriesResponse.builder()
                .success(false)
                .errorCode(AdminReadInquiriesErrorCode.INVALID_SORT_PARAMETER)
                .build();
        }
        Sort.Direction direction = request.getDirection() != null ? request.getDirection() : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortBy);

        List<Inquiry>inquiryList;

        if(request.getStatus() == null){
            inquiryList = inquiryRepository.findAllWithUser(sort);
        }else{
            inquiryList = inquiryRepository.findAllByStatusWithUser(request.getStatus(),sort);
        }

        List<org.quizly.quizly.admin.dto.response.AdminReadInquiriesResponse.AdminInquiryDetail>dtoInquiryList = inquiryList.stream()
            .map(inquiry -> new org.quizly.quizly.admin.dto.response.AdminReadInquiriesResponse.AdminInquiryDetail(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getUser().getName(),
                inquiry.getUser().getId(),
                inquiry.getReply(),
                inquiry.getRepliedAt(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
            )).toList();

        return AdminReadInquiriesResponse.builder()
            .success(true)
            .inquiryList(dtoInquiryList)
            .build();

    }


    @Getter
    @RequiredArgsConstructor
    public enum AdminReadInquiriesErrorCode implements BaseErrorCode<DomainException> {
        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
        INVALID_SORT_PARAMETER(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬 기준입니다.");

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
        private UserPrincipal userPrincipal;
        private String sortBy;
        private Sort.Direction direction;

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
    public static class AdminReadInquiriesResponse extends BaseResponse<AdminReadInquiriesService.AdminReadInquiriesErrorCode> {

        private List<AdminInquiryDetail> inquiryList;

    }
}
