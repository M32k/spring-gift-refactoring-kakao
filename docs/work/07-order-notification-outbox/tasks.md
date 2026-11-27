# 주문 알림 아웃박스 작업 목록

[`tech-spec.md`](tech-spec.md)를 바탕으로 한 작업 목록이다. 구조 변경(C0~T1)을 먼저 끝내고 작동 변경(B1, B2)을 한다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. B1, B2는 고친 테스트, 바꾼 코드, 실패 출력(이 파일)을 한 커밋에 담는다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 발송 요청 테스트(C0 뒤부터): `./gradlew test --tests 'gift.order.KakaoMessageClientTest'`
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
  - ArchUnit: `./gradlew test --tests 'gift.architecture.LayerArchitectureTest'`
- git 명령은 사용자가 실행한다.
- 응답 코드는 바꾸지 않는다. 알림이 실패해도 주문은 201이다.
- 새 클래스 이름은 `…Service`로 끝나지 않게 한다(tech-spec 10장).

---

### C0. 카카오 발송 요청을 지금 그대로 고정한다 (테스트만)

- 이유: 초기 데이터 회원에게는 카카오 액세스 토큰이 없어서 인수 테스트가 알림 코드를 지나지 않는다. R1, R2를 안전망 없이 하지 않으려고 먼저 만든다.
- `src/test/java/gift/order/KakaoMessageClientTest.java`(새 파일)
  - 스프링 컨텍스트 없이 `RestClient.builder()`에 `MockRestServiceServer.bindTo(builder)`를 붙인다. 그 builder로 `new KakaoMessageClient(builder)`를 만든다.
- 확인하는 것:
  - POST `https://kapi.kakao.com/v2/api/talk/memo/default/send`
  - `Authorization: Bearer <카카오 액세스 토큰>`
  - form 본문의 `template_object`가 기대 문자열과 같다
- 경우:
  - 주문 메시지가 있을 때: 문구 끝에 `\n\n💌 <주문 메시지>`가 붙는다.
  - 주문 메시지가 null일 때, 공백뿐일 때: 붙지 않는다.
- 입력은 생성자로 직접 만든다: `Category`, `Product`(이름, 가격), `Option`(이름), `Order`(주문 수량, 주문 메시지).
- `src/main`은 고치지 않는다. 지금 코드에서 통과해야 한다. 기대값 하나를 일부러 틀려 실패하는지도 본다.
- 바꾸는 파일: `src/test/java/gift/order/KakaoMessageClientTest.java`
- 확인 명령: 발송 요청 테스트
- 커밋 메시지 예: `test: 카카오 주문 알림 요청을 고정하는 테스트 추가`

### R1. 알림 문구 만들기를 KakaoMessageClient 밖으로

- `buildTemplate`(`KakaoMessageClient.java:32-45`)을 새 클래스 `gift.order.OrderNotificationMessage`로 옮긴다. 문구는 한 글자도 바꾸지 않는다.
- `sendToMe`는 다 만든 문구(`String`)를 받는다. 문구는 `OrderService`가 만들어 넘긴다.
- C0은 새 시그니처에 맞춰 부르는 줄만 고친다. 기대값(주소, 헤더, 문구)은 고치지 않는다. 그러면 C0은 `OrderNotificationMessage`로 만든 문구를 보내는 모양이 된다.
- 바꾸는 파일: `KakaoMessageClient.java`, `OrderNotificationMessage.java`(새 파일), `OrderService.java`, `KakaoMessageClientTest.java`(부르는 줄만)
- 확인 명령: 발송 요청 테스트, 인수 테스트, ArchUnit
- 커밋 메시지 예: `refactor: 주문 알림 문구 만들기를 KakaoMessageClient 밖으로 옮김`

### R2. 발송을 인터페이스 뒤로

- `gift.order.OrderNotificationSender` 인터페이스(`send(String kakaoAccessToken, String message)`)를 만든다. `KakaoMessageClient`가 이 인터페이스를 구현한다.
- `OrderService`는 `KakaoMessageClient`가 아니라 `OrderNotificationSender`에 의존한다.
- 바꾸는 파일: `OrderNotificationSender.java`(새 파일), `KakaoMessageClient.java`, `OrderService.java`, `KakaoMessageClientTest.java`(메서드 이름이 바뀌면 부르는 줄만)
- 확인 명령: 발송 요청 테스트, 인수 테스트, ArchUnit
- 커밋 메시지 예: `refactor: 주문 알림 발송을 OrderNotificationSender 뒤로 옮김`

### R3. 아웃박스 테이블 만들기 (Flyway V3)

- `src/main/resources/db/migration/V3__Create_order_notification.sql`. 내용은 tech-spec 5장 그대로다. 아직 쓰는 코드는 없다.
- 바꾸는 파일: `V3__Create_order_notification.sql`(새 파일)
- 확인 명령: 인수 테스트(시나리오마다 clean + migrate로 V3까지 만든다), ArchUnit
- 커밋 메시지 예: `refactor: 주문 알림 아웃박스 테이블 추가 (Flyway V3)`

### T1. 가짜 발송기와 단계 정의 (테스트만)

- `src/test/java/gift/acceptance/FakeOrderNotificationSender.java`: `@Primary`로 넣는다.
  - 받은 (토큰, 문구)를 기록한다.
  - "다음 N번 실패", "항상 실패"를 정할 수 있다.
  - `@Before`로 시나리오마다 초기화한다.
- `src/test/java/gift/acceptance/steps/OrderNotificationSteps.java`에 단계 정의를 둔다.
  - 회원에게 카카오 액세스 토큰 주기: `update member set kakao_access_token = ? where email = ?`
  - 발송 실패 정하기
  - 알림 상태를 DB에서 조회하기
  - 가짜 발송기가 받은 횟수, 문구, 토큰 확인하기
- 시나리오는 아직 더하지 않는다. 지금 68개가 그대로 통과해야 한다.
- 바꾸는 파일: `FakeOrderNotificationSender.java`, `OrderNotificationSteps.java`(새 파일 2개)
- 확인 명령: 인수 테스트, ArchUnit
- 커밋 메시지 예: `test: 가짜 주문 알림 발송기와 단계 정의 추가`

### B1. 아웃박스 저장과 커밋 직후 발송

- 테스트 먼저: `src/test/resources/features/order-notification.feature`(새 파일)
  - 16-8: 토큰 있는 회원이 주문하면 알림이 SENT가 된다. 받은 문구와 토큰도 본다(tech-spec O-1).
  - 16-9: 발송이 실패하면 201, PENDING, 시도 1, 마지막 오류가 남는다(O-2 앞부분).
  - 16-10: 토큰 없는 회원이 주문하면 알림이 0건이다(O-4).
  - 16-11: 포인트가 부족하면 500, 알림 0건, 재고 그대로다(O-5).
- 코드를 바꾸기 전에 16-8, 16-9가 "알림은 1건이다"(실제 0건)에서 실패하는 출력을 "B1 실패 출력"에 붙인다. 16-10, 16-11은 지키는 시나리오라 처음부터 통과한다.
- 코드
  - `createOrder`는 토큰 있는 회원에게만 `order_notification`을 PENDING으로 저장하고, 이벤트를 발행한다.
  - `@TransactionalEventListener(AFTER_COMMIT)` 리스너가 같은 스레드에서 보낸다(tech-spec 6장 결정 A).
  - 결과는 `REQUIRES_NEW`로 저장한다. 성공하면 SENT, 실패하면 시도 횟수 +1과 마지막 오류다.
  - `catch (Exception ignored)`를 없앤다.
  - 엔티티, 저장소, 이벤트, 리스너 이름은 `…Service`로 끝나지 않게 한다.
- 코드 주석과 커밋 메시지에 ADR 0002를 적는다. `docs/adr/0002-order-notification-outbox.md`를 같은 커밋에 넣는다.
- 바꾸는 파일: `order-notification.feature`, 이 파일, `OrderService.java`, 새 클래스들(엔티티, 저장소, 이벤트, 리스너), ADR 0002
- 확인 명령: 인수 테스트, 발송 요청 테스트, ArchUnit
- 커밋 메시지 예: `feat: 주문 알림을 아웃박스에 저장하고 커밋 뒤 보냄 (ADR 0002)`

#### B1 실패 출력

- 날짜: 2026-11-27, 코드를 바꾸기 전
- 명령: `./gradlew test --tests 'gift.acceptance.CucumberTest' --rerun-tasks`

```
CucumberTest > Cucumber > 16. 회원이 주문하면 카카오 주문 알림을 보낸다 > gift.acceptance.CucumberTest.16-8. 카카오 액세스 토큰이 있는 회원이 주문하면 알림이 "보냄"이 된다 FAILED
    org.opentest4j.AssertionFailedError at OrderNotificationSteps.java:75

CucumberTest > Cucumber > 16. 회원이 주문하면 카카오 주문 알림을 보낸다 > gift.acceptance.CucumberTest.16-9. 알림 발송이 실패해도 주문은 201이고 알림은 "보낼 것"으로 남는다 FAILED
    org.opentest4j.AssertionFailedError at OrderNotificationSteps.java:75

72 tests completed, 2 failed
```

테스트 결과 파일의 실패 메시지(두 시나리오 같음):

```
org.opentest4j.AssertionFailedError: [응답의 주문 알림 수]
expected: 1
 but was: 0
    at gift.acceptance.steps.OrderNotificationSteps.notificationCountOfResponseOrder(OrderNotificationSteps.java:75)
```

해석: 16-8, 16-9만 "응답의 주문 알림은 1건이다"에서 실패했다. 지금 코드에는 아웃박스가 없어서 알림이 0건이다. 16-10, 16-11(알림이 생기지 않아야 하는 경우)은 처음부터 통과했다.

### B2. 주기 재시도와 5번 제한

- 테스트 먼저(`order-notification.feature`)
  - 16-9에 재시도 부분을 더한다: 재시도가 한 번 돌면 SENT가 된다.
  - 16-12: 다섯 번 실패하면 FAILED가 되고, 재시도를 한 번 더 해도 받은 횟수가 그대로다(O-3).
- 재시도 메서드가 없으면 단계 정의가 컴파일되지 않는다. 그래서 빈 재시도 메서드를 먼저 두고, 상태 확인 단계에서 실패하는 출력을 "B2 실패 출력"에 붙인다(tech-spec 9장).
- 코드
  - 재시도 작업(예: `OrderNotificationRetryJob`)의 대상은 PENDING이면서, 시도가 1번 이상이거나 `created_at`이 1분보다 오래된 행이다.
  - 다섯 번째 실패에서 FAILED로 표시한다.
  - `@EnableScheduling`과 `@Scheduled(fixedDelay = 60000)`를 쓴다.
  - 테스트 프로필에서는 스케줄을 끈다. `application-test.properties`에 `gift.order-notification.retry.enabled=false`를 두고 `@ConditionalOnProperty`로 건다. 시나리오는 재시도 메서드를 직접 부른다.
- 바꾸는 파일: `order-notification.feature`, 이 파일, 재시도 작업(새 파일), 설정 클래스 또는 `Application.java`, `application-test.properties`, `OrderNotificationSteps.java`
- 확인 명령: 인수 테스트, ArchUnit
- 커밋 메시지 예: `feat: 실패한 주문 알림을 5번까지 다시 보냄 (ADR 0002)`

#### B2 실패 출력

```
(사용자가 붙인다)
```

### D. 문서 정리

- "포인트만 DB" 예외가 넓어진 것(tech-spec 8장)을 반영한다. 포인트와 주문 알림 상태를 DB에서 확인한다.
  - `AGENTS.md` "저장소에 없는 결정"
  - `test-strategy.md` 5장 표와 "포인트만 DB에서 확인하는 이유"
  - `MemberSteps.java:28` 주석 "DB를 읽는 유일한 곳"
- ADR 0001 "결과"의 두 줄(알림이 트랜잭션 안에 있음, 알림 예외가 롤백되지 않음)은 지우지 않고 끝에 "(07에서 풀림, ADR 0002)"를 붙인다.
- `open-questions.md` 10번: 상태를 "4단계에서 고침, ADR 0002"로 바꾸고 줄 번호를 맞춘다.
- `user-stories.md` 16번
  - 비고: 주문 알림은 아웃박스에 저장해 커밋 뒤 보내고, 실패하면 5번까지 다시 보낸다(ADR 0002).
  - 인수 조건 줄: `order-notification.feature (16-8~16-12)`를 더한다.
- `AGENTS.md` "코드가 오해를 부르는 곳"의 `// order flow:` 줄: "7. send kakao notification"이 이제 커밋 뒤 리스너에서 일어난다는 점을 적고 줄 번호를 맞춘다.
- 이 파일에 끝 표시를 한다.
- 바꾸는 파일: `AGENTS.md`, `docs/work/02-acceptance-test/test-strategy.md`, `src/test/java/gift/acceptance/steps/MemberSteps.java`(주석만), `docs/adr/0001-order-transaction-boundary.md`, `docs/open-questions.md`, `docs/user-stories.md`, 이 파일
- 확인 명령: 인수 테스트(주석을 바꿨으므로 한 번)
- 커밋 메시지 예: `docs: 주문 알림 아웃박스 결과를 문서에 반영 (ADR 0002)`

---

## 작동 변경 후보

이번 작업에서는 고치지 않는다. 하기로 하면 해당 테스트의 기대값을 먼저 바꾼다.

| 후보 | 바뀌는 작동 | 깨지는 테스트 | open-questions |
|---|---|---|---|
| 알림 금액 형식(`String.format("%,d")`)이 JVM 기본 로케일을 따른다 | 쉼표가 아닌 구분자를 쓰는 로케일에서 알림 금액 모양이 바뀐다 | C0 | 없음 |
