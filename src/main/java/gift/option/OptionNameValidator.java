package gift.option;

import gift.product.NameRule;
import java.util.List;

/*
 * 옵션 이름 규칙: 필수, 공백을 포함해 최대 50자, 허용된 문자만.
 * 규칙 자체는 상품 이름과 같아서 NameRule에 맡긴다(ADR 0005).
 */
public class OptionNameValidator {
    private static final NameRule RULE = new NameRule("옵션", 50);

    private OptionNameValidator() {}

    public static List<String> validate(String name) {
        return RULE.validate(name);
    }
}
