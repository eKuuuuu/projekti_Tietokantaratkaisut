package fi.metropolia.erikroi.projekti.orders;

import fi.metropolia.erikroi.projekti.common.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    private OrderDTO convertToDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                )).toList()
        );
    }

    // Example URL: http://localhost:8081/api/orders?page=0&size=10
    @GetMapping
    public List<OrderDTO> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Order> orders = orderRepository.findAll(PageRequest.of(page, size)).getContent();

        return orders.stream().map(order -> new OrderDTO(
                order.getId(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                )).toList()
        )).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getOrderDate(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getItems().stream().map(item -> new OrderItemDTO(
                                item.getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        )).toList()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/total")
    public ResponseEntity<Double> getOrderTotal(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> ResponseEntity.ok(order.getTotalAmount()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<Map<String, Object>> getReceipt(@PathVariable Long id) {
        return orderRepository.findById(id).map(order -> {
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("orderId", order.getId());
            receipt.put("date", order.getOrderDate());
            receipt.put("customer", order.getCustomerId());
            receipt.put("items", order.getItems().stream().map(item ->
                    item.getProduct().getName() + " x" + item.getQuantity() + " @ " + item.getUnitPrice()
            ).toList());
            receipt.put("total", order.getTotalAmount());
            receipt.put("status", order.getStatus());
            return ResponseEntity.ok(receipt);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            return ResponseEntity.ok(convertToDTO(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
}