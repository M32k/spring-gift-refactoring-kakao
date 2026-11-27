# 도메인 책임 되찾기 작업 목록

[`tech-spec.md`](tech-spec.md)를 바탕으로 한 작업 목록이다. 모두 구조 변경이다. 대상마다 테스트 보강(T) 다음에 구조 변경(R)을 한다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.

## 공통 규칙

- 한 항목 = 커밋 하나.
- 확인 명령은 사용자가 실행한다. 결과가 너무 빨리 나오면 `--rerun-tasks`를 붙인다.
  - 전체: `./gradlew build` (spotless, 인수 테스트, ArchUnit, 단위 테스트 포함)
  - 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`
  - ArchUnit: `./gradlew test --tests 'gift.architecture.LayerArchitectureTest'`
- git 명령은 사용자가 실행한다.
- 작동은 바꾸지 않는다. 기존 인수 테스트는 고치지 않는다.
- T1, T2의 테스트는 지금 코드에서 통과해야 한다. 기대값 하나를 일부러 틀려 실패하는지 확인한다.
- R1, R2는 T1, T2와 기존 테스트를 고치지 않고 통과해야 한다. 각 커밋에 ADR(0004, 0005)을 넣고, 코드 주석과 커밋 메시지에 번호를 적는다.
- 새 클래스 이름은 `…Service`로 끝나지 않게 한다(tech-spec "ArchUnit").

---

### T1. 주문 금액 특성화 테스트 (테스트와 문서만)
끝: 41d5384

- `order.feature` 16-20(tech-spec 1-3)
  - 상품 1에 재고 2000인 옵션을 API로 만든다. user1이 그 옵션을 1,279개 주문한다.
  - 201이 오고, user1 포인트는 5,000,000 → 2,527,296, 그 옵션 재고는 2000 → 721이다.
  - 새 옵션 번호는 미리 정하지 않고 등록 응답에서 기억한다.
- `src/test/java/gift/order/OrderNotificationMessageTest.java`: 가격 3,360,000원 상품, 주문 수량 1,279개로 문구 전체를 기대 문자열과 비교한다("금액: 2,472,704원").
- `docs/open-questions.md` 29번: 주문 금액이 int로 계산돼 넘친다.
- `src/main`은 고치지 않는다.
- 바꾸는 파일: `order.feature`, 단계 정의 파일, `OrderNotificationMessageTest.java`(새 파일), `docs/open-questions.md`, 이 파일
- 확인 명령: 전체
- 커밋 메시지 예: `test: 주문 금액 int 넘침을 고정하는 특성화 테스트 추가`

#### T1 한 일

- `order.feature`에 16-20을 더했다.
  - 상품 1에 옵션 "대량 구매 / M1 Pro"(재고 2000)를 API로 등록한다. 이름 규칙에 맞고, 상품 1의 기존 옵션과 겹치지 않는다.
  - user1이 1,279개를 주문한다 → 201, 포인트 2,527,296, 그 옵션 재고 721.
  - 주문 전에 등록한 옵션 재고 2000과 user1 포인트 5,000,000도 다시 조회해 확인한다.
  - 시나리오 위 주석에 계산(3,360,000 × 1,279 = 4,297,440,000 → int 2,472,704)과 "버그로 보이지만 고치지 않는다. open-questions 29번"을 적었다.
- 새 단계 정의 3개(`OrderSteps.java`). 옵션 번호는 `ScenarioContext`의 `registeredOptionId`에 기억한다.
  - `응답의 옵션을 등록한 옵션으로 기억한다`
  - `상품 N의 등록한 옵션 재고는 (그대로) M이다`: 기존 재고 단계(`stockIs`)를 그 번호로 부른다.
  - `user1이 등록한 옵션을 주문 수량 N으로 주문한다`
  - 포인트는 기존 `MemberSteps`의 `user1의 포인트는 …이다`를 쓴다.
- `src/test/java/gift/order/OrderNotificationMessageTest.java`를 더했다. 가격 3,360,000원 상품, 주문 수량 1,279개로 문구 전체를 기대 문자열과 비교한다("금액: 2,472,704원"). 객체는 `KakaoMessageClientTest`처럼 생성자로 만든다.
- `docs/open-questions.md`에 29번("주문 금액이 int로 계산돼 넘친다")을 B 섹션에 더했다. 코드 위치(`OrderService.java:61`, `OrderNotificationMessage.java:10`), 인수 16-20, 단위 테스트를 적고, 상태는 tech-spec 09를 가리킨다.
- `src/main`은 고치지 않았다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 81개, ArchUnit 3개, KakaoMessageClientTest 3개, OrderNotificationMessageTest 1개 모두 통과.

#### T1 일부러 틀린 결과

```
# 1) 16-20의 "user1의 포인트는 2,527,296이다"를 2,527,297로 바꿈
$ ./gradlew test --tests 'gift.acceptance.CucumberTest' --rerun-tasks
81 tests completed, 1 failed

16-20. 주문 금액이 int 범위를 넘으면 넘친 금액만 차감되고 주문된다 (open-questions 29번) FAILED
org.opentest4j.AssertionFailedError:
expected: 2527297
 but was: 2527296
    at gift.acceptance.steps.MemberSteps.pointIs(MemberSteps.java:36)
    at ✽.user1의 포인트는 2,527,297이다(classpath:features/order.feature:94)

# 2) OrderNotificationMessageTest의 "2,472,704원"을 "2,472,705원"으로 바꿈
$ ./gradlew test --tests 'gift.order.OrderNotificationMessageTest' --rerun-tasks
1 test completed, 1 failed

주문_금액이_int_범위를_넘으면_넘친_금액이_문구에_들어간다() FAILED
org.opentest4j.AssertionFailedError:
expected: ... "text": "🎁 선물이 도착했어요!\n\n맥북 (스페이스 블랙)\n수량: 1279개\n금액: 2,472,705원" ...
 but was: ... "text": "🎁 선물이 도착했어요!\n\n맥북 (스페이스 블랙)\n수량: 1279개\n금액: 2,472,704원" ...
    at gift.order.OrderNotificationMessageTest(OrderNotificationMessageTest.java:34)
```

해석: 두 테스트 모두 기대값을 1만 바꿔도 실패한다. 실제 값(포인트 2,527,296, 문구 2,472,704원)이 넘친 금액을 그대로 고정하고 있다.

확인한 뒤 두 파일을 되돌렸고, `./gradlew build`로 모두 통과를 다시 확인했다.

### R1. 주문 금액을 Product.totalPrice로 (ADR 0004)
끝: dced891

- `Product.totalPrice(int quantity)`: `price * quantity`를 int 곱셈 그대로 돌려준다.
- `OrderService.java:61`과 `OrderNotificationMessage.java:10`이 이를 부른다.
- `docs/adr/0004-…`을 같은 커밋에 넣는다. 코드 주석과 커밋 메시지에 ADR 0004를 적는다.
- 바꾸는 파일: `Product.java`, `OrderService.java`, `OrderNotificationMessage.java`, ADR 0004, 이 파일
- 확인 명령: 전체
- 커밋 메시지 예: `refactor: 주문 금액 계산을 Product.totalPrice로 옮김 (ADR 0004)`

#### R1 한 일

- `Product.totalPrice(int quantity)`를 `getPrice()` 바로 아래에 더했다. `return price * quantity;`이고, int 곱셈 그대로다(`Math.multiplyExact`, long 안 씀). 주석: "int 곱셈이라 넘친다. 알면서 유지한다(ADR 0004, open-questions 29번)".
- `OrderService.java:61`은 `option.getProduct().totalPrice(request.quantity())`로, `OrderNotificationMessage.java:10`은 `product.totalPrice(order.getQuantity())`로 바꿨다. 두 파일 모두 같은 줄 안에서 바꿔서 줄 번호는 그대로다.
- `src/main`에 `getPrice() *` 곱셈이 남은 곳은 없다.
- `docs/adr/0004-order-total-price-in-product.md`를 0002, 0003 형식으로 썼다.
  - 고려한 선택지: Option, Order, 넘침을 지금 고치기(`multiplyExact`/long)
  - 결과의 생기는 문제: tech-spec ADR 표 그대로
- 테스트는 고치지 않았다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 81개, ArchUnit 3개, KakaoMessageClientTest 3개, OrderNotificationMessageTest 1개 모두 통과.

### T2. 이름 검증 특성화 테스트 (테스트만)
끝: 04bddb5

- `src/test/java/gift/product/ProductNameValidatorTest.java`, `src/test/java/gift/option/OptionNameValidatorTest.java`: 결과 목록 전체를 `isEqualTo`로 비교한다(tech-spec 2-4).
- 바꾸는 파일: 두 테스트 파일(새 파일), 이 파일
- 확인 명령: 전체
- 커밋 메시지 예: `test: 이름 검증 메시지를 고정하는 특성화 테스트 추가`

#### T2 한 일

- `src/test/java/gift/product/ProductNameValidatorTest.java`(12개)와 `src/test/java/gift/option/OptionNameValidatorTest.java`(7개)를 만들었다. 결과 목록 전체를 `isEqualTo(List.of(...))`로 비교한다.
- 기대 메시지는 tech-spec 표가 아니라 지금 Validator 소스에서 그대로 복사했다.
- 다룬 경우(tech-spec 2-4)
  - null, `""`, `"   "` → 필수 하나뿐(상품, 옵션)
  - 경계: 상품 15자(`무지개 우산 대형 세트 AB`) 통과, 16자 길이 메시지. 옵션 50자(`"가 "` × 25) 통과, 51자 길이 메시지. 길이는 `hasSize`로 함께 확인한다.
  - 특수 문자 하나(`골드*`)
  - 길이 + 특수 문자 → 길이, 특수 문자 순서
  - 상품 카카오: false면 붙고, true면 안 붙고, `validate(name)`은 `validate(name, false)`와 같다.
  - 상품 16자 + 특수 문자 + 카카오(`카카오 무지개 우산 세트 A*`) → 길이, 특수 문자, 카카오 순서
  - `"   "` + `allowKakao = false` → 필수 하나뿐
- 처음엔 특수 문자 메시지 상수가 한 줄에 너무 길어 spotlessCheck가 실패했다. 포매터가 보여 준 모양(값을 다음 줄로)으로 고쳤다. 메시지 문자열은 그대로다.
- `src/main`은 고치지 않았다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 81개, ArchUnit 3개, KakaoMessageClientTest 3개, OrderNotificationMessageTest 1개, ProductNameValidatorTest 12개, OptionNameValidatorTest 7개 모두 통과.

#### T2 일부러 틀린 결과

```
# R2에서 날 수 있는 실수를 흉내 냄: src/main 메시지에서 마침표 하나씩 뺌
#   ProductNameValidator 길이 메시지 "...입력할 수 있습니다." → "...입력할 수 있습니다"
#   OptionNameValidator 필수 메시지 "옵션 이름은 필수입니다." → "옵션 이름은 필수입니다"
$ ./gradlew test --tests 'gift.product.ProductNameValidatorTest' --tests 'gift.option.OptionNameValidatorTest' --rerun-tasks
19 tests completed, 6 failed

ProductNameValidatorTest > 공백을_포함해_16자인_이름은_길이_메시지가_나온다() FAILED
expected: ["상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다."]
 but was: ["상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다"]
ProductNameValidatorTest > 길이와_특수_문자가_함께_틀리면_길이_특수_문자_순서다() FAILED
ProductNameValidatorTest > 길이와_특수_문자와_카카오가_함께_틀리면_세_메시지가_이_순서다() FAILED
OptionNameValidatorTest > 이름이_없거나_공백뿐이면_필수_메시지_하나뿐이다(String) > [1] name=null, [2] name=, [3] name=    FAILED
expected: ["옵션 이름은 필수입니다."]
 but was: ["옵션 이름은 필수입니다"]
```

해석:
- 마침표 하나만 달라도 그 메시지를 쓰는 테스트가 모두 실패한다. 인수 테스트는 부분 문자열만 봐서 이 차이를 못 잡는다. 그래서 R2(메시지를 틀로 만들기) 전에 이 단위 테스트가 필요하다.
- 확인한 뒤 src/main 두 파일을 git checkout으로 되돌렸고, `./gradlew build`로 모두 통과를 다시 확인했다.

### R2. 이름 검증 공통 규칙을 NameRule로 (ADR 0005)
끝: 57ee333

- `gift.product.NameRule(String target, int maxLength)`과 `List<String> validate(String name)`. 두 Validator가 이를 쓴다.
- 카카오 검사를 건너뛰는 판단은 `name == null || name.isBlank()` 조건으로 한다.
- 시그니처와 부르는 곳은 그대로다.
- `docs/adr/0005-…`을 같은 커밋에 넣는다. 코드 주석과 커밋 메시지에 ADR 0005를 적는다.
- 바꾸는 파일: `NameRule.java`(새 파일), `ProductNameValidator.java`, `OptionNameValidator.java`, ADR 0005, 이 파일
- 확인 명령: 전체
- 커밋 메시지 예: `refactor: 이름 검증 공통 규칙을 NameRule로 뽑음 (ADR 0005)`

#### R2 한 일

- `gift.product.NameRule`을 만들었다(tech-spec 2-3 모양).
  - 생성자는 `public NameRule(String target, int maxLength)`, 메서드는 `public List<String> validate(String name)`이다.
  - 허용 패턴은 이 클래스 한 곳에만 있다.
  - 필수(`name == null || name.isBlank()`)이면 필수 메시지 하나만 담은 `ArrayList`를 돌려준다. 아니면 길이 → 특수 문자 순서로 검사한다.
  - 메시지는 `target`과 `maxLength`만 끼워 만든다.
  - 클래스 주석에 ADR 0005와 규칙(한국어)을 적었다.
- `ProductNameValidator`
  - `private static final NameRule RULE = new NameRule("상품", 15)`를 쓴다.
  - `validate(name, allowKakao)`는 `RULE.validate(name)` 뒤에, `name == null || name.isBlank()`이면 그대로 돌려주고, 아니면 카카오 규칙만 더한다. 메시지 문자열에 기대는 분기는 없다.
- `OptionNameValidator`
  - `new NameRule("옵션", 50)`에 맡긴다.
  - 위쪽 영어 규칙 주석은 한국어로 바꾸고 ADR 0005를 가리킨다.
- 두 Validator의 `MAX_LENGTH`, `ALLOWED_PATTERN` 상수는 지웠다. public 시그니처와 부르는 곳(`ProductService:64`, `AdminProductController:45,78`, `OptionService:78`)은 그대로다.
- `docs/adr/0005-name-rule-in-product.md`를 0004 형식으로 썼다.
  - 고려한 선택지: 패턴만 공유, 값 객체, `gift.common` 새 패키지
  - 결과의 생기는 문제: tech-spec ADR 표 그대로
- 테스트는 고치지 않았다.
- `./gradlew build` 성공(2026-11-27): spotlessCheck 통과. 인수 테스트 81개, ArchUnit 3개, ProductNameValidatorTest 12개, OptionNameValidatorTest 7개, KakaoMessageClientTest 3개, OrderNotificationMessageTest 1개 모두 통과.

### D. 문서 정리
끝: 79cf6c8 docs: 도메인 책임 되찾기 결과를 문서에 반영

- `open-questions.md` 2번, 29번: 금액 계산 근거를 `Product.totalPrice`로 맞춘다.
- R1, R2로 밀린 `파일:줄`을 맞춘다.
- 이 파일에 끝 표시를 한다.
- 바꾸는 파일: `docs/open-questions.md`, 이 파일(그 밖은 줄 번호가 밀린 곳)
- 확인 명령: 없음(문서만)
- 커밋 메시지 예: `docs: 도메인 책임 되찾기 결과를 문서에 반영`
