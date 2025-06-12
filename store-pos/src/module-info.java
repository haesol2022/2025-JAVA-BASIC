module store.pos {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jasperreports;
    requires com.oracle.database.jdbc; // ojdbc11의 실제 모듈명
    requires org.controlsfx.controls;  // ControlsFX의 실제 모듈명

    opens main to javafx.fxml;
    exports main;
}
