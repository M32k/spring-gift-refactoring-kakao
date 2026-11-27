# 도메인 책임 되찾기 기술 명세

흩어진 도메인 규칙 두 가지를 제자리로 옮기는 작업이다. 구조 변경(`refactor:`)만 하고 작동은 바꾸지 않는다. 기존 인수 테스트는 고치지 않는다.

- 작성일: 2026-11-27
- 기준 브랜치: `M32k`
- 용어는 [`docs/glossary.md`](../../glossary.md)를 따른다.
- 관련 확인 필요 목록: [`docs/open-questions.md`](../../open-questions.md) 2번(금액은 상품 가격 기준), 20번(API와 관리자 화면의 "카카오" 규칙), 29번(금액 int 넘침, T1에서 새로 적는다)
- 관련 문서: [`docs/work/02-acceptance-test/test-strategy.md`](../02-acceptance-test/test-strategy.md)

## 정해진 결정

- 주문 금액 계산은 `Product`로 옮긴다(`Product.totalPrice(int quantity)`). int 곱셈과 넘침 결과는 지금과 같다.
- 이름 검증은 공통 클래스 `gift.product.NameRule`로 뽑는다. 새 패키지는 만들지 않는다.
  - 오류 메시지 문자열과 순서는 한 글자도 바꾸지 않는다.
  - `ProductNameValidator`/`OptionNameValidator`의 public 시그니처와 부르는 곳은 그대로 둔다.
- 리팩터링 전에 지키는 테스트가 없는 작동은 특성화 테스트(단위, 인수)를 먼저 더한다. 이 테스트는 지금 코드에서 통과해야 한다.

---

## 대상 1. 주문 금액 계산

### 1-1. 지금 코드와 부르는 곳

| 곳 | 코드 | 쓰임 |
|---|---|---|
| `OrderService.java:61` | `var totalPrice = option.getProduct().getPrice() * request.quantity();` | 바로 다음 줄 `member.deductPoint(totalPrice)`(`:62`)에서 포인트를 뺀다. Order를 만들기 전이다 |
| `OrderNotificationMessage.java:10` | `String.format("%,d", product.getPrice() * order.getQuantity())` | 주문 알림 문구의 금액이다. `OrderService.java:81`에서 `OrderNotificationMessage.build`를 부른다 |

- 가격은 `Product.getPrice()`(`Product.java:57-59`)에서 온다. 옵션에는 가격이 없다(open-questions 2번).
- 둘 다 int 곱셈이다. 3,360,000 × 1,279 = 4,297,440,000은 int 최댓값을 넘어 2,472,704가 된다.

### 1-2. 자리 비교

| 자리 | 장점 | 단점 |
|---|---|---|
| `Product.totalPrice(int quantity)` | 가격이 있는 곳이다. 두 곳 모두 `Product`를 이미 갖고 있다. Order 없이 부를 수 있다. "금액은 상품 가격 기준"(open-questions 2번)이 그대로 드러난다 | 주문 수량을 받는 메서드가 상품에 생긴다 |
| `Option.totalPrice(int quantity)` | "옵션을 주문한다"에 가깝다 | 옵션에는 가격이 없어서 상품 가격에 맡기기만 한다. "상품 가격 기준"이 가려진다 |
| `Order.totalPrice()` | 의미가 가장 맞다 | `OrderService`는 포인트를 뺀 뒤 Order를 만든다(`:62` → `:66`). Order를 먼저 만들어야 해서 객체를 만드는 순서와 `orderDateTime`이 바뀐다 |

**결정: Product.** 두 곳에 이미 있는 값만으로 부를 수 있고, 만드는 순서를 바꾸지 않는다.

```java
// Product
public int totalPrice(int quantity) {
    return price * quantity; // int 곱셈. 넘침도 지금과 같다
}
```

- `Math.multiplyExact`나 long은 쓰지 않는다. 쓰면 넘칠 때의 작동(예외 또는 다른 금액)이 바뀐다.
- `OrderService.java:61`은 `option.getProduct().totalPrice(request.quantity())`로, `OrderNotificationMessage.java:10`은 `product.totalPrice(order.getQuantity())`로 바꾼다.

### 1-3. 바뀌지 않아야 할 작동

| 작동 | 지키는 테스트 |
|---|---|
| 정상 주문 금액 차감(3,360,000원, user1 5,000,000 → 1,640,000) | `order.feature` 16-1 |
| 포인트 부족 500, 상태 그대로 | `order.feature` 16-2, `wish-cleanup.feature` 16-18 |
| 알림 문구의 금액(3,360,000원) | `order-notification.feature` 16-8 |
| 알림 문구의 금액(6,720,000원) | `KakaoMessageClientTest` 3개 |
| 금액 int 넘침: 1,279개 주문 → 2,472,704원 차감, 201 | 테스트 없음 → T1에서 더한다 |
| 알림 문구의 넘친 금액("2,472,704원") | 테스트 없음 → T1에서 더한다 |

**특성화 테스트 판단: 더한다.** 넘침은 리팩터링에서 가장 바뀌기 쉬운 곳이다(long이나 `multiplyExact`로 "고치고" 싶어진다). 그런데 지키는 테스트가 없다.
- 인수 `order.feature` 16-20: 상품 1(3,360,000원)에 재고 2000인 옵션을 API로 만들고, user1이 1,279개를 주문한다.
  - 201이 온다.
  - user1 포인트는 5,000,000 → 2,527,296이다(5,000,000 − 2,472,704).
  - 그 옵션 재고는 2000 → 721이다.
  - 버그로 보이지만 이번은 구조 변경이라 고치지 않는다. 시나리오 설명과 open-questions 29번에 적는다.
  - 640개처럼 넘치지 않아도 포인트 부족으로 500이 나는 수량은 넘침을 고정하지 못한다. 그래서 넘친 결과가 양수로 포인트 안에 들어오는 1,279개를 쓴다.
- 단위 `OrderNotificationMessageTest`: 같은 상품 가격 3,360,000원, 주문 수량 1,279개로 문구에 "금액: 2,472,704원"이 들어가는지 본다.
- 새로 만든 옵션 번호는 미리 정하지 않는다. 등록 응답의 옵션 번호를 기억하고 그 옵션을 주문하거나 조회하는 단계 정의를 더한다(테스트 코드만 바뀐다).

---

## 대상 2. 이름 검증 중복

### 2-1. 지금 코드와 부르는 곳

| 곳 | 내용 |
|---|---|
| `ProductNameValidator.java:7-39` | 최대 15자. `validate(name)`(`:13-15`)은 `validate(name, false)`와 같다. 카카오 규칙은 `:33-35` |
| `OptionNameValidator.java:13-37` | 최대 50자 |
| 허용 패턴 | 두 클래스가 같다(`ProductNameValidator.java:9`, `OptionNameValidator.java:15`) |

부르는 곳:
- `ProductService.java:64`(`validateName`): `createProduct`(`:30`), `updateProduct`(`:42`)에서 부른다. 메시지를 `", "`로 합쳐 `IllegalArgumentException`으로 던지고, `ProductController`의 `@ExceptionHandler`가 400 본문으로 돌려준다.
- `AdminProductController.java:45`(등록), `:78`(수정): `validate(name, true)`로 카카오를 허용한다. 오류 목록은 폼에 보여 준다.
- `OptionService.java:78`(`validateName`): `createOption`(`:42`)에서 부른다. 합치는 방식은 상품과 같고, `OptionController`의 `@ExceptionHandler`가 400 본문으로 돌려준다.

지금 메시지(이 문자열이 그대로 나와야 한다):

| 순서 | 상품 | 옵션 |
|---|---|---|
| 1. 필수(여기서 끝) | `상품 이름은 필수입니다.` | `옵션 이름은 필수입니다.` |
| 2. 길이 | `상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.` | `옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.` |
| 3. 특수 문자 | `상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _` | `옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _` |
| 4. 카카오(상품만, `allowKakao`가 false일 때) | `"카카오"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.` | (없음) |

### 2-2. 공통으로 뽑을 것과 남길 것

| 뽑을 것(`NameRule`) | 남길 것 |
|---|---|
| 허용 패턴 | 대상 이름(상품/옵션): 인자로 넘긴다 |
| 검사 순서: 필수면 바로 끝 → 길이 → 특수 문자 | 최대 길이(15/50): 인자로 넘긴다 |
| 세 메시지의 틀(대상 이름과 최대 길이만 끼운다) | 카카오 규칙과 `allowKakao`: `ProductNameValidator`에 남긴다 |

### 2-3. 방법 비교

| | 공통 클래스 | 패턴만 공유 | 값 객체(`ProductName`, `OptionName`) |
|---|---|---|---|
| 줄어드는 중복 | 패턴, 검사 순서, 메시지 틀 | 패턴뿐 | 패턴, 순서, 메시지 |
| 부르는 곳 | 그대로(두 Validator의 static 시그니처 유지) | 그대로 | 엔티티, 요청 타입, 관리자 화면까지 바뀐다 |
| 관리자 화면 | 오류 목록을 그대로 받는다 | 그대로 | 만들 때 예외를 던지는 방식이라 폼에 보여 줄 오류 목록이 따로 필요하다 |
| 위험 | 메시지를 틀로 만들면서 글자가 달라질 수 있다 → 특성화 단위 테스트로 막는다 | 거의 없다 | 이번 범위(구조 변경)보다 크다 |

**결정: 공통 클래스.** 검사 순서 중복까지 없애면서 시그니처와 부르는 곳이 그대로다.

```java
// gift.product.NameRule
public NameRule(String target, int maxLength)
public List<String> validate(String name)   // 고칠 수 있는 ArrayList를 돌려준다
//  - name == null || name.isBlank(): "<target> 이름은 필수입니다." 하나만 돌려준다
//  - length > maxLength: "<target> 이름은 공백을 포함하여 최대 <maxLength>자까지 입력할 수 있습니다."
//  - 패턴 불일치: "<target> 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _"

// ProductNameValidator.validate(name, allowKakao)
List<String> errors = RULE.validate(name);
if (name == null || name.isBlank()) {
    return errors;              // 필수에서 멈추면 카카오 검사도 안 한다(조건으로 판단, 메시지에 기대지 않는다)
}
if (!allowKakao && name.contains("카카오")) {
    errors.add(...);            // 지금 문자열 그대로
}
return errors;
```

- 위치는 `gift.product`다. 3단계 규칙의 "패키지 재편 금지" 때문에 새 패키지를 만들지 않는다. `gift.option`은 이미 `Option.product`로 `gift.product`에 의존하므로 의존 방향이 새로 생기지 않는다.
- 이름은 `…Validator`와 겹치지 않게 `NameRule`로 한다.

### 2-4. 바뀌지 않아야 할 작동

| 작동 | 지키는 테스트 |
|---|---|
| 상품명 15자는 등록, 16자는 400("최대 15자") | `product.feature` 7-2, 7-3, `product-update.feature` 8-2 |
| 이름 검사가 없는 상품·카테고리 확인(404)보다 먼저 | `product-update.feature` 8-6, `option-create.feature` 11-6 |
| API에서 "카카오" 불허(400, "담당 MD와 협의한 경우에만") | `product-update.feature` 8-3 |
| 옵션명 특수 문자 400("허용되지 않는 특수 문자") | `option-create.feature` 11-4, 11-6 |
| 정상 옵션명 등록 | `option-create.feature` 11-1, 11-3 |
| 메시지 전체 문자열과 합치는 순서 | 테스트 없음. 인수 테스트는 부분 문자열만 본다 |
| "필수" 메시지 | 테스트 없음. API는 `@NotBlank`가 먼저 막고, 관리자 화면에서 빈 문자열일 때만 닿는다 |
| 옵션명 50/51자 경계 | 테스트 없음 |
| 상품명 특수 문자 | 테스트 없음 |
| 관리자 화면의 "카카오" 허용(`allowKakao = true`) | 테스트 없음(HTML 화면) |
| 필수에서 멈추면 카카오 검사도 안 함 | 테스트 없음 |

**특성화 테스트 판단: 더한다.** 메시지를 틀로 만들면서 한 글자라도 달라지면 인수 테스트(부분 문자열)로는 못 잡는다. 두 Validator는 static이라 단위 테스트로 전체 목록을 바로 볼 수 있다.
- `ProductNameValidatorTest`, `OptionNameValidatorTest`(단위): 결과 목록 전체를 `isEqualTo`로 비교한다.
  - 경계: 상품 15/16자, 옵션 50/51자(공백 포함)
  - null, `""`, `"   "` → 필수 하나뿐
  - 특수 문자 하나
  - 길이와 특수 문자가 함께 틀릴 때의 순서
  - 상품의 카카오: `allowKakao` false이면 카카오 메시지가 붙고 true이면 안 붙는다. `validate(name)`은 false와 같다.
  - 상품 16자 + 특수 문자 + 카카오: 세 메시지가 이 순서로 나온다.
  - `"   "`에 `allowKakao = false`: 필수 하나뿐이다(카카오 검사 안 함).
- 관리자 화면의 카카오 허용은 Validator 단위 테스트(`allowKakao = true`)로 지킨다. 화면 자체의 인수 테스트는 이번에도 만들지 않는다.

---

## 커밋 순서

대상마다 테스트 보강(`test:`) → 구조 변경(`refactor:`)을 하나씩 한다. 구조 변경 커밋에서는 테스트를 고치지 않는다.

| 순서 | 종류 | 내용 |
|---|---|---|
| T1 | test | 금액 특성화: `order.feature` 16-20, `OrderNotificationMessageTest`, 단계 정의(새로 만든 옵션 기억). open-questions 29번(금액 int 넘침)을 같이 더한다 |
| R1 | refactor | `Product.totalPrice(int)`를 만들고 `OrderService.java:61`, `OrderNotificationMessage.java:10`이 이를 부른다. ADR 0004를 같은 커밋에 넣고, 코드 주석과 커밋 메시지에 번호를 적는다 |
| T2 | test | `ProductNameValidatorTest`, `OptionNameValidatorTest` |
| R2 | refactor | `gift.product.NameRule`을 만들고 두 Validator가 이를 쓴다. 시그니처와 부르는 곳은 그대로다. ADR 0005를 같은 커밋에 넣고, 코드 주석과 커밋 메시지에 번호를 적는다 |
| D | docs | open-questions 2번의 금액 근거를 `Product.totalPrice`로 맞춘다. 밀린 `파일:줄`을 맞추고 끝 표시를 한다 |

- T1, T2의 테스트는 지금 코드에서 통과해야 한다. 기대값 하나를 일부러 틀려 실패하는지 확인한다.
- R1, R2는 T1, T2와 기존 인수 테스트를 고치지 않고 통과해야 한다.

## ArchUnit

- 새 클래스 `NameRule`과 새 메서드 `Product.totalPrice`는 `…Service`가 아니다. 둘 다 web 패키지에 의존하지 않아서 규칙 B와 상관없다.
- `gift.option` → `gift.product` 의존은 이미 있다(`Option.product`). 컨트롤러와 Repository 사이 의존은 바뀌지 않아서 규칙 0, A와도 상관없다.
- 규칙(`LayerArchitectureTest`)은 고치지 않는다.

## ADR 판단

**남긴다. 두 개로 나눈다.**
- 작동은 바뀌지 않는다. 하지만 두 결정 모두 선택지가 둘 이상이고 트레이드오프가 있다(1-2, 2-3 비교 표). 그래서 AGENTS.md:49 규칙("선택지가 둘 이상이고 트레이드오프가 있던 결정은 ADR")에 맞는다.
- 하나로 묶어 R2에 넣지 않는다. 묶으면 R1의 코드 주석이 아직 없는 ADR을 가리키게 된다. 그래서 결정마다 ADR을 하나씩 두고, 그 결정을 반영한 커밋에 넣는다.

| ADR | 제목 | 넣는 커밋 | "결과"에 넣을 생기는 문제 |
|---|---|---|---|
| 0004 | 주문 금액은 `Product.totalPrice`로 계산하고, int 넘침은 알면서 유지한다 | R1 | 금액 int 넘침 버그가 남는다(open-questions 29번). 1,279개 주문이 2,472,704원만 빼고 201로 성공한다. 고치려면 작동 변경으로 다시 연다 |
| 0005 | 이름 검증의 공통 규칙은 `gift.product.NameRule`에 둔다 | R2 | `gift.option`이 `gift.product`의 규칙 클래스를 쓴다(패키지 의존은 이미 있지만, 옵션 규칙이 상품 패키지에 놓인다). 메시지를 틀로 만들어서 글자가 달라질 위험이 있고, 이것은 T2 단위 테스트(전체 문자열 비교)로 막는다 |

- 본문(맥락, 결정, 고려한 선택지, 결과)은 R1, R2를 할 때 쓴다. 형식은 0002, 0003과 같다.

## 알려진 한계와 작동 변경 후보 (기록만)

- 주문 금액의 int 넘침(open-questions 29번)은 고치지 않는다. 고치려면 작동 변경으로 `Math.multiplyExact`나 long 금액을 쓰고, 인수 16-20과 `OrderNotificationMessageTest`의 기대값을 먼저 바꾼다.
- API는 "카카오"를 막고 관리자 화면은 허용한다(open-questions 20번). 이번에도 그대로 둔다.
