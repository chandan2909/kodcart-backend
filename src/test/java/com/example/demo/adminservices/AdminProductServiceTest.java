package com.example.demo.adminservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminProductService adminProductService;

    @Test
    void addProductWithImage_savesProductAndImage() {
        Category category = new Category(5, "shirts");
        when(categoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = adminProductService.addProductWithImage("Shirt", "Cotton", 499.0, 10, 5, "https://img/shirt.png");

        assertEquals("Shirt", saved.getName());
        assertEquals(499.0, saved.getPrice().doubleValue());
        assertEquals(10, saved.getStock());
        assertEquals(category, saved.getCategory());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void addProductWithImage_throws_whenCategoryInvalid() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminProductService.addProductWithImage("X", null, 1.0, 1, 99, "img.png"));

        assertEquals("Invalid category ID", ex.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void addProductWithImage_throws_whenImageUrlBlank() {
        when(categoryRepository.findById(5)).thenReturn(Optional.of(new Category(5, "shirts")));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminProductService.addProductWithImage("Shirt", null, 499.0, 10, 5, ""));

        assertEquals("Product image URL cannot be empty", ex.getMessage());
        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void deleteProduct_deletesImagesThenProduct_whenExists() {
        when(productRepository.existsById(7)).thenReturn(true);

        adminProductService.deleteProduct(7);

        verify(productImageRepository).deleteByProductId(7);
        verify(productRepository).deleteById(7);
    }

    @Test
    void deleteProduct_throws_whenMissing() {
        when(productRepository.existsById(7)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminProductService.deleteProduct(7));

        assertEquals("Product not found", ex.getMessage());
        verify(productImageRepository, never()).deleteByProductId(7);
        verify(productRepository, never()).deleteById(7);
    }
}
