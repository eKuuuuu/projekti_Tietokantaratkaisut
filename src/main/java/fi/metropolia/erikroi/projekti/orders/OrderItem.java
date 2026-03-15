package fi.metropolia.erikroi.projekti.orders;

import com.fasterxml.jackson.annotation.JsonBackReference;
import fi.metropolia.erikroi.projekti.products.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "orderitems")
@IdClass(OrderItemId.class)
public class OrderItem {

    @JsonBackReference
    @Id
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Id
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    @Column(name = "unit_price")
    private Double unitPrice;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return null;
    }
}