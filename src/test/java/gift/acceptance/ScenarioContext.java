package gift.acceptance;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 한 시나리오 안에서 단계 정의 클래스들이 함께 쓰는 값. 시나리오마다 새로 만들어진다.
 */
@Component
@ScenarioScope
public class ScenarioContext {

    private Response lastResponse;
    private final Map<String, String> loginTokens = new HashMap<>();
    private Long registeredOptionId;

    public void setLastResponse(Response response) {
        this.lastResponse = response;
    }

    public Response lastResponse() {
        if (lastResponse == null) {
            throw new IllegalStateException("아직 보낸 요청이 없다.");
        }
        return lastResponse;
    }

    // 시나리오에서 새로 등록한 옵션의 번호. 번호를 미리 정하지 않고 등록 응답에서 기억한다.
    public void rememberRegisteredOption(Long optionId) {
        this.registeredOptionId = optionId;
    }

    public Long registeredOptionId() {
        if (registeredOptionId == null) {
            throw new IllegalStateException("아직 기억한 옵션이 없다.");
        }
        return registeredOptionId;
    }

    public String authorizationOf(String alias) {
        return "Bearer " + loginTokens.computeIfAbsent(alias, Members::login);
    }

    public static int amount(String text) {
        return Integer.parseInt(text.replace(",", ""));
    }
}
