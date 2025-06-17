package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import database.DbConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;


public class StockTransferDetailController {

    @FXML private DatePicker datePickerFilter;
    @FXML private ComboBox<String> comboBoxItemFilter;
    @FXML private ComboBox<String> comboBoxLocationFilter;
    @FXML private TableView<StockTransferDetailController.StockTransferItem> tableViewHistory;

    private final ObservableList<StockTransferDetailController.StockTransferItem> historyItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadFilterOptions();
        loadHistoryFromDB(null, null, null);

        // 필터 변경 시 자동 조회
        datePickerFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadHistory());
        comboBoxItemFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadHistory());
        comboBoxLocationFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadHistory());

        tableViewHistory.setItems(historyItems);

        if (tableViewHistory.getColumns().isEmpty()) {
            TableColumn<StockTransferDetailController.StockTransferItem, String> colItem = new TableColumn<>("품목");
            colItem.setCellValueFactory(cell -> cell.getValue().itemProperty());
            TableColumn<StockTransferDetailController.StockTransferItem, String> colUom = new TableColumn<>("단위");
            colUom.setCellValueFactory(cell -> cell.getValue().uomProperty());
            TableColumn<StockTransferDetailController.StockTransferItem, Integer> colQty = new TableColumn<>("수량");
            colQty.setCellValueFactory(cell -> cell.getValue().qtyProperty().asObject());
            TableColumn<StockTransferDetailController.StockTransferItem, String> colFrom = new TableColumn<>("이동 전 위치");
            colFrom.setCellValueFactory(cell -> cell.getValue().fromLocationProperty());
            TableColumn<StockTransferDetailController.StockTransferItem, String> colTo = new TableColumn<>("이동 후 위치");
            colTo.setCellValueFactory(cell -> cell.getValue().toLocationProperty());
            TableColumn<StockTransferDetailController.StockTransferItem, String> colRemarks = new TableColumn<>("비고");
            colRemarks.setCellValueFactory(cell -> cell.getValue().remarksProperty());
            tableViewHistory.getColumns().addAll(colItem, colUom, colQty, colFrom, colTo, colRemarks);
        }
    }

    private void loadFilterOptions() {
        // DB에서 품목, 위치 목록 불러오기 (예시)
        ObservableList<String> items = FXCollections.observableArrayList("전체", "품목A", "품목B");
        ObservableList<String> locations = FXCollections.observableArrayList("전체", "창고1", "매장1", "매장2");
        comboBoxItemFilter.setItems(items);
        comboBoxItemFilter.getSelectionModel().selectFirst();
        comboBoxLocationFilter.setItems(locations);
        comboBoxLocationFilter.getSelectionModel().selectFirst();
    }

    private void reloadHistory() {
        LocalDate date = datePickerFilter.getValue();
        String item = comboBoxItemFilter.getValue();
        String location = comboBoxLocationFilter.getValue();
        loadHistoryFromDB(date, item, location);
    }

    private void loadHistoryFromDB(LocalDate date, String item, String location) {
        historyItems.clear();
        String sql = "SELECT t.TRANSFER_DATE, i.NAME as ITEM, u.NAME as UOM, t.QTY, t.FROM_LOCATION, t.TO_LOCATION, t.REMARKS " +
                "FROM STOCK_TRANSFERS t " +
                "JOIN ITEMS i ON t.ITEM_ID = i.ITEM_ID " +
                "JOIN UOMS u ON t.UOM_ID = u.UOM_ID WHERE 1=1";
        if (date != null) sql += " AND t.TRANSFER_DATE = ?";
        if (item != null && !"전체".equals(item)) sql += " AND i.NAME = ?";
        if (location != null && !"전체".equals(location)) sql += " AND (t.FROM_LOCATION = ? OR t.TO_LOCATION = ?)";
        sql += " ORDER BY t.TRANSFER_DATE DESC";

        try (Connection conn = DbConnection.getDatabaseConnection().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int idx = 1;
            if (date != null) pstmt.setDate(idx++, java.sql.Date.valueOf(date));
            if (item != null && !"전체".equals(item)) pstmt.setString(idx++, item);
            if (location != null && !"전체".equals(location)) {
                pstmt.setString(idx++, location);
                pstmt.setString(idx++, location);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                historyItems.add(new StockTransferDetailController.StockTransferItem(
                        rs.getString("ITEM"),
                        rs.getString("UOM"),
                        rs.getInt("QTY"),
                        rs.getString("FROM_LOCATION"),
                        rs.getString("TO_LOCATION"),
                        rs.getString("REMARKS")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class StockTransferItem {
        private final StringProperty item;
        private final StringProperty uom;
        private final IntegerProperty qty;
        private final StringProperty fromLocation;
        private final StringProperty toLocation;
        private final StringProperty remarks;

        public StockTransferItem(String item, String uom, int qty, String fromLocation, String toLocation, String remarks) {
            this.item = new SimpleStringProperty(item);
            this.uom = new SimpleStringProperty(uom);
            this.qty = new SimpleIntegerProperty(qty);
            this.fromLocation = new SimpleStringProperty(fromLocation);
            this.toLocation = new SimpleStringProperty(toLocation);
            this.remarks = new SimpleStringProperty(remarks);
        }

        public String getItem() { return item.get(); }
        public StringProperty itemProperty() { return item; }
        public String getUom() { return uom.get(); }
        public StringProperty uomProperty() { return uom; }
        public int getQty() { return qty.get(); }
        public IntegerProperty qtyProperty() { return qty; }
        public String getFromLocation() { return fromLocation.get(); }
        public StringProperty fromLocationProperty() { return fromLocation; }
        public String getToLocation() { return toLocation.get(); }
        public StringProperty toLocationProperty() { return toLocation; }
        public String getRemarks() { return remarks.get(); }
        public StringProperty remarksProperty() { return remarks; }
    }
}

