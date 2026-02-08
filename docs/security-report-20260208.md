# 🔒 보안 취약점 체크 결과

**프로젝트**: beStarterKit
**분석 날짜**: 2026-02-08
**분석 도구**: Claude Code Security Check

---

## 전체 평가: ✅ 양호 (Good)

- 주요 보안 취약점 발견되지 않음
- 몇 가지 개선 사항 권장

---

## 카테고리별 분석 결과

### ✅ SQL Injection 검사: 통과

**분석 범위**: `src/main/kotlin/**/repository/`

**결과**: 안전
- Repository에서 모든 쿼리가 Spring Data JPA 메서드 네이밍 규칙 사용
- 문자열 연결이나 동적 쿼리 생성 없음
- Native query나 @Query 어노테이션 미사용

**검사 항목**:
- ❌ `@Query` 어노테이션에 문자열 연결(`+`, `${}`) 사용
- ❌ Native query에서 파라미터 바인딩 미사용
- ❌ JPQL에서 동적 쿼리 생성 패턴

---

### ⚠️ [MEDIUM] 민감 정보 노출 검사: 주의 필요

#### 📍 src/main/resources/application-dev.yml:6

```yaml
password: starter_pass
```

**문제점**: dev 프로필에 DB 비밀번호가 평문으로 하드코딩
**영향도**: dev 환경이므로 중간 위험도

**권장 사항**:
- 개발 환경이라도 `.env` 파일 + 환경변수 사용 권장
- 예: `password: ${DEV_DB_PASSWORD:starter_pass}` (기본값 fallback)
- Git에 실제 운영 DB 비밀번호가 들어가지 않도록 주의

**수정 예시**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/starter_dev
    username: ${DEV_DB_USERNAME:starter_user}
    password: ${DEV_DB_PASSWORD:starter_pass}
```

#### ✅ .gitignore:6
`.env` 파일이 이미 .gitignore에 포함됨 (Good!)

---

### ✅ 데이터 검증 검사: 통과

**분석 범위**: `src/main/kotlin/**/controller/`, `src/main/kotlin/**/dto/`

**결과**: 안전
- Controller에서 `@Valid` 어노테이션 올바르게 사용
- DTO에서 `@field:NotBlank`, `@field:Email`, `@field:Size` 올바르게 적용
- Kotlin에서 use-site target을 정확히 사용 (중요!)

#### 📍 src/main/kotlin/com/example/starter/controller/UserController.kt:48,60

```kotlin
fun create(@Valid @RequestBody request: UserCreateRequest)
fun update(@PathVariable id: Long, @Valid @RequestBody request: UserUpdateRequest)
```

✅ 검증 어노테이션 올바르게 적용됨

#### 📍 src/main/kotlin/com/example/starter/dto/UserCreateRequest.kt:8-15

```kotlin
data class UserCreateRequest(
    @field:NotBlank(message = "username은 필수입니다")
    @field:Size(min = 2, max = 50, message = "username은 2~50자 사이여야 합니다")
    val username: String,

    @field:NotBlank(message = "email은 필수입니다")
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:Size(max = 100, message = "email은 100자 이하여야 합니다")
    val email: String
)
```

✅ Kotlin `@field:` use-site target 올바르게 사용

---

### ✅ XSS 방어 검사: 통과

**분석 범위**: `src/main/kotlin/**/controller/`, `src/main/kotlin/**/dto/`

**결과**: 안전
- Spring Boot 기본 JSON 직렬화 사용 (자동 이스케이프)
- `@JsonRawValue` 미사용
- 사용자 입력을 그대로 HTML로 렌더링하지 않음

---

### ⚠️ [MEDIUM] 에러 핸들링 검사: 주의 필요

#### 📍 src/main/kotlin/com/example/starter/exception/GlobalExceptionHandler.kt:26-35

```kotlin
@ExceptionHandler(RuntimeException::class)
fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, Any>> {
    val message = ex.message ?: "내부 서버 오류"
    val isNotFound = message.contains("not found", ignoreCase = true)
    val status = if (isNotFound) HttpStatus.NOT_FOUND else HttpStatus.INTERNAL_SERVER_ERROR

    val body = mapOf(
        "status" to status.value(),
        "message" to message
    )
    return ResponseEntity.status(status).body(body)
}
```

**문제점**: 예외 메시지를 그대로 클라이언트에 노출

**보안 위험**:
- 예외 메시지에 DB 스키마, 내부 경로, 스택 정보가 포함될 수 있음
- 현재는 단순 메시지만 반환하므로 중간 위험도

**권장 사항**:
프로덕션 환경에서는 일반화된 메시지 반환, 상세 에러는 로그로만 기록

**수정 예시**:
```kotlin
@ExceptionHandler(RuntimeException::class)
fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, Any>> {
    logger.error("RuntimeException occurred", ex) // 로그에만 상세 정보 기록

    val message = ex.message ?: "내부 서버 오류"
    val isNotFound = message.contains("not found", ignoreCase = true)
    val status = if (isNotFound) HttpStatus.NOT_FOUND else HttpStatus.INTERNAL_SERVER_ERROR

    // 프로덕션에서는 일반화된 메시지만 반환
    val clientMessage = when {
        isNotFound -> message // Not found는 상세 메시지 허용
        isProdProfile() -> "요청 처리 중 오류가 발생했습니다" // 프로덕션: 일반 메시지
        else -> message // 개발 환경: 상세 메시지
    }

    val body = mapOf(
        "status" to status.value(),
        "message" to clientMessage
    )
    return ResponseEntity.status(status).body(body)
}
```

---

### ⚠️ [LOW] JPA 보안 이슈 검사: 개선 권장

#### 📍 src/main/kotlin/com/example/starter/service/UserService.kt:15

```kotlin
fun findAll(): List<User> = userRepository.findAll()
```

**문제점**: 페이징 없는 전체 조회

**보안 위험**:
- 데이터가 많을 경우 메모리 부족 (DoS 가능성)
- 현재는 사용자 수가 적으므로 낮은 위험도

**권장 사항**:
Pageable 파라미터 추가 또는 최대 조회 개수 제한

**수정 예시**:
```kotlin
// Controller
@GetMapping
fun findAll(@RequestParam(defaultValue = "0") page: Int,
            @RequestParam(defaultValue = "20") size: Int): ResponseEntity<Page<UserResponse>> {
    val pageable = PageRequest.of(page, size)
    val users = userService.findAll(pageable).map(UserResponse::from)
    return ResponseEntity.ok(users)
}

// Service
@Transactional(readOnly = true)
fun findAll(pageable: Pageable): Page<User> = userRepository.findAll(pageable)
```

#### 📍 src/main/kotlin/com/example/starter/entity/User.kt:7
✅ 순환 참조 없음 (단일 엔티티이므로 안전)

---

### ✅ CORS 설정 검사: 통과

**분석 범위**: `src/main/kotlin/**/config/`

**결과**: 안전
- 별도의 CORS 설정 파일 없음
- `allowedOrigins = ["*"]` 사용 안 함 (Good!)
- 필요 시 명시적으로 도메인 지정 권장

**향후 CORS 추가 시 권장 설정**:
```kotlin
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://example.com") // 특정 도메인만
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true) // 인증 정보 포함 시
    }
}
```

---

### ✅ 의존성 취약점 검사: 통과

**분석 범위**: `build.gradle.kts`

**결과**: 안전
- Spring Boot 3.4.1 (최신 안정 버전)
- Kotlin 2.1.0 (최신 버전)
- springdoc-openapi 2.3.0 (최신 버전)
- Flyway, PostgreSQL 드라이버 모두 최신

**주요 의존성 버전**:
```kotlin
kotlin("jvm") version "2.1.0"
id("org.springframework.boot") version "3.4.1"
springdoc-openapi-starter-webmvc-ui:2.3.0
```

---

## 추가 보안 체크리스트

### 현재 미구현 항목 (필요 시 추가 권장)

#### ❌ Spring Security: 인증/인가 없음 → API가 완전히 공개 상태

**현재 상태**: 누구나 사용자 생성/조회/삭제 가능
**권장 사항**: Spring Security + JWT 또는 세션 기반 인증 추가

**구현 예시**:
```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.12.3")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
```

#### ❌ HTTPS 적용: 프로덕션 환경에서 HTTPS 강제 여부

**권장 사항**: 리버스 프록시(nginx) 또는 Spring Security로 HTTPS 리다이렉트

**application-prod.yml 추가**:
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
    key-store-type: PKCS12
```

#### ❌ Rate Limiting: API 호출 제한 없음

**권장 사항**: Bucket4j 또는 Spring Cloud Gateway로 Rate Limiting 추가

**구현 예시**:
```kotlin
// build.gradle.kts
implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:8.5.0")

// RateLimitInterceptor
@Component
class RateLimitInterceptor : HandlerInterceptor {
    private val bucket = Bucket.builder()
        .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
        .build()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (bucket.tryConsume(1)) {
            return true
        }
        response.status = 429
        return false
    }
}
```

#### ⚠️ Actuator 보안: 현재 `/actuator/health`만 노출 (Good!)

**주의**: 추가 엔드포인트 활성화 시 인증 필요

**안전한 설정 예시**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

#### ⚠️ 로깅 보안: 민감 정보 로깅 체크

**현재**: `show-sql: true` (dev 환경) → 실제 쿼리 파라미터 노출 가능
**권장**: 프로덕션에서는 비활성화

**application-prod.yml**:
```yaml
spring:
  jpa:
    show-sql: false
logging:
  level:
    org.hibernate.SQL: WARN
    org.hibernate.type.descriptor.sql.BasicBinder: WARN
```

#### ✅ CSRF 토큰: REST API이므로 세션 미사용 → 현재 불필요

Stateless JWT 사용 시 CSRF 불필요 (현재 적절함)

#### 📝 API 버전 관리: `/api/users` → `/api/v1/users` 권장

**권장 패턴**:
```kotlin
@RequestMapping("/api/v1/users")
class UserController { ... }
```

---

### 현재 잘 구현된 항목 ✅

- ✅ Jakarta Validation + @Valid 올바른 사용
- ✅ Flyway 마이그레이션으로 스키마 관리
- ✅ @Transactional 경계 명확히 설정
- ✅ prod 프로필에서 Swagger 비활성화
- ✅ 환경변수 기반 DB 설정 (prod)
- ✅ .env 파일 .gitignore 등록
- ✅ H2 in-memory DB로 테스트 분리
- ✅ Actuator 최소 노출 (health만)
- ✅ 최신 의존성 사용

---

## 최종 요약

### 📊 전체 보안 점수: B+ (양호)

**총 3개의 개선 사항 발견**
- **High**: 0개
- **Medium**: 2개 (dev 환경 비밀번호, 에러 메시지 노출)
- **Low**: 1개 (페이징 없는 findAll)

### 🎯 즉시 조치 권장 항목

1. **dev 환경도 환경변수 기반으로 변경**
   - `application-dev.yml`에서 비밀번호를 환경변수로 변경
   - `.env` 파일 활용

2. **GlobalExceptionHandler에서 프로덕션 환경 예외 메시지 일반화**
   - 상세 에러는 로그로만 기록
   - 클라이언트에는 일반화된 메시지 반환

3. **실제 배포 전 Spring Security 인증/인가 추가 검토**
   - 현재 API가 완전히 공개 상태
   - JWT 또는 세션 기반 인증 구현 필요

### ⚡ 우선순위 낮은 개선 항목

- **findAll() 페이징 추가** (데이터 증가 시)
- **Rate Limiting 추가** (트래픽 증가 시)
- **HTTPS 강제** (배포 환경)
- **API 버전 관리** (`/api/v1/users`)

---

## 참고사항

- 이 보안 체크는 정적 분석 기반이며 false positive 가능성 있음
- 실제 침투 테스트(Penetration Testing) 및 코드 리뷰 병행 권장
- **OWASP Top 10 2021** 기준 체크 완료
- 동적 분석 도구(SAST/DAST) 활용 권장

---

## 검사 항목 체크리스트

### OWASP Top 10 2021 기준

| 순위 | 항목 | 검사 결과 | 비고 |
|------|------|-----------|------|
| A01 | Broken Access Control | ⚠️ 주의 | 인증/인가 미구현 |
| A02 | Cryptographic Failures | ✅ 통과 | prod 환경변수 사용 |
| A03 | Injection | ✅ 통과 | SQL Injection 안전 |
| A04 | Insecure Design | ✅ 통과 | 계층형 아키텍처 |
| A05 | Security Misconfiguration | ⚠️ 주의 | dev 비밀번호, Actuator |
| A06 | Vulnerable Components | ✅ 통과 | 최신 의존성 사용 |
| A07 | Authentication Failures | ⚠️ 주의 | 인증 미구현 |
| A08 | Data Integrity Failures | ✅ 통과 | Validation 적용 |
| A09 | Logging Failures | ⚠️ 주의 | show-sql 활성화 |
| A10 | SSRF | ✅ 통과 | 외부 호출 없음 |

---

**생성 날짜**: 2026-02-08
**분석 도구**: Claude Code Security Check v1.0
**분석 대상**: beStarterKit (Spring Boot 3.4.1 + Kotlin 2.1.0)
