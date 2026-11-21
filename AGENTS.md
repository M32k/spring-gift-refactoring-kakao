# AGENTS.md

이 저장소에서 일하는 AI 에이전트를 위한 안내다. 저장소를 읽어서 알 수 없는 것과 다시 찾는 데 오래 걸리는 것만 적는다.

## 작업을 확인하는 방법

- `./gradlew build`: 컴파일하고 인수 테스트도 함께 실행한다.
- `./gradlew bootRun`: `localhost:8080`에 서버가 뜬다.
- 요청으로 확인하는 일은 사용자가 한다. `http/*.http` 파일이나 curl을 쓴다. AI는 보낼 요청을 적어 줄 수만 있고 실행하지 않는다.
- 인수 테스트: `./gradlew test --tests 'gift.acceptance.CucumberTest'`. 결과 보고서는 `build/reports/tests/test/index.html`에 생긴다. 시나리오마다 초기 데이터로 되돌린 뒤 실행한다.

## 코드가 오해를 부르는 곳

같은 코드 이름이 서로 다른 뜻으로 쓰이거나, 용어 사전과 다르게 불린다. 문서와 대화에는 오른쪽의 용어 사전 이름을 쓴다.

| 코드 | 실제 뜻 | 위치 |
|---|---|---|
| `quantity` | `Option.quantity`는 **재고**이고, `OrderRequest.quantity`와 `Order.quantity`는 **주문 수량**이다. `option.subtractQuantity(request.quantity())`는 재고에서 주문 수량을 빼는 코드다. | `order/OrderController.java:87` |
| `price` | `Product.price`는 상품 1개의 **가격**이다. 주문 처리 안의 지역 변수 `price`와 알림의 `totalPrice`는 **주문 금액**(가격 × 주문 수량)이다. | `order/OrderController.java:91`, `order/KakaoMessageClient.java:32` |
| `token`, `accessToken` | `TokenResponse.token`과 `JwtProvider`는 **로그인 토큰**이다. 주석에는 "JWT access token"이라고 적혀 있다. `Member.kakaoAccessToken`과 `KakaoLoginClient`의 `accessToken`은 **카카오 액세스 토큰**이다. 둘 다 `Authorization: Bearer`로 보낸다. | `auth/TokenResponse.java`, `member/Member.java:24` |
| `message` | `Order.message`와 `OrderRequest.message`는 **주문 메시지**다. `KakaoMessageClient`와 `sendKakaoMessageIfPossible`은 **주문 알림**을 보낸다. `e.getMessage()`는 오류 메시지다. | `order/` |
| `admin`, `Admin*Controller` | 이름만 관리자용이다. `/admin/**`에는 권한 확인이 없다. 초기 데이터의 `admin@example.com`도 일반 회원이다. | `member/AdminMemberController.java`, `product/AdminProductController.java` |
| 회원 id와 `user` 번호 | 초기 데이터의 `user1@example.com`은 회원 id 2이고, `user2@example.com`은 회원 id 3이다. id 1은 admin이다. | `db/migration/V2__Insert_default_data.sql` |

## 저장소에 없는 결정

- 지금(1·2단계)은 분석과 인수 테스트만 한다. `src/main`은 고치지 않는다. 리팩터링 목표는 나중에 정한다.
- 현재 동작은 버그처럼 보여도 그대로 둔다. 이상한 동작은 `docs/open-questions.md`에 기록만 하고, 고칠지는 사람이 정한다.
- `docs/open-questions.md`의 항목 번호는 다시 매기지 않는다. 다른 문서가 번호로 항목을 가리킨다.
- 인수 테스트는 HTTP 요청과 응답으로만 검증한다. 상태 코드, 응답 본문, 바뀐 상태(다음 요청으로 확인)까지 본다. 포인트만 예외로 DB에서 확인한다. 이유는 `docs/work/02-acceptance-test/test-strategy.md`에 있다.
- 카카오 키는 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 환경 변수로 넣는다. 저장소에 키를 넣지 않는다.
- PR은 Fork의 `M32k` 브랜치에서 원본 미션 저장소로 보낸다.

## 두 번 이상 고친 것

- AI가 curl을 직접 실행해서 그 결과가 "직접 확인"과 섞였다. 요청 확인은 사용자가 하고, AI는 실행하지 않는다.
- AI가 git 명령을 실행했다. git 명령은 사용자가 터미널에서 직접 한다.
- "토큰", "등록" 같은 표현을 넓게 써서 용어 사전에 맞춰 다시 고쳤다. 로그인 토큰과 카카오 액세스 토큰, 등록과 회원 가입을 구분해 쓴다.
- 연습 브랜치와 작업 브랜치가 섞였다. 작업 브랜치는 `M32k` 하나다. `practice-interface`는 쓰지 않는다.
- 새로 만든 상품의 번호를 미리 정할 뻔했다. 새로 만든 것은 응답에 나온 번호로 다음 요청을 보낸다.
- 인수 조건을 user-stories와 feature 파일 두 곳에 썼다. 인수 조건 본문은 feature 파일에만 쓰고, 수의 근거는 Gherkin 주석으로 남긴다.
- 기대값을 바꿨는데 테스트가 1초 안에 통과했다(Gradle이 이전 결과를 다시 씀). 결과가 너무 빨리 나오면 `--rerun-tasks`로 다시 실행한다.

## 문서 위치

- `docs/glossary.md`: 용어 사전. 문서와 대화에 쓰는 한글명과, 코드에 쓰는 영문명.
- `docs/user-stories.md`: 사용자 스토리. 누가 무엇을 왜 하는지와 확인한 응답.
- `docs/open-questions.md`: 확인 필요 목록. 의도인지 버그인지 결정이 필요한 동작.
- `src/test/resources/features/`: 인수 조건(feature 파일).
- `docs/work/02-acceptance-test/test-strategy.md`: 인수 테스트 전략. 고른 기준, 데이터 초기화, 포인트만 DB로 확인하는 이유.
- `.claude/skills/acceptance-test/SKILL.md`: 스토리 하나를 인수 테스트로 옮기는 스킬(`/acceptance-test [스토리 번호]`).

## 규칙

- 작업 중 세 문서와 다른 이름이나 작동을 발견하면 사용자에게 묻고, 답에 따라 문서를 고친다.
