# 위시 정리 작업 목록

[`tech-spec.md`](tech-spec.md)를 바탕으로 한 작업 목록이다. 구조 변경(R1, R2, T1)을 먼저 끝내고 작동 변경(B1)을 한다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나. B1은 고친 테스트, 바꾼 코드, 실패 출력(이 파일), ADR 0003을 한 커밋에 담는다.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 전체: `./gradlew build` (spotless, 인수 테스트, ArchUnit 포함)
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
  - ArchUnit: `./gradlew test --tests 'gift.architecture.LayerArchitectureTest'`
- git 명령은 사용자가 실행한다.
- 응답 코드는 바꾸지 않는다. 위시 정리가 실패해도 주문은 201이다.
- 새 클래스 이름은 `…Service`로 끝나지 않게 한다(tech-spec 9장).
- 트랜잭션은 B1 리스너의 `REQUIRES_NEW`에서만 연다(tech-spec 4장). R1, R2에서는 `@Transactional`을 붙이지 않는다.

---

### R1. 위시 정리 인터페이스와 구현 (부르는 곳 없음)

- `gift.wish.WishCleaner` 인터페이스: `void clean(Long memberId, Long productId)`
- `gift.wish.RepositoryWishCleaner`(`@Component`): `WishRepository`로 그 회원 + 상품의 위시를 모두 지운다. 하나만 찾거나 `Optional`로 받는 방식은 쓰지 않는다(tech-spec 2장).
- `WishRepository`에 회원 번호 + 상품 번호로 모두 지우는 메서드를 더한다.
- 트랜잭션은 B1 리스너의 REQUIRES_NEW에서 연다(파생 delete는 트랜잭션 필요).
- 아직 부르는 곳은 없다. `OrderService`, 테스트, feature 파일은 건드리지 않는다.
- 바꾸는 파일: `WishCleaner.java`(새 파일), `RepositoryWishCleaner.java`(새 파일), `WishRepository.java`, 이 파일
- 확인 명령: 전체
- 커밋 메시지 예: `refactor: 위시 정리 인터페이스와 저장소 구현 추가`

#### R1 한 일

- `WishRepository`에 파생 delete `long deleteByMemberIdAndProductId(Long memberId, Long productId)`를 더했다.
  - `Wish`는 `memberId` 필드 + `product` 연관이라, 기존 `findByMemberIdAndProductId`처럼 `memberId`와 `product.id`로 찾는다.
  - 반환값(지운 개수)은 쓰지 않는다. 확인은 위시 목록 API로 한다.
- `WishCleaner`(인터페이스, `void clean(Long memberId, Long productId)`)와 `RepositoryWishCleaner`(`@Component`)를 만들었다. `@Transactional`은 붙이지 않았다.
- 부르는 곳은 없다. `OrderService`, 테스트, feature 파일은 그대로다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 75개, ArchUnit 3개, KakaoMessageClientTest 3개 모두 통과.

### R2. 주문마다 OrderPlacedEvent 발행 (받는 곳 없음)

- `gift.order.OrderPlacedEvent(orderId, memberId, productId)`를 만든다.
- `createOrder`가 주문 저장 뒤 이 이벤트를 발행한다. 받는 리스너가 아직 없어서 작동은 같다.
- 바꾸는 파일: `OrderPlacedEvent.java`(새 파일), `OrderService.java`
- 확인 명령: 전체
- 커밋 메시지 예: `refactor: 주문마다 OrderPlacedEvent 발행`

### T1. 위시 정리 감싸개와 단계 정의 (테스트만)

- `src/test/java/gift/acceptance/FailableWishCleaner.java`(`@Component @Primary`): 평소에는 진짜 `RepositoryWishCleaner`에 맡기고, 정해 두면 예외를 던진다. 시나리오마다 초기화한다.
- 단계 정의: "위시 정리가 실패한다". 위시 확인은 기존 `WishSteps`의 `위시 번호는 …다`를 쓴다.
- 시나리오는 아직 더하지 않는다. 지금 시나리오가 그대로 통과해야 한다.
- 바꾸는 파일: `FailableWishCleaner.java`(새 파일), 단계 정의 파일
- 확인 명령: 전체
- 커밋 메시지 예: `test: 위시 정리 감싸개와 단계 정의 추가`

### B1. 주문 커밋 뒤 위시 정리

- 테스트 먼저: `src/test/resources/features/wish-cleanup.feature`(새 파일). 16-15~16-19 = tech-spec 7장 W-1~W-5.
- 코드를 바꾸기 전에 W-1(16-15), W-3(16-17)이 위시 번호 단계에서 실패하는 출력을 "B1 바꾸기 전 실패 출력"에 붙인다. W-2, W-4, W-5는 처음부터 통과한다.
- 코드
  - `gift.wish`에 `OrderPlacedEvent`를 받는 `@TransactionalEventListener(AFTER_COMMIT)` 리스너를 둔다(예: `OrderWishCleanupListener`).
  - `REQUIRES_NEW` 트랜잭션에서 `WishCleaner.clean`을 부른다.
  - 실패는 warn 로그로 남기고 주문 응답(201)으로 새어 나가지 않게 한다. 빈 catch는 만들지 않는다.
- 코드를 바꾼 뒤 W-5에서 "위시 정리가 실패한다"를 빼서 위시 단계에서 실패하는지 확인하고, "W-5 일부러 틀린 결과"에 붙인다. 확인한 뒤 되돌린다.
- 코드 주석과 커밋 메시지에 ADR 0003을 적는다. `docs/adr/0003-wish-cleanup-after-order-commit.md`를 같은 커밋에 넣는다.
- 바꾸는 파일: `wish-cleanup.feature`, 이 파일, 리스너(새 파일), ADR 0003
- 확인 명령: 전체
- 커밋 메시지 예: `feat: 주문 커밋 뒤 주문한 상품을 위시에서 뺌 (ADR 0003)`

#### B1 바꾸기 전 실패 출력

```
(사용자가 붙인다)
```

#### W-5 일부러 틀린 결과

```
(사용자가 붙인다)
```

### D. 문서 정리

tech-spec 11장대로 한다.
- `AGENTS.md` "코드가 오해를 부르는 곳"의 `// order flow:` 줄: "6. cleanup wish"가 이제 커밋 뒤 `gift.wish`의 리스너에서 일어난다고 적고 줄 번호를 맞춘다.
- `open-questions.md` 8번: 상태를 "4단계에서 고침, ADR 0003"으로 바꾸고 인수 테스트 줄을 더한다. 06부터 밀린 줄 번호(`:58`, `:61`)를 맞추거나 "4단계 전 코드"로 표시한다.
- `open-questions.md` 9번: 상태 끝에 "위시 정리는 08에서 OrderController가 아닌 gift.wish 리스너로 구현함(ADR 0003)"을 더한다.
- `docs/work/05-service-layer/tasks.md` "작동 변경 후보"의 "위시 정리 구현" 줄: 지우지 않고 끝에 "(08에서 구현, ADR 0003)"을 붙인다.
- `user-stories.md` 16번: 비고와 인수 조건(`wish-cleanup.feature` 16-15~16-19)을 더한다.
- R2로 밀린 `OrderService.java` 줄 번호를 맞춘다.
- 이 파일에 끝 표시를 한다.
- 바꾸는 파일: `AGENTS.md`, `docs/open-questions.md`, `docs/work/05-service-layer/tasks.md`, `docs/user-stories.md`, 이 파일
- 확인 명령: 없음(문서만)
- 커밋 메시지 예: `docs: 위시 정리 결과를 문서에 반영 (ADR 0003)`

---

## 작동 변경 후보

이번 작업에서는 고치지 않는다. 하기로 하면 해당 테스트의 기대값을 먼저 바꾼다.

| 후보 | 바뀌는 작동 | 깨지는 테스트 | open-questions |
|---|---|---|---|
| (아직 없음) | | | |
