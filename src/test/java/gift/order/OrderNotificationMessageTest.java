package gift.order;

import static org.assertj.core.api.Assertions.assertThat;

import gift.category.Category;
import gift.option.Option;
import gift.product.Product;
import org.junit.jupiter.api.Test;

/**
 * 주문 알림 문구의 금액이 int 곱셈으로 넘치는 지금 작동을 고정한다(open-questions 29번).
 * docs/work/09-domain-responsibility/tasks.md T1 참고.
 */
class OrderNotificationMessageTest {

    // 3,360,000 × 1,279 = 4,297,440,000 → int로 넘쳐 4,297,440,000 − 4,294,967,296 = 2,472,704
    private static final String TEMPLATE_OVERFLOWED_AMOUNT = """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n맥북 (스페이스 블랙)\\n수량: 1279개\\n금액: 2,472,704원",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """;

    @Test
    void 주문_금액이_int_범위를_넘으면_넘친_금액이_문구에_들어간다() {
        Category category = new Category("교환권", "#6c95d1", "https://example.com/category.jpg", "설명");
        Product product = new Product("맥북", 3_360_000, "https://example.com/macbook.jpg", category);
        Option option = new Option(product, "스페이스 블랙", 2000);

        String message = OrderNotificationMessage.build(new Order(option, 2L, 1279, null), product);

        assertThat(message).isEqualTo(TEMPLATE_OVERFLOWED_AMOUNT);
    }
}
