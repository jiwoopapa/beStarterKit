# AI로 Starter Kit을 만드는 법 — 프롬프트 엔지니어링 가이드

> 기존 `Starter-Kit-Guide.md`의 동반 문서입니다.
> `Starter-Kit-Guide.md`는 **무엇을** 만들었는지(What)를 설명하고,
> 이 가이드는 **어떻게** AI에게 만들어달라고 했는지(How)를 설명합니다.

---

## 목차 (Table of Contents)

1. [개요](#1-개요)
2. [프롬프트 구성의 5가지 원칙](#2-프롬프트-구성의-5가지-원칙)
3. [Phase 0: CLAUDE.md — 프로젝트 규칙 정의](#3-phase-0-claudemd--프로젝트-규칙-정의)
4. [Phase 1: 빌드 및 인프라 세팅](#4-phase-1-빌드-및-인프라-세팅)
5. [Phase 2: DB 스키마 및 엔티티](#5-phase-2-db-스키마-및-엔티티)
6. [Phase 3: API 레이어 (DTO → Service → Controller)](#6-phase-3-api-레이어-dto--service--controller)
7. [Phase 4: 테스트 코드 생성](#7-phase-4-테스트-코드-생성)
8. [Phase 5: 반복 개선 루프](#8-phase-5-반복-개선-루프)
9. [실습: "Todo" 도메인으로 직접 따라하기](#9-실습-todo-도메인으로-직접-따라하기)
10. [프롬프트 템플릿 참고본](#10-프롬프트-템플릿-참고본)

---

## 1. 개요

### 왜? (Why)

AI 코딩 어시스턴트(Claude Code 등)는 코드 생성의 속도를 크게 올릴 수 있습니다.
그러나 **프롬프트를 어떻게 작성하느냐**에 따라 출력의 품질이 크게 달라집니다.
특히 Kotlin + Spring Boot 조합에서는 AI가 자주 실수하는 패턴이 있고,
이를 프롬프트 단계에서 미리 방지하면 반복적인 빌드 실패와 수정 사이클을 줄일 수 있습니다.

이 가이드는 `beStarterKit` 프로젝트를 실제로 생성하는 과정에서 사용한 프롬프트 전략과 예시를 정리한 것입니다.

### 예시 (Example)

두 문서의 역할 분리:

| 문서 | 질문 | 내용 |
|---|---|---|
| `Starter-Kit-Guide.md` | What — 무엇을 만들었는가? | 각 파일의 코드와 기술적 근거 |
| 이 가이드 (현재) | How — 어떻게 AI에게 했는가? | 프롬프트 작성법과 반복·검증 전략 |

**진행 순서 (Phase):**

```
Phase 0: CLAUDE.md 작성   → AI에게 프로젝트 규칙 정의 (가장 먼저)
Phase 1: 빌드 및 인프라    → build.gradle.kts, docker-compose.yml
Phase 2: DB 및 엔티티     → 마이그레이션 SQL, Entity, Repository
Phase 3: API 레이어       → DTO, Service, Controller
Phase 4: 테스트           → 컨트롤러 테스트, 통합 테스트
Phase 5: 반복 개선        → 빌드 실패 시 프롬프트 수정 전략
```

각 Phase는 이전 Phase의 결과물 위에 의존합니다. 순서를 바꾸면 컴파일이 실패합니다.

---

## 2. 프롬프트 구성의 5가지 원칙

### 왜? (Why)

좋은 프롬프트는 단순히 "무엇을 만들어달라는지"만 말하는 것이 아닙니다.
**컨텍스트, 구조, 제약조건, 단계, 검증** 5가지 요소를 포함하면
AI의 출력이 실제로 빌드·테스트 가능한 코드가 됩니다.

### 예시 (Example)

#### 원칙 1: 컨텍스트 제공 — 배경과 환경을 먼저 설명

AI는 프로젝트의 기술 스택과 버전을 모르는 상태입니다. 구체적인 정보를 먼저 제공해야 합니다.

```prompt
❌ Bad
Spring Boot 프로젝트 빌드 파일 만들어주세요.
```

```prompt
✅ Good
Spring Boot 3.4.1 + Kotlin 2.1.0 프로젝트의 build.gradle.kts를 생성해주세요.
Java toolchain 버전은 23입니다.
의존성: spring-boot-starter-web, spring-boot-starter-data-jpa, ...
```

#### 원칙 2: 구조 지정 — 파일명, 패키지, 클래스명을 명시

```prompt
❌ Bad
엔티티와 리포지토리 만들어주세요.
```

```prompt
✅ Good
다음 SQL을 기반으로 두 파일을 생성해주세요:
- Entity: com.example.starter.entity.User (User.kt)
- Repository: com.example.starter.repository.UserRepository (UserRepository.kt)
```

#### 원칙 3: 제약조건 명시 — AI가 자주 실수하는 포인트를 미리 지정

이것이 가장 중요한 원칙입니다. beStarterKit 개발 중 AI가 반복적으로 실수한 패턴을 직접 프롬프트에 넣습니다.

```prompt
❌ Bad
Kotlin DTO를 만들어주세요. Jakarta Validation 사용.
```

```prompt
✅ Good
Kotlin data class DTO를 생성해주세요.
Jakarta Validation 어노테이션은 반드시 @field: use-site target을 사용합니다:
  @NotBlank → @field:NotBlank
  @Email   → @field:Email
  @Size    → @field:Size
이를 빠뜨리면 Bean Validation이 트리거되지 않습니다.
```

#### 원칙 4: 단계 분할 — 전체를 한 번에 생성하지 않음

큰 작업을 한 프롬프트로 요청하면 출력이 불완전하거나 빠진 부분이 생깁니다.

```prompt
❌ Bad
Spring Boot Starter Kit 전체를 만들어주세요.
Entity, DTO, Controller, 테스트 포함.
```

```prompt
✅ Good
Step 1로 먼저 build.gradle.kts만 생성합니다.
빌드 검증(./gradlew compileKotlin) 후에
Step 2로 마이그레이션 파일을 생성하겠습니다.
```

#### 원칙 5: 검증 요청 — 출력 후 검증할 방법을 프롬프트에 포함

```prompt
❌ Bad
Controller 테스트 만들어주세요.
```

```prompt
✅ Good
UserController의 POST 엔드포인트 테스트를 생성해주세요.
생성 후 다음 명령으로 검증할 수 있도록 작성합니다:
  ./gradlew test --tests "com.example.starter.controller.UserControllerTest"
테스트가 실제로 통과하는 형태로 작성합니다.
```

> ⚠️ **주의사항**
> - 5가지 원칙이 모두 포함될 때 출력의 정확도가 가장 높습니다.
> - 원칙 3(제약조건)은 한 번 실수를 겪은 후에도 매번 반복 적용해야 합니다. AI는 이전 대화의 실수를 자동으로 기억하지 않습니다.

---

## 3. Phase 0: CLAUDE.md — 프로젝트 규칙 정의

### 왜? (Why)

`CLAUDE.md`는 Claude Code가 프로젝트를 작업할 때 **항상 참조하는 규칙 파일**입니다.
여기에 프로젝트의 빌드 명령, 아키텍처 규칙, 주의사항을 정의하면
매번 프롬프트마다 같은 정보를 반복하지 않아도 됩니다.

이것이 **Phase 0** — 모든 작업 이전에 해야 할 단계입니다.

### 예시 (Example)

CLAUDE.md를 생성하기 위한 프롬프트:

```prompt
우리 팀의 백엔드 프로젝트용 CLAUDE.md를 작성해주세요.
다음 정보를 기반으로 구성합니다:

## 기술 스택
- Spring Boot 3.4.1 + Kotlin 2.1.0
- PostgreSQL (dev), H2 (test), Flyway 마이그레이션
- Swagger (SpringDoc OpenAPI)

## 빌드 및 실행 명령
- 빌드: ./gradlew build
- 테스트: ./gradlew test
- dev 프로필 실행: ./gradlew bootRun --args='--spring.profiles.active=dev'
- 컴파일만: ./gradlew compileKotlin

## 아키텍처
- 계층형: Controller → Service → Repository → Entity
- 패키지: controller/, service/, repository/, entity/, dto/, exception/, config/

## 반드시 포함할 주의사항 (AI가 자주 실수하는 포인트)
- Jakarta Validation: @field: use-site target 필수
    (@field:NotBlank, @field:Email, @field:Size)
- HttpStatus 정수값: .value() 메서드 호출
    (Kotlin 2.1에서 .value 프로퍼티 불가)
- Mockito when: 백틱 필요 → Mockito.`when`(...)
- Entity는 일반 class (data class 아님)
- SpringApplication.run(): *args spread operator 필수
- Swagger 모델 임포트: io.swagger.v3.oas.models.info.Info 패키지
- MockBean 임포트: org.springframework.boot.test.mock.mockito.MockBean

## 테스트 패턴
- 컨트롤러: @WebMvcTest + @MockBean + MockMvc
- 통합: @SpringBootTest + @ActiveProfiles("test")
- 테스트 프로필은 H2 사용 (docker 불필요)
```

**beStarterKit의 CLAUDE.md 각 섹션이 절약하는 반복 작업:**

| CLAUDE.md 섹션 | 역할 | 절약 효과 |
|---|---|---|
| 빌드 및 실행 명령 | 빌드·검증 명령을 AI가 자동 참조 | 매번 명령을 프롬프트에 적기 불필요 |
| 아키텍처 및 구조 | 패키지·파일 배치 규칙 정의 | "어디에 만들어야 하는지" 반복 불필요 |
| Kotlin + Spring 주의사항 | 제약조건 항상 참조 | 원칙 3의 제약조건 자동 적용 |
| 테스트 작성 패턴 | 테스트 구조 규칙 정의 | 테스트 프롬프트가 짧아짐 |

> ⚠️ **주의사항**
> - `CLAUDE.md`는 프로젝트 루트에 위치해야 합니다. 다른 경로에 있으면 자동 참조되지 않습니다.
> - "주의사항" 섹션이 빠지면 AI가 반복적으로 같은 실수를 합니다. beStarterKit에서 가장 많이 경험한 문제입니다.

---

## 4. Phase 1: 빌드 및 인프라 세팅

### 왜? (Why)

빌드 파일과 인프라 설정은 프로젝트의 **기본 토대**입니다.
여기서 실수하면 이후 모든 단계에서 빌드가 실패하므로,
제약조건을 **가장 신중하게** 프롬프트에 넣어야 하는 단계입니다.

> 참조: `Starter-Kit-Guide.md` — 4. 빌드 파일 세팅 / 12. 로컬 개발 환경 세팅

### 예시 (Example)

**build.gradle.kts 생성 프롬프트:**

```prompt
Spring Boot 3.4.1 + Kotlin 2.1.0 프로젝트의 build.gradle.kts를 생성해주세요.
group = "com.example", version = "0.0.1-SNAPSHOT"

플러그인:
- kotlin("jvm") version "2.1.0"
- kotlin("plugin.spring") version "2.1.0"   // Spring 어노테이션 클래스 open 처리
- kotlin("plugin.jpa") version "2.1.0"      // Entity no-arg constructor 생성
- id("org.springframework.boot") version "3.4.1"
- id("io.spring.dependency-management") version "1.1.7"

의존성:
- implementation: spring-boot-starter-web, spring-boot-starter-data-jpa,
  spring-boot-starter-validation, springdoc-openapi-starter-webmvc-ui:2.3.0,
  flyway-core, flyway-database-postgresql,
  jackson-module-kotlin, kotlin-reflect
- runtimeOnly: postgresql
- testImplementation: spring-boot-starter-test
- testRuntimeOnly: h2

반드시 준수할 주의사항 (아래를 그대로 적용):
1. KotlinCompile은 전체 패키지명 사용:
   tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
2. jvmTarget은 enum 타입 (문자열 아님):
   jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23
3. 테스트 타스크에 <Test> 타입 파라미터 필수:
   tasks.named<Test>("test") { useJUnitPlatform() }
4. compilerOptions에 추가:
   freeCompilerArgs.add("-Xjsr305=strict")
   javaParameters = true
5. Java toolchain: languageVersion = JavaLanguageVersion.of(23)
```

검증 명령: `./gradlew compileKotlin`

---

**docker-compose.yml 생성 프롬프트:**

```prompt
개발용 PostgreSQL DB의 docker-compose.yml을 생성해주세요.

요구사항:
- image: postgres:16-alpine
- container_name: starter_postgres
- DB 이름: starter_dev
- 사용자 / 비밀번호: starter_user / starter_pass
- 호스트 포트 매핑: 5432:5432
- healthcheck: pg_isready -U starter_user -d starter_dev
  (interval: 5s, timeout: 5s, retries: 5)
- 볼륨: pgdata (named volume) → /var/lib/postgresql/data
```

검증 명령: `docker compose up -d` → `docker compose ps` (STATUS: healthy 확인)

> ⚠️ **주의사항**
> - `build.gradle.kts` 생성 후 반드시 `./gradlew compileKotlin`으로 검증합니다.
> - KotlinCompile 패키지명과 JvmTarget enum이 **가장 자주** 실수되는 포인트입니다. 프롬프트에 명시적으로 넣어야 합니다.
> - docker-compose.yml에 healthcheck가 빠지면 앱 시작 시 DB 연결 실패가 간歇적으로 발생합니다.

---

## 5. Phase 2: DB 스키마 및 엔티티

### 왜? (Why)

**SQL 마이그레이션을 먼저 정의하고**, 그 후 Entity를 유도하는 순서가 올바릅니다.
Entity를 먼저 만들고 테이블을 맞추려면 실제 스키마와 불일치가 발생할 수 있습니다.
3단계로 나누어 각각 컴파일 검증하면 오류가 빨리 잡힙니다.

> 참조: `Starter-Kit-Guide.md` — 6. DB 마이그레이션 / 7. 엔티티 및 Repository

### 예시 (Example)

**Step 1 — 마이그레이션 SQL 생성:**

```prompt
Flyway 마이그레이션 파일을 생성해주세요.

파일명: V1__create_users_table.sql
경로: src/main/resources/db/migration/

테이블: users
컬럼:
- id         BIGSERIAL    PRIMARY KEY
- username   VARCHAR(50)  NOT NULL UNIQUE
- email      VARCHAR(100) NOT NULL UNIQUE
- created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
- updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP

인덱스:
- idx_users_username ON users (username)
- idx_users_email ON users (email)

파일명 규칙: V{순서}__{설명}.sql (밑줄 2개)
```

검증: 파일 생성만으로 충분 (SQL은 컴파일 대상이 아님)

---

**Step 2 — Entity 클래스 생성 (SQL 기반 유도):**

```prompt
위의 V1__create_users_table.sql 스키마를 기반으로 JPA Entity 클래스를 생성해주세요.

패키지: com.example.starter.entity
파일: User.kt

반드시 준수할 주의사항:
1. 일반 class 사용 (data class 아님)
   - data class는 equals()/hashCode()/toString()을 자동 생성하여
     JPA 프록시와 충돌
2. kotlin("plugin.jpa")가 no-arg constructor를 자동 생성 → 수동 정의 불필요
3. @Entity + @Table(name = "users")
4. @Id + @GeneratedValue(strategy = GenerationType.IDENTITY)
5. snake_case 컬럼명 매핑: @Column(name = "created_at")
6. 필드명은 camelCase: createdAt, updatedAt
7. id 타입은 Long? (nullable) — 생성 시점까지 null
```

검증 명령: `./gradlew compileKotlin`

---

**Step 3 — Repository 인터페이스 생성:**

```prompt
User Entity에 대한 Spring Data JPA Repository 인터페이스를 생성해주세요.

패키지: com.example.starter.repository
파일: UserRepository.kt

확장: JpaRepository<User, Long>

커스텀 메서드 (Spring Data JPA가 이름만으로 쿼리 자동 생성):
- findByUsername(username: String): User?
- findByEmail(email: String): User?
```

검증 명령: `./gradlew compileKotlin`

> ⚠️ **주의사항**
> - Entity를 `data class`로 생성하면 JPA 프록시가 올바르게 동작하지 않습니다. AI가 **가장 자주** 실수하는 포인트입니다.
> - 마이그레이션 파일은 한 번 실행되면 수정 불가합니다. Flyway가 체크섬을 검증합니다. 스키마를 신중하게 설계한 후 생성합니다.

---

## 6. Phase 3: API 레이어 (DTO → Service → Controller)

### 왜? (Why)

**DTO → Service → Controller** 순서로 생성합니다.
Controller는 Service에, Service는 Repository에 의존하므로
아래 레이어부터 순차적으로 만들어야 각 단계에서 컴파일이 가능합니다.

> 참조: `Starter-Kit-Guide.md` — 8. DTO 패턴 / 9. 컨트롤러 및 유효성 검사

### 예시 (Example)

**Step 1 — DTO 세트 생성:**

```prompt
User 도메인의 DTO 클래스 3개를 생성해주세요. 각 클래스는 별도 파일로 합니다.

패키지: com.example.starter.dto

1. UserCreateRequest (생성 요청 — 모든 필드 필수)
   - username: String (@NotBlank, @Size min=2 max=50)
   - email: String (@NotBlank, @Email, @Size max=100)

2. UserUpdateRequest (수정 요청 — 부분 수정용)
   - username: String? = null (@Size min=2 max=50)
   - email: String? = null (@Email, @Size max=100)
   (null이면 해당 필드를 수정하지 않음)

3. UserResponse (응답)
   - id: Long, username: String, email: String
   - createdAt: LocalDateTime, updatedAt: LocalDateTime
   - companion object에 from(user: User): UserResponse 팩토리 메서드

반드시 준수할 주의사항:
- Jakarta Validation 어노테이션은 @field: use-site target 반드시 사용
    @NotBlank → @field:NotBlank
    @Email   → @field:Email
    @Size    → @field:Size
- @field:를 빠뜨리면 Bean Validation이 트리거되지 않음 (무소음 실패)
```

검증 명령: `./gradlew compileKotlin`

---

**Step 2 — Service 클래스 생성:**

```prompt
UserService 클래스를 생성해주세요.

패키지: com.example.starter.service
파일: UserService.kt
생성자 주입: UserRepository

메서드:
- findAll(): List<User>
    @Transactional(readOnly = true)
- findById(id: Long): User
    @Transactional(readOnly = true)
    없으면: throw RuntimeException("User with id $id not found")
- create(request: UserCreateRequest): User
    @Transactional
- update(id: Long, request: UserUpdateRequest): User
    @Transactional
    nullable 필드는 값이 있을 때만 수정
    updatedAt = LocalDateTime.now() 갱신
- delete(id: Long)
    @Transactional
    findById로 존재 확인 후 삭제
```

검증 명령: `./gradlew compileKotlin`

---

**Step 3 — Controller 클래스 생성:**

```prompt
UserController REST 컨트롤러를 생성해주세요.

패키지: com.example.starter.controller
파일: UserController.kt
생성자 주입: UserService
기본 경로: @RequestMapping("/api/users")
Swagger: @Tag(name = "User", description = "사용자 관리 API")

엔드포인트:
- GET   /api/users       → 200  List<UserResponse>
- GET   /api/users/{id}  → 200  UserResponse  /  404
- POST  /api/users       → 201  UserResponse  /  400  — @Valid @RequestBody
- PUT   /api/users/{id}  → 200  UserResponse  /  400, 404 — @Valid @RequestBody
- DELETE /api/users/{id}  → 204  /  404

반드시 준수할 주의사항:
- 각 엔드포인트에 @Operation(summary) + @ApiResponses 어노테이션
- 응답 타입: ResponseEntity<UserResponse>
- Swagger 어노테이션 임포트: io.swagger.v3.oas.annotations.* 패키지
```

검증 명령: `./gradlew compileKotlin`

> ⚠️ **주의사항**
> - DTO의 `@field:` 빠진 경우는 빌드 오류가 아니라 **런타임 무소음 실패**입니다. 테스트로 유효성 검사가 실제로 동작하는지 반드시 확인합니다.
> - `HttpStatus`의 정수값을 사용할 때는 `.value()` (메서드 호출)을 사용합니다. Kotlin 2.1에서 `.value` (프로퍼티)로 접근하면 컴파일 오류가 발생합니다.

---

## 7. Phase 4: 테스트 코드 생성

### 왜? (Why)

테스트 코드는 일반 코드보다 특수한 규칙과 제약조건이 많습니다.
Mockito의 `when` 키워드 충돌, MockBean의 특정 임포트 경로, 테스트명 규칙 등을
프롬프트에 명시적으로 넣지 않으면 AI가 반복적으로 실수합니다.

> 참조: `Starter-Kit-Guide.md` — 11. 테스트 전략

### 예시 (Example)

**컨트롤러 테스트 생성 프롬프트:**

```prompt
UserController의 POST 엔드포인트 테스트를 생성해주세요.

패키지: com.example.starter.controller
파일: UserControllerTest.kt

테스트 설정:
- @WebMvcTest(UserController::class)
- @Autowired MockMvc
- @Autowired ObjectMapper (요청 본문 직렬화용)
- @MockBean UserService

테스트 케이스 2개:
1. 유효한 요청 시 201 반환 검증
   - Mockito로 userService.create()의 반환값을 모킹
   - POST /api/users에 유효한 JSON 요청 전송
   - 상태 코드 201, 응답 본문의 id / username / email 검증 (jsonPath)

2. 유효성 검사 실패 시 400 반환 검증
   - username: "" (빈 문자열), email: "invalid" (이메일 형식 아님)
   - 상태 코드 400, 응답 본문에 errors 필드 존재 검증 (jsonPath)

반드시 준수할 주의사항:
- @MockBean 임포트: org.springframework.boot.test.mock.mockito.MockBean
  (Spring Boot 3.4.x 특정 경로)
- Mockito.when() → 백틱 필수: Mockito.`when`(...)
  (Kotlin의 when 키워드와 이름 충돌)
- 테스트 메서드 이름에 특수 문자(/, > 등) 사용 불가
  한글과 공백만 사용 가능 (예: postUsers_유효한요청시_201반환)
```

검증 명령: `./gradlew test --tests "com.example.starter.controller.UserControllerTest"`

---

**통합 테스트 생성 프롬프트:**

```prompt
ApplicationTests 통합 테스트를 생성해주세요.

패키지: com.example.starter
파일: ApplicationTests.kt

설정:
- @SpringBootTest
- @ActiveProfiles("test")
  임포트: org.springframework.test.context.ActiveProfiles
  (H2 in-memory DB 프로필 사용)

테스트:
- contextLoads() — Spring ApplicationContext가 정상 로드되는지 검증
```

검증 명령: `./gradlew test --tests "com.example.starter.ApplicationTests"`

> ⚠️ **주의사항**
> - `Mockito.when()`은 Kotlin의 `when` 키워드와 이름이 같아 백틱 없이는 컴파일 오류가 발생합니다.
> - `@MockBean`의 임포트 경로는 Spring Boot 버전에 따라 달라집니다. 3.4.x에서는 `org.springframework.boot.test.mock.mockito.MockBean`입니다.
> - 테스트 메서드 이름에 `/`를 쓰면 Gradle 테스트 필터(`--tests`)에서 문제가 생깁니다.

---

## 8. Phase 5: 반복 개선 루프

### 왜? (Why)

첫 번째 프롬프트로 완벽한 코드가 나오는 것은 드물습니다.
**생성 → 빌드 검증 → 오류 수정** 루프를 반복하는 것이 정상적인 프로세스입니다.

오류 수정 프롬프트에서는 **현재 코드와 기대하는 코드를 구체적으로** 보여주면
같은 실수가 반복되지 않습니다. 그리고 한 번 학습한 실수를 **MEMORY.md**에 기록하면
다음 프로젝트에서도 재사용할 수 있습니다.

### 예시 (Example)

실제로 beStarterKit 개발 중에 발생했던 오류와 수정 프롬프트 예시:

---

**오류 시나리오 1: KotlinCompile resolve 실패**

```
빌드 오류: Unresolved reference: KotlinCompile
발생 위치: build.gradle.kts
```

```prompt
build.gradle.kts에서 빌드 오류가 발생했습니다:
"Unresolved reference: KotlinCompile"

KotlinCompile을 전체 패키지명으로 변경해주세요:

현재:  tasks.withType<KotlinCompile>
수정:  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
```

---

**오류 시나리오 2: Bean Validation 무소음 실패**

```
빌드는 성공하지만 테스트에서 유효성 검사가 통과 → 빈 문자열이 저장됨
발생 위치: DTO 클래스
```

```prompt
Jakarta Validation이 트리거되지 않습니다.
DTO 클래스의 어노테이션에 @field: use-site target을 추가해주세요.

현재:
    @NotBlank(message = "username은 필수입니다")
    val username: String

수정:
    @field:NotBlank(message = "username은 필수입니다")
    val username: String

email과 @Size 어노테이션도 동일하게 @field: 접두사를 추가합니다.
```

---

**오류 시나리오 3: Mockito when 컴파일 오류**

```
빌드 오류: Expecting member declaration
발생 위치: 테스트 파일의 when() 호출
```

```prompt
테스트 파일에서 컴파일 오류가 발생했습니다.
Mockito.when()이 Kotlin의 when 키워드와 이름 충돌합니다.

현재:  Mockito.when(userService.create(request)).thenReturn(mockUser)
수정:  Mockito.`when`(userService.create(request)).thenReturn(mockUser)

when을 백틱으로 감싸주세요.
```

---

**학습한 실수를 MEMORY.md에 기록하는 프롬프트:**

```prompt
오늘 빌드 중 다음 오류를 경험했습니다.
이 내용을 MEMORY.md에 간결하게 기록해주세요
(다음 프로젝트에서도 재사용 가능한 형태로):

1. KotlinCompile 참조 실패 → 전체 패키지명 필수
2. jvmTarget에 문자열을 넣었을 때 타입 오류 → enum 타입 사용
3. @field: use-site target 빠지면 Bean Validation 무시됨
```

> ⚠️ **주의사항**
> - 오류 수정 프롬프트에는 **현재 코드와 수정된 코드**를 구체적으로 넣어야 합니다. "수정해주세요"만 하면 같은 실수가 반복됩니다.
> - MEMORY.md는 팀 전체가 공유할 학습 메모입니다. 간결하고 구체적으로 작성합니다.

---

## 9. 실습: "Todo" 도메인으로 직접 따라하기

### 왜? (Why)

앞의 Phase 0~5를 기존 User 도메인으로 설명했습니다.
이제 **Todo 도메인**을 직접 만들어보면 실제로 프롬프트를 작성하고 검증하는 경험을 할 수 있습니다.

beStarterKit 프로젝트에 Todo를 추가하므로:
- 기존 빌드 환경을 그대로 사용 (세팅 불필요)
- User 도메인의 패턴을 참조 가능 (검증이 쉬움)

> 실습 완료 후, 변경사항은 원복합니다: `git checkout .` + `git clean -fd`

### 예시 (Example)

**Todo 도메인 스키마 (실습 기준):**

| 컬럼 | 타입 | 조건 |
|---|---|---|
| id | BIGSERIAL | PRIMARY KEY |
| title | VARCHAR(100) | NOT NULL |
| completed | BOOLEAN | NOT NULL DEFAULT FALSE |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP |

---

**단계별 실습 체크리스트:**

- [ ] **Step 1 — 마이그레이션 파일 생성**
  - 프롬프트: 위의 Todo 스키마를 기반으로 `V2__create_todos_table.sql` 생성
  - 경로: `src/main/resources/db/migration/`
  - 인덱스: `idx_todos_title`, `idx_todos_completed`
  - 검증: `./gradlew compileKotlin` (파일 경로·형식 확인)

- [ ] **Step 2 — Entity + Repository 생성**
  - 프롬프트: `V2__create_todos_table.sql`을 참조하여 생성
  - Entity: `com.example.starter.entity.Todo` (일반 class, data class 아님)
  - Repository: `com.example.starter.repository.TodoRepository`
    - 커스텀 메서드: `findByTitle(title: String): Todo?`, `findByCompleted(completed: Boolean): List<Todo>`
  - 검증: `./gradlew compileKotlin`

- [ ] **Step 3 — DTO 세트 생성**
  - 프롬프트: 3개 DTO 생성
  - `TodoCreateRequest`: title (필수, 1~100자), completed (기본값 false)
  - `TodoUpdateRequest`: title, completed 모두 nullable (부분 수정)
  - `TodoResponse`: `from(todo: Todo)` 팩토리 메서드 포함
  - 제약조건: `@field:` use-site target 반드시 적용
  - 검증: `./gradlew compileKotlin`

- [ ] **Step 4 — Service 생성**
  - 프롬프트: `TodoService` 생성
  - CRUD + `findByCompleted(completed: Boolean): List<Todo>`
  - findById 시 `RuntimeException("Todo with id $id not found")`
  - `@Transactional` 경계 설정
  - 검증: `./gradlew compileKotlin`

- [ ] **Step 5 — Controller 생성**
  - 프롬프트: `TodoController` REST 컨트롤러 생성
  - 경로: `/api/todos`
  - 엔드포인트: GET(목록/단일), POST(201), PUT(200), DELETE(204)
  - `@Valid`, Swagger `@Operation`/`@ApiResponses`
  - 검증: `./gradlew compileKotlin`

- [ ] **Step 6 — 테스트 코드 생성**
  - 프롬프트: `TodoControllerTest` 생성
  - POST 유효한요청 → 201 검증, POST 유효성검사실패 → 400 검증
  - 제약조건: `@MockBean` 임포트, `Mockito.`when`()` 백틱, 테스트명 규칙
  - 검증: `./gradlew test --tests "com.example.starter.controller.TodoControllerTest"`

- [ ] **Step 7 — 전체 빌드·테스트 검증**
  - 명령: `./gradlew test`
  - User 테스트와 Todo 테스트 모두 통과 확인

- [ ] **Step 8 — 실습 완료 후 원복**
  - 명령: `git checkout .` → 추적된 파일 복원
  - 명령: `git clean -fd` → 추적되지 않는 파일(마이그레이션 등) 삭제

> ⚠️ **주의사항**
> - 각 Step 후 반드시 검증 명령을 실행합니다. 오류가 나오면 Phase 5의 반복 개선 루프를 따라 프롬프트를 수정합니다.
> - `V2__create_todos_table.sql`은 기존 `V1__`과 같은 디렉토리에 생성됩니다. 버전 번호 조정 시 주의합니다.

---

## 10. 프롬프트 템플릿 참고본

### 왜? (Why)

새 도메인을 추가할 때마다 처음부터 프롬프트를 작성하면 반복 작업입니다.
아래 템플릿에서 `{PLACEHOLDER}` 부분만 교체하면 바로 사용할 수 있는 프롬프트가 됩니다.
제약조건 섹션은 그대로 유지하는 것이 핵심입니다.

### 예시 (Example)

---

**템플릿 1: 마이그레이션 파일**

```prompt
Flyway 마이그레이션 파일을 생성해주세요.

파일명: V{VERSION}__{DESCRIPTION}.sql
경로: src/main/resources/db/migration/

테이블: {TABLE_NAME}
컬럼:
{COLUMN_DEFINITIONS}

인덱스:
{INDEX_DEFINITIONS}

파일명 규칙: V{순서}__{설명}.sql (밑줄 2개)
```

---

**템플릿 2: Entity + Repository**

```prompt
다음 마이그레이션 SQL을 기반으로 Entity와 Repository를 생성해주세요.

SQL: [V{VERSION}__{DESCRIPTION}.sql 내용 붙여넣기]

Entity:
- 패키지: com.example.starter.entity
- 클래스명: {ENTITY_NAME}
- 일반 class 사용 (data class 아님) — JPA 프록시 충돌 방지
- @Entity + @Table(name = "{TABLE_NAME}")
- @Id + @GeneratedValue(strategy = GenerationType.IDENTITY)
- snake_case → camelCase 매핑 (@Column(name = "..."))

Repository:
- 패키지: com.example.starter.repository
- 클래스명: {ENTITY_NAME}Repository
- JpaRepository<{ENTITY_NAME}, Long> 확장
- 커스텀 메서드: {CUSTOM_METHODS}
```

---

**템플릿 3: DTO 세트**

```prompt
{ENTITY_NAME} 도메인의 DTO 클래스 3개를 생성해주세요 (각 파일 분리).

패키지: com.example.starter.dto

1. {ENTITY_NAME}CreateRequest
   {CREATE_FIELDS}

2. {ENTITY_NAME}UpdateRequest (모든 필드 nullable, 부분 수정용)
   {UPDATE_FIELDS}

3. {ENTITY_NAME}Response
   {RESPONSE_FIELDS}
   + companion object에 from(entity: {ENTITY_NAME}): {ENTITY_NAME}Response 팩토리

반드시 준수:
- Jakarta Validation 어노테이션은 @field: use-site target 사용
    @NotBlank → @field:NotBlank
    @Email   → @field:Email
    @Size    → @field:Size
- @field:를 빠뜨리면 Bean Validation이 트리거되지 않음
```

---

**템플릿 4: Service**

```prompt
{ENTITY_NAME}Service 클래스를 생성해주세요.

패키지: com.example.starter.service
생성자 주입: {ENTITY_NAME}Repository

메서드:
- findAll(): List<{ENTITY_NAME}>
    @Transactional(readOnly = true)
- findById(id: Long): {ENTITY_NAME}
    @Transactional(readOnly = true)
    없으면: throw RuntimeException("{ENTITY_NAME} with id $id not found")
- create(request: {ENTITY_NAME}CreateRequest): {ENTITY_NAME}
    @Transactional
- update(id: Long, request: {ENTITY_NAME}UpdateRequest): {ENTITY_NAME}
    @Transactional — nullable 필드는 값 있을 때만 수정, updatedAt 갱신
- delete(id: Long)
    @Transactional — findById로 존재 확인 후 삭제
{EXTRA_METHODS}
```

---

**템플릿 5: Controller + 테스트**

```prompt
{ENTITY_NAME}Controller와 테스트를 생성해주세요.

[Controller]
- 패키지: com.example.starter.controller
- 경로: /api/{PLURAL_NAME}
- @Tag(name = "{ENTITY_NAME}", description = "{DESCRIPTION}")
- 엔드포인트: GET(목록/단일), POST(201), PUT(200), DELETE(204)
- @Valid, Swagger @Operation/@ApiResponses

[테스트]
- 클래스명: {ENTITY_NAME}ControllerTest
- @WebMvcTest({ENTITY_NAME}Controller::class)
- POST 유효한요청 → 201 검증
- POST 유효성검사실패 → 400 검증

반드시 준수:
- @MockBean 임포트: org.springframework.boot.test.mock.mockito.MockBean
- Mockito.`when`() — 백틱 필수 (Kotlin when 키워드 충돌)
- 테스트명 특수문자(/, >) 불가 — 공백·한글만 사용
```

> ⚠️ **주의사항**
> - 템플릿의 `{PLACEHOLDER}` 부분만 교체합니다. **제약조건 섹션은 반드시 그대로 유지**합니다.
> - 새 도메인 추가 시 마이그레이션 파일의 버전 번호(`V{VERSION}`)는 기존 파일 번호 + 1로 증가합니다.
> - 새로운 실수 패턴을 경험하면 템플릿의 제약조건에 추가하고, MEMORY.md에도 기록합니다.
