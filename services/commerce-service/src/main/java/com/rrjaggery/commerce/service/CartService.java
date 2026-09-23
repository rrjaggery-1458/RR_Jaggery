package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.AddToCartRequest;
import com.rrjaggery.commerce.dto.CartDto;
import com.rrjaggery.commerce.dto.CartItemDto;
import com.rrjaggery.commerce.entity.CartItem;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.repository.CartItemRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CartService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.05"); // 5% GST for jaggery products
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("50.00");

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartDto getCart(UUID userId, String customerType) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<CartItemDto> itemDtos = new ArrayList<>();
        int totalItems = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product product = item.getProduct();
            BigDecimal applicablePrice = resolvePrice(product, item.getQuantity(), customerType);
            CartItemDto dto = CartItemDto.fromEntity(item, applicablePrice);
            itemDtos.add(dto);

            totalItems += item.getQuantity();
            subtotal = subtotal.add(dto.getLineTotal());
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingAmount = BigDecimal.ZERO;
        if (totalItems > 0 && subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0) {
            shippingAmount = STANDARD_SHIPPING_FEE;
        }
        BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount).setScale(2, RoundingMode.HALF_UP);

        return new CartDto(itemDtos, totalItems, subtotal, taxAmount, shippingAmount, totalAmount);
    }

    public CartItemDto addToCart(UUID userId, AddToCartRequest request, String customerType) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + request.getProductId()));

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product '" + product.getName() + "' is currently inactive and cannot be added to cart.");
        }

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, product.getId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.getQuantity());
                    return existing;
                })
                .orElseGet(() -> new CartItem(userId, product, request.getQuantity()));

        CartItem saved = cartItemRepository.save(cartItem);
        BigDecimal price = resolvePrice(product, saved.getQuantity(), customerType);
        return CartItemDto.fromEntity(saved, price);
    }

    public CartItemDto updateQuantity(UUID userId, UUID cartItemId, int quantity, String customerType) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
            return null;
        }

        cartItem.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(cartItem);
        BigDecimal price = resolvePrice(saved.getProduct(), saved.getQuantity(), customerType);
        return CartItemDto.fromEntity(saved, price);
    }

    public void removeItem(UUID userId, UUID cartItemId) {
        cartItemRepository.deleteByUserIdAndId(userId, cartItemId);
    }

    public void clearCart(UUID userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    public static BigDecimal resolvePrice(Product product, int quantity, String customerType) {
        if ("REGISTERED_WHOLESALE".equalsIgnoreCase(customerType) && quantity >= product.getWholesaleMoq()) {
            return product.getWholesalePrice();
        }
        return product.getRetailPrice();
    }
}
