# SuperStore Sales Analyzer

Desktop tool for the Superstore orders dataset. Load the bundled history, look up a customer, and answer the questions a sales review actually asks: average order value, best customer, customers by state and segment, and sales by year and region.

The project started as a course assignment. Parsing, statistics, and the JavaFX window are now separate, so the business rules run and are tested without opening a UI.

## Requirements

- JDK 21 (Temurin or any other build)

No database and no API keys. The orders file ships in the repository.

## Run

```bash
./gradlew run
```

On Windows:

```bat
gradlew.bat run
```

The first run downloads the Gradle wrapper distribution and dependencies. Then press **Load Data**.

## Tests and checks

```bash
./gradlew test
./gradlew check
```

`check` runs the unit tests, Checkstyle, and the Spotless format check.

## What the buttons do

- **Load Data** reads `src/main/resources/data/SuperStoreOrders.csv` off the JavaFX thread.
- **Find** filters the table by customer name. The match is case-insensitive. An empty query shows every loaded line again.
- **Average sales** averages orders, not lines. Lines that share an order id are summed first.
- **Best customer** is the customer id with the highest total sales.
- **Customers per state / segment** counts distinct customer ids, not rows.
- **Total sales per year / region** sums every line. Statistics always use the full loaded dataset, even when the table is filtered.

## Layout

```text
src/main/java/com/salesanalysis/superstore
├── MainApp.java                  window
├── SalesDataController.java      JavaFX adapter only
├── model/                        OrderLine, CustomerTotal
├── csv/OrderCsvParser.java       semicolon CSV, quoted fields, decimal commas
└── service/SalesAnalytics.java   pure statistics
```

Dates in the file are day.month.year (`8.11.2016` is 8 November 2016). Product names can contain semicolons and inch marks; they are quoted. A bad row is skipped and reported. It does not fail the whole load.

## Demo

[Screencast of the original UI](https://youtu.be/Gb8K7r2unjI). The buttons are the same; the calculations behind them are the ones described above.

## License

[MIT](LICENSE)
