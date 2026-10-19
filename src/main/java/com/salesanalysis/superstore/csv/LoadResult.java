package com.salesanalysis.superstore.csv;

import com.salesanalysis.superstore.model.OrderLine;
import java.util.List;

/** Order lines parsed from a CSV file, plus a warning for each skipped row. */
public record LoadResult(List<OrderLine> orders, List<String> warnings) {

  public LoadResult {
    orders = List.copyOf(orders);
    warnings = List.copyOf(warnings);
  }
}
