package fi.metropolia.erikroi.projekti.products;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public int increaseCategoryPrices(Integer categoryId, Double multiplier) {
        return productRepository.updatePricesByCategory(categoryId, multiplier);
    }

    public List<Product> searchProducts(String name, Double minPrice) {
        return productRepository.findProductsDynamically(name, minPrice);
    }
}