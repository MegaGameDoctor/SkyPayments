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
    private final boolean is_online;
    @Setter
    private OrderStatus status;
}