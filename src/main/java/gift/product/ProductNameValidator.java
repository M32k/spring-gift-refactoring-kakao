package gift.product;

import java.util.List;

public class ProductNameValidator {
    private static final NameRule RULE = new NameRule("상품", 15);

    private ProductNameValidator() {}

    public static List<String> validate(String name) {
        return validate(name, false);
    }

    public static List<String> validate(String name, boolean allowKakao) {
        List<String> errors = RULE.validate(name);

        // 필수에서 멈추면 카카오 검사도 하지 않는다. 메시지가 아니라 같은 조건으로 판단한다.
        if (name == null || name.isBlank()) {
            return errors;
        }

        if (!allowKakao && name.contains("카카오")) {
            errors.add("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }

        return errors;
    }
}
