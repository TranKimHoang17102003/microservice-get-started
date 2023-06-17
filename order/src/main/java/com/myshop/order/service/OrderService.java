package com.myshop.order.service;

import com.myshop.order.model.Order;
import com.myshop.order.model.OrderLineItems;
import com.myshop.order.payload.InventoryDto;
import com.myshop.order.payload.OrderLineItemsDto;
import com.myshop.order.payload.OrderRequest;
import com.myshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClient;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(orderLineItemsDto -> mapToEntity(orderLineItemsDto))
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList()
                .stream()
                .map(OrderLineItems::getSkuCode)
                .collect(Collectors.toList());

        //call inventory service, and place order if product in stock
        InventoryDto[] inventoryDtoArray = webClient.build().get()
                .uri("http://inventory/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                                .bodyToMono(InventoryDto[].class)
                                        .block();

        boolean allProductInStock = Arrays.stream(inventoryDtoArray)
                .allMatch(InventoryDto::isInStock);

        if(allProductInStock) {
            orderRepository.save(order);
            return "Order Places Successfully";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please tray again later");
        }
    }

    private OrderLineItems mapToEntity(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
