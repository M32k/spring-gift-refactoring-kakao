package gift.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ProductNameValidator의 오류 메시지 목록 전체(문자열과 순서)를 지금 그대로 고정한다.
 * 메시지는 ProductNameValidator 소스에서 그대로 복사했다.
 * docs/work/09-domain-responsibility/tasks.md T2 참고.
 */
class ProductNameValidatorTest {

    private static final String REQUIRED = "상품 이름은 필수입니다.";
    private static final String TOO_LONG = "상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.";
    private static final String NOT_ALLOWED_CHARACTER =
            "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _";
    private static final String KAKAO = "\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.";

    // 공백을 포함해 15자, 16자
    private static final String NAME_15 = "무지개 우산 대형 세트 AB";
    private static final String NAME_16 = "무지개 우산 대형 세트 ABC";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void 이름이_없거나_공백뿐이면_필수_메시지_하나뿐이다(String name) {
        assertThat(ProductNameValidator.validate(name)).isEqualTo(List.of(REQUIRED));
    }

    @Test
    void 공백뿐인_이름은_카카오를_막아도_필수_메시지_하나뿐이다() {
        assertThat(ProductNameValidator.validate("   ", false)).isEqualTo(List.of(REQUIRED));
    }

    @Test
    void 공백을_포함해_15자인_이름은_오류가_없다() {
        assertThat(NAME_15).hasSize(15);
        assertThat(ProductNameValidator.validate(NAME_15)).isEqualTo(List.of());
    }

    @Test
    void 공백을_포함해_16자인_이름은_길이_메시지가_나온다() {
        assertThat(NAME_16).hasSize(16);
        assertThat(ProductNameValidator.validate(NAME_16)).isEqualTo(List.of(TOO_LONG));
    }

    @Test
    void 허용되지_않는_특수_문자가_있으면_특수_문자_메시지가_나온다() {
        assertThat(ProductNameValidator.validate("골드*")).isEqualTo(List.of(NOT_ALLOWED_CHARACTER));
    }

    @Test
    void 길이와_특수_문자가_함께_틀리면_길이_특수_문자_순서다() {
        String name = NAME_15 + "*";
        assertThat(name).hasSize(16);
        assertThat(ProductNameValidator.validate(name)).isEqualTo(List.of(TOO_LONG, NOT_ALLOWED_CHARACTER));
    }

    @Test
    void 카카오를_막으면_카카오_메시지가_붙는다() {
        assertThat(ProductNameValidator.validate("카카오 선물 세트", false)).isEqualTo(List.of(KAKAO));
    }

    @Test
    void 카카오를_허용하면_카카오_메시지가_붙지_않는다() {
        assertThat(ProductNameValidator.validate("카카오 선물 세트", true)).isEqualTo(List.of());
    }

    @Test
    void 카카오_허용_여부를_주지_않으면_막는_것과_같다() {
        assertThat(ProductNameValidator.validate("카카오 선물 세트"))
                .isEqualTo(ProductNameValidator.validate("카카오 선물 세트", false))
                .isEqualTo(List.of(KAKAO));
    }

    @Test
    void 길이와_특수_문자와_카카오가_함께_틀리면_세_메시지가_이_순서다() {
        String name = "카카오 무지개 우산 세트 A*";
        assertThat(name).hasSize(16);
        assertThat(ProductNameValidator.validate(name, false))
                .isEqualTo(List.of(TOO_LONG, NOT_ALLOWED_CHARACTER, KAKAO));
    }
}
