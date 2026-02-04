# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 빌드 및 실행 명령

```bash
# 빌드 및 테스트
./gradlew build

# 테스트만 실행
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.example.starter.controller.UserControllerTest"

# 특정 테스트 메서드만 실행
./gradlew test --tests "com.example.starter.controller.UserControllerTest.postUsers_유효한요청시_201반환"

# dev 프로필로 앱 시작 (docker postgres 필요)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Kotlin 컴파일만 실행 (빠른 컴파일 검증)
./gradlew compileKotlin

# 테스트 컴파일만 실행
./gradlew compileTestKotlin
```

## 로컬 개발 환경 세팅

```bash
# PostgreSQL 컨테이너 시작
docker compose up -d

# 컨테이너 상태 확인
docker compose ps
```

앱 시작 후 접근 가능한 URL:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- API Docs (JSON): `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

---

## 아키텍처 및 구조

**계층형 아키텍처**: Controller → Service → Repository → Entity

모든 소스는 `src/main/kotlin/com/example/starter/` 하위에 역할별 패키지로 분리되어 있습니다.

| 패키지 | 역할 |
|---|---|
| `controller/` | REST 엔드포인트, `@Valid` 유효성 검사 트리거, HTTP 응답 코드 관리 |
| `service/` | 비즈니스 로직, `@Transactional` 경계 관리 |
| `repository/` | Spring Data JPA 인터페이스 |
| `entity/` | JPA 엔티티 (일반 class, data class 아님 — kotlin plugin.jpa가 no-arg constructor 생성) |
| `dto/` | Request/Response DTO 분리. `UserResponse`에 `from(entity)` 팩토리 메서드 |
| `exception/` | `@RestControllerAdvice` 전역 예외 핸들러 |
| `config/` | Swagger OpenAPI Bean 정의 |

## 프로필별 환경

| 프로필 | DB | Swagger | 사용 시점 |
|---|---|---|---|
| `dev` | docker-compose PostgreSQL (`localhost:5432/starter_dev`) | 활성 | 로컬 개발 |
| `prod` | 환경변수 (`PROD_DB_URL` 등) | 비활성 | 배포 환경 |
| `test` | H2 in-memory (`MODE=PostgreSQL`) | — | `./gradlew test` |

## DB 마이그레이션

스키마는 **Flyway**로 관리됩니다. `spring.jpa.hibernate.ddl-auto=none`이므로 테이블을 직접 생성하면 안 됩니다.

- 마이그레이션 파일 위치: `src/main/resources/db/migration/`
- 파일명 규칙: `V{순서}__{설명}.sql` (예: `V1__create_users_table.sql`)
- 테스트 프로필에서도 Flyway가 활성화되어 H2에 동일한 스키마를 적용합니다.

## Kotlin + Spring 주의사항 (빌드 시 자주 발생하는 오류)

- **Jakarta Validation**: Kotlin data class 생성자 파라미터에 반드시 `@field:` use-site target 사용. 예: `@field:NotBlank`, `@field:Email`. 이렇지 않으면 Bean Validation이 트리거되지 않음.
- **HttpStatus 정수값**: `HttpStatus.BAD_REQUEST.value()` (메서드 호출). Kotlin 2.1에서 `.value` 프로퍼티로 접근하면 컴파일 오류.
- **SpringApplication.run()**: `Array<String>` 전달 시 spread operator 필요 → `*args`.
- **Mockito `when`**: Kotlin 키워드이므로 백틱 필요 → `` Mockito.`when`(...) ``.
- **Swagger 모델 임포트**: `io.swagger.v3.oas.models.info.Info` / `Contact` (`.models.info` 하위 패키지).
- **MockBean 임포트**: `org.springframework.boot.test.mock.mockito.MockBean` (Spring Boot 3.4.x).
- **테스트 메서드 이름**: Kotlin 백틱 함수명에 `/` 등 특수 문자 사용 불가.

## 테스트 작성 패턴

- **컨트롤러 테스트**: `@WebMvcTest(대상Controller::class)` + `@MockBean` Service 주입 + MockMvc 사용.
- **컨텍스트 로드 테스트**: `@SpringBootTest` + `@ActiveProfiles("test")`.
- 테스트 프로필은 H2를 사용하므로 PostgreSQL 컨테이너 불필요.

## 예외 응답 형식

```json
{
  "status": 400,
  "message": "유효성 검사 실패",
  "errors": { "username": "username은 필수입니다" }
}
```

- `MethodArgumentNotValidException` → 400 + 필드별 오류 메시지
- `RuntimeException` → 메시지에 "not found" 포함 시 404, 그 외 500
