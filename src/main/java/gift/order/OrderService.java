package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;

    public OrderService(
            OrderRepository orderRepository, OptionRepository optionRepository, MemberRepository memberRepository) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
    }

    public Page<OrderResponse> findOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    // order flow:
    // 1. auth check
    // 2. validate option
    // 3. subtract stock
    // 4. deduct points
    // 5. save order
    // 6. cleanup wish
    // 7. send kakao notification

    public Optional<Order> createOrder(Member member, OrderRequest request) {
        // validate option
        var option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return Optional.empty();
        }

        // subtract stock
        option.subtractStock(request.quantity());
        optionRepository.save(option);

        // deduct points
        var totalPrice = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(totalPrice);
        memberRepository.save(member);

        // save order
        var saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));
        return Optional.of(saved);
    }
}
