import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        var url = getClass().getResource("/fxml/login.fxml");

        if (url == null) {
            throw new RuntimeException("login.fxml NOT FOUND — check resources/fxml/");
        }

        Parent root = FXMLLoader.load(url);
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}