package sn.naat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    private static Stage stagePrincipal;

    @Override
    public void start(Stage stage) {
        stagePrincipal = stage;
        stage.setTitle("NAAT — Tontine sénégalaise modernisée");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        naviguerVers("/sn/naat/view/tontines_publiques.fxml");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.centerOnScreen();
        stage.show();
    }

    /** Change l'écran affiché dans la fenêtre principale. */
    public static void naviguerVers(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    MainApp.class.getResource("/sn/naat/css/style.css")).toExternalForm());
            stagePrincipal.setScene(scene);
            stagePrincipal.centerOnScreen();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la vue : " + fxmlPath, e);
        }
    }

    public static Stage getStagePrincipal() {
        return stagePrincipal;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
