package gift.order;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationResolver authenticationResolver;
    private final KakaoMessageClient kakaoMessageClient;
    private final OrderService orderService;

    public OrderController(
            OrderRepository orderRepository,
            OptionRepository optionRepository,
            MemberRepository memberRepository,
            AuthenticationResolver authenticationResolver,
            KakaoMessageClient kakaoMessageClient,
            OrderService orderService) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.authenticationResolver = authenticationResolver;
        this.kakaoMessageClient = kakaoMessageClient;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(@RequestHeader("Authorization") String authorization, Pageable pageable) {
        // auth check
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        var orders = orderService.findOrders(member.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("Authorization") String authorization, @Valid @RequestBody OrderRequest request) {
        // auth check
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        var saved = orderService.createOrder(member, request).orElse(null);
        if (saved == null) {
            return ResponseEntity.notFound().build();
        }

        // best-effort kakao notification
        sendOrderNotificationIfPossible(member, saved, saved.getOption());
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getId()))
                .body(OrderResponse.from(saved));
    }

    private void sendOrderNotificationIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            var product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
    }
}
