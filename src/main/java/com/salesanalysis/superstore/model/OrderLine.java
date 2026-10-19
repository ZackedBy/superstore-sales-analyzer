package com.salesanalysis.superstore.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** One product line inside a Superstore order. Several lines can share an {@code orderId}. */
public record OrderLine(
    int rowId,
    String orderId,
    LocalDate orderDate,
    LocalDate shipDate,
    String shipMode,
    String customerId,
    String customerName,
    String segment,
    String country,
    String city,
    String state,
    String postalCode,
    String region,
    String productId,
    String category,
    String subCategory,
    String productName,
    BigDecimal sales,
    int quantity,
    BigDecimal discount,
    BigDecimal profit) {

  public OrderLine {
    Objects.requireNonNull(orderId, "orderId");
    Objects.requireNonNull(orderDate, "orderDate");
    Objects.requireNonNull(shipDate, "shipDate");
    Objects.requireNonNull(shipMode, "shipMode");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(customerName, "customerName");
    Objects.requireNonNull(segment, "segment");
    Objects.requireNonNull(country, "country");
    Objects.requireNonNull(city, "city");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(postalCode, "postalCode");
    Objects.requireNonNull(region, "region");
    Objects.requireNonNull(productId, "productId");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(subCategory, "subCategory");
    Objects.requireNonNull(productName, "productName");
    Objects.requireNonNull(sales, "sales");
    Objects.requireNonNull(discount, "discount");
    Objects.requireNonNull(profit, "profit");
  }
}
