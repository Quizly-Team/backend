# CLAUDE.md

Project conventions for **Quizly Backend**.

Stack: **Java 17**, **Spring Boot 3.4.5**, **Lombok**, **Spring Data JPA** (MySQL),
Spring Security + OAuth2 + JWT (jjwt), Spring AI / OpenAI, Redis, Spring Batch.

Base package: `org.quizly.quizly`.

These rules are mandatory. A change that violates any of them should be flagged in review.

## Package layout

- Each feature domain is its own package containing `controller`, `service`, and `dto`
  (`dto/request`, `dto/response`).
- Controllers are split into sub-packages by HTTP verb: `controller/post`, `controller/get`,
  `controller/patch`, `controller/delete`, `controller/put`.
- Shared code lives under `core`:
    - `core.application` — `BaseService`, `BaseRequest`, `BaseResponse`, `BaseAsyncService`
    - `core.exception` — `DomainException`, `SystemExceptionHandler`, `error.BaseErrorCode`,
      `error.GlobalErrorCode`
    - `core.presentation` — `ErrorResponse`, `Pagination`, `BasePaginationRequest`
    - `core.domain` — `entity`, `repository`, `shared.BaseEntity`
- `external` holds outbound integrations. Each integration should have its own `error` package;
  purely one-way notification integrations (e.g. Slack alerts) may omit it when errors are not
  propagated to callers.

## Use-case services

- One service per use case, named `<Verb><Noun>Service` (e.g. `CreateQuizService`,
  `ReadQuizzesService`).
- A service implements `BaseService<Q, R>` and exposes a single `execute(request)` method.
- Define the service's `Request`, `Response`, and `ErrorCode` as nested types inside the service
  class, named `<ServiceName>Request`, `<ServiceName>Response`, `<ServiceName>ErrorCode`
  (e.g. `CreateQuizRequest`, `CreateQuizResponse`, `CreateQuizErrorCode`):
    - `<ServiceName>Request` implements `BaseRequest` (override `isValid()` for validation).
    - `<ServiceName>Response` extends `BaseResponse` (built via `@SuperBuilder`).
- Inject dependencies with constructor injection (`@RequiredArgsConstructor`), never field
  injection.

## Error handling

Services **do not throw** for business/validation failures — the controller decides the HTTP
outcome.

- On failure, a service returns its `Response` with `success = false` and an `errorCode`
  (a `BaseErrorCode`). It does not throw.
- Each `ErrorCode` enum implements `BaseErrorCode<DomainException>`, and its `toException()`
  returns `new DomainException(httpStatus, this)`.
- Controllers check `response.isSuccess()`; on failure they `throw errorCode.toException()`,
  falling back to `GlobalErrorCode.INTERNAL_ERROR.toException()`.
- `SystemExceptionHandler` (`@RestControllerAdvice`) maps `DomainException` → `ErrorResponse`.
- Never throw a raw `RuntimeException(message)` or `ResponseStatusException(...)` in business or
  service logic. Route every business error through an `ErrorCode` → `DomainException`.
  Low-level utility or infrastructure code (e.g. I/O helpers, HTTP clients) may throw
  `RuntimeException` when there is no meaningful error code to attach.
- Error messages are written in **Korean**.

## Controllers

- Keep controllers thin: map the request DTO to the service `Request`, call `execute`, translate
  the failure into an exception, then map the service `Response` to the response DTO.
- Do not call repositories directly from a controller — go through a service.
- Do not put business logic in a controller.
- Document each endpoint with Swagger (`@Tag`, `@Operation`) and declare possible errors with
  `@ApiErrorCode(errorCodes = { ... })`.
- Read the authenticated user via `@AuthenticationPrincipal UserPrincipal`.

## Entities & persistence

- Entities extend `BaseEntity` (`@SuperBuilder`, JPA auditing), which supplies `id`
  (`GenerationType.IDENTITY`), `createdAt`, `updatedAt`, and the soft-delete flag `deleted`.
- **Soft delete only** — never physically delete a record. Set `deleted = true` instead.
- Read queries must exclude soft-deleted rows, e.g. derived methods like
  `findByIdAndDeletedFalse(...)` / `findAllByDeletedFalseOrderByCreatedAt()`.
- Associations must use `@ManyToOne(fetch = FetchType.LAZY)` etc. Never `FetchType.EAGER`.
- Build entities via the builder; avoid mutating state through setters where possible.
- Annotate state-mutating service methods with `@Transactional`; never mutate entity state without
  it.

## Style

- Prefer Lombok: `@RequiredArgsConstructor`, `@Getter`, `@Builder`/`@SuperBuilder`,
  `@NoArgsConstructor`/`@AllArgsConstructor`.
- Logging via `@Slf4j` or `@Log4j2` with a `[ClassName]` prefix in the message.
- Follow the existing Google Java Format style used in the codebase.
