package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class LoginController {

    @FXML
    private ImageView characterImage;

    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            Parent mainPanel = FXMLLoader.load(getClass().getResource("/view/MainPanelView.fxml"));
            Scene scene = new Scene(mainPanel);

            // 현재 윈도우(Stage) 가져오기
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Store POS - Main");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        characterImage.setImage(new Image(getClass().getResource("/asset/character.png").toExternalForm()));
    }
}
