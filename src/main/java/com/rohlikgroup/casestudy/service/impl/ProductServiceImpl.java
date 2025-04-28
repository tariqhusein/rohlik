package com.rohlikgroup.casestudy.service.impl;

import com.rohlikgroup.casestudy.dto.ProductDto;
import com.rohlikgroup.casestudy.entity.OrderStatus;
import com.rohlikgroup.casestudy.entity.Product;
import com.rohlikgroup.casestudy.mapper.ProductMapper;
import com.rohlikgroup.casestudy.repository.OrderRepository;
import com.rohlikgroup.casestudy.repository.ProductRepository;
import com.rohlikgroup.casestudy.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto product) {
        // Validate product doesn't exist with same name
        if (productRepository.existsByName(product.name())) {
            throw new IllegalStateException("Product with name '" + product.name() + "' already exists");
        }

        var newProductEntity = productMapper.map(product);
        
        // Additional validation
        validateProduct(newProductEntity);
        
        return productMapper.map(productRepository.save(newProductEntity));
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        // Check if product exists
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found with id: " + productId);
        }

        boolean isProductInActiveOrders = orderRepository.existsByOrderItemsProductIdAndStatusNotIn(
                productId, 
                OrderStatus.getInactiveStatuses()
        );
        
        if (isProductInActiveOrders) {
            throw new IllegalStateException("Cannot delete product that is part of an active order.");
        }
        
        productRepository.deleteById(productId);
        log.info("Product with id {} has been deleted", productId);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(Long productId, ProductDto updatedProduct) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        // Check if name is being updated and if it conflicts with existing products
        if (updatedProduct.name() != null && 
            !updatedProduct.name().equals(existingProduct.getName()) && 
            productRepository.existsByName(updatedProduct.name())) {
            throw new IllegalStateException("Product with name '" + updatedProduct.name() + "' already exists");
        }

        // Update only non-null fields (partial update support)
        if (updatedProduct.name() != null) {
            existingProduct.setName(updatedProduct.name());
        }
        if (updatedProduct.price() != null) {
            existingProduct.setPrice(updatedProduct.price());
        }
        if (updatedProduct.quantityInStock() != null) {
            existingProduct.setStockAmount(updatedProduct.quantityInStock());
        }

        // Validate the updated product
        validateProduct(existingProduct);

        Product savedProduct = productRepository.save(existingProduct);
        log.info("Product with id {} has been updated", productId);
        return productMapper.map(savedProduct);
    }

    @Override
    public List<ProductDto> getProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::map)
                .toList();
    }

    @Override
    public ProductDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
        return productMapper.map(product);
    }

    private void validateProduct(Product product) {
        Objects.requireNonNull(product.getName(), "Product name cannot be null");
        Objects.requireNonNull(product.getPrice(), "Product price cannot be null");
        Objects.requireNonNull(product.getStockAmount(), "Product stock amount cannot be null");

        if (product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (product.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Product price cannot be negative");
        }
        if (product.getStockAmount() < 0) {
            throw new IllegalArgumentException("Product stock amount cannot be negative");
        }
    }
}

