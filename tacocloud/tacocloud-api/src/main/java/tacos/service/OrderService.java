package tacos.service;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.inventory.InventoryService;
import tacos.web.api.OrderPatchRequest;
import tacos.web.api.OrderReplaceRequest;

import reactor.core.publisher.Flux;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository repo, InventoryService inventoryService) {
        this.repo = repo;
        this.inventoryService = inventoryService;
    }

    public Mono<TacoOrder> patchOrder(
            String orderId,
            OrderPatchRequest patch,
            User authenticatedUser) {

        return findAuthorizedOrder(
                orderId,
                authenticatedUser)
            .flatMap(order -> {
                applyPatch(order, patch);
                return repo.save(order);
            });
    }

    public Mono<TacoOrder> replaceOrder(
            String orderId,
            OrderReplaceRequest request,
            User authenticatedUser) {

        return findAuthorizedOrder(
                orderId,
                authenticatedUser)
            .flatMap(order -> {
                if (!isMutable(order)) {
                    return stateConflict(order);
                }

                applyReplacement(order, request);
                return repo.save(order);
            });
    }

    public Mono<Void> deleteOrder(
            String orderId,
            User authenticatedUser) {

        return findAuthorizedOrder(
                orderId,
                authenticatedUser)
            .flatMap(order -> {
                if (!isMutable(order)) {
                    return stateConflict(order);
                }

                return inventoryService.release(order.getId()).then(repo.delete(order));
            });
    }

    public Flux<TacoOrder> findVisibleOrders(User authenticatedUser) {
        if (authenticatedUser == null) {
            return Flux.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication is required"));
        }

        if (isAdmin(authenticatedUser)) {
            return repo.findAll();
        }

        String userId = authenticatedUser.getId();

        if (userId == null) {
            return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user has no identifier"));
        }

        return repo.findByUser_IdOrderByPlacedAtDesc(userId);
    }

    private Mono<TacoOrder> findAuthorizedOrder(
            String orderId,
            User authenticatedUser) {

        return repo.findById(orderId)
            .switchIfEmpty(Mono.error(
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found: " + orderId)))
            .flatMap(order -> {
                if (!isAuthorized(
                        order,
                        authenticatedUser)) {

                    return Mono.error(
                        new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "You cannot modify this order"));
                }

                return Mono.just(order);
            });
    }

    private boolean isAuthorized(TacoOrder order, User authenticatedUser) {
        String authenticatedId = authenticatedUser != null
                ? authenticatedUser.getId()
                : null;

        String ownerId = order.getUser() != null
                ? order.getUser().getId()
                : null;

        boolean isOwner = authenticatedId != null
                && authenticatedId.equals(ownerId);

        return isOwner || isAdmin(authenticatedUser);
    }

    private boolean isMutable(TacoOrder order) {
        return order.getStatus() == null
            || order.getStatus() == OrderStatus.PLACED;
    }

    private <T> Mono<T> stateConflict(
            TacoOrder order) {

        return Mono.error(
            new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order cannot be modified in state: "
                    + order.getStatus()));
    }

    private void applyPatch(
            TacoOrder order,
            OrderPatchRequest patch) {

        if (patch.getDeliveryName() != null) {
            order.setDeliveryName(
                patch.getDeliveryName());
        }

        if (patch.getDeliveryStreet() != null) {
            order.setDeliveryStreet(
                patch.getDeliveryStreet());
        }

        if (patch.getDeliveryCity() != null) {
            order.setDeliveryCity(
                patch.getDeliveryCity());
        }

        if (patch.getDeliveryState() != null) {
            order.setDeliveryState(
                patch.getDeliveryState());
        }

        if (patch.getDeliveryZip() != null) {
            order.setDeliveryZip(
                patch.getDeliveryZip());
        }
    }

    private void applyReplacement(
            TacoOrder order,
            OrderReplaceRequest request) {

        order.setDeliveryName(
            request.getDeliveryName());
        order.setDeliveryStreet(
            request.getDeliveryStreet());
        order.setDeliveryCity(
            request.getDeliveryCity());
        order.setDeliveryState(
            request.getDeliveryState());
        order.setDeliveryZip(
            request.getDeliveryZip());

        order.setTacos(
            new ArrayList<>(request.getTacos()));
    }

    private boolean isAdmin(User authenticatedUser) {
        return authenticatedUser != null
            && authenticatedUser.getAuthorities() != null
            && authenticatedUser.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
