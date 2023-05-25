package com.psk.eshop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.psk.eshop.dto.ProductRequestDTO;
import com.psk.eshop.enums.OrderStatus;
import com.psk.eshop.model.Order;
import com.psk.eshop.model.Product;
import com.psk.eshop.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ProductDateServiceImpl implements ProductService{
    private ProductService productService;
    private ProductRepository productRepository;

    @Override
    public Product createProduct(ProductRequestDTO productRequest, MultipartFile file) {
        var newProduct = Product.builder()
                .userEmail(productRequest.getUserEmail())
                .discountId(productRequest.getDiscountId())
                .price(productRequest.getPrice())
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .picturePath(getCloudinaryPicture(file))
                .quantity(productRequest.getQuantity())
                .build();
        return productRepository.save(newProduct);
    }

    @Override
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
    @Override
    public Product getProductById(Long productId) {
        Product product = productService.getProductById(productId);
        product.setDateReceived(Timestamp.from(Instant.now()));
        return product;
    }
    @Override
    public Product updateProduct(Long productId, ProductRequestDTO productRequest, MultipartFile file) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return productRepository.findById(productId)
                .map(product -> {
                    product.setUserEmail(productRequest.getUserEmail());
                    product.setDiscountId(productRequest.getDiscountId());
                    product.setPrice(productRequest.getPrice());
                    product.setName(productRequest.getName());
                    product.setDescription(productRequest.getDescription());
                    product.setPicturePath(getCloudinaryPicture(file));
                    product.setQuantity(productRequest.getQuantity());
                    return productRepository.save(product);
                })
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Product with id %d not found", productId))
                );
    }

    @Override
    public void deleteProductById(Long productId) {
        Product product = getProductById(productId);

        if (hasActiveOrdersWithProduct(product)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("Cannot delete product. There are active orders with this product (productId = %d).", productId));
        }

        productRepository.deleteById(productId);
    }
    @Override
    public Long getProductQuantityById(Long productId){
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Product with id %d not found", productId))
        );
        return product.getQuantity();
    }

    private boolean hasActiveOrdersWithProduct(Product product) {
        for (Order order : product.getOrders()) {
            OrderStatus orderStatus = order.getOrderStatus();
            if (orderStatus != OrderStatus.REJECTED) {
                return true;
            }
        }
        return false;
    }

    private String getCloudinaryPicture(MultipartFile file){
        Map config = new HashMap();
        config.put("cloud_name", "drlkduluz");
        config.put("api_key", "138657524274591");
        config.put("api_secret", "ov27viA2NibOXi9eAZHXh05IiSI");
        Cloudinary cloudinary = new Cloudinary(config);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        }
        catch (IOException e){
            return null;
        }
    }
}
