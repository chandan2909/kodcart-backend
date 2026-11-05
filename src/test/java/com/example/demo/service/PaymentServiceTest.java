package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.entity.CartItem;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String SECRET = "test_secret_123";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(orderRepository, orderItemRepository, cartRepository);
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", SECRET);
    }

    private String signature(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private CartItem cartItem(int productId, int quantity, String price) {
        Product product = new Product(productId, "Shirt", null, new BigDecimal(price), 10, null, null, null);
        User user = new User(1, "chandan", "c@x.io", "hash", com.example.demo.entity.Role.CUSTOMER, null, null);
        return new CartItem(null, user, product, quantity);
    }

    private Order order(String id) {
        Order order = new Order();
        order.setOrderId(id);
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @Test
    void verifyPayment_returnsFalse_whenSignatureInvalid() throws Exception {
        boolean result = paymentService.verifyPayment("ORD-1", "pay_bad", "not-a-signature", 1);

        assertFalse(result);
        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void verifyPayment_marksSuccessConvertsCartAndClearsIt_whenSignatureValid() throws Exception {
        String sig = signature("ORD-1", "pay_1");
        Order order = order("ORD-1");
        when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order));
        when(cartRepository.findCartItemsWithProductDetails(1)).thenReturn(List.of(cartItem(7, 2, "500.00")));

        boolean result = paymentService.verifyPayment("ORD-1", "pay_1", sig, 1);

        assertTrue(result);
        assertEquals(OrderStatus.SUCCESS, order.getStatus());
        verify(orderRepository).save(order);
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartRepository).deleteAllCartItemsByUserId(1);
    }

    @Test
    void verifyPayment_returnsFalse_whenOrderMissing() throws Exception {
        String sig = signature("ORD-MISSING", "pay_1");
        when(orderRepository.findById("ORD-MISSING")).thenReturn(Optional.empty());

        boolean result = paymentService.verifyPayment("ORD-MISSING", "pay_1", sig, 1);

        assertFalse(result);
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).deleteAllCartItemsByUserId(1);
    }

    @Test
    void saveOrderItems_attachesOrderAndSavesEachItem() {
        Order order = order("ORD-1");
        when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order));
        OrderItem item = new OrderItem();
        item.setProductId(7);

        paymentService.saveOrderItems("ORD-1", List.of(item, new OrderItem()));

        assertEquals(order, item.getOrder());
        verify(orderItemRepository, org.mockito.Mockito.times(2)).save(any(OrderItem.class));
    }

    @Test
    void saveOrderItems_throws_whenOrderMissing() {
        when(orderRepository.findById("ORD-X")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.saveOrderItems("ORD-X", List.of(new OrderItem())));
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }
}
