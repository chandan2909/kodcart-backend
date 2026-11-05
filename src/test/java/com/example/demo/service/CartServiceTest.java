package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private CartService cartService;

    private User customer() {
        return new User(1, "chandan", "c@x.io", "hash", Role.CUSTOMER,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Product shirt() {
        Product product = new Product(7, "Shirt", "Cotton", new BigDecimal("500.00"), 10, null,
                LocalDateTime.now(), LocalDateTime.now());
        return product;
    }

    @Test
    void addToCart_createsNewItem_whenProductNotInCart() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.of(shirt()));
        when(cartRepository.findByUserAndProduct(1, 7)).thenReturn(Optional.empty());

        cartService.addToCart(1, 7, 2);

        verify(cartRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_incrementsExistingQuantity() {
        Product product = shirt();
        CartItem existing = new CartItem(5, customer(), product, 2);
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(1, 7)).thenReturn(Optional.of(existing));

        cartService.addToCart(1, 7, 3);

        assertEquals(5, existing.getQuantity());
        verify(cartRepository).save(existing);
    }

    @Test
    void addToCart_throws_whenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cartService.addToCart(99, 7, 1));

        assertEquals("User not found with ID: 99", ex.getMessage());
    }

    @Test
    void addToCart_throws_whenProductMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cartService.addToCart(1, 7, 1));

        assertEquals("Product not found with ID: 7", ex.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getCartItems_buildsResponseWithTotalsAndFirstImage() {
        Product product = shirt();
        User user = customer();
        CartItem item = new CartItem(5, user, product, 2);
        when(cartRepository.findCartItemsWithProductDetails(1)).thenReturn(List.of(item));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(productImageRepository.findByProduct_ProductId(7))
                .thenReturn(List.of(new ProductImage(product, "https://img/shirt.png")));

        Map<String, Object> response = cartService.getCartItems(1);

        assertEquals("chandan", response.get("username"));
        assertEquals("CUSTOMER", response.get("role"));
        Map<String, Object> cart = (Map<String, Object>) response.get("cart");
        assertEquals(1000.0, ((Number) cart.get("overall_total_price")).doubleValue(), 0.001);
        List<Map<String, Object>> products = (List<Map<String, Object>>) cart.get("products");
        assertEquals(1, products.size());
        assertEquals("https://img/shirt.png", products.get(0).get("image_url"));
        assertEquals(1000.0, ((Number) products.get(0).get("total_price")).doubleValue(), 0.001);
        assertEquals(2, products.get(0).get("quantity"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getCartItems_usesDefaultImage_whenNoImages() {
        Product product = shirt();
        User user = customer();
        CartItem item = new CartItem(5, user, product, 1);
        when(cartRepository.findCartItemsWithProductDetails(1)).thenReturn(List.of(item));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(productImageRepository.findByProduct_ProductId(7)).thenReturn(List.of());

        Map<String, Object> response = cartService.getCartItems(1);

        Map<String, Object> cart = (Map<String, Object>) response.get("cart");
        List<Map<String, Object>> products = (List<Map<String, Object>>) cart.get("products");
        assertEquals("default-image-url", products.get(0).get("image_url"));
    }

    @Test
    void updateCartItemQuantity_zeroDeletesItem() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.of(shirt()));
        when(cartRepository.findByUserAndProduct(1, 7))
                .thenReturn(Optional.of(new CartItem(5, customer(), shirt(), 2)));

        cartService.updateCartItemQuantity(1, 7, 0);

        verify(cartRepository).deleteCartItem(1, 7);
        verify(cartRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateCartItemQuantity_nonZeroSavesNewQuantity() {
        CartItem existing = new CartItem(5, customer(), shirt(), 2);
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.of(shirt()));
        when(cartRepository.findByUserAndProduct(1, 7)).thenReturn(Optional.of(existing));

        cartService.updateCartItemQuantity(1, 7, 4);

        assertEquals(4, existing.getQuantity());
        verify(cartRepository).save(existing);
    }

    @Test
    void deleteCartItem_throws_whenProductMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> cartService.deleteCartItem(1, 7));
        verify(cartRepository, never()).deleteCartItem(anyInt(), anyInt());
    }

    @Test
    void deleteCartItem_deletesWhenUserAndProductExist() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(7)).thenReturn(Optional.of(shirt()));

        cartService.deleteCartItem(1, 7);

        verify(cartRepository).deleteCartItem(1, 7);
    }
}
