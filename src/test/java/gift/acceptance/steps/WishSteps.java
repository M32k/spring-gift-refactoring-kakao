package gift.acceptance.steps;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;

public class WishSteps {

    private final ScenarioContext context;

    public WishSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("^(user1|user2)의 위시 번호는 (?:그대로 )?(\\d+(?:, \\d+)*)(?:이다|다)$")
    public void wishIdsAre(String member, String ids) {
        List<Integer> expected = Arrays.stream(ids.split(", "))
            .map(Integer::valueOf)
            .toList();

        List<Integer> actual = given()
            .header("Authorization", context.authorizationOf(member))
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(200)
            .extract()
            .path("content.id");

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @When("^(user1|user2)(?:이|가) 자기 로그인 토큰으로 위시 (\\d+)(?:을|를) 삭제한다$")
    public void deleteWish(String member, long wishId) {
        context.setLastResponse(
            given()
                .header("Authorization", context.authorizationOf(member))
            .when()
                .delete("/api/wishes/{id}", wishId)
        );
    }
}
