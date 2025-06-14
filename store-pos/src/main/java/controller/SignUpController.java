package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Node;
import database.DbConnection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;


public class SignUpController {

    @FXML private ImageView characterImage;
    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private PasswordField passwordConfirm;
    @FXML private TextField name;
    @FXML private TextField email;

    @FXML
    private void handleSignUp(ActionEvent event) {
        if (!validateInputs()) return;

        String insertQuery = "INSERT INTO users (username, password, name, email) VALUES (?, ?, ?, ?)";

        try (Connection conn = DbConnection.getDatabaseConnection().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, username.getText());
            pstmt.setString(2, password.getText()); // 실제 프로젝트에서는 암호화 필수
            pstmt.setString(3, name.getText());
            pstmt.setString(4, email.getText());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                showAlert("회원가입 성공", "로그인 화면으로 이동합니다.");
                switchToLoginView(event);
            }
        } catch (SQLException e) {
            showAlert("데이터베이스 오류", "오류 코드: " + e.getErrorCode());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (username.getText().isEmpty() || password.getText().isEmpty() ||
                name.getText().isEmpty() || email.getText().isEmpty()) {
            showAlert("입력 오류", "모든 필드를 채워주세요.");
            return false;
        }

        if (!password.getText().equals(passwordConfirm.getText())) {
            showAlert("비밀번호 불일치", "비밀번호 확인이 일치하지 않습니다.");
            return false;
        }
        return true;
    }

    private void switchToLoginView(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Scene scene = new Scene(loginView);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Store POS - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void initialize() {
        characterImage.setImage(new Image(getClass().getResource("/asset/character.png").toExternalForm()));
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Scene scene = new Scene(loginView);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Store POS - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
