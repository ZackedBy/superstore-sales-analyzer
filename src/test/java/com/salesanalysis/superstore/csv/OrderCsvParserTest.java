package com.salesanalysis.superstore.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesanalysis.superstore.model.OrderLine;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderCsvParserTest {

  private static final String HEADER =
      "Row ID;Order ID;Order Date;Ship Date;Ship Mode;Customer ID;Customer Name;"
          + "Segment;Country;City;State;Postal Code;Region;Product ID;Category;"
          + "Sub-Category;Product Name;Sales;Quantity;Discount;Profit";

  private final OrderCsvParser parser = new OrderCsvParser();

  @Test
  void parsesDecimalCommaDayFirstDateAndNegativeProfit() throws IOException {
    LoadResult result =
        parser.parse(
            new StringReader(
                HEADER
                    + "\n"
                    + "4;US-2015-108966;11.10.2015;18.10.2015;Standard Class;SO-20335;"
                    + "Sean O'Donnell;Consumer;United States;Fort Lauderdale;Florida;33311;South;"
                    + "FUR-TA-10000577;Furniture;Tables;Bretford CR4500 Series Slim Rectangular Table;"
                    + "957,5775;5;0,45;-383,031\n"));

    assertTrue(result.warnings().isEmpty());
    OrderLine line = result.orders().get(0);
    assertEquals("Sean O'Donnell", line.customerName());
    assertEquals(LocalDate.of(2015, 10, 11), line.orderDate());
    assertEquals(LocalDate.of(2015, 10, 18), line.shipDate());
    assertEquals(0, line.sales().compareTo(new BigDecimal("957.5775")));
    assertEquals(0, line.profit().compareTo(new BigDecimal("-383.031")));
    assertEquals(0, line.discount().compareTo(new BigDecimal("0.45")));
    assertEquals(5, line.quantity());
  }

  @Test
  void keepsSemicolonInsideQuotedProductName() throws IOException {
    LoadResult result =
        parser.parse(
            new StringReader(
                HEADER
                    + "\n"
                    + "9;CA-1;1.2.2016;2.2.2016;Second Class;ID-1;Ada Lovelace;Consumer;"
                    + "United States;London;Ohio;00000;East;P-1;Technology;Accessories;"
                    + "\"Flash Drive; 16GB\";10,5;1;0;1,5\n"));

    assertTrue(result.warnings().isEmpty());
    OrderLine line = result.orders().get(0);
    assertEquals("Flash Drive; 16GB", line.productName());
    assertEquals("Ada Lovelace", line.customerName());
    assertEquals(0, line.sales().compareTo(new BigDecimal("10.5")));
    assertEquals(LocalDate.of(2016, 2, 1), line.orderDate());
  }

  @Test
  void skipsInvalidRowAndKeepsTheNextOne() throws IOException {
    String good =
        "2;CA-2;8.11.2016;11.11.2016;Second Class;CG-1;Claire Gute;Consumer;"
            + "United States;Henderson;Kentucky;42420;South;P-2;Furniture;Bookcases;Bookcase;"
            + "261,96;2;0;41,91";
    LoadResult result =
        parser.parse(
            new StringReader(
                HEADER + "\n1;CA-1;32.13.2016;1.1.2016;Second Class\n\n" + good + "\n"));

    assertEquals(1, result.orders().size());
    assertEquals("Claire Gute", result.orders().get(0).customerName());
    assertEquals(1, result.warnings().size());
    assertTrue(result.warnings().get(0).contains("invalid Order Date"));
  }

  @Test
  void skipsBlankCustomerName() throws IOException {
    LoadResult result =
        parser.parse(
            new StringReader(
                HEADER
                    + "\n"
                    + "1;CA-1;1.1.2016;2.1.2016;Second Class;ID-1;;Consumer;"
                    + "United States;City;Ohio;00000;East;P-1;Furniture;Chairs;Chair;10;1;0;1\n"));

    assertTrue(result.orders().isEmpty());
    assertEquals(1, result.warnings().size());
    assertTrue(result.warnings().get(0).contains("blank Customer Name"));
  }

  @Test
  void ignoresLeadingByteOrderMark() throws IOException {
    LoadResult result =
        parser.parse(
            new StringReader(
                "\uFEFF"
                    + HEADER
                    + "\n"
                    + "1;CA-1;1.1.2016;2.1.2016;Second Class;ID-1;Ann;Consumer;"
                    + "United States;City;Ohio;00000;East;P-1;Furniture;Chairs;Chair;10;1;0;1\n"));

    assertTrue(result.warnings().isEmpty());
    assertEquals(1, result.orders().size());
    assertEquals("Ann", result.orders().get(0).customerName());
  }

  @Test
  void rejectsFileWithoutRequiredColumns() {
    IOException error =
        assertThrows(IOException.class, () -> parser.parse(new StringReader("Order ID;Sales\n")));
    assertTrue(error.getMessage().contains("missing columns"));
  }

  @Test
  void rejectsNullReader() {
    assertThrows(NullPointerException.class, () -> parser.parse(null));
  }

  @Test
  void bundledDatasetParsesEveryRow() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/data/SuperStoreOrders.csv")) {
      assertTrue(input != null, "bundled orders file is missing");
      try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
        LoadResult result = parser.parse(reader);
        assertEquals(List.of(), result.warnings(), () -> String.join("\n", result.warnings()));
        assertEquals(9994, result.orders().size());

        OrderLine first =
            result.orders().stream().filter(order -> order.rowId() == 1).findFirst().orElseThrow();
        assertEquals("Claire Gute", first.customerName());
        assertEquals(LocalDate.of(2016, 11, 8), first.orderDate());
        assertEquals(0, first.sales().compareTo(new BigDecimal("261.96")));

        assertTrue(
            result.orders().stream()
                .anyMatch(
                    order ->
                        order.productName().contains("Flash Drive;")
                            && order.sales().compareTo(new BigDecimal("408.744")) == 0));
        assertTrue(
            result.orders().stream()
                .anyMatch(
                    order ->
                        order.productName().contains("72\"H")
                            && order.sales().compareTo(new BigDecimal("665.88")) == 0));
      }
    }
  }
}
