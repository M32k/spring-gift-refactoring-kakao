package gift.acceptance.steps;

import static gift.acceptance.ScenarioContext.amount;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;

public class ProductSteps {

    private final ScenarioContext context;

    public ProductSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("^상품이 (\\d+)개 있다$")
    public void productCountIs(int expected) {
        assertThat(productCount()).isEqualTo(expected);
    }

    @Then("^상품 목록의 상품 수는 (?:그대로 )?(\\d+)개다$")
    public void productListCountIs(int expected) {
        assertThat(productCount()).isEqualTo(expected);
    }

    @When("^로그인 토큰 없이 상품명 \"([^\"]*)\", 가격 ([\\d,]+)원, 카테고리 (\\d+)로 상품을 등록한다$")
    public void createProductWithoutLoginToken(String name, String price, long categoryId) {
        Response response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", amount(price),
                "imageUrl", "https://example.com/a.png",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products");
        context.setLastResponse(response);
    }

    @Then("^응답의 상품명은 \"([^\"]*)\"이고 가격은 ([\\d,]+)원이다$")
    public void responseProductIs(String name, String price) {
        Response response = context.lastResponse();
        assertThat(response.jsonPath().getString("name")).isEqualTo(name);
        assertThat(response.jsonPath().getInt("price")).isEqualTo(amount(price));
    }

    @Then("^응답의 상품 번호로 조회하면 상품명 \"([^\"]*)\", 가격 ([\\d,]+)원이 온다$")
    public void createdProductCanBeFetched(String name, String price) {
        long id = context.lastResponse().jsonPath().getLong("id");

        Response response = given().when().get("/api/products/{id}", id);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("name")).isEqualTo(name);
        assertThat(response.jsonPath().getInt("price")).isEqualTo(amount(price));
    }

    private int productCount() {
        return given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .extract()
            .path("totalElements");
    }
}
