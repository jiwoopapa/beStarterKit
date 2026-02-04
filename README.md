# beStarterKit

Spring Boot + Kotlin + PostgreSQL 백엔드 스타터 키트입니다.
사용자 관리(CRUD) API를 포함한 프로젝트 구조와 개발 환경을 미리 구성해 둔 템플릿입니다.

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Kotlin 2.1.0 |
| 프레임워크 | Spring Boot 3.4.1 |
| 빌드 | Gradle 8.12.1 (Kotlin DSL) |
| JDK | Java 23 |
| DB | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| 마이그레이션 | Flyway |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 테스트 DB | H2 (MODE=PostgreSQL) |

---

## 시작하기

### 사전 요구사항

- JDK 23 이상
- Docker & Docker Compose

### 1. PostgreSQL 컨테이너 시작

```bash
docker compose up -d
```

### 2. 앱 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 3. 확인

| 항목 | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| API Docs | http://localhost:8080/v3/api-docs |
| Health Check | http://localhost:8080/actuator/health |

---

## API 엔드포인트

기본 경로: `/api/users`

| 메서드 | 경로 | 설명 | 응답 코드 |
|---|---|---|---|
| GET | `/api/users` | 전체 사용자 조회 | 200 |
| GET | `/api/users/{id}` | 단일 사용자 조회 | 200, 404 |
| POST | `/api/users` | 사용자 생성 | 201, 400 |
| PUT | `/api/users/{id}` | 사용자 수정 (부분 수정 가능) | 200, 400, 404 |
| DELETE | `/api/users/{id}` | 사용자 삭제 | 204, 404 |

### 요청/응답 예시

**POST `/api/users`**

```json
// 요청
{
  "username": "johndoe",
  "email": "john@example.com"
}

// 응답 (201)
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "createdAt": "2026-02-05T10:00:00",
  "updatedAt": "2026-02-05T10:00:00"
}
```

**PUT `/api/users/{id}`** — 원하는 필드만 포함하면 부분 수정됩니다.

```json
{
  "email": "newemail@example.com"
}
```

---

## 프로젝트 구조

```
src/main/kotlin/com/example/starter/
├── Application.kt              # 진입점
├── config/                     # SwaggerConfig
├── controller/                 # REST 엔드포인트
├── service/                    # 비즈니스 로직, 트랜잭션 관리
├── repository/                 # Spring Data JPA 인터페이스
├── entity/                     # JPA 엔티티
├── dto/                        # 요청/응답 DTO
└── exception/                  # 전역 예외 핸들러
```

---

## 환경 프로필

| 프로필 | DB | Swagger | 활성화 방법 |
|---|---|---|---|
| `dev` | docker-compose PostgreSQL | 활성 | `--spring.profiles.active=dev` |
| `prod` | 환경변수 기반 | 비활성 | `--spring.profiles.active=prod` |
| `test` | H2 in-memory | — | `./gradlew test` 시 자동 적용 |

### Prod 환경변수

`.env.example`을 참고하여 실제 환경에서 다음 변수를 설정합니다.

```
PROD_DB_URL
PROD_DB_USERNAME
PROD_DB_PASSWORD
```

---

## DB 마이그레이션

스키마 변경은 Flyway 마이그레이션으로 관리합니다.

- 파일 위치: `src/main/resources/db/migration/`
- 파일명 규칙: `V{순서}__{설명}.sql`

```
V1__create_users_table.sql   # 기존
V2__add_nickname_column.sql  # 새 마이그레이션 추가 예시
```

---

## 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스만 실행
./gradlew test --tests "com.example.starter.controller.UserControllerTest"
```

테스트는 H2 in-memory DB를 사용하므로 PostgreSQL 컨테이너가 불필요합니다.
