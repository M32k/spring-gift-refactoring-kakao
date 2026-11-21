package gift.acceptance.steps;

import static gift.acceptance.ScenarioContext.amount;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.Members;
import io.cucumber.java.en.Given;
import org.springframework.jdbc.core.JdbcTemplate;

public class MemberSteps {

    private final JdbcTemplate jdbcTemplate;

    public MemberSteps(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 인수 테스트에서 DB를 읽는 유일한 곳이다.
     * 회원이 자기 포인트를 조회하는 API가 없어서(open-questions 4번) 포인트만 DB에서 확인한다.
     * 관리자 화면을 쓰지 않는 이유는 docs/work/02-acceptance-test/test-strategy.md에 있다.
     */
    @Given("^(user1|user2)의 포인트는 (?:그대로 )?([\\d,]+)이다$")
    public void pointIs(String member, String expected) {
        Integer point = jdbcTemplate.queryForObject(
            "select point from member where email = ?",
            Integer.class,
            Members.email(member)
        );
        assertThat(point).isEqualTo(amount(expected));
    }
}
