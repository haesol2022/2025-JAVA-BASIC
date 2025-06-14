package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyEvent;

import database.DbConnection; // DB 연결 싱글턴
import java.sql.*;
import java.time.LocalDate;
// TableView에 표시할 데이터 모델 (JavaFX Property 사용)
import javafx.beans.property.*;

public class StockTransferController {

    @FXML private DatePicker date;
    @FXML private TextField textFieldItem;
    @FXML private ComboBox<String> comboBoxUom;
    @FXML private TextField textFieldQty;
    @FXML private ComboBox<String> comboBoxFromLocation;
    @FXML private ComboBox<String> comboBoxToLocation;
    @FXML private TextField textFieldRemarks;
    @FXML private TableView<StockTransferItem> tableViewItem;
    @FXML private TextField textFieldTotalQuantity;

    private final ObservableList<StockTransferItem> transferItems = FXCollections.observableArrayList();

    // 품목명-ITEM_ID, 단위명-UOM_ID 매핑용
    private ObservableList<String> itemNames = FXCollections.observableArrayList();
    private ObservableList<String> uomNames = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadItemsFromDB();
        loadUomsFromDB();
        loadLocations();

        tableViewItem.setItems(transferItems);
        textFieldTotalQuantity.setText("0");

        // TableView 컬럼 동적 생성 (실제 환경에 맞게 수정)
        if (tableViewItem.getColumns().isEmpty()) {
            TableColumn<StockTransferItem, String> colItem = new TableColumn<>("품목");
            colItem.setCellValueFactory(cell -> cell.getValue().itemProperty());
            TableColumn<StockTransferItem, String> colUom = new TableColumn<>("단위");
            colUom.setCellValueFactory(cell -> cell.getValue().uomProperty());
            TableColumn<StockTransferItem, Integer> colQty = new TableColumn<>("수량");
            colQty.setCellValueFactory(cell -> cell.getValue().qtyProperty().asObject());
            TableColumn<StockTransferItem, String> colFrom = new TableColumn<>("이동 전 위치");
            colFrom.setCellValueFactory(cell -> cell.getValue().fromLocationProperty());
            TableColumn<StockTransferItem, String> colTo = new TableColumn<>("이동 후 위치");
            colTo.setCellValueFactory(cell -> cell.getValue().toLocationProperty());
            TableColumn<StockTransferItem, String> colRemarks = new TableColumn<>("비고");
            colRemarks.setCellValueFactory(cell -> cell.getValue().remarksProperty());
            tableViewItem.getColumns().addAll(colItem, colUom, colQty, colFrom, colTo, colRemarks);
        }
    }

    private void loadItemsFromDB() {
        itemNames.clear();
        try (Connection conn = DbConnection.getDatabaseConnection().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT NAME FROM ITEMS ORDER BY NAME")) {
            while (rs.next()) {
                itemNames.add(rs.getString("NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 품목 자동완성 (간단 예시)
        textFieldItem.setOnKeyReleased(event -> {
            // 실제 자동완성 기능은 별도 구현 필요
        });
    }


    private void loadUomsFromDB() {
        uomNames.clear();
        try (Connection conn = DbConnection.getDatabaseConnection().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT NAME FROM UOMS ORDER BY NAME")) {
            while (rs.next()) {
                uomNames.add(rs.getString("NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        comboBoxUom.setItems(uomNames);
    }


    private void loadLocations() {
        // 임시 하드코딩, 실제로는 LOCATION 테이블에서 불러오는 것이 좋음
        ObservableList<String> locations = FXCollections.observableArrayList("창고1", "매장1", "매장2");
        comboBoxFromLocation.setItems(locations);
        comboBoxToLocation.setItems(locations);
    }

    @FXML
    private void addItemInTableView(ActionEvent event) {
        String item = textFieldItem.getText();
        String uom = comboBoxUom.getValue();
        String qtyStr = textFieldQty.getText();
        String fromLoc = comboBoxFromLocation.getValue();
        String toLoc = comboBoxToLocation.getValue();
        String remarks = textFieldRemarks.getText();

        if (item.isEmpty() || uom == null || qtyStr.isEmpty() || fromLoc == null || toLoc == null) {
            showAlert("입력 오류", "모든 필드를 입력해 주세요.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("입력 오류", "수량은 1 이상의 숫자여야 합니다.");
            return;
        }

        transferItems.add(new StockTransferItem(item, uom, qty, fromLoc, toLoc, remarks));
        updateTotalQuantity();
        clearHeaderForm(null);
    }

    @FXML
    private void deleteTableViewRow(ActionEvent event) {
        StockTransferItem selected = tableViewItem.getSelectionModel().getSelectedItem();
        if (selected != null) {
            transferItems.remove(selected);
            updateTotalQuantity();
        }
    }

    @FXML
    private void clearHeaderForm(ActionEvent event) {
        textFieldItem.clear();
        comboBoxUom.getSelectionModel().clearSelection();
        textFieldQty.clear();
        comboBoxFromLocation.getSelectionModel().clearSelection();
        comboBoxToLocation.getSelectionModel().clearSelection();
        textFieldRemarks.clear();
    }

    @FXML
    private void clearWholeForm(ActionEvent event) {
        clearHeaderForm(event);
        transferItems.clear();
        textFieldTotalQuantity.setText("0");
        date.setValue(null);
    }

    @FXML
    private void save(ActionEvent event) {
        if (transferItems.isEmpty()) {
            showAlert("저장 오류", "이동할 품목을 추가하세요.");
            return;
        }
        if (date.getValue() == null) {
            showAlert("입력 오류", "이동일자를 입력하세요.");
            return;
        }

        try (Connection conn = DbConnection.getDatabaseConnection().getConnection()) {
            String sql = "INSERT INTO STOCK_TRANSFERS (TRANSFER_DATE, ITEM_ID, UOM_ID, QTY, FROM_LOCATION, TO_LOCATION, REMARKS) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (StockTransferItem item : transferItems) {
                    pstmt.setDate(1, java.sql.Date.valueOf(date.getValue()));
                    pstmt.setInt(2, getItemId(item.getItem(), conn));
                    pstmt.setInt(3, getUomId(item.getUom(), conn));
                    pstmt.setInt(4, item.getQty());
                    pstmt.setString(5, item.getFromLocation());
                    pstmt.setString(6, item.getToLocation());
                    pstmt.setString(7, item.getRemarks());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            showAlert("저장 완료", "재고 이동 내역이 저장되었습니다.");
            clearWholeForm(null);
        } catch (SQLException e) {
            showAlert("DB 오류", e.getMessage());
            e.printStackTrace();
        }
    }

    private int getItemId(String itemName, Connection conn) throws SQLException {
        String sql = "SELECT ITEM_ID FROM ITEMS WHERE NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ITEM_ID");
            } else {
                throw new SQLException("품목명에 해당하는 ITEM_ID 없음: " + itemName);
            }
        }
    }


    private int getUomId(String uomName, Connection conn) throws SQLException {
        String sql = "SELECT UOM_ID FROM UOMS WHERE NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uomName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("UOM_ID");
            } else {
                throw new SQLException("단위명에 해당하는 UOM_ID 없음: " + uomName);
            }
        }
    }

    private void updateTotalQuantity() {
        int total = transferItems.stream().mapToInt(StockTransferItem::getQty).sum();
        textFieldTotalQuantity.setText(String.valueOf(total));
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
