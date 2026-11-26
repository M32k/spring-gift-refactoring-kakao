package gift.acceptance.steps;

import static gift.acceptance.ScenarioContext.amount;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
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
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of(
                        "name",
                        name,
                        "price",
                        amount(price),
                        "imageUrl",
                        "https://example.com/a.png",
                        "categoryId",
                        categoryId))
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

    @When("^비회원이 상품 목록을 조회한다$")
    public void fetchProducts() {
        context.setLastResponse(given().when().get("/api/products"));
    }

    @When("^비회원이 상품 목록을 (\\d+)쪽, 쪽 크기 (\\d+)(?:으로|로) 조회한다$")
    public void fetchProductsPage(int page, int size) {
        context.setLastResponse(
                given().queryParam("page", page).queryParam("size", size).when().get("/api/products"));
    }

    @Then("^응답의 상품 번호는 (\\d+(?:, \\d+)*)(?:이다|다)$")
    public void responseProductIdsAre(String ids) {
        List<Integer> expected =
                Arrays.stream(ids.split(", ")).map(Integer::valueOf).toList();
        List<Integer> actual = context.lastResponse().jsonPath().getList("content.id", Integer.class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("^응답의 상품은 (\\d+)개, 전체 상품은 (\\d+)개, 전체 쪽은 (\\d+)쪽이다$")
    public void responsePageIs(int contentSize, int totalElements, int totalPages) {
        JsonPath body = context.lastResponse().jsonPath();
        assertThat(body.getList("content")).hasSize(contentSize);
        assertThat(body.getInt("totalElements")).isEqualTo(totalElements);
        assertThat(body.getInt("totalPages")).isEqualTo(totalPages);
    }

    @When("^비회원이 상품 (\\d+)(?:을|를) 조회한다$")
    public void fetchProduct(long id) {
        context.setLastResponse(given().when().get("/api/products/{id}", id));
    }

    @Then("^응답의 카테고리는 (\\d+)(?:이다|다)$")
    public void responseCategoryIs(long categoryId) {
        assertThat(context.lastResponse().jsonPath().getLong("categoryId")).isEqualTo(categoryId);
    }

    private int productCount() {
        return given().when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .extract()
                .path("totalElements");
    }
}
