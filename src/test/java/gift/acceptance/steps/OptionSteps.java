package gift.acceptance.steps;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OptionSteps {

    private final ScenarioContext context;

    public OptionSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("^비회원이 상품 (\\d+)의 옵션 목록을 조회한다$")
    public void fetchOptions(long productId) {
        context.setLastResponse(given().when().get("/api/products/{productId}/options", productId));
    }

    @Then("^응답의 옵션 번호는 ([\\d, ]+?)(?:이다|다)$")
    public void responseOptionIdsAre(String ids) {
        List<Integer> expected = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
        List<Integer> actual = context.lastResponse().jsonPath().getList("id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("^응답의 옵션 (\\d+)(?:은|는) 옵션명 \"([^\"]*)\", 재고 (\\d+)(?:이다|다)$")
    public void responseOptionIs(int optionId, String name, int stock) {
        Map<String, Object> option =
                context.lastResponse().jsonPath().param("optionId", optionId).get("find { it.id == optionId }");
        assertThat(option).as("옵션 %d", optionId).isNotNull();
        assertThat(option.get("name")).isEqualTo(name);
        assertThat(((Number) option.get("quantity")).intValue()).isEqualTo(stock);
    }
}
