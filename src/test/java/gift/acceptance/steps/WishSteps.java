package gift.acceptance.steps;

import static gift.acceptance.ScenarioContext.amount;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WishSteps {

    private final ScenarioContext context;

    public WishSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("^(user1|user2)의 위시 번호는 (?:그대로 )?(\\d+(?:, \\d+)*)(?:이다|다)$")
    public void wishIdsAre(String member, String ids) {
        List<Integer> expected =
                Arrays.stream(ids.split(", ")).map(Integer::valueOf).toList();

        List<Integer> actual = given().header("Authorization", context.authorizationOf(member))
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
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .when()
                .delete("/api/wishes/{id}", wishId));
    }

    @When("^(user1|user2)(?:이|가) 자기 로그인 토큰으로 위시 목록을 조회한다$")
    public void fetchWishes(String member) {
        context.setLastResponse(given().header("Authorization", context.authorizationOf(member))
                .when()
                .get("/api/wishes"));
    }

    @When("^틀린 로그인 토큰으로 위시 목록을 조회한다$")
    public void fetchWishesWithWrongToken() {
        context.setLastResponse(
                given().header("Authorization", "Bearer wrong-token").when().get("/api/wishes"));
    }

    @When("^로그인 토큰 헤더 없이 위시 목록을 조회한다$")
    public void fetchWishesWithoutHeader() {
        context.setLastResponse(given().when().get("/api/wishes"));
    }

    @Then("^응답의 위시 번호는 (\\d+(?:, \\d+)*)(?:이다|다)$")
    public void responseWishIdsAre(String ids) {
        List<Integer> expected =
                Arrays.stream(ids.split(", ")).map(Integer::valueOf).toList();
        List<Integer> actual = context.lastResponse().jsonPath().getList("content.id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("^응답의 위시 (\\d+)(?:은|는) 상품 (\\d+), 상품명 \"([^\"]*)\", 가격 ([\\d,]+)(?:이다|다)$")
    public void responseWishInListIs(int wishId, long productId, String name, String price) {
        Map<String, Object> wish =
                context.lastResponse().jsonPath().param("wishId", wishId).get("content.find { it.id == wishId }");
        assertThat(wish).as("위시 %d", wishId).isNotNull();
        assertThat(((Number) wish.get("productId")).longValue()).isEqualTo(productId);
        assertThat(wish.get("name")).isEqualTo(name);
        assertThat(((Number) wish.get("price")).intValue()).isEqualTo(amount(price));
    }
}
