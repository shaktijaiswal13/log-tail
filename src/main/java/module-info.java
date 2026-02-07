module org.taillogs.taillogs {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.fxmisc.richtext;
    requires com.google.gson;

    opens org.taillogs.taillogs to javafx.fxml;
    opens org.taillogs.taillogs.screens to javafx.fxml;
    opens org.taillogs.taillogs.config to javafx.fxml;
    opens org.taillogs.taillogs.ui to javafx.fxml;

    exports org.taillogs.taillogs;
    exports org.taillogs.taillogs.screens;
    exports org.taillogs.taillogs.config;
    exports org.taillogs.taillogs.ui;
    exports org.taillogs.taillogs.utils;
    exports org.taillogs.taillogs.managers;
    exports org.taillogs.taillogs.models;
}