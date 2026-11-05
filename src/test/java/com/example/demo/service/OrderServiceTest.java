package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private OrderService orderService;

    private User customer() {
        return new User(1, "chandan", "c@x.io", "hash", Role.CUSTOMER, null, null);
    }

    private Order order(String id) {
        Order order = new Order();
        order.setOrderId(id);
        return order;
    }

    private Product product(int id) {
        return new Product(id, "Shirt", "Cotton", new BigDecimal("500.00"), 10, null, null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getOrdersForUser_mapsSuccessfulOrderItems() {
        User user = customer();
        Order order = order("ORD-1");
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(7);
        item.setQuantity(2);
        item.setPricePerUnit(new BigDecimal("500.00"));
        item.setTotalPrice(new BigDecimal("1000.00"));
        when(orderItemRepository.findSuccessfulOrderItemsByUserId(1)).thenReturn(List.of(item));
        when(productRepository.findById(7)).thenReturn(java.util.Optional.of(product(7)));
        when(productImageRepository.findByProduct_ProductId(7))
                .thenReturn(List.of(new ProductImage(product(7), "https://img/shirt.png")));

        Map<String, Object> response = orderService.getOrdersForUser(user);

        assertEquals("chandan", response.get("username"));
        assertEquals(Role.CUSTOMER, response.get("role"));
        List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("products");
        assertEquals(1, products.size());
        assertEquals("ORD-1", products.get(0).get("order_id"));
        assertEquals(7, products.get(0).get("product_id"));
        assertEquals(2, products.get(0).get("quantity"));
        assertEquals(new BigDecimal("1000.00"), products.get(0).get("total_price"));
        assertEquals("https://img/shirt.png", products.get(0).get("image_url"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getOrdersForUser_skipsItemsWhoseProductIsGone() {
        User user = customer();
        OrderItem item = new OrderItem();
        item.setOrder(order("ORD-1"));
        item.setProductId(999);
        item.setQuantity(1);
        when(orderItemRepository.findSuccessfulOrderItemsByUserId(1)).thenReturn(List.of(item));
        when(productRepository.findById(999)).thenReturn(java.util.Optional.empty());

        Map<String, Object> response = orderService.getOrdersForUser(user);

        List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("products");
        assertTrue(products.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getOrdersForUser_setsNullImage_whenProductHasNoImages() {
        User user = customer();
        OrderItem item = new OrderItem();
        item.setOrder(order("ORD-1"));
        item.setProductId(7);
        item.setQuantity(1);
        when(orderItemRepository.findSuccessfulOrderItemsByUserId(1)).thenReturn(List.of(item));
        when(productRepository.findById(7)).thenReturn(java.util.Optional.of(product(7)));
        when(productImageRepository.findByProduct_ProductId(7)).thenReturn(List.of());

        Map<String, Object> response = orderService.getOrdersForUser(user);

        List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("products");
        assertEquals(null, products.get(0).get("image_url"));
    }
}
