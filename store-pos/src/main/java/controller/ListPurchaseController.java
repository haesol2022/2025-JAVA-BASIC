/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import database.DbConnection;
import helper.AlertHelper;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import model.PurchaseModel;
import javafx.scene.text.Font;
import print.ReportGenerator;

public class ListPurchaseController implements Initializable {

    private Connection con;

    @FXML
    private Button deleteButton;

    @FXML
    private Pagination pagination;

    @FXML
    private TableView<PurchaseModel> purchaseTable;

    @FXML
    private Button btnPrint;

    private static final int rowsPerPage = 100;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        DbConnection dbc = DbConnection.getDatabaseConnection();
        con = dbc.getConnection();
        createTableColumns();
        pagination.setPageFactory(this::createPage);
    }

    private void createTableColumns() {
        String rightPositionCSS = "-fx-alignment: CENTER-RIGHT;";

        TableColumn<PurchaseModel, Long> columnInvoiceId = new TableColumn<>("주문번호");
        columnInvoiceId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        columnInvoiceId.setStyle("-fx-font-family: 'Malgun Gothic';");

        TableColumn<PurchaseModel, String> columnInvoiceDate = new TableColumn<>("거래일자");
        columnInvoiceDate.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        columnInvoiceDate.setStyle("-fx-font-family: 'Malgun Gothic';");

        TableColumn<PurchaseModel, String> columnPartyName = new TableColumn<>("거래처명");
        columnPartyName.setCellValueFactory(new PropertyValueFactory<>("partyName"));
        columnPartyName.setStyle("-fx-font-family: 'Malgun Gothic';");

        TableColumn<PurchaseModel, Float> columnTotalQuantity = new TableColumn<>("수량");
        columnTotalQuantity.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));
        columnTotalQuantity.setStyle(rightPositionCSS);

        TableColumn<PurchaseModel, Float> columnTotalAmount = new TableColumn<>("총액");
        columnTotalAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        columnTotalAmount.setStyle(rightPositionCSS);

        TableColumn<PurchaseModel, Float> columnOtherAmount = new TableColumn<>("기타금액");
        columnOtherAmount.setCellValueFactory(new PropertyValueFactory<>("otherAmount"));
        columnOtherAmount.setStyle(rightPositionCSS);

        TableColumn<PurchaseModel, Float> columnTotalPaybleAmount = new TableColumn<>("지급예정액");
        columnTotalPaybleAmount.setCellValueFactory(new PropertyValueFactory<>("totalPaybleAmount"));
        columnTotalPaybleAmount.setStyle(rightPositionCSS);

        TableColumn<PurchaseModel, Float> columnTotalPaidAmount = new TableColumn<>("지급액");
        columnTotalPaidAmount.setCellValueFactory(new PropertyValueFactory<>("totalPaidAmount"));
        columnTotalPaidAmount.setStyle(rightPositionCSS);

        TableColumn<PurchaseModel, Float> columnTotalDueAmount = new TableColumn<>("미지급액");
        columnTotalDueAmount.setCellValueFactory(new PropertyValueFactory<>("totalDueAmount"));
        columnTotalDueAmount.setStyle(rightPositionCSS);

        purchaseTable.getColumns().addAll(
                columnInvoiceId, columnInvoiceDate, columnPartyName,
                columnTotalQuantity, columnTotalAmount, columnOtherAmount,
                columnTotalPaybleAmount, columnTotalPaidAmount, columnTotalDueAmount
        );
    }

    private Node createPage(int pageIndex) {
        this.createData(pageIndex);
        return purchaseTable;
    }

    private void createData(int pageIndex) {
        try {
            Statement stmt = con.createStatement();
            String query = "SELECT * FROM ( SELECT a.*, rownum r__ FROM ( SELECT * FROM purchases ORDER BY order_id desc ) a WHERE rownum < ((" + (pageIndex + 1) + " * " + rowsPerPage + ") + 1 )) WHERE r__ >= (((" + (pageIndex + 1) + "-1) * " + rowsPerPage + ") + 1)";
            ResultSet rs = stmt.executeQuery(query);

            purchaseTable.getItems().clear();
            while (rs.next()) {
                purchaseTable.getItems().add(new PurchaseModel(
                        rs.getLong("order_id"),
                        String.valueOf(rs.getDate("invoice_date")),
                        rs.getString("party_name"),
                        rs.getFloat("total_quantity"),
                        rs.getFloat("total_amount"),
                        rs.getFloat("other_amount"),
                        rs.getFloat("total_payble_amount"),
                        rs.getFloat("total_paid_amount"),
                        rs.getFloat("total_due_amount")
                ));
            }

        } catch (SQLException ex) {
            Logger.getLogger(ListPurchaseController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void viewInvoice(ActionEvent event) {
        List<PurchaseModel> collect = purchaseTable.getSelectionModel().getSelectedItems().stream().collect(Collectors.toList());

        if (collect.isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.WARNING,
                    ((Node) event.getSource()).getScene().getWindow(),
                    "경고",
                    "선택된 항목이 없습니다."
            );
            return;
        }

        long orderId = collect.get(0).getOrderId();
        EditPurchaseController.orderId = orderId;
        Scene scene = (Scene) ((Node) event.getSource()).getScene();
        Parent parent;
        try {
            parent = FXMLLoader.load(getClass().getResource("/view/EditPurchaseView.fxml"));
            BorderPane borderPane = (BorderPane) scene.lookup("#borderPane");
            borderPane.setCenter(parent);
        } catch (IOException ex) {
            Logger.getLogger(ListPurchaseController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void deleteInvoice(ActionEvent event) {
        Window owner = deleteButton.getScene().getWindow();
        AlertHelper.showAlert(Alert.AlertType.CONFIRMATION, owner, "확인", "정말로 삭제하시겠습니까?");
        if (AlertHelper.result) {
            List<PurchaseModel> collect = purchaseTable.getSelectionModel().getSelectedItems().stream().collect(Collectors.toList());

            if (collect.isEmpty()) {
                AlertHelper.showAlert(Alert.AlertType.WARNING, owner, "경고", "삭제할 항목을 선택해주세요.");
                return;
            }

            long orderId = collect.get(0).getOrderId();
            EditPurchaseController.orderId = orderId;
            try {
                Statement stmt = con.createStatement();
                stmt.executeQuery("delete from purchases where order_id = " + orderId);
                purchaseTable.getItems().remove(collect.get(0));
                AlertHelper.showAlert(Alert.AlertType.INFORMATION, owner, "알림", "항목이 성공적으로 삭제되었습니다.");
            } catch (SQLException ex) {
                Logger.getLogger(ListPurchaseController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @FXML
    private void onPrintReceiptClicked() {
        PurchaseModel selected = purchaseTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ReportGenerator.printInvoice(selected);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("알림");
            alert.setHeaderText(null);
            alert.setContentText("선택된 구매내역이 없습니다.");
            alert.showAndWait();
        }
    }
}