package com.salesanalysis.superstore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesanalysis.superstore.model.CustomerTotal;
import com.salesanalysis.superstore.model.OrderLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SalesAnalyticsTest {

  @Test
  void averageIsTakenPerOrderNotPerLine() {
    List<OrderLine> lines =
        List.of(
            line("A", "Ann", "CA-1", "2016-11-08", "10.00"),
            line("A", "Ann", "CA-1", "2016-11-08", "30.00"),
            line("B", "Bob", "CA-2", "2016-11-09", "10.00"));

    assertEquals(new BigDecimal("25.00"), SalesAnalytics.averageOrderSales(lines).orElseThrow());
  }

  @Test
  void averageOfEmptyDataIsEmpty() {
    assertTrue(SalesAnalytics.averageOrderSales(List.of()).isEmpty());
  }

  @Test
  void bestCustomerIncludesEveryLineAndDoesNotMergeDifferentIds() {
    List<OrderLine> lines =
        List.of(
            line("A", "Ann", "CA-1", "2016-01-01", "70.00"),
            line("A", "Ann", "CA-2", "2016-02-01", "40.00"),
            line("B", "Ann", "CA-3", "2016-03-01", "100.00"));

    CustomerTotal best = SalesAnalytics.bestCustomer(lines).orElseThrow();
    assertEquals("A", best.customerId());
    assertEquals(0, best.totalSales().compareTo(new BigDecimal("110.00")));
  }

  @Test
  void tiedCustomersAreOrderedByName() {
    List<OrderLine> lines =
        List.of(
            line("Z", "Zoe", "CA-1", "2016-01-01", "50.00"),
            line("A", "Ann", "CA-2", "2016-01-01", "50.00"));

    assertEquals("Ann", SalesAnalytics.bestCustomer(lines).orElseThrow().customerName());
  }

  @Test
  void bestCustomerOfEmptyDataIsEmpty() {
    assertTrue(SalesAnalytics.bestCustomer(List.of()).isEmpty());
  }

  @Test
  void customersPerStateCountsDistinctCustomers() {
    List<OrderLine> lines =
        List.of(
            line("A", "Ann", "CA", "California", "West", "Consumer", "2016-01-01", "10"),
            line("A", "Ann", "CA-2", "California", "West", "Consumer", "2016-02-01", "15"),
            line("B", "Bob", "CA-3", "California", "West", "Corporate", "2016-03-01", "5"),
            line("A", "Ann", "CA-4", "Texas", "Central", "Consumer", "2016-04-01", "8"));

    assertEquals(Map.of("California", 2, "Texas", 1), SalesAnalytics.customersPerState(lines));
  }

  @Test
  void customersPerSegmentCountsDistinctCustomers() {
    List<OrderLine> lines =
        List.of(
            line("A", "Ann", "CA-1", "Ohio", "East", "Consumer", "2016-01-01", "10"),
            line("A", "Ann", "CA-2", "Ohio", "East", "Consumer", "2016-02-01", "12"),
            line("B", "Bob", "CA-3", "Ohio", "East", "Corporate", "2016-03-01", "9"),
            line("C", "Cara", "CA-4", "Ohio", "East", "Home Office", "2016-04-01", "7"));

    assertEquals(
        Map.of("Consumer", 1, "Corporate", 1, "Home Office", 1),
        SalesAnalytics.customersPerSegment(lines));
  }

  @Test
  void totalSalesPerYearUsesOrderDate() {
    List<OrderLine> lines =
        List.of(
            line("A", "Ann", "CA-1", "2016-11-08", "10.00"),
            line("A", "Ann", "CA-2", "2016-01-02", "5.50"),
            line("B", "Bob", "CA-3", "2015-05-01", "7.25"));

    Map<Integer, BigDecimal> totals = SalesAnalytics.totalSalesPerYear(lines);
    assertEquals(List.of(2015, 2016), List.copyOf(totals.keySet()));
    assertEquals(0, totals.get(2015).compareTo(new BigDecimal("7.25")));
    assertEquals(0, totals.get(2016).compareTo(new BigDecimal("15.50")));
  }

  @Test
  void totalSalesPerRegionSumsEveryLine() {
    List<OrderLine> lines =
        List.of(
            line("A", "Ann", "CA-1", "Ohio", "East", "Consumer", "2016-01-01", "10.00"),
            line("A", "Ann", "CA-2", "Ohio", "East", "Consumer", "2016-01-02", "2.50"),
            line("B", "Bob", "CA-3", "Texas", "Central", "Corporate", "2016-01-03", "4.00"));

    Map<String, BigDecimal> totals = SalesAnalytics.totalSalesPerRegion(lines);
    assertEquals(List.of("Central", "East"), List.copyOf(totals.keySet()));
    assertEquals(0, totals.get("East").compareTo(new BigDecimal("12.50")));
    assertEquals(0, totals.get("Central").compareTo(new BigDecimal("4.00")));
  }

  @Test
  void findByCustomerNameIsCaseInsensitiveAndDoesNotMutateSource() {
    OrderLine ann = line("A", "Ann", "CA-1", "2016-01-01", "10");
    OrderLine annette = line("B", "Annette", "CA-2", "2016-01-01", "12");
    List<OrderLine> source = new ArrayList<>(List.of(ann, annette));

    List<OrderLine> found = SalesAnalytics.findByCustomerName(source, " ANN ");
    source.add(line("C", "Cara", "CA-3", "2016-01-01", "1"));

    assertEquals(List.of(ann, annette), found);
    assertTrue(SalesAnalytics.findByCustomerName(source, "zzz").isEmpty());
    assertEquals(3, SalesAnalytics.findByCustomerName(source, "  ").size());
  }

  @Test
  void nullInputsAreRejected() {
    assertThrows(NullPointerException.class, () -> SalesAnalytics.averageOrderSales(null));
    assertThrows(NullPointerException.class, () -> SalesAnalytics.bestCustomer(null));
    assertThrows(
        NullPointerException.class, () -> SalesAnalytics.findByCustomerName(List.of(), null));
  }

  private static OrderLine line(
      String customerId, String name, String orderId, String date, String sales) {
    return line(customerId, name, orderId, "Ohio", "East", "Consumer", date, sales);
  }

  private static OrderLine line(
      String customerId,
      String name,
      String orderId,
      String state,
      String region,
      String segment,
      String date,
      String sales) {
    LocalDate parsed = LocalDate.parse(date);
    return new OrderLine(
        1,
        orderId,
        parsed,
        parsed,
        "Standard Class",
        customerId,
        name,
        segment,
        "United States",
        "City",
        state,
        "00000",
        region,
        "P-1",
        "Furniture",
        "Chairs",
        "Chair",
        new BigDecimal(sales),
        1,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }
}
