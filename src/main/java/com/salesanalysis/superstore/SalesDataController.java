package com.salesanalysis.superstore;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Controller class for managing sales data in the SuperStore Sales Analyzer
 * application.
 * This class handles loading sales data, searching for customers, calculating
 * statistics,
 * and generating reports.
 */
public class SalesDataController {

    @FXML
    TableView<String[]> dataTable;

    @FXML
    TextField customerNameInput;

    /**
     * Loads sales data from a CSV file and displays it in the TableView.
     * This method reads data from the SuperStoreOrders.csv file and populates
     * the TableView with the parsed data.
     *
     * @throws IOException if an error occurs while reading the data file.
     */
    @FXML
    void showData() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/data/SuperStoreOrders.csv"))))) {

            String[] columns = {
                    "Row ID", "Order ID", "Order Date", "Ship Date", "Ship Mode",
                    "Customer ID", "Customer Name", "Segment", "Country", "City", "State", "Postal Code", "Region",
                    "Product ID", "Category", "Sub-Category", "Product Name", "Sales", "Quantity", "Discount",
                    "Profit"
            };

            ObservableList<String[]> data = FXCollections.observableArrayList();

            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                line = line.replace("Drive;", "Drive,");
                String[] row = line.split(";");
                data.add(row);
            }

            dataTable.getColumns().clear();
            for (int i = 0; i < columns.length; i++) {
                final int columnIndex = i;
                TableColumn<String[], String> column = new TableColumn<>(columns[i]);
                column.setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    return new SimpleStringProperty(row.length > columnIndex ? row[columnIndex] : "");
                });
                dataTable.getColumns().add(column);
            }

            dataTable.setItems(data);
        } catch (IOException e) {
            showAlert("Error reading data from file.", e.getMessage());
        }
    }

    /**
     * Toggles the visibility of the customer name input field.
     * If the input field is currently visible, it hides it; otherwise, it makes it
     * visible
     * and sets the input focus to it.
     */
    @FXML
    private void findCustomer() {
        customerNameInput.setVisible(!customerNameInput.isVisible());
        if (customerNameInput.isVisible()) {
            customerNameInput.requestFocus();
        }
    }

    /**
     * Searches for a customer based on the entered name in the customer name input
     * field.
     * If a customer name is entered, it filters the sales data to display only the
     * rows
     * where the customer name matches (case-insensitive). If no customer name is
     * entered,
     * it displays an error message prompting the user to enter a customer name.
     */
    @FXML
    void searchCustomer() {
        String customerName = customerNameInput.getText().trim().toLowerCase();
        System.out.println("Searching for customer: " + customerName);

        if (customerName.isEmpty()) {
            showAlert("Error", "Please enter a customer name.");
            return;
        }

        ObservableList<String[]> data = dataTable.getItems();
        ObservableList<String[]> filteredData = FXCollections.observableArrayList();

        for (String[] row : data) {
            String fullName = row[6].toLowerCase();
            if (fullName.contains(customerName)) {
                filteredData.add(row);
            }
        }

        if (filteredData.isEmpty()) {
            showAlert("Not Found", "Customer not found.");
        } else {
            dataTable.getColumns().clear();
            addColumns();
            dataTable.setItems(filteredData);
        }
    }

    /**
     * Adds columns to the TableView for displaying sales data.
     * This method dynamically creates TableColumn instances for each column in the
     * sales data,
     * sets the cell value factory to retrieve the appropriate data from the rows,
     * and adds
     * the columns to the TableView.
     */
    private void addColumns() {
        String[] columns = {
                "Row ID", "Order ID", "Order Date", "Ship Date", "Ship Mode",
                "Customer ID", "Customer Name", "Segment", "Country", "City",
                "State", "Postal Code", "Region",
                "Product ID", "Category", "Sub-Category", "Product Name",
                "Sales", "Quantity", "Discount",
                "Profit"
        };

        for (int i = 0; i < columns.length; i++) {
            final int columnIndex = i;
            TableColumn<String[], String> column = new TableColumn<>(columns[i]);
            column.setCellValueFactory(cellData -> {
                String[] row = cellData.getValue();
                return row.length > columnIndex ? new SimpleStringProperty(row[columnIndex]) : null;
            });
            dataTable.getColumns().add(column);
        }
    }

    /**
     * Calculates and displays statistics related to sales data.
     * This method computes the average sales amount per order by iterating over the
     * sales data,
     * parsing the sales amount, and then calculating the average. It displays the
     * result in
     * an information dialog box.
     */
    @FXML
    void showStatistics() {
        ObservableList<String[]> data = dataTable.getItems();
        if (data.isEmpty()) {
            showAlert("Error", "No data available to calculate statistics.");
            return;
        }

        double totalSales = 0;
        int orderCount = 0;

        for (String[] row : data) {
            try {
                double salesAmount = parseSalesAmount(row[17]);
                totalSales += salesAmount;
                orderCount++;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing sales amount: " + e.getMessage());
            }
        }

        double averageSales = totalSales / orderCount;

        showWindow("Average Sales Amount",
                "The average sales amount of the orders is: $" + String.format("%.2f", averageSales));
    }

    /**
     * Parses the sales amount from a string and converts it to a double.
     * This method takes a string representation of the sales amount, which may
     * contain
     * commas as decimal separators, and parses it into a double value. It replaces
     * any
     * commas with dots to ensure proper parsing.
     *
     * @param salesString the string representation of the sales amount
     * @return the parsed sales amount as a double
     * @throws NumberFormatException if the string cannot be parsed as a double
     */
    private double parseSalesAmount(String salesString) {
        return Double.parseDouble(salesString.replace(",", "."));
    }

    /**
     * Identifies the customer with the highest total sales.
     * This method calculates the total sales amount for each customer by summing up
     * their sales from the sales data. It then determines the customer with the
     * highest total sales and displays their name and total sales amount in an
     * information dialog box.
     */
    @FXML
    void findBestCustomer() {
        ObservableList<String[]> data = dataTable.getItems();
        if (data.isEmpty()) {
            showAlert("Error", "No data available to calculate best customer.");
            return;
        }

        Map<String, Double> customerSalesMap = new HashMap<>();

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);
            String customerName = row[6];
            try {
                double salesAmount = parseSalesAmount(row[17]);
                customerSalesMap.merge(customerName, salesAmount, Double::sum);
            } catch (NumberFormatException e) {
                System.err.println("Invalid sales amount in row " + (i + 1) + ": " + e.getMessage());
            }
        }

        String bestCustomer = Collections.max(customerSalesMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        double highestTotalSales = customerSalesMap.get(bestCustomer);

        showWindow("Best Customer", "The best customer is: " + bestCustomer + " with total sales of $"
                + String.format("%.2f", highestTotalSales));
    }

    /**
     * Calculates the number of customers per state.
     * This method iterates over the sales data and counts the number of unique
     * customers
     * per state. It then displays the count of customers per state in an
     * information dialog box.
     */
    @FXML
    void customersPerState() {
        ObservableList<String[]> data = dataTable.getItems();
        if (data.isEmpty()) {
            showAlert("Error", "No data available to calculate customers per state.");
            return;
        }

        Map<String, Integer> customersPerStateMap = new HashMap<>();

        for (String[] row : data) {
            String state = row[10];
            customersPerStateMap.merge(state, 1, Integer::sum);
        }

        StringBuilder result = new StringBuilder();
        result.append("Customers per State:\n");
        for (Map.Entry<String, Integer> entry : customersPerStateMap.entrySet()) {
            result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        showWindow("Customers per State", result.toString());
    }

    /**
     * Calculates the number of customers per segment.
     * This method iterates over the sales data and counts the number of unique
     * customers
     * per segment. It then displays the count of customers per segment in an
     * information
     * dialog box.
     */
    @FXML
    void customersPerSegment() {
        ObservableList<String[]> data = dataTable.getItems();
        if (data.isEmpty()) {
            showAlert("Error", "No data available to calculate customers per segment.");
            return;
        }

        Map<String, Integer> customersPerSegmentMap = new HashMap<>();

        for (String[] row : data) {
            String segment = row[7];
            customersPerSegmentMap.merge(segment, 1, Integer::sum);
        }

        StringBuilder result = new StringBuilder();
        result.append("Customers per Segment:\n");
        for (Map.Entry<String, Integer> entry : customersPerSegmentMap.entrySet()) {
            result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        showWindow("Customers per Segment", result.toString());
    }

    /**
     * Calculates the total sales amount per year.
     * This method iterates over the sales data, extracts the year from the order
     * date,
     * and accumulates the total sales amount for each year. It then displays the
     * total
     * sales amount per year in an information dialog box.
     */
    @FXML
    void totalSalesPerYear() {
        ObservableList<String[]> data = dataTable.getItems();
        if (data.isEmpty()) {
            showAlert("Error", "No data available to calculate total sales per year.");
            return;
        }

        Map<String, Double> totalSalesPerYearMap = new HashMap<>();

        for (String[] row : data) {
            String orderDate = row[2];
            String salesAmountString = row[17];

            try {
                double salesAmount = parseSalesAmount(salesAmountString);
                String year = extractYear(orderDate);

                totalSalesPerYearMap.merge(year, salesAmount, Double::sum);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing sales amount for row: " + Arrays.toString(row));
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("Total Sales per Year:\n");
        for (Map.Entry<String, Double> entry : totalSalesPerYearMap.entrySet()) {
            result.append(entry.getKey()).append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
        }

        showWindow("Total Sales per Year", result.toString());
    }

    /**
     * Extracts the year from a date string.
     * This method takes a date string in the format "MM.dd.yyyy" and extracts the
     * year
     * component from it. It returns the extracted year as a string.
     *
     * @param orderDate the date string from which to extract the year
     * @return the year extracted from the date string
     */
    private String extractYear(String orderDate) {
        return orderDate.split("\\.")[2];
    }

    /**
     * Calculates the total sales amount per region.
     * This method iterates over the sales data, accumulates the total sales amount
     * for each region,
     * and then displays the total sales amount per region in an information dialog
     * box.
     */
    @FXML
    void totalSalesPerRegion() {
        ObservableList<String[]> data = dataTable.getItems();
        if (data.isEmpty()) {
            showAlert("Error", "No data available to calculate total sales per region.");
            return;
        }

        Map<String, Double> totalSalesPerRegionMap = new HashMap<>();

        for (String[] row : data) {
            String region = row[12];
            String salesAmountString = row[17];

            try {
                double salesAmount = parseSalesAmount(salesAmountString);
                totalSalesPerRegionMap.merge(region, salesAmount, Double::sum);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing sales amount for row: " + Arrays.toString(row));
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("Total Sales per Region:\n");
        for (Map.Entry<String, Double> entry : totalSalesPerRegionMap.entrySet()) {
            result.append(entry.getKey()).append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
        }

        showWindow("Total Sales per Region", result.toString());
    }

    /**
     * Displays an alert dialog box with the given title and message.
     * This method creates an alert dialog box with the specified title and message,
     * then displays it to the user.
     *
     * @param title   the title of the alert dialog
     * @param message the message to be displayed in the alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays an information dialog box with the given title and message.
     * This method creates an information dialog box with the specified title and
     * message,
     * then displays it to the user.
     *
     * @param title   the title of the dialog box
     * @param message the message to be displayed in the dialog box
     */
    private void showWindow(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}