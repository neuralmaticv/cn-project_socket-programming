module com.vladocodes.cn_project {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.vladocodes.cn_project to javafx.fxml;
    exports com.vladocodes.cn_project;
}