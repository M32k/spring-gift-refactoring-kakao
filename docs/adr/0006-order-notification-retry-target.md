# 0006. 주문 알림 재시도 대상에 "시도 0번이고 만든 지 1분이 넘은 알림"을 더한다

- 날짜: 2026-11-27
- 상태: 채택
- 관련: [ADR 0002](0002-order-notification-outbox.md)(재시도 대상을 대체) / [`docs/work/07-order-notification-outbox/tasks.md`](../work/07-order-notification-outbox/tasks.md) B2, B3
- 이 ADR은 코드(a470f24)와 테스트(e172a04)보다 늦게, 4단계 검토에서 썼다. 결정은 a470f24 때 한 것이다.

## 맥락

- ADR 0002는 "실패한 것만 주기 작업이 다시 보낸다"고 정했다.
- 커밋 직후 발송(`OrderNotificationRelay`)은 주문이 커밋된 뒤 보내고, 결과는 새 트랜잭션(`OrderNotificationDispatcher`)에서 저장한다.
- 발송 중에 앱이 꺼지거나 결과를 저장하지 못하면, 알림은 시도 0번의 "보낼 것"(PENDING)으로 남는다. 실패로 기록되지 않았으므로, 실패한 것만 보는 재시도는 이 알림을 영원히 보내지 않는다.

## 결정

- 재시도 대상은 "보낼 것" 가운데 다음 둘이다.
  - 시도 횟수가 1 이상인 알림(한 번 이상 실패함)
  - 시도 0번이고 만든 지 1분이 넘은 알림
- 만든 지 1분이 안 된 시도 0번 알림은 커밋 직후 발송이 아직 진행 중일 수 있어서 건드리지 않는다.
- 주기(1분 간격)와 5번 제한은 ADR 0002 그대로다.

## 고려한 선택지

| 선택지 | 고르지 않은 이유 |
|---|---|
| 실패한 것만(ADR 0002의 결정) | 커밋 직후 발송 중에 앱이 꺼지거나 결과를 저장하지 못한 알림은 시도 0번으로 남아 영원히 보내지 않는다 |
| 시도 0번은 모두 | 커밋 직후 발송이 아직 진행 중인 알림을 재시도가 함께 보내서, 같은 알림이 두 번 갈 수 있다 |
| 시도 0번 + 만든 지 1분이 넘은 것(고름) | - |

## 결과

- 시도 0번으로 1분 넘게 남은 알림은 재시도가 보낸다. 만든 지 1분이 안 된 알림은 건드리지 않는다.
- **생기는 문제**
  - 커밋 직후 발송이 1분 넘게 걸리면 재시도(`created_at`이 1분보다 오래된 시도 0번 행)와 겹쳐 두 번 보낼 수 있다.
  - 카카오에는 보냈는데 "보냄"으로 저장하기 전에 앱이 꺼지면, 시도 0번으로 남은 알림을 1분 뒤 재시도가 다시 보낸다. 카카오 "나에게 보내기"에는 중복을 막는 키가 없다.
  - 1분은 코드의 상수(`OrderNotificationRetryJob.UNATTEMPTED_GRACE_MINUTES`)다. 바꾸려면 코드를 고쳐야 한다.

## 근거

- 테스트: `order-notification.feature` 16-13(시도 0번으로 1분 넘게 남은 알림은 재시도가 보낸다), 16-14(만든 지 1분이 안 된 알림은 건드리지 않는다)
- 일부러 틀린 결과: 07 tasks.md "B3 일부러 틀린 결과". 16-14의 "만든 시각 0분 전"을 "2분 전"으로 바꾸자 `[주문 알림 상태] expected: "PENDING" but was: "SENT"`로 16-14만 실패했다(75개 중 1개).
- 코드: `OrderNotificationRepository.findRetryTargetIds`(`status = PENDING and (attemptCount >= 1 or createdAt < :createdBefore)`), `OrderNotificationRetryJob.retryOnce`(`createdBefore = now - 1분`)
