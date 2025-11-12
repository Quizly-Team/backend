package org.quizly.quizly.external.storage.service;
import org.apache.tika.Tika;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.external.storage.error.StorageErrorCode;
import org.quizly.quizly.external.storage.error.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService implements
        BaseService<ProfileImageService.UploadProfileImageRequest, ProfileImageService.UploadProfileImageResponse> {

    private final S3Client s3Client;
    private static final Tika tika = new Tika();

    @Value("${object-storage.bucket}")
    private String bucket;

    @Value("${object-storage.endpoint}")
    private String endpoint;

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public UploadProfileImageResponse execute(UploadProfileImageRequest request) {
        if (request == null || !request.isValid()) {
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.INVALID_REQUEST)
                    .build();
        }

        MultipartFile file = request.getFile();
        Long userId = request.getUserId();

        String originalName = file.getOriginalFilename();
        if (file.isEmpty()) {
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.EMPTY_FILE)
                    .build();
        }
        if (originalName == null || !originalName.contains(".")) {
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.MISSING_EXTENSION)
                    .build();
        }

        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.INVALID_FILE_EXTENSION)
                    .build();
        }

        String detectedType;
        try {
            detectedType = tika.detect(file.getInputStream());
        } catch (IOException e) {
            log.error("[ProfileImageService] MIME 탐지 실패", e);
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.INVALID_MIME_TYPE)
                    .build();
        }

        if (detectedType == null || !detectedType.startsWith("image/")) {
            log.warn("[ProfileImageService] MIME 검증 실패 - 감지된 타입: {}", detectedType);
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.INVALID_MIME_TYPE)
                    .build();
        }

        if (file.getSize() > 1 * 1024 * 1024) {
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.FILE_TOO_LARGE)
                    .build();
        }

        try {
            if (request.getExistingUrl() != null && !request.getExistingUrl().isEmpty()) {
                String existingKey = request.getExistingUrl()
                        .replace(endpoint + "/" + bucket + "/", "");
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(existingKey)
                        .build());
                log.info("[ProfileImageService] 기존 프로필 이미지 삭제 완료: {}", existingKey);
            }

            String fileName = "profiles/" + userId + "/" + UUID.randomUUID() + "_" + originalName;
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("%s/%s/%s", endpoint, bucket, fileName);
            log.info("[ProfileImageService] 업로드 성공 - userId: {}, url: {}", userId, fileUrl);

            return UploadProfileImageResponse.builder()
                    .success(true)
                    .fileUrl(fileUrl)
                    .build();

        } catch (IOException e) {
            log.error("[ProfileImageService] S3 업로드 실패", e);
            return UploadProfileImageResponse.builder()
                    .success(false)
                    .errorCode(StorageErrorCode.FAILED_UPLOAD)
                    .build();
        }
    }



    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadProfileImageRequest implements BaseRequest {
        private MultipartFile file;
        private Long userId;
        private String existingUrl;
        @Override
        public boolean isValid() {
            return file != null && userId != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadProfileImageResponse extends BaseResponse<StorageErrorCode> {
        private String fileUrl;
    }
}
