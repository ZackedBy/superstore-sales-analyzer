package com.salesanalysis.superstore;

import com.salesanalysis.superstore.csv.LoadResult;
import com.salesanalysis.superstore.csv.OrderCsvParser;
import com.salesanalysis.superstore.model.CustomerTotal;
import com.salesanalysis.superstore.model.OrderLine;
import com.salesanalysis.superstore.service.SalesAnalytics;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * JavaFX adapter. Loading and statistics live in {@link OrderCsvParser} and {@link SalesAnalytics};
 * this class only moves results onto the table and into dialogs.
 */
public class SalesDataController {

  static final String ORDERS_RESOURCE = "/data/SuperStoreOrders.csv";

  private static final Logger LOG = Logger.getLogger(SalesDataController.class.getName());
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d.M.uuuu");

  @FXML private TableView<OrderLine> dataTable;
  @FXML private TextField customerNameInput;

  private final OrderCsvParser parser = new OrderCsvParser();
  private List<OrderLine> orders = List.of();
  private boolean loading;

  @FXML
  private void initialize() {
    dataTable.setPlaceholder(
        new Label("Press \"Load Data\" to read the bundled Superstore orders."));
    dataTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    dataTable.setTableMenuButtonVisible(true);
    addColumn("Row ID", 70, order -> Integer.toString(order.rowId()));
    addColumn("Order ID", 130, OrderLine::orderId);
    addColumn("Order Date", 100, order -> order.orderDate().format(DATE));
    addColumn("Ship Date", 100, order -> order.shipDate().format(DATE));
    addColumn("Ship Mode", 120, OrderLine::shipMode);
    addColumn("Customer ID", 110, OrderLine::customerId);
    addColumn("Customer Name", 160, OrderLine::customerName);
    addColumn("Segment", 110, OrderLine::segment);
    addColumn("Country", 120, OrderLine::country);
    addColumn("City", 140, OrderLine::city);
    addColumn("State", 140, OrderLine::state);
    addColumn("Postal Code", 100, OrderLine::postalCode);
    addColumn("Region", 90, OrderLine::region);
    addColumn("Product ID", 140, OrderLine::productId);
    addColumn("Category", 130, OrderLine::category);
    addColumn("Sub-Category", 130, OrderLine::subCategory);
    addColumn("Product Name", 280, OrderLine::productName);
    addColumn("Sales", 90, order -> order.sales().toPlainString());
    addColumn("Quantity", 80, order -> Integer.toString(order.quantity()));
    addColumn("Discount", 80, order -> order.discount().toPlainString());
    addColumn("Profit", 90, order -> order.profit().toPlainString());
  }

  @FXML
  private void showData() {
    if (loading) {
      return;
    }
    loading = true;
    Task<LoadResult> task =
        new Task<>() {
          @Override
          protected LoadResult call() throws IOException {
            InputStream input = SalesDataController.class.getResourceAsStream(ORDERS_RESOURCE);
            if (input == null) {
              throw new IOException("Sales file was not found: " + ORDERS_RESOURCE);
            }
            try (InputStream stream = input;
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
              return parser.parse(reader);
            }
          }
        };
    task.setOnSucceeded(
        event -> {
          loading = false;
          applyLoaded(task.getValue());
        });
    task.setOnFailed(
        event -> {
          loading = false;
          Throwable error = task.getException();
          LOG.log(Level.SEVERE, "Failed to load sales data", error);
          showAlert("Could not load sales data", errorMessage(error));
        });
    Thread thread = new Thread(task, "sales-csv-loader");
    thread.setDaemon(true);
    thread.start();
  }

  @FXML
  private void findCustomer() {
    boolean show = !customerNameInput.isVisible();
    customerNameInput.setVisible(show);
    customerNameInput.setManaged(show);
    if (show) {
      customerNameInput.requestFocus();
    }
  }

  @FXML
  private void searchCustomer() {
    if (!requireOrders()) {
      return;
    }
    String query = customerNameInput.getText() == null ? "" : customerNameInput.getText();
    List<OrderLine> matches = SalesAnalytics.findByCustomerName(orders, query);
    if (!query.isBlank() && matches.isEmpty()) {
      showAlert("Not found", "No customer matches \"" + query.trim() + "\".");
      return;
    }
    dataTable.setItems(FXCollections.observableArrayList(matches));
  }

  @FXML
  private void showStatistics() {
    if (!requireOrders()) {
      return;
    }
    BigDecimal average = SalesAnalytics.averageOrderSales(orders).orElseThrow();
    long orderCount = orders.stream().map(OrderLine::orderId).distinct().count();
    showInfo(
        "Average sales",
        "Average sales per order: "
            + money(average)
            + " ("
            + orderCount
            + " orders, lines with the same order id summed first).");
  }

  @FXML
  private void findBestCustomer() {
    if (!requireOrders()) {
      return;
    }
    CustomerTotal best = SalesAnalytics.bestCustomer(orders).orElseThrow();
    showInfo(
        "Best customer",
        best.customerName()
            + " ("
            + best.customerId()
            + ") with total sales "
            + money(best.totalSales())
            + ".");
  }

  @FXML
  private void customersPerState() {
    if (!requireOrders()) {
      return;
    }
    showCounts("Customers per state", SalesAnalytics.customersPerState(orders));
  }

  @FXML
  private void customersPerSegment() {
    if (!requireOrders()) {
      return;
    }
    showCounts("Customers per segment", SalesAnalytics.customersPerSegment(orders));
  }

  @FXML
  private void totalSalesPerYear() {
    if (!requireOrders()) {
      return;
    }
    showMoney("Total sales per year", SalesAnalytics.totalSalesPerYear(orders));
  }

  @FXML
  private void totalSalesPerRegion() {
    if (!requireOrders()) {
      return;
    }
    showMoney("Total sales per region", SalesAnalytics.totalSalesPerRegion(orders));
  }

  private void applyLoaded(LoadResult result) {
    orders = result.orders();
    dataTable.setItems(FXCollections.observableArrayList(orders));
    for (String warning : result.warnings()) {
      LOG.warning(warning);
    }
    if (orders.isEmpty()) {
      showAlert("No data", "The sales file did not contain any orders.");
      return;
    }
    if (!result.warnings().isEmpty()) {
      showInfo(
          "Data loaded with skips",
          "Loaded "
              + orders.size()
              + " orders. Skipped "
              + result.warnings().size()
              + " invalid rows.");
    }
  }

  private boolean requireOrders() {
    if (orders.isEmpty()) {
      showAlert("No data", "Load sales data first.");
      return false;
    }
    return true;
  }

  private void addColumn(String title, double width, Function<OrderLine, String> value) {
    TableColumn<OrderLine, String> column = new TableColumn<>(title);
    column.setPrefWidth(width);
    column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
    dataTable.getColumns().add(column);
  }

  private void showCounts(String title, Map<String, Integer> counts) {
    StringBuilder body = new StringBuilder();
    counts.forEach((key, value) -> body.append(key).append(": ").append(value).append('\n'));
    showInfo(title, body.toString());
  }

  private void showMoney(String title, Map<?, BigDecimal> totals) {
    StringBuilder body = new StringBuilder();
    totals.forEach((key, value) -> body.append(key).append(": ").append(money(value)).append('\n'));
    showInfo(title, body.toString());
  }

  private static String money(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private void showAlert(String title, String message) {
    showDialog(AlertType.ERROR, title, message);
  }

  private void showInfo(String title, String message) {
    showDialog(AlertType.INFORMATION, title, message);
  }

  private void showDialog(AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.setResizable(true);
    alert.getDialogPane().setPrefWidth(480);
    if (dataTable.getScene() != null) {
      alert.initOwner(dataTable.getScene().getWindow());
    }
    alert.showAndWait();
  }

  private static String errorMessage(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current.getMessage() != null && !current.getMessage().isBlank()) {
        return current.getMessage();
      }
      current = current.getCause();
    }
    return "Unexpected error while reading the sales file.";
  }
}
