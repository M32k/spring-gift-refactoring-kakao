# 0004. 주문 금액은 Product.totalPrice로 계산하고, int 넘침은 알면서 유지한다

- 날짜: 2026-11-27
- 상태: 채택
- 관련: [`docs/open-questions.md`](../open-questions.md) 2번, 29번 / [`docs/work/09-domain-responsibility/tech-spec.md`](../work/09-domain-responsibility/tech-spec.md)

## 맥락

- 주문 금액(상품 가격 × 주문 수량)을 두 곳에서 따로 계산했다.
  - 포인트 차감: `OrderService`의 `option.getProduct().getPrice() * request.quantity()`
  - 주문 알림 문구: `OrderNotificationMessage`의 `product.getPrice() * order.getQuantity()`
- 둘 다 int 곱셈이라 결과가 int 범위를 넘으면 넘친다. 1,279개 주문이 2,472,704원만 빼고 201로 성공한다(open-questions 29번).
- `OrderService`는 Order를 만들기 전에 포인트를 빼므로, 그때 금액이 필요하다.
- 이번 작업은 구조 변경이다. 작동은 바꾸지 않는다.

## 결정

- 주문 금액은 `Product.totalPrice(int quantity)`가 계산한다. 두 곳이 이를 부른다.
- 계산은 지금처럼 int 곱셈이다. 넘침도 지금과 같다. 알면서 유지한다.
- 넘침은 특성화 테스트로 고정한다: 인수 `order.feature` 16-20, 단위 `OrderNotificationMessageTest`.

## 고려한 선택지

| 선택지 | 고르지 않은 이유 |
|---|---|
| `Option.totalPrice(int quantity)` | "옵션을 주문한다"에 가깝지만, 옵션에는 가격이 없어 상품 가격에 맡기기만 한다. "금액은 상품 가격 기준"(open-questions 2번)이 가려진다 |
| `Order.totalPrice()` | 의미는 가장 맞지만, 포인트를 빼기 전에 Order를 먼저 만들어야 한다. 객체를 만드는 순서와 `orderDateTime`이 바뀐다 |
| 넘침을 지금 고치기(`Math.multiplyExact` 또는 long 금액) | 넘칠 때의 작동(201과 차감 금액)이 바뀐다. 구조 변경 작업에 작동 변경을 섞지 않는다 |

## 결과

- 금액 계산이 한 곳(`Product.totalPrice`)으로 모인다. 포인트 차감과 알림 문구의 금액이 같은 규칙을 쓴다.
- **생기는 문제**
  - 금액 int 넘침 버그가 남는다(open-questions 29번). 1,279개 주문이 2,472,704원만 빼고 201로 성공한다. 고치려면 작동 변경으로 다시 열고, 인수 16-20과 `OrderNotificationMessageTest`의 기대값을 먼저 바꾼다.
