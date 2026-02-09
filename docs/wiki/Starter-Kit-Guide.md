# Starter Kit을 만드는 법

> 팀 내 백엔드 프로젝트를 빠르고 일관하게 시작하기 위한 기술 공유용 Wiki 페이지입니다.
> `beStarterKit` 프로젝트의 실제 코드를 예시로 활용하여 작성되었습니다.

---

## 목차 (Table of Contents)

1. [개요](#1-개요)
2. [기술 스택 선택](#2-기술-스택-선택)
3. [프로젝트 구조 설계](#3-프로젝트-구조-설계)
4. [빌드 파일 세팅](#4-빌드-파일-세팅)
5. [환경 분리 (프로필)](#5-환경-분리-프로필)
6. [DB 마이그레이션](#6-db-마이그레이션)
7. [엔티티 및 Repository](#7-엔티티-및-repository)
8. [DTO 패턴](#8-dto-패턴)
9. [컨트롤러 및 유효성 검사](#9-컨트롤러-및-유효성-검사)
10. [예외 처리](#10-예외-처리)
11. [테스트 전략](#11-테스트-전략)
12. [로컬 개발 환경 세팅](#12-로컬-개발-환경-세팅)
13. [새 프로젝트 시작 체크리스트](#13-새-프로젝트-시작-체크리스트)

---

## 1. 개요

### 왜? (Why)

새 백엔드 프로젝트마다 기본 구조를 처음부터 세우면 시간이 낭비되고, 팀 간 일관성도 떨어집니다.
**Starter Kit**은 팀이 공유하는 프로젝트 템플릿으로서 다음을 제공합니다.

- **반복 작업 제거**: 빌드 파일, 프로필 분리, 마이그레이션 설정 등 보일러플레이트를 미리 완성
- **일관한 아키텍처**: 모든 프로젝트가 동일한 계층 구조와 패턴을 따름
- **온보딩 속도 향상**: 새 팀원이 프로젝트를 이해하는 데 필요한 시간 단축

`beStarterKit`은 **Spring Boot 3.4 + Kotlin 2.1 + PostgreSQL** 조합으로 구성된 실제 Starter Kit입니다.

---

## 2. 기술 스택 선택

### 왜? (Why)

각 기술은 단순히 인기 때문이 아니라, 구체적인 실용적 근거로 선택되었습니다.

### 예시 (Example)

| 영역 | 기술 | 선택 근거 |
|---|---|---|
| 언어 | **Kotlin 2.1** | 간결한 문법, null-safety로 런타임 오류 감소, JVM 생태계 완전 호환 |
| 프레임워크 | **Spring Boot 3.4** | 성숙한 생태계, 자동 설정 기능, 팀 내 기존 경험 축적 |
| ORM | **Spring Data JPA** | 자동 쿼리 생성으로 단순 CRUD 개발 속도 향상 |
| DB | **PostgreSQL 16** | JSON 지원, 고급 타입 시스템, 엔터프라이즈 안정성 |
| 마이그레이션 | **Flyway** | 버전 관리된 SQL로 스키마 변경 추적 및 복원 가능 |
| API 문서 | **Springdoc OpenAPI** | Spring Boot와 자동 통합, Swagger UI 제공 |
| 테스트 DB | **H2 (PostgreSQL MODE)** | 외부 DB 없이 CI/CD에서 빠르게 테스트 실행 |

---

## 3. 프로젝트 구조 설계

### 왜? (Why)

패키지를 **역할별로 분리**하면 각 클래스의 책임이 명확해지고, 새 기능 추가 시 어떤 파일을 만들어야 할지 고민 없이 바로 시작할 수 있습니다.
이 구조는 **계층형 아키텍처(Layered Architecture)**를 기반으로 합니다.

### 예시 (Example)

```
src/main/kotlin/com/example/starter/
├── Application.kt                  # 앱 진입점
├── config/
│   └── SwaggerConfig.kt            # OpenAPI Bean 정의
├── controller/
│   └── UserController.kt           # REST 엔드포인트, HTTP 응답 코드 관리
├── service/
│   └── UserService.kt              # 비즈니스 로직, @Transactional 경계
├── repository/
│   └── UserRepository.kt           # Spring Data JPA 인터페이스
├── entity/
│   └── User.kt                     # JPA 엔티티
├── dto/
│   ├── UserCreateRequest.kt        # 생성 요청 DTO
│   ├── UserUpdateRequest.kt        # 수정 요청 DTO (nullable 부분수정)
│   └── UserResponse.kt             # 응답 DTO (from() 팩토리)
└── exception/
    └── GlobalExceptionHandler.kt   # @RestControllerAdvice 전역 예외 핸들러
```

**데이터 흐름:**

```
HTTP 요청 → Controller (@Valid) → Service (@Transactional) → Repository → Entity ↔ DB
                                                                    ↓
HTTP 응답 ← Controller ← UserResponse.from(entity) ← Service ← Repository
```

---

## 4. 빌드 파일 세팅

### 왜? (Why)

Gradle 빌드 파일은 프로젝트의 **의존성, 컴파일 옵션, 플러그인**을 일괄 관리합니다.
Kotlin + Spring 조합에서는 플러그인 순서와 컴파일 옵션이 잘못되면 빌드가 실패하거나 기능이 동작하지 않습니다.

### 예시 (Example)

```kotlin
// 참조: build.gradle.kts

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"   // @Component 등 Spring 어노테이션 클래스의 open 처리
    kotlin("plugin.jpa") version "2.1.0"      // Entity의 no-arg constructor 자동 생성
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")  // Jakarta Validation

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0") // Swagger UI

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")  // 런타임만 필요 (JDBC 드라이버)

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")      // 테스트용 in-memory DB
}

// ⬇️ KotlinCompile 옵션 — 패키지명과 타입이 중요
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")                          // null-safety strict 모드
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23   // enum 타입, 문자열 아님
        javaParameters = true                                            // 런타임 파라미터 이름 보존
    }
}

tasks.named<Test>("test") {   // ⬇️ <Test> 타입 파라미터 필수
    useJUnitPlatform()
}
```

> ⚠️ **주의사항**
> - `KotlinCompile`은 전체 패키지명 `org.jetbrains.kotlin.gradle.tasks.KotlinCompile`을 사용해야 합니다. 짧은 이름만으로는 resolve가 안 됩니다.
> - `jvmTarget`은 **enum** 타입입니다. `"23"` (문자열)로 넣으면 타입 불일치 오류가 발생합니다. `org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23`을 사용합니다.
> - `tasks.named("test")`에 `<Test>` 타입 파라미터가 없으면 `useJUnitPlatform()` 호출이 불가합니다.

---

## 5. 환경 분리 (프로필)

### 왜? (Why)

개발(dev), 테스트(test), 프로덕션(prod) 환경은 DB 주소, 로그 레벨, 기능 활성화 여부가 다릅니다.
Spring Profile을 사용하면 **하나의 코드베이스**로 환경별 설정을 깔끔하게 분리할 수 있습니다.

### 예시 (Example)

공통 설정은 `application.yml`에, 환경별 차이만 프로필 파일에 작성합니다.

```yaml
# 참조: src/main/resources/application.yml (공통)
server:
  port: 8080

spring:
  jpa:
    hibernate:
      ddl-auto: none        # Flyway가 스키마 관리 → JPA 자동 DDL 비활성화

  flyway:
    locations: classpath:db/migration

springdoc:
  swagger-ui:
    path: /swagger-ui
  api-docs:
    path: /v3/api-docs

management:
  endpoints:
    web:
      exposure:
        include: health
```

| 프로필 | DB 연결 | Swagger | 주요 특징 |
|---|---|---|---|
| `dev` | `localhost:5432` (docker) | 활성 | `show-sql: true`로 쿼리 로그 확인 |
| `test` | H2 in-memory (`MODE=PostgreSQL`) | — | CI/CD 외부 DB 불필요 |
| `prod` | 환경변수 (`${PROD_DB_URL}`) | **비활성** | HikariCP 풀 크기 조정, API 문서 숨김 |

```yaml
# 참조: src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/starter_dev
    username: starter_user
    password: starter_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    show-sql: true
springdoc:
  swagger-ui:
    enabled: true
```

```yaml
# 참조: src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
```

```yaml
# 참조: src/main/resources/application-prod.yml
spring:
  datasource:
    url: ${PROD_DB_URL}
    username: ${PROD_DB_USERNAME}
    password: ${PROD_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

> ⚠️ **주의사항**
> - `application-prod.yml`의 DB 비밀정보는 **환경변수**로 주입됩니다. `.env` 파일이나 하드코딩된 비밀번호는 절대 Git에 커밋하지 않습니다.
> - 프로필 우선순위: 프로필 파일(`application-{profile}.yml`)의 값이 공통 파일(`application.yml`)을 **덮어씁니다.**

---

## 6. DB 마이그레이션

### 왜? (Why)

`spring.jpa.hibernate.ddl-auto=none`으로 설정되었으므로 **테이블을 코드로 직접 생성하면 안 됩니다.**
스키마 변경은 반드시 **Flyway 마이그레이션 파일**을 통해 관리합니다.

Flyway를 사용하는 근거:
- 스키마 변경 **버전 관리** → 누가 언제 무엇을 변경했는지 추적 가능
- dev, test, prod 모든 환경에 동일한 스키마 적용 보장
- **되돌리기(rollback)** 가능 (undo 파일 활용)

### 예시 (Example)

```sql
-- 참조: src/main/resources/db/migration/V1__create_users_table.sql

-- 사용자 테이블 초기 생성
CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 검색 성능을 위한 인덱스
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email    ON users (email);
```

**파일명 규칙:** `V{순서}__{설명}.sql`

| 파일명 | 의미 |
|---|---|
| `V1__create_users_table.sql` | 첫 번째 마이그레이션: users 테이블 생성 |
| `V2__add_age_column.sql` | 두 번째 마이그레이션: age 컬럼 추가 (예시) |

> ⚠️ **주의사항**
> - 마이그레이션 파일은 **한번 실행되면 수정할 수 없습니다.** Flyway는 파일의 체크섬을 저장하고, 기존 파일을 수정하면 앱 시작 시 오류가 발생합니다.
> - 기존 마이그레이션을 변경해야 하면 **새로운 버전의 파일**로 추가합니다 (예: `V2__alter_...`).

---

## 7. 엔티티 및 Repository

### 왜? (Why)

**엔티티(Entity)**는 DB 테이블과 1:1 매핑되는 클래스입니다.
**Repository**는 Spring Data JPA가 제공하는 인터페이스로, 메서드 이름만으로 **쿼리를 자동 생성**합니다.

### 예시 (Example)

```kotlin
// 참조: src/main/kotlin/com/example/starter/entity/User.kt

@Entity
@Table(name = "users")
class User {                          // ⬅️ 일반 class (data class 아님)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true, length = 50)
    var username: String = ""

    @Column(nullable = false, unique = true, length = 100)
    var email: String = ""

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
```

```kotlin
// 참조: src/main/kotlin/com/example/starter/repository/UserRepository.kt

interface UserRepository : JpaRepository<User, Long> {

    // Spring Data JPA가 메서드 이름으로 자동 쿼리 생성
    // → SELECT * FROM users WHERE username = ?
    fun findByUsername(username: String): User?

    // → SELECT * FROM users WHERE email = ?
    fun findByEmail(email: String): User?
}
```

> ⚠️ **주의사항**
> - 엔티티를 `data class`로 바꾸면 안 됩니다. `data class`는 `equals()`, `hashCode()`, `toString()`을 자동 생성하여 JPA의 프록시 메커니즘과 충돌할 수 있습니다.
> - `kotlin("plugin.jpa")`이 엔티티에 **no-arg constructor**를 자동 생성합니다. 이 플러그인이 빠지면 JPA가 엔티티를 생성할 수 없습니다.

---

## 8. DTO 패턴

### 왜? (Why)

엔티티를 직접 컨트롤러로 반환하면:
- DB 컬럼 변경 시 API 응답 형식도 바뀜 (강결합)
- 불필요한 필드가 외부로 노출됨 (보안 위험)

**DTO(Data Transfer Object)**로 Request와 Response를 분리하면 이 문제가 해결됩니다.

### 예시 (Example)

**생성 요청 DTO — 필드 모두 필수:**

```kotlin
// 참조: src/main/kotlin/com/example/starter/dto/UserCreateRequest.kt

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

**수정 요청 DTO — 부분 수정(PATCH 스타일)을 위해 모든 필드 nullable:**

```kotlin
// 참조: src/main/kotlin/com/example/starter/dto/UserUpdateRequest.kt

data class UserUpdateRequest(
    @field:Size(min = 2, max = 50, message = "username은 2~50자 사이여야 합니다")
    val username: String? = null,       // null이면 수정하지 않음

    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:Size(max = 100, message = "email은 100자 이하여야 합니다")
    val email: String? = null
)
```

**응답 DTO — `from()` 팩토리 메서드로 엔티티 → DTO 변환:**

```kotlin
// 참조: src/main/kotlin/com/example/starter/dto/UserResponse.kt

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                username = user.username,
                email = user.email,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
```

> ⚠️ **주의사항**
> - Jakarta Validation 어노테이션은 반드시 **`@field:` use-site target**을 붙여야 합니다.
>   - `@NotBlank` → `@field:NotBlank`
>   - `@Email` → `@field:Email`
>   - `@Size` → `@field:Size`
> - `@field:`를 빠뜨리면 Bean Validation이 트리거되지 않아 유효성 검사가 **무시**됩니다.

---

## 9. 컨트롤러 및 유효성 검사

### 왜? (Why)

컨트롤러는 HTTP 요청을 받아 Service에 위임하고, 적절한 **HTTP 상태 코드**로 응답합니다.
`@Valid` 어노테이션이 붙으면 Spring이 DTO의 Jakarta Validation 규칙을 **자동으로 검증**합니다.
Swagger 어노테이션(`@Operation`, `@ApiResponses`)은 API 문서를 자동 생성합니다.

### 예시 (Example)

```kotlin
// 참조: src/main/kotlin/com/example/starter/controller/UserController.kt

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
class UserController(private val userService: UserService) {

    // POST — 생성 (201 Created)
    @PostMapping
    @Operation(summary = "사용자 생성")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "사용자 생성 완료"),
        ApiResponse(responseCode = "400", description = "유효성 검사 실패")
    ])
    fun create(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
        val user = userService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    // DELETE — 삭제 (204 No Content)
    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 삭제")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "사용자 삭제 완료"),
        ApiResponse(responseCode = "404", description = "사용자 미발견")
    ])
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
```

**Swagger 설정 Bean:**

```kotlin
// 참조: src/main/kotlin/com/example/starter/config/SwaggerConfig.kt

@Configuration
class SwaggerConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        val info = Info()
            .title("beStarterKit API")
            .version("1.0.0")
            .description("Spring Boot + Kotlin + PostgreSQL 백엔드 스타터 키트")
            .contact(
                Contact()
                    .name("Dev Team")
                    .email("dev@example.com")
            )
        return OpenAPI().info(info)
    }
}
```

> ⚠️ **주의사항**
> - `HttpStatus.CREATED` 등의 정수값을 가져올 때 `.value()` (메서드 호출)을 사용해야 합니다. Kotlin 2.1에서 `.value` (프로퍼티)로 접근하면 컴파일 오류가 발생합니다.
> - Swagger 모델 임포트 패키지: `io.swagger.v3.oas.models.info.Info`, `io.swagger.v3.oas.models.info.Contact` (`.models.info` 하위)입니다.

---

## 10. 예외 처리

### 왜? (Why)

각 컨트롤러마다 try-catch를 작성하면 코드가 중복되고 응답 형식이 불일관합니다.
`@RestControllerAdvice`를 사용한 **전역 예외 핸들러**로 모든 예외를 한 곳에서 처리하고, **통일된 응답 형식**을 보장합니다.

### 예시 (Example)

```kotlin
// 참조: src/main/kotlin/com/example/starter/exception/GlobalExceptionHandler.kt

@RestControllerAdvice
class GlobalExceptionHandler {

    // Jakarta Validation 실패 시 → 400 + 필드별 오류 메시지
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "오류") }
        val body = mapOf(
            "status" to HttpStatus.BAD_REQUEST.value(),
            "message" to "유효성 검사 실패",
            "errors" to errors
        )
        return ResponseEntity.badRequest().body(body)
    }

    // RuntimeException → "not found" 포함 시 404, 그 외 500
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
}
```

**응답 형식 예시:**

유효성 검사 실패 시 (400):
```json
{
  "status": 400,
  "message": "유효성 검사 실패",
  "errors": {
    "username": "username은 필수입니다",
    "email": "유효한 이메일 형식이어야 합니다"
  }
}
```

리소스 미발견 시 (404):
```json
{
  "status": 404,
  "message": "User with id 99 not found"
}
```

> ⚠️ **주의사항**
> - 현재 `RuntimeException`의 메시지 문자열로 404와 500을 구분하는 방식은 **스타터 키트 단계의 단순한 접근법**입니다.
> - 실제 프로젝트로 성장하면 `NotFoundException`, `ConflictException` 등 **커스텀 예외 클래스**를 도입하여 타입 기반으로 상태 코드를 결정하는 것을 권장합니다.

---

## 11. 테스트 전략

### 왜? (Why)

테스트를 유형별로 나누면 각 테스트의 **실행 범위와 속도가 달라져** 효율적으로 사용할 수 있습니다.

| 테스트 유형 | 어노테이션 | 범위 | 장점 |
|---|---|---|---|
| **컨트롤러 테스트** | `@WebMvcTest` | Controller 레이어만 로드 | 빠르고 가벼운 단위 테스트 |
| **통합 테스트** | `@SpringBootTest` | 전체 ApplicationContext 로드 | 실제 앱과 동일한 환경 검증 |

### 예시 (Example)

**컨트롤러 테스트 — MockMvc + MockBean:**

```kotlin
// 참조: src/test/kotlin/com/example/starter/controller/UserControllerTest.kt

@WebMvcTest(UserController::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean                                   // Service를 Mock으로 주입
    private lateinit var userService: UserService

    @Test
    fun postUsers_유효한요청시_201반환() {
        val request = UserCreateRequest(username = "testuser", email = "test@example.com")
        val mockUser = User().apply {
            id = 1L
            username = "testuser"
            email = "test@example.com"
        }

        Mockito.`when`(userService.create(request)).thenReturn(mockUser)  // ⬅️ 백틱 필수

        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.username").value("testuser"))
    }

    @Test
    fun postUsers_유효성검사실패시_400반환() {
        val invalidRequest = mapOf(
            "username" to "",        // @NotBlank 위반
            "email" to "invalid"    // @Email 위반
        )

        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isMap())
    }
}
```

**통합 테스트 — ApplicationContext 로드 확인:**

```kotlin
// 참조: src/test/kotlin/com/example/starter/ApplicationTests.kt

@SpringBootTest
@ActiveProfiles("test")         // H2 in-memory DB 사용
class ApplicationTests {

    @Test
    fun contextLoads() {
        // Spring ApplicationContext가 정상 로드되는지 검증
    }
}
```

> ⚠️ **주의사항**
> - `Mockito.when()`은 Kotlin 키워드 `when`과 충돌합니다. 백틱으로 감싸야 합니다: `` Mockito.`when`(...) ``
> - `@MockBean` 임포트: `org.springframework.boot.test.mock.mockito.MockBean` (Spring Boot 3.4.x)
> - 테스트 메서드 이름에 특수 문자(`/`, `>` 등)는 사용할 수 없습니다. 백틱 함수명은 문자와 공백만 사용합니다.

---

## 12. 로컬 개발 환경 세팅

### 왜? (Why)

로컬 개발에는 실제 PostgreSQL이 필요하지만, 매번 수동으로 설치하고 관리하면 불편합니다.
**Docker Compose**로 DB 컨테이너를 선언적으로 관리하면 한 명령으로 환경을 시작·정지할 수 있습니다.

### 예시 (Example)

```yaml
# 참조: docker-compose.yml

services:
  postgres:
    image: postgres:16-alpine
    container_name: starter_postgres
    environment:
      POSTGRES_USER: starter_user
      POSTGRES_PASSWORD: starter_pass
      POSTGRES_DB: starter_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data    # 데이터 퍼시스턴스
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U starter_user -d starter_dev"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

**단계별 실행:**

```bash
# 1단계: PostgreSQL 컨테이너 시작
docker compose up -d

# 2단계: 컨테이너 상태 및 healthcheck 확인
docker compose ps
# STATUS 열이 "healthy"가 될 때까지 대기

# 3단계: 앱 시작 (dev 프로필)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 접근 가능한 URL
# Swagger UI:  http://localhost:8080/swagger-ui/index.html
# Health:      http://localhost:8080/actuator/health
```

> ⚠️ **주의사항**
> - 컨테이너 상태가 `healthy`가 아닌 상태에서 앱을 시작하면 DB 연결 실패가 발생할 수 있습니다. `docker compose ps`로 확인한 후 앱을 시작합니다.
> - `pgdata` volume은 컨테이너를 삭제해도 데이터가 남습니다. 완전한 초기화가 필요하면 `docker compose down -v`를 사용합니다.

---

## 13. 새 프로젝트 시작 체크리스트

아래 체크리스트를 순서대로 완료하면 Starter Kit과 동일한 구조의 프로젝트가 준비됩니다.

- [ ] `build.gradle.kts` — 플러그인, 의존성, KotlinCompile 옵션 설정
- [ ] `application.yml` (공통) — 포트, JPA, Flyway, Actuator 설정
- [ ] `application-dev.yml` — docker DB 연결, show-sql 활성화
- [ ] `application-prod.yml` — 환경변수 기반 DB 연결, Swagger 비활성화
- [ ] `application-test.yml` — H2 in-memory (`MODE=PostgreSQL`), Flyway 활성화
- [ ] `docker-compose.yml` — PostgreSQL 컨테이너 + healthcheck 정의
- [ ] 첫 번째 Flyway 마이그레이션 파일 (`V1__...`) 작성
- [ ] Entity 클래스 생성 (일반 `class`, `data class` 아님)
- [ ] Repository 인터페이스 생성 (`JpaRepository` 확장)
- [ ] Request DTO 생성 (`@field:` use-site target 적용)
- [ ] Response DTO 생성 (`from()` 팩토리 메서드 포함)
- [ ] Service 클래스 생성 (`@Transactional` 경계 설정)
- [ ] Controller 클래스 생성 (`@Valid`, Swagger 어노테이션)
- [ ] `GlobalExceptionHandler` 생성 (`@RestControllerAdvice`)
- [ ] `SwaggerConfig` Bean 정의
- [ ] `ApplicationTests` (통합 테스트) 작성 및 실행 확인
- [ ] Controller 테스트 (`@WebMvcTest`) 작성 및 실행 확인
- [ ] `./gradlew build` 최종 빌드 성공 확인
