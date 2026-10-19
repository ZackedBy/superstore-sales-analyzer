package com.salesanalysis.superstore.service;

import com.salesanalysis.superstore.model.CustomerTotal;
import com.salesanalysis.superstore.model.OrderLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/** Statistics over order lines. Every method is pure and safe to call with an empty list. */
public final class SalesAnalytics {

  private static final int MONEY_SCALE = 2;

  private SalesAnalytics() {}

  /**
   * Average sales per order. Lines that share an order id are summed first, then those order totals
   * are averaged. Rounded half up to cents. Empty input returns {@link Optional#empty()}.
   */
  public static Optional<BigDecimal> averageOrderSales(List<OrderLine> lines) {
    Objects.requireNonNull(lines, "lines");
    if (lines.isEmpty()) {
      return Optional.empty();
    }
    Map<String, BigDecimal> perOrder = new HashMap<>();
    for (OrderLine line : lines) {
      perOrder.merge(line.orderId(), line.sales(), BigDecimal::add);
    }
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal orderTotal : perOrder.values()) {
      total = total.add(orderTotal);
    }
    BigDecimal average =
        total.divide(BigDecimal.valueOf(perOrder.size()), MONEY_SCALE, RoundingMode.HALF_UP);
    return Optional.of(average);
  }

  /**
   * Customer with the highest total sales. Identity is the customer id, so two people who share a
   * display name stay separate. A tie goes to the name that comes first alphabetically.
   */
  public static Optional<CustomerTotal> bestCustomer(List<OrderLine> lines) {
    Objects.requireNonNull(lines, "lines");
    Map<String, CustomerTotal> totals = new HashMap<>();
    for (OrderLine line : lines) {
      totals.merge(
          line.customerId(),
          new CustomerTotal(line.customerId(), line.customerName(), line.sales()),
          (left, right) ->
              new CustomerTotal(
                  left.customerId(),
                  left.customerName(),
                  left.totalSales().add(right.totalSales())));
    }
    return totals.values().stream()
        .max(
            Comparator.comparing(CustomerTotal::totalSales)
                .thenComparing(CustomerTotal::customerName, Comparator.reverseOrder()));
  }

  /**
   * Distinct customer ids seen in each state. A customer who bought in two states is counted in
   * both.
   */
  public static Map<String, Integer> customersPerState(List<OrderLine> lines) {
    return distinctCounts(lines, OrderLine::state);
  }

  /** Distinct customer ids seen in each segment. */
  public static Map<String, Integer> customersPerSegment(List<OrderLine> lines) {
    return distinctCounts(lines, OrderLine::segment);
  }

  /** Sum of line sales for each order year. Keys are sorted ascending. */
  public static Map<Integer, BigDecimal> totalSalesPerYear(List<OrderLine> lines) {
    Objects.requireNonNull(lines, "lines");
    Map<Integer, BigDecimal> totals = new TreeMap<>();
    for (OrderLine line : lines) {
      totals.merge(line.orderDate().getYear(), line.sales(), BigDecimal::add);
    }
    return Collections.unmodifiableMap(totals);
  }

  /** Sum of line sales for each region. Keys are sorted alphabetically. */
  public static Map<String, BigDecimal> totalSalesPerRegion(List<OrderLine> lines) {
    Objects.requireNonNull(lines, "lines");
    Map<String, BigDecimal> totals = new TreeMap<>();
    for (OrderLine line : lines) {
      totals.merge(line.region(), line.sales(), BigDecimal::add);
    }
    return Collections.unmodifiableMap(totals);
  }

  /**
   * Case-insensitive substring match on the customer name. A blank query returns every line. The
   * input list is not modified.
   */
  public static List<OrderLine> findByCustomerName(List<OrderLine> lines, String query) {
    Objects.requireNonNull(lines, "lines");
    Objects.requireNonNull(query, "query");
    String needle = query.trim().toLowerCase(Locale.ROOT);
    if (needle.isEmpty()) {
      return List.copyOf(lines);
    }
    List<OrderLine> matches = new ArrayList<>();
    for (OrderLine line : lines) {
      if (line.customerName().toLowerCase(Locale.ROOT).contains(needle)) {
        matches.add(line);
      }
    }
    return List.copyOf(matches);
  }

  private static Map<String, Integer> distinctCounts(
      List<OrderLine> lines, Function<OrderLine, String> groupBy) {
    Objects.requireNonNull(lines, "lines");
    Map<String, Set<String>> grouped = new TreeMap<>();
    for (OrderLine line : lines) {
      grouped.computeIfAbsent(groupBy.apply(line), key -> new HashSet<>()).add(line.customerId());
    }
    Map<String, Integer> counts = new TreeMap<>();
    grouped.forEach((key, customerIds) -> counts.put(key, customerIds.size()));
    return Collections.unmodifiableMap(counts);
  }
}
