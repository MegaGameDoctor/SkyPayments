package ru.skydonate.skypayments.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Order {
    private final String order_id;
    private final String type;
    private final String username;
    private final String[] actions;
    @Setter
    private OrderStatus status;
}