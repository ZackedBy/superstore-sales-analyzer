module com.salesanalysis.superstore {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.apache.commons.csv;
  requires java.logging;

  opens com.salesanalysis.superstore to
      javafx.fxml;

  exports com.salesanalysis.superstore;
  exports com.salesanalysis.superstore.csv;
  exports com.salesanalysis.superstore.model;
  exports com.salesanalysis.superstore.service;
}
