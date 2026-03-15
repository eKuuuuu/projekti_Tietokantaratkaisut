package fi.metropolia.erikroi.projekti.products;

import java.util.List;

public interface ProductCustomRepository {
    List<Product> findProductsDynamically(String name, Double minPrice);
}