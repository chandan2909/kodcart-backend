package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductsByCategory_returnsProductsForFoundCategory() {
        Category category = new Category(5, "shirts");
        Product product = new Product(7, "Shirt", null, null, null, category, null, null);
        when(categoryRepository.findByCategoryName("shirts")).thenReturn(java.util.Optional.of(category));
        when(productRepository.findByCategory_CategoryId(5)).thenReturn(List.of(product));

        List<Product> result = productService.getProductsByCategory("shirts");

        assertEquals(List.of(product), result);
    }

    @Test
    void getProductsByCategory_throws_whenCategoryNotFound() {
        when(categoryRepository.findByCategoryName("ghosts")).thenReturn(java.util.Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productService.getProductsByCategory("ghosts"));

        assertEquals("Category not found", ex.getMessage());
    }

    @Test
    void getProductsByCategory_returnsAll_whenNameBlank() {
        when(productRepository.findAll()).thenReturn(List.of(new Product()));

        assertEquals(1, productService.getProductsByCategory(null).size());
        assertEquals(1, productService.getProductsByCategory("").size());
        verify(productRepository, org.mockito.Mockito.times(2)).findAll();
    }

    @Test
    void getProductImages_mapsToUrlStrings() {
        Product product = new Product(7, "Shirt", null, null, null, null, null, null);
        when(productImageRepository.findByProduct_ProductId(7))
                .thenReturn(List.of(new ProductImage(product, "a.png"), new ProductImage(product, "b.png")));

        assertEquals(List.of("a.png", "b.png"), productService.getProductImages(7));
    }
}
