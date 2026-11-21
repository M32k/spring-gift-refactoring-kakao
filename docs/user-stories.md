# 사용자 스토리 (User Stories)

컨트롤러(API와 관리자 화면)를 읽고, 사용자가 할 수 있는 행동을 "누가 무엇을 왜 한다"로 정리한 문서다.

- 기준 브랜치: `practice-interface`
- 작성일: 2026-11-22
- **누가**는 비회원, 회원, 관리자 중 하나로 적는다.
  - 회원: `Authorization` 토큰을 확인하는 기능
  - 관리자: `/admin` 화면과, 관리자용으로 보이는 상품, 카테고리, 옵션의 등록, 수정, 삭제 API. 단, 실제 코드에는 관리자 권한 확인이 없다(비고 참고).
  - 비회원: 토큰 없이 쓰도록 만든 기능
- **왜**는 코드에 목적이 적혀 있을 때만 쓰고 근거를 비고에 붙인다. 코드로 알 수 없으면 "왜: 확인 필요"라고 적는다.
- **확인한 응답**에는 요청으로 직접 확인한 결과만 적는다. 확인하지 않은 API는 "아직 확인 안 함"으로 둔다.
- 비고의 "open-questions N번"은 [`docs/open-questions.md`](open-questions.md)의 항목 번호다.

---

## 1. 비회원은 이메일과 비밀번호로 회원 가입을 한다 (왜: 확인 필요)
- API: `POST /api/members/register`
- 확인한 응답: 아직 확인 안 함
- 비고: 가입하면 바로 토큰을 준다

## 2. 비회원은 이메일과 비밀번호로 로그인한다 (왜: 확인 필요)
- API: `POST /api/members/login`
- 확인한 응답: 200, token
- 비고: 비밀번호를 평문으로 비교한다(open-questions 7번)

## 3. 비회원은 카카오 계정으로 로그인한다 (왜: 확인 필요)
- API: `GET /api/auth/kakao/login`, `GET /api/auth/kakao/callback`
- 확인한 응답: 아직 확인 안 함
- 비고: 처음이면 자동으로 가입된다. 단서: scope에 `talk_message`가 있고, 받은 카카오 토큰을 주문 알림 발송에 쓴다

## 4. 비회원은 상품 목록과 상세를 본다 (왜: 확인 필요)
- API: `GET /api/products`, `GET /api/products/{id}`
- 확인한 응답:
  - `GET /api/products`: 아직 확인 안 함
  - `GET /api/products/{id}`: 200, 상품 이름과 가격
- 비고: 없음

## 5. 비회원은 카테고리 목록을 본다 (왜: 확인 필요)
- API: `GET /api/categories`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

## 6. 비회원은 상품의 옵션 목록을 본다 (왜: 확인 필요)
- API: `GET /api/products/{productId}/options`
- 확인한 응답: 200, 옵션 id와 재고
- 비고: 없음

## 7. 관리자는 상품을 등록한다 (왜: 확인 필요)
- API: `POST /api/products`
- 확인한 응답: 201 (토큰 없이도 성공)
- 비고: ⚠ 실제로는 토큰 없이 누구나 할 수 있다(open-questions 1번). "카카오"가 들어간 이름은 거부한다

## 8. 관리자는 상품을 수정한다 (왜: 확인 필요)
- API: `PUT /api/products/{id}`
- 확인한 응답: 아직 확인 안 함
- 비고: ⚠ 실제로도 토큰 없이 누구나 할 수 있을 것으로 보인다(open-questions C-1, 요청으로는 미확인)

## 9. 관리자는 상품을 삭제한다 (왜: 확인 필요)
- API: `DELETE /api/products/{id}`
- 확인한 응답: 204, 다시 조회하면 404
- 비고: ⚠ 실제로는 토큰 없이 누구나 할 수 있다(open-questions 11번)

## 10. 관리자는 카테고리를 등록, 수정, 삭제한다 (왜: 확인 필요)
- API: `POST /api/categories`, `PUT /api/categories/{id}`, `DELETE /api/categories/{id}`
- 확인한 응답:
  - `POST /api/categories`: 201 (토큰 없이도 성공)
  - `PUT /api/categories/{id}`: 아직 확인 안 함
  - `DELETE /api/categories/{id}`: 204
- 비고: ⚠ 등록과 삭제는 토큰 없이 누구나 할 수 있다(open-questions 15번). 수정은 미확인

## 11. 관리자는 상품에 옵션을 추가한다 (왜: 확인 필요)
- API: `POST /api/products/{productId}/options`
- 확인한 응답: 201 (토큰 없이도 성공)
- 비고: ⚠ 실제로는 토큰 없이 누구나 할 수 있다(open-questions 16번). 같은 상품 안에서 옵션 이름 중복은 막는다

## 12. 관리자는 상품의 옵션을 삭제한다 (왜: 확인 필요)
- API: `DELETE /api/products/{productId}/options/{optionId}`
- 확인한 응답: 204
- 비고: ⚠ 실제로는 토큰 없이 누구나 할 수 있다(open-questions 17번). 옵션이 1개뿐이면 삭제할 수 없다(`OptionController.java:21` 주석은 규칙만 적고 이유는 없다)

## 13. 회원은 자기 위시 목록을 본다 (왜: 확인 필요)
- API: `GET /api/wishes`
- 확인한 응답: 200, 내 위시 목록
- 비고: 없음

## 14. 회원은 상품을 위시에 담는다 (왜: 확인 필요)
- API: `POST /api/wishes`
- 확인한 응답: 아직 확인 안 함
- 비고: 이미 담았으면 기존 위시를 200으로 돌려준다

## 15. 회원은 자기 위시를 삭제한다 (왜: 확인 필요)
- API: `DELETE /api/wishes/{id}`
- 확인한 응답: 남의 위시면 403, 위시는 그대로
- 비고: 남의 위시는 삭제할 수 없다(open-questions 5번)

## 16. 회원은 포인트로 상품 옵션을 주문한다 (왜: 확인 필요)
- API: `POST /api/orders`
- 확인한 응답:
  - 성공: 201, 재고와 포인트 차감
  - 포인트 부족: 500, 재고는 줄어듦(open-questions 3번)
  - 재고 부족: 500, 상태 그대로(open-questions 12번)
- 비고: "선물이 도착했어요" 메시지를 주문한 본인에게 보낸다(open-questions 14번). 주문 금액은 옵션이 아니라 상품 가격으로 계산한다(open-questions 2번)

## 17. 회원은 자기 주문 내역을 본다 (왜: 확인 필요)
- API: `GET /api/orders`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

## 18. 관리자는 상품 목록 화면을 본다 (왜: 확인 필요)
- API: `GET /admin/products`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

## 19. 관리자는 담당 MD와 협의한 "카카오" 상품명을 쓰려고, "카카오"가 들어간 이름으로도 상품을 등록한다
- API: `GET /admin/products/new`, `POST /admin/products`
- 확인한 응답: 아직 확인 안 함
- 비고: 근거는 `ProductNameValidator.java:36` 메시지와 `AdminProductController.java:47`의 `allowKakao = true`

## 20. 관리자는 담당 MD와 협의한 "카카오" 상품명을 쓰려고, 상품을 수정할 때도 "카카오"가 들어간 이름을 쓴다
- API: `GET /admin/products/{id}/edit`, `POST /admin/products/{id}/edit`
- 확인한 응답: 아직 확인 안 함
- 비고: 근거는 `AdminProductController.java:80`

## 21. 관리자는 상품을 삭제한다 (왜: 확인 필요)
- API: `POST /admin/products/{id}/delete`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

## 22. 관리자는 회원 목록과 포인트를 본다 (왜: 확인 필요)
- API: `GET /admin/members`
- 확인한 응답: 회원 목록과 포인트 (토큰 없이 열림)
- 비고: 로그인 없이 열린다(open-questions 13번)

## 23. 관리자는 회원을 등록한다 (왜: 확인 필요)
- API: `GET /admin/members/new`, `POST /admin/members`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

## 24. 관리자는 회원의 이메일과 비밀번호를 수정한다 (왜: 확인 필요)
- API: `GET /admin/members/{id}/edit`, `POST /admin/members/{id}/edit`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

## 25. 관리자는 회원 포인트를 충전한다 (왜: 확인 필요)
- API: `POST /admin/members/{id}/charge-point`
- 확인한 응답: 아직 확인 안 함
- 비고: 단서: `Member.java:56` 주석 "point deduction for order payment"에서 포인트는 주문 결제에 쓰인다

## 26. 관리자는 회원을 삭제한다 (왜: 확인 필요)
- API: `POST /admin/members/{id}/delete`
- 확인한 응답: 아직 확인 안 함
- 비고: 없음

---

## 코드에 없는 행동

- 회원이 자기 포인트를 조회하는 기능은 없다. 포인트는 관리자 화면(22번)에서만 보인다(open-questions 4번).
