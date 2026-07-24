package com.possystem.service;

import com.possystem.dto.PosProductResponse;
import com.possystem.dto.ProductRequest;
import com.possystem.dto.ProductResponse;
import com.possystem.dto.ProductStatusRequest;
import com.possystem.entity.Category;
import com.possystem.entity.Product;
import com.possystem.repository.CategoryRepository;
import com.possystem.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProductService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Path productsUploadDirectory;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            @Value("${app.upload.products-dir}") String productsUploadDirectory
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productsUploadDirectory = Paths.get(productsUploadDirectory)
                .toAbsolutePath()
                .normalize();
    }


    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Category category = findCategoryById(request.getCategoryId());

        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot create a product under an inactive category"
            );
        }

        String sku = normalizeSku(request.getSku());
        String barcode = cleanOptionalText(request.getBarcode());

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product with this SKU already exists"
            );
        }

        if (barcode != null && productRepository.existsByBarcode(barcode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product with this barcode already exists"
            );
        }

        Product product = Product.builder()
                .category(category)
                .name(request.getName().trim())
                .sku(sku)
                .barcode(barcode)
                .brand(cleanOptionalText(request.getBrand()))
                .description(cleanOptionalText(request.getDescription()))
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .currentStock(BigDecimal.ZERO)
                .reorderLevel(request.getReorderLevel())
                .status("ACTIVE")
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToProductResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {

        return productRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PosProductResponse> getActiveProductsForPos() {

        return productRepository
                .findActiveProductsOrderByName()
                .stream()
                .map(this::mapToPosProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        Product product = findProductById(id);

        return mapToProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<PosProductResponse> searchActiveProductsForPos(
            String query
    ) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search query must contain at least 2 characters"
            );
        }



        String cleanedQuery = query.trim();

        return productRepository
                .searchActiveProductsByNameOrSku(cleanedQuery)
                .stream()
                .map(this::mapToPosProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosProductResponse getActiveProductForPosByBarcode(
            String barcode
    ) {
        String cleanedBarcode = cleanOptionalText(barcode);

        if (cleanedBarcode == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Barcode is required"
            );
        }

        Product product = productRepository
                .findActiveByBarcode(cleanedBarcode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active product not found for barcode: " + cleanedBarcode
                ));

        return mapToPosProductResponse(product);
    }

    @Transactional
    public ProductResponse uploadProductImage(
            Long id,
            MultipartFile image
    ) {
        Product product = findProductById(id);

        validateProductImage(image);

        String contentType = image.getContentType();
        String fileExtension = "image/png".equalsIgnoreCase(contentType)
                ? ".png"
                : ".jpg";

        String generatedFileName = UUID.randomUUID() + fileExtension;

        try {
            Files.createDirectories(productsUploadDirectory);

            Path targetFile = productsUploadDirectory
                    .resolve(generatedFileName)
                    .normalize();

            /*
             * Security check:
             * Ensures the generated file stays inside uploads/products.
             */
            if (!targetFile.startsWith(productsUploadDirectory)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid image file path"
                );
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(
                        inputStream,
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            product.setImageUrl(
                    "/uploads/products/" + generatedFileName
            );

            Product updatedProduct = productRepository.saveAndFlush(product);

            return mapToProductResponse(updatedProduct);

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save product image"
            );
        }
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = findProductById(id);
        Category newCategory = findCategoryById(request.getCategoryId());

        /*
         * A product may remain in its existing category even if an Admin
         * later deactivates that category.
         *
         * But the Admin cannot move a product into a different inactive category.
         */
        boolean categoryChanged =
                !product.getCategory().getId().equals(newCategory.getId());

        if (categoryChanged
                && !"ACTIVE".equalsIgnoreCase(newCategory.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot move a product to an inactive category"
            );
        }

        String updatedSku = normalizeSku(request.getSku());
        String updatedBarcode = cleanOptionalText(request.getBarcode());

        Optional<Product> productWithSameSku =
                productRepository.findBySkuIgnoreCase(updatedSku);

        if (productWithSameSku.isPresent()
                && !productWithSameSku.get().getId().equals(product.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another product with this SKU already exists"
            );
        }

        if (updatedBarcode != null) {

            Optional<Product> productWithSameBarcode =
                    productRepository.findByBarcode(updatedBarcode);

            if (productWithSameBarcode.isPresent()
                    && !productWithSameBarcode.get().getId().equals(product.getId())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Another product with this barcode already exists"
                );
            }
        }

        product.setCategory(newCategory);
        product.setName(request.getName().trim());
        product.setSku(updatedSku);
        product.setBarcode(updatedBarcode);
        product.setBrand(cleanOptionalText(request.getBrand()));
        product.setDescription(cleanOptionalText(request.getDescription()));
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setReorderLevel(request.getReorderLevel());

        /*
         * currentStock is intentionally not updated here.
         * It will be changed only through the Inventory module.
         */

        Product updatedProduct = productRepository.saveAndFlush(product);

        return mapToProductResponse(updatedProduct);
    }

    @Transactional
    public ProductResponse updateProductStatus(
            Long id,
            ProductStatusRequest request
    ) {

        Product product = findProductById(id);

        String newStatus = request.getStatus()
                .trim()
                .toUpperCase(Locale.ROOT);

        /*
         * A product cannot be activated while its category is inactive.
         */
        if ("ACTIVE".equals(newStatus)
                && !"ACTIVE".equalsIgnoreCase(
                product.getCategory().getStatus()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot activate this product because its category is inactive"
            );
        }

        product.setStatus(newStatus);

        Product updatedProduct = productRepository.saveAndFlush(product);

        return mapToProductResponse(updatedProduct);
    }
    private Product findProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with ID: " + id
                ));
    }

    private Category findCategoryById(Long categoryId) {

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found with ID: " + categoryId
                ));
    }

    private PosProductResponse mapToPosProductResponse(
            Product product
    ) {
        return PosProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .sellingPrice(product.getSellingPrice())
                .currentStock(product.getCurrentStock())
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .currentStock(product.getCurrentStock())
                .reorderLevel(product.getReorderLevel())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String cleanOptionalText(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validateProductImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product image file is required"
            );
        }

        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product image must not exceed 5 MB"
            );
        }

        String contentType = image.getContentType();

        if (contentType == null
                || !ALLOWED_IMAGE_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPG, JPEG, and PNG image files are allowed"
            );
        }
    }
}
