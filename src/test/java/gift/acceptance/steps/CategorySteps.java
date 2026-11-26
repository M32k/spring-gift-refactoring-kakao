package gift.acceptance.steps;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CategorySteps {

    private final ScenarioContext context;

    public CategorySteps(ScenarioContext context) {
        this.context = context;
    }

    @When("^비회원이 카테고리 목록을 조회한다$")
    public void fetchCategories() {
        context.setLastResponse(given().when().get("/api/categories"));
    }

    @Then("^응답의 카테고리 번호는 ([\\d, ]+?)(?:이다|다)$")
    public void responseCategoryIdsAre(String ids) {
        List<Integer> expected = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
        List<Integer> actual = context.lastResponse().jsonPath().getList("id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("^응답의 카테고리 (\\d+)(?:은|는) 이름 \"([^\"]*)\", 색 \"([^\"]*)\", 이미지 URL \"([^\"]*)\", 설명 \"([^\"]*)\"(?:이다|다)$")
    public void responseCategoryInListIs(
            int categoryId, String name, String color, String imageUrl, String description) {
        Map<String, Object> category = context.lastResponse()
                .jsonPath()
                .param("categoryId", categoryId)
                .get("find { it.id == categoryId }");
        assertThat(category).as("카테고리 %d", categoryId).isNotNull();
        assertThat(category.get("name")).isEqualTo(name);
        assertThat(category.get("color")).isEqualTo(color);
        assertThat(category.get("imageUrl")).isEqualTo(imageUrl);
        assertThat(category.get("description")).isEqualTo(description);
    }
}
