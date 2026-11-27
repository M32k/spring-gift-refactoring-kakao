package gift.option;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * OptionNameValidator의 오류 메시지 목록 전체(문자열과 순서)를 지금 그대로 고정한다.
 * 메시지는 OptionNameValidator 소스에서 그대로 복사했다.
 * docs/work/09-domain-responsibility/tasks.md T2 참고.
 */
class OptionNameValidatorTest {

    private static final String REQUIRED = "옵션 이름은 필수입니다.";
    private static final String TOO_LONG = "옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.";
    private static final String NOT_ALLOWED_CHARACTER =
            "옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _";

    // "가 "를 25번 이으면 공백을 포함해 50자다.
    private static final String NAME_50 = "가 ".repeat(25);
    private static final String NAME_51 = NAME_50 + "나";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void 이름이_없거나_공백뿐이면_필수_메시지_하나뿐이다(String name) {
        assertThat(OptionNameValidator.validate(name)).isEqualTo(List.of(REQUIRED));
    }

    @Test
    void 공백을_포함해_50자인_이름은_오류가_없다() {
        assertThat(NAME_50).hasSize(50);
        assertThat(OptionNameValidator.validate(NAME_50)).isEqualTo(List.of());
    }

    @Test
    void 공백을_포함해_51자인_이름은_길이_메시지가_나온다() {
        assertThat(NAME_51).hasSize(51);
        assertThat(OptionNameValidator.validate(NAME_51)).isEqualTo(List.of(TOO_LONG));
    }

    @Test
    void 허용되지_않는_특수_문자가_있으면_특수_문자_메시지가_나온다() {
        assertThat(OptionNameValidator.validate("골드*")).isEqualTo(List.of(NOT_ALLOWED_CHARACTER));
    }

    @Test
    void 길이와_특수_문자가_함께_틀리면_길이_특수_문자_순서다() {
        String name = NAME_50 + "*";
        assertThat(name).hasSize(51);
        assertThat(OptionNameValidator.validate(name)).isEqualTo(List.of(TOO_LONG, NOT_ALLOWED_CHARACTER));
    }
}
