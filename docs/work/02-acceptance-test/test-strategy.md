# 인수 테스트 전략

[`docs/user-stories.md`](../../user-stories.md)의 인수 조건을 자동 인수 테스트로 어떻게 옮겼는지 정리한 문서다. 이번 대상은 7-1~7-3, 15-1, 16-1~16-3이다.

- 작성일: 2026-11-22
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

---

## 1. 목표

- 인수 테스트는 지금 동작을 그대로 고정한다.
- 버그처럼 보이는 동작도 지금 동작이면 그대로 검증한다. 예: 16-2는 포인트가 부족하면 500이 오고, 재고는 줄어든 채로 남는다(open-questions 3번).
- 프로덕션 코드(`src/main`)는 고치지 않는다.

## 2. 도구

| 도구 | 쓰는 곳 |
|---|---|
| Cucumber (cucumber-bom 7.20.1) | 시나리오를 `src/test/resources/features/*.feature`에 Given/When/Then으로 쓴다. 단계 문장은 user-stories의 인수 조건과 같다 |
| RestAssured (버전은 Spring Boot가 관리) | HTTP 요청을 보내고 응답을 검증한다 |
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | 실제 서버를 임의 포트로 띄운다. `@LocalServerPort`로 받은 포트로 요청한다 |

테스트 코드는 `src/test/java/gift/acceptance`에 있다.

## 3. 테스트 데이터: 매 시나리오 전에 초기 데이터로 되돌린다

시나리오가 번호를 직접 쓴다(옵션 1, 위시 3, 상품 6개 → 7개). 그래서 데이터뿐 아니라 자동 증가 번호까지 처음으로 되돌려야 한다.

**방법**: 매 시나리오 전에 Flyway `clean()` 다음 `migrate()`를 실행한다(`DatabaseHook`).
- 테스트 설정(`src/test/resources/application-test.properties`)에서 `spring.flyway.clean-disabled=false`로 허용한다.
- 앱과 같은 V1(테이블)과 V2(초기 데이터)를 다시 실행한다. 그래서 초기 데이터와 번호가 실제 앱을 처음 켰을 때와 같다.
- 테이블 목록을 테스트 코드에서 따로 관리하지 않는다. 마이그레이션이 늘어도 그대로 따라간다.

**쓰지 않은 방법**

| 방법 | 쓰지 않은 이유 |
|---|---|
| TRUNCATE + 번호 초기화 + V2 다시 실행 | 더 빠르지만, H2 전용 SQL과 테이블 목록을 테스트 코드가 직접 관리해야 한다 |
| 매 시나리오 `@DirtiesContext` | 시나리오마다 서버를 다시 띄워서 매우 느리다 |
| `@Transactional` 롤백 | RANDOM_PORT에서는 요청이 테스트와 다른 스레드에서 처리돼 롤백되지 않는다 |

**실행 순서와 상관없는지 확인하는 방법**
- 16-1과 16-2는 같은 옵션 1(재고 10)을 쓰고, 둘 다 재고 10에서 시작한다고 확인한다.
- 7-1과 7-2는 둘 다 상품 수 6에서 시작한다고 확인한다.
- 이 두 쌍이 한 번의 실행에서 모두 통과하면 초기화가 동작한다는 뜻이다.

## 4. 검증 방법: 포인트만 DB, 나머지는 API

| 값 | 확인 방법 |
|---|---|
| 상품, 상품 수 | `GET /api/products/{id}`, `GET /api/products`의 `totalElements` |
| 재고 | `GET /api/products/{productId}/options` |
| 위시 | `GET /api/wishes` (회원의 로그인 토큰) |
| 주문 내역 | `GET /api/orders`의 `totalElements` (회원의 로그인 토큰) |
| **포인트** | **DB**: `select point from member where email = ?` |

**포인트만 DB에서 확인하는 이유**
- 회원이 자기 포인트를 조회하는 API가 없다(open-questions 4번).
- 미션 규칙상 API로 확인할 수 없는 값은 DB에서 확인해도 된다.

**관리자 화면으로 포인트를 확인하지 않는 이유**
- 관리자 화면(`/admin/members`)에서도 포인트를 볼 수 있다.
- 하지만 관리자 화면에 인증이 없는 것은 확인 필요 목록 13번, 22번에 올라 있다.
- 나중에 관리자 화면에 인증이 붙으면, 포인트를 보려고 관리자 화면을 쓰는 주문 테스트까지 깨질 수 있다.

**비용**
- 포인트 확인은 테이블과 컬럼 이름(`member.point`)에 묶인다.
- DB를 읽는 곳은 `MemberSteps`의 포인트 확인 단계 한 곳뿐이다. 포인트 저장 방식이 바뀌거나 회원 포인트 API가 생기면 그 한 곳만 고치면 된다.

## 5. 주문 알림: 외부 호출이 없다 (코드로만 판단)

- 주문 알림은 회원에게 카카오 액세스 토큰이 있을 때만 보낸다(`OrderController.java:104-113`). 없으면 카카오로 요청을 보내지 않는다.
- 초기 데이터의 회원은 카카오 액세스 토큰이 비어 있다. V2의 회원 insert에 이 값이 없다.
- 카카오 액세스 토큰은 카카오 로그인 콜백(`KakaoAuthController.java:63`)에서만 저장된다. 테스트는 이메일 로그인(`POST /api/members/login`)만 쓴다.
- 따라서 이번 시나리오에서는 카카오로 나가는 호출이 없다.

## 6. 실행

```
./gradlew test --tests 'gift.acceptance.CucumberTest'
```

- 환경 변수는 필요 없다. `JWT_SECRET`은 예시 기본값이 있다(open-questions 23번).
- 결과 보고서는 `build/reports/tests/test/index.html`에 생긴다.
