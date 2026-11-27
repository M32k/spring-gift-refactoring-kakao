# 0005. 이름 검증의 공통 규칙은 gift.product.NameRule에 둔다

- 날짜: 2026-11-27
- 상태: 채택
- 관련: [`docs/open-questions.md`](../open-questions.md) 20번 / [`docs/work/09-domain-responsibility/tech-spec.md`](../work/09-domain-responsibility/tech-spec.md)

## 맥락

- `ProductNameValidator`와 `OptionNameValidator`가 같은 허용 패턴과 같은 검사 순서(필수면 바로 끝 → 길이 → 특수 문자)를 따로 갖고 있었다.
- 다른 것은 대상 이름(상품/옵션), 최대 길이(15/50), 상품의 "카카오" 규칙(`allowKakao`)뿐이다.
- 두 Validator는 서비스와 관리자 화면에서 static으로 불린다. 관리자 화면은 오류 목록을 폼에 보여 준다.
- 이번 작업은 구조 변경이다. 오류 메시지 문자열과 순서는 한 글자도 바꾸지 않는다.

## 결정

- 공통 규칙은 `gift.product.NameRule(String target, int maxLength)`이 맡는다. `validate(String name)`은 고칠 수 있는 `ArrayList`를 돌려준다.
  - 허용 패턴은 `NameRule` 한 곳에만 둔다.
  - 메시지는 대상 이름과 최대 길이만 끼워 만든다.
- `ProductNameValidator`는 `NameRule("상품", 15)`의 결과에 카카오 규칙만 더한다. 필수에서 멈춘 경우는 메시지가 아니라 같은 조건(`name == null || name.isBlank()`)으로 판단해 카카오 검사를 하지 않는다.
- `OptionNameValidator`는 `NameRule("옵션", 50)`에 맡긴다.
- 두 Validator의 public 시그니처와 부르는 곳은 그대로다.
- 메시지는 특성화 단위 테스트(`ProductNameValidatorTest`, `OptionNameValidatorTest`)로 고정한다.

## 고려한 선택지

| 선택지 | 고르지 않은 이유 |
|---|---|
| 패턴만 공유 | 가장 작은 변경이지만, 검사 순서와 메시지 틀의 중복이 남는다 |
| 값 객체(`ProductName`, `OptionName`) | 만들 때 예외를 던지는 방식이라 관리자 화면에 보여 줄 오류 목록이 따로 필요하고, 엔티티와 요청 타입까지 바뀐다. 구조 변경 작업의 범위보다 크다 |
| `gift.common` 새 패키지에 두기 | 의미는 더 맞지만 새 패키지를 만드는 것이라 3단계 규칙("패키지 재편 금지")과 부딪힌다. `gift.option`은 이미 `Option.product`로 `gift.product`에 의존하므로 `gift.product`에 두어도 의존 방향이 새로 생기지 않는다 |

## 결과

- 허용 패턴과 검사 순서가 한 곳으로 모인다. 대상별로 다른 것(이름, 최대 길이, 카카오 규칙)만 각 Validator에 남는다.
- **생기는 문제**
  - `gift.option`이 `gift.product`의 규칙 클래스를 쓴다. 패키지 의존은 이미 있지만, 옵션 규칙이 상품 패키지에 놓인다.
  - 메시지를 틀로 만들어서 글자가 달라질 위험이 있다. 이것은 T2 단위 테스트(전체 문자열 비교)로 막는다.
