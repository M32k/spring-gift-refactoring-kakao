package gift.product;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 상품 이름과 옵션 이름이 함께 쓰는 이름 규칙(ADR 0005).
 * - null이거나 공백뿐이면 안 된다(이때는 필수 메시지 하나만 돌려준다)
 * - 공백을 포함해 최대 길이까지
 * - 한글, 영문, 숫자, 공백과 특수 문자 ( ) [ ] + - & / _ 만 쓸 수 있다
 * 대상 이름(상품/옵션)과 최대 길이만 다르고, 검사 순서와 메시지 틀은 같다.
 */
public class NameRule {
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    private final String target;
    private final int maxLength;

    public NameRule(String target, int maxLength) {
        this.target = target;
        this.maxLength = maxLength;
    }

    public List<String> validate(String name) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add(target + " 이름은 필수입니다.");
            return errors;
        }

        if (name.length() > maxLength) {
            errors.add(target + " 이름은 공백을 포함하여 최대 " + maxLength + "자까지 입력할 수 있습니다.");
        }

        if (!ALLOWED_PATTERN.matcher(name).matches()) {
            errors.add(target + " 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }

        return errors;
    }
}
