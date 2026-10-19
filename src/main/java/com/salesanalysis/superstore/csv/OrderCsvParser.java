package com.salesanalysis.superstore.csv;

import com.salesanalysis.superstore.model.OrderLine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads the Superstore orders export. The file is semicolon-separated, decimals use a comma, and
 * product names are quoted when they themselves contain a semicolon or an inch mark.
 */
public final class OrderCsvParser {

  private static final DateTimeFormatter ORDER_DATE =
      DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);

  private static final List<String> REQUIRED_COLUMNS =
      List.of(
          "Row ID",
          "Order ID",
          "Order Date",
          "Ship Date",
          "Ship Mode",
          "Customer ID",
          "Customer Name",
          "Segment",
          "Country",
          "City",
          "State",
          "Postal Code",
          "Region",
          "Product ID",
          "Category",
          "Sub-Category",
          "Product Name",
          "Sales",
          "Quantity",
          "Discount",
          "Profit");

  private static final CSVFormat FORMAT =
      CSVFormat.DEFAULT
          .builder()
          .setDelimiter(';')
          .setHeader()
          .setSkipHeaderRecord(true)
          .setIgnoreEmptyLines(true)
          .setTrim(true)
          .build();

  /**
   * Parses {@code reader}. A row that is missing a column or has an unreadable date or amount is
   * skipped and described in {@link LoadResult#warnings()}. A missing header fails the whole load.
   */
  public LoadResult parse(Reader reader) throws IOException {
    Objects.requireNonNull(reader, "reader");
    List<OrderLine> orders = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    try (CSVParser parser = CSVParser.parse(stripBom(reader), FORMAT)) {
      validateHeader(parser.getHeaderNames());
      for (CSVRecord record : parser) {
        try {
          orders.add(toOrder(record));
        } catch (IllegalArgumentException ex) {
          warnings.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
        }
      }
    }
    return new LoadResult(orders, warnings);
  }

  private static Reader stripBom(Reader reader) throws IOException {
    BufferedReader buffered =
        reader instanceof BufferedReader bufferedReader
            ? bufferedReader
            : new BufferedReader(reader);
    buffered.mark(1);
    if (buffered.read() != '\uFEFF') {
      buffered.reset();
    }
    return buffered;
  }

  private static void validateHeader(List<String> headerNames) throws IOException {
    List<String> missing = new ArrayList<>();
    for (String column : REQUIRED_COLUMNS) {
      if (!headerNames.contains(column)) {
        missing.add(column);
      }
    }
    if (!missing.isEmpty()) {
      throw new IOException("Sales file is missing columns: " + String.join(", ", missing));
    }
  }

  private static OrderLine toOrder(CSVRecord record) {
    return new OrderLine(
        integer(record, "Row ID"),
        required(record, "Order ID"),
        date(record, "Order Date"),
        date(record, "Ship Date"),
        required(record, "Ship Mode"),
        required(record, "Customer ID"),
        required(record, "Customer Name"),
        required(record, "Segment"),
        required(record, "Country"),
        required(record, "City"),
        required(record, "State"),
        required(record, "Postal Code"),
        required(record, "Region"),
        required(record, "Product ID"),
        required(record, "Category"),
        required(record, "Sub-Category"),
        required(record, "Product Name"),
        decimal(record, "Sales"),
        integer(record, "Quantity"),
        decimal(record, "Discount"),
        decimal(record, "Profit"));
  }

  private static String required(CSVRecord record, String column) {
    final String value;
    try {
      value = record.get(column);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("missing column " + column);
    }
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("blank " + column);
    }
    return value.trim();
  }

  private static LocalDate date(CSVRecord record, String column) {
    String raw = required(record, column);
    try {
      return LocalDate.parse(raw, ORDER_DATE);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("invalid " + column + " \"" + raw + "\"");
    }
  }

  private static int integer(CSVRecord record, String column) {
    String raw = required(record, column);
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("invalid " + column + " \"" + raw + "\"");
    }
  }

  private static BigDecimal decimal(CSVRecord record, String column) {
    String raw = required(record, column);
    String normalized = raw.replace("\u00A0", "").replace(" ", "").replace(',', '.');
    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("invalid " + column + " \"" + raw + "\"");
    }
  }
}
