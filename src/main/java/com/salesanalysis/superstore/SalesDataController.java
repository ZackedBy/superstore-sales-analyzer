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

public class SalesDataController {

    @FXML
    TableView<String[]> dataTable;

    @FXML
    TextField customerNameInput;

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


    @FXML
    private void findCustomer() {
        customerNameInput.setVisible(!customerNameInput.isVisible());
        if (customerNameInput.isVisible()) {
            customerNameInput.requestFocus();
        }
    }


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


    private void addColumns() {
        String[] columns = {
                "Row ID", "Order ID", "Order Date", "Ship Date", "Ship Mode",
                "Customer ID", "Customer Name", "Segment", "Country", "City", "State", "Postal Code", "Region",
                "Product ID", "Category", "Sub-Category", "Product Name", "Sales", "Quantity", "Discount",
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

        showWindow("Average Sales Amount", "The average sales amount of the orders is: $" + String.format("%.2f", averageSales));
    }

    private double parseSalesAmount(String salesString) {
        return Double.parseDouble(salesString.replace(",", "."));
    }



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

        showWindow("Best Customer", "The best customer is: " + bestCustomer + " with total sales of $" + String.format("%.2f", highestTotalSales));
    }
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


    private String extractYear(String orderDate) {
        return orderDate.split("\\.")[2];
    }

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


    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showWindow(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
//
//    public void setDataTable(TableView<String[]> dataTable) {
//        this.dataTable = dataTable;
//    }
//
//
//    public void setCustomerNameInput(String customerName) {
//        customerNameInput.setText(customerName);
//    }
//    public TableView<String[]> getDataTable() {
//        return dataTable;
//    }
}