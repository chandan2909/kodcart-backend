package com.example.demo.adminservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class AdminBusinessServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminBusinessService adminBusinessService;

    private Order order(String id, String amount) {
        Order order = new Order();
        order.setOrderId(id);
        order.setTotalAmount(new BigDecimal(amount));
        order.setStatus(OrderStatus.SUCCESS);
        return order;
    }

    private OrderItem item(String orderId, int productId, int quantity) {
        Order order = order(orderId, "0");
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void calculateMonthlyBusiness_rejectsInvalidMonthAndYear() {
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateMonthlyBusiness(0, 2026));
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateMonthlyBusiness(13, 2026));
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateMonthlyBusiness(6, 1999));
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateMonthlyBusiness(6, 2101));
    }

    @Test
    void calculateMonthlyBusiness_aggregatesTotalsAndCategorySales() {
        when(orderRepository.findSuccessfulOrdersByMonthAndYear(11, 2026))
                .thenReturn(List.of(order("o1", "1000.00"), order("o2", "500.00")));
        when(orderItemRepository.findByOrderId("o1")).thenReturn(List.of(item("o1", 7, 2), item("o1", 8, 3)));
        when(orderItemRepository.findByOrderId("o2")).thenReturn(List.of(item("o2", 7, 1)));
        when(productRepository.findCategoryNameByProductId(7)).thenReturn("shirts");
        when(productRepository.findCategoryNameByProductId(8)).thenReturn("pants");

        Map<String, Object> report = adminBusinessService.calculateMonthlyBusiness(11, 2026);

        assertEquals(1500.0, (Double) report.get("totalBusiness"), 0.001);
        Map<String, Integer> categorySales = (Map<String, Integer>) report.get("categorySales");
        assertEquals(3, categorySales.get("shirts"));
        assertEquals(3, categorySales.get("pants"));
    }

    @Test
    void calculateMonthlyBusiness_returnsZeroes_whenNoOrders() {
        when(orderRepository.findSuccessfulOrdersByMonthAndYear(1, 2026)).thenReturn(List.of());

        Map<String, Object> report = adminBusinessService.calculateMonthlyBusiness(1, 2026);

        assertEquals(0.0, (Double) report.get("totalBusiness"), 0.001);
        assertEquals(0, ((Map<?, ?>) report.get("categorySales")).size());
    }

    @Test
    void calculateDailyBusiness_rejectsNullDate() {
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateDailyBusiness(null));
    }

    @Test
    void calculateDailyBusiness_aggregatesOrdersForDate() {
        when(orderRepository.findSuccessfulOrdersByDate(LocalDate.of(2026, 11, 5)))
                .thenReturn(List.of(order("o1", "250.00")));
        when(orderItemRepository.findByOrderId("o1")).thenReturn(List.of(item("o1", 7, 4)));
        when(productRepository.findCategoryNameByProductId(7)).thenReturn("electronics");

        Map<String, Object> report = adminBusinessService.calculateDailyBusiness(LocalDate.of(2026, 11, 5));

        assertEquals(250.0, (Double) report.get("totalBusiness"), 0.001);
        assertEquals(4, ((Map<String, Integer>) report.get("categorySales")).get("electronics"));
    }

    @Test
    void calculateYearlyBusiness_rejectsInvalidYear() {
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateYearlyBusiness(1900));
        assertThrows(IllegalArgumentException.class, () -> adminBusinessService.calculateYearlyBusiness(2200));
    }

    @Test
    void calculateYearlyBusiness_aggregatesOrdersForYear() {
        when(orderRepository.findSuccessfulOrdersByYear(2026)).thenReturn(List.of(order("o1", "999.00")));
        when(orderItemRepository.findByOrderId("o1")).thenReturn(List.of(item("o1", 7, 1)));
        when(productRepository.findCategoryNameByProductId(7)).thenReturn("shirts");

        Map<String, Object> report = adminBusinessService.calculateYearlyBusiness(2026);

        assertEquals(999.0, (Double) report.get("totalBusiness"), 0.001);
    }

    @Test
    void calculateOverallBusiness_usesRepositoryAggregateAndCategorySales() {
        when(orderRepository.calculateOverallBusiness()).thenReturn(new BigDecimal("2500.00"));
        when(orderRepository.findAllByStatus(OrderStatus.SUCCESS))
                .thenReturn(List.of(order("o1", "1500.00"), order("o2", "1000.00")));
        when(orderItemRepository.findByOrderId("o1")).thenReturn(List.of(item("o1", 7, 2)));
        when(orderItemRepository.findByOrderId("o2")).thenReturn(List.of(item("o2", 8, 5)));
        when(productRepository.findCategoryNameByProductId(7)).thenReturn("shirts");
        when(productRepository.findCategoryNameByProductId(8)).thenReturn("mobiles");

        Map<String, Object> report = adminBusinessService.calculateOverallBusiness();

        assertEquals(2500.0, (Double) report.get("totalBusiness"), 0.001);
        Map<String, Integer> categorySales = (Map<String, Integer>) report.get("categorySales");
        assertEquals(2, categorySales.get("shirts"));
        assertEquals(5, categorySales.get("mobiles"));
    }
}
