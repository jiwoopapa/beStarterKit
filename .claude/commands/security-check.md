# security-check

Spring Boot + Kotlin 프로젝트의 보안 취약점을 자동으로 분석하고 리포트를 생성합니다.

---

## 실행 단계

### 1. 코드 정적 분석

다음 보안 취약점을 체크합니다:

#### SQL Injection 검사
- `src/main/kotlin/**/repository/` 하위 모든 파일에서:
  - `@Query` 어노테이션에 문자열 연결(`+`, `${}`) 사용 여부
  - Native query에서 파라미터 바인딩 미사용
  - JPQL에서 동적 쿼리 생성 패턴
- **위험 패턴**: `@Query("... WHERE name = '" + variable + "'")`
- **안전 패턴**: `@Query("... WHERE name = :name")`

#### 민감 정보 노출 검사
- `src/main/resources/application*.yml` 파일에서:
  - 하드코딩된 비밀번호, API 키 탐지
  - `password:`, `secret:`, `key:`, `token:` 키워드 검색
  - 평문으로 저장된 credentials
- `src/main/kotlin/` 하위 모든 Kotlin 파일에서:
  - 하드코딩된 비밀번호 문자열 패턴
  - `val password = "..."`, `const val API_KEY = "..."` 패턴
- `.env` 파일이 `.gitignore`에 포함되었는지 확인

#### 데이터 검증 누락 검사
- `src/main/kotlin/**/controller/` 하위 모든 파일에서:
  - `@RequestBody` 사용 시 `@Valid` 어노테이션 누락
  - `@PathVariable`, `@RequestParam`에 대한 검증 누락
- `src/main/kotlin/**/dto/` 하위 Request DTO에서:
  - Jakarta Validation 어노테이션 누락 (`@NotBlank`, `@Email` 등)
  - Kotlin에서 `@field:` use-site target 미사용 (중요!)

#### XSS 방어 검사
- Controller Response에서:
  - 사용자 입력을 그대로 반환하는 패턴
  - HTML 이스케이프 누락
- DTO에 `@JsonRawValue` 사용 시 경고

#### 부적절한 에러 핸들링 검사
- `src/main/kotlin/**/exception/` 하위 파일에서:
  - 예외 메시지에 스택 트레이스 포함 여부
  - `e.printStackTrace()` 사용 여부
  - 민감한 정보(DB 스키마, 내부 경로) 노출 가능성
- `@RestControllerAdvice`에서 과도한 에러 정보 반환

#### JPA 보안 이슈 검사
- `src/main/kotlin/**/entity/` 하위 파일에서:
  - `@ToString` 사용 시 순환 참조 위험
  - `fetchType = LAZY` 미사용으로 인한 N+1 쿼리
- Repository에서:
  - 페이징 없는 `findAll()` 사용 (DoS 위험)
  - 대량 데이터 조회 시 limit 누락

#### CORS 설정 검사
- `src/main/kotlin/**/config/` 하위 파일에서:
  - `allowedOrigins = ["*"]` 사용 (프로덕션에서 위험)
  - `allowCredentials = true` + `allowedOrigins = ["*"]` 조합 (보안 위험)

#### 의존성 취약점 검사
- `build.gradle.kts`에서 Spring Boot 버전 확인
- 주요 라이브러리 버전이 최신인지 체크 (권장 사항)

---

### 2. 취약점 분류 및 리포트 생성

발견된 각 취약점을 다음과 같이 분류:

- **[HIGH]**: 즉시 수정 필요 (SQL Injection, 민감 정보 노출)
- **[MEDIUM]**: 가능한 빨리 수정 (검증 누락, 에러 핸들링)
- **[LOW]**: 개선 권장 (코드 품질, 성능)

각 항목마다 다음 정보 포함:
- 📍 파일 경로 및 라인 번호
- ❌ 문제가 되는 코드
- ✅ 수정 방법 또는 권장 사항

---

### 3. 출력 형식

```
🔒 보안 취약점 체크 결과
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[HIGH] SQL Injection 위험
  📍 src/main/kotlin/com/example/starter/repository/UserRepository.kt:15
  ❌ @Query("SELECT u FROM User u WHERE u.name = '" + name + "'")
  ✅ 수정: @Query("SELECT u FROM User u WHERE u.name = :name")

[MEDIUM] 데이터 검증 누락
  📍 src/main/kotlin/com/example/starter/controller/ProductController.kt:23
  ❌ fun createProduct(@RequestBody request: ProductRequest)
  ✅ 수정: fun createProduct(@Valid @RequestBody request: ProductRequest)

[MEDIUM] 민감 정보 노출
  📍 src/main/resources/application-dev.yml:8
  ❌ password: mySecretPassword123
  ✅ 환경변수 사용 권장: ${POSTGRES_PASSWORD}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✨ 총 3개의 취약점 발견
   High: 1개 | Medium: 2개 | Low: 0개
```

---

### 4. 추가 권장 사항

마지막에 다음 항목들을 체크리스트로 제공:

- [ ] Spring Security 의존성 추가 여부
- [ ] HTTPS 적용 여부 (프로덕션)
- [ ] Rate limiting 설정
- [ ] Actuator 엔드포인트 보안 설정
- [ ] 로깅에 민감 정보 제외 설정
- [ ] CSRF 토큰 적용 (세션 기반 인증 사용 시)
- [ ] API 버전 관리 전략

---

## 실행 예시

1. 전체 프로젝트 스캔 실행
2. 각 카테고리별로 파일 검색 및 패턴 매칭
3. 발견된 취약점을 우선순위별로 정렬
4. 콘솔에 컬러풀한 리포트 출력
5. 선택적으로 `docs/security-report-YYYYMMDD.md` 파일로 저장

---

## 주의사항

- False positive 가능성이 있으므로 각 항목을 수동으로 검토 필요
- 프로젝트 특성에 따라 일부 경고는 무시해도 됨
- 정적 분석만으로는 모든 보안 이슈를 찾을 수 없음
- 실제 침투 테스트와 코드 리뷰를 병행해야 함
