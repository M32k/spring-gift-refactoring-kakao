Feature: 15. 회원은 자기 위시를 삭제한다

  Scenario: 15-1. 다른 회원의 위시는 삭제할 수 없다
    Given user2의 위시 번호는 3, 4다
    When user1이 자기 로그인 토큰으로 위시 3을 삭제한다
    Then 403이 온다
    And user2의 위시 번호는 그대로 3, 4다
