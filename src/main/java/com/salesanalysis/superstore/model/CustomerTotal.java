package com.salesanalysis.superstore.model;

import java.math.BigDecimal;
import java.util.Objects;

/** A customer's identity and the sum of sales across that customer's order lines. */
public record CustomerTotal(String customerId, String customerName, BigDecimal totalSales) {

  public CustomerTotal {
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(customerName, "customerName");
    Objects.requireNonNull(totalSales, "totalSales");
  }
}
