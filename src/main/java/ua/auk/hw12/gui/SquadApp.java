package ua.auk.hw12.gui;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ua.auk.hw12.client.SquadServiceClient;
import ua.auk.hw12.model.Player;
import ua.auk.hw12.model.Position;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SquadApp extends Application {

    private static final String SERVICE_URL = "http://localhost:8080/api/players";
    private static final String ALL_POSITIONS = "All positions";

    private final SquadServiceClient client = new SquadServiceClient(SERVICE_URL);
    private final TableView<Player> table = new TableView<>();
    private final ComboBox<String> positionFilter = new ComboBox<>();
    private final TextField findField = new TextField();
    private final Label countLabel = new Label();
    private final PlayerForm form = new PlayerForm();
    private final Label status = new Label("Ready");
    private final ProgressIndicator progress = new ProgressIndicator();
    private Stage stage;
    private int runningTasks;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        setUpTable();

        positionFilter.getItems().add(ALL_POSITIONS);
        for (Position position : Position.values()) {
            positionFilter.getItems().add(position.name());
        }
        positionFilter.setValue(ALL_POSITIONS);
        positionFilter.setId("positionFilter");
        positionFilter.valueProperty().addListener((observable, oldValue, value) -> loadPlayers(null, null));

        form.getAddButton().setOnAction(event -> addPlayer());
        form.getSaveButton().setOnAction(event -> updatePlayer());
        form.getDeleteButton().setOnAction(event -> deletePlayer());
        form.getClearButton().setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            form.clear();
            showStatus("Ready", false);
        });

        BorderPane content = new BorderPane(tablePage());
        content.setRight(form);

        BorderPane root = new BorderPane(content);
        root.setTop(header());
        root.setBottom(statusBar());

        Scene scene = new Scene(root, 1180, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("Squad Manager");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        loadPlayers(null, null);
    }

    private void setUpTable() {
        table.setId("playersTable");
        table.getColumns().setAll(List.of(
                column("ID", Player::id, 60, "number"),
                column("Name", Player::name, 210, null),
                positionColumn(),
                column("No.", Player::shirtNumber, 60, "number"),
                column("Nationality", Player::nationality, 140, null),
                column("Goals", Player::goals, 80, "goals")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No players"));
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldPlayer, player) -> {
            if (player != null) {
                form.show(player);
            }
        });
    }

    private TableColumn<Player, Object> column(String title, Function<Player, Object> value, double width, String styleClass) {
        TableColumn<Player, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(value.apply(data.getValue())));
        column.setPrefWidth(width);
        column.setReorderable(false);
        if (styleClass != null) {
            column.getStyleClass().add(styleClass);
        }
        return column;
    }

    private TableColumn<Player, Object> positionColumn() {
        TableColumn<Player, Object> column = column("Position", Player::position, 130, null);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object position, boolean empty) {
                super.updateItem(position, empty);
                if (empty || position == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(position.toString());
                badge.getStyleClass().addAll("position-badge", "position-" + position.toString().toLowerCase());
                setGraphic(badge);
            }
        });
        return column;
    }

    private HBox header() {
        Label logo = new Label("⚽");
        logo.getStyleClass().add("logo");
        Label title = new Label("Squad Manager");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Client of my own REST web service  ·  " + SERVICE_URL);
        subtitle.getStyleClass().add("app-subtitle");

        HBox header = new HBox(14, logo, new VBox(2, title, subtitle));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    private VBox tablePage() {
        Label heading = new Label("Squad");
        heading.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("count");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        findField.setId("findField");
        findField.setPromptText("Player id");
        findField.setPrefWidth(90);
        findField.setOnAction(event -> findPlayer());
        Button findButton = new Button("Find");
        findButton.setId("findButton");
        findButton.setOnAction(event -> findPlayer());
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> loadPlayers(null, null));

        HBox toolbar = new HBox(10, heading, countLabel, spacer, new Label("Position"), positionFilter,
                findField, findButton, refreshButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(12, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("page");
        return page;
    }

    private HBox statusBar() {
        progress.setId("progress");
        progress.setPrefSize(16, 16);
        progress.setVisible(false);
        status.setId("status");
        HBox bar = new HBox(8, progress, status);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void loadPlayers(Integer selectId, String doneMessage) {
        String filter = positionFilter.getValue();
        boolean all = ALL_POSITIONS.equals(filter);
        String request = all ? "GET /api/players" : "GET /api/players?position=" + filter;

        runInBackground(request,
                () -> all ? client.getAllPlayers() : client.getPlayersByPosition(Position.valueOf(filter)),
                players -> {
                    table.getItems().setAll(players);
                    countLabel.setText(players.size() + (players.size() == 1 ? " player" : " players"));
                    if (selectId != null) {
                        selectInTable(selectId);
                    }
                    showStatus(doneMessage != null ? doneMessage : request + "  →  " + countLabel.getText(), false);
                });
    }

    private void findPlayer() {
        int id;
        try {
            id = Integer.parseInt(findField.getText().trim());
        } catch (NumberFormatException e) {
            showStatus("Player id must be a whole number", true);
            return;
        }
        String request = "GET /api/players/" + id;
        runInBackground(request, () -> client.getPlayer(id), player -> {
            table.getSelectionModel().clearSelection();
            form.show(player);
            selectInTable(id);
            showStatus(request + "  →  found " + player.name(), false);
        });
    }

    private void addPlayer() {
        Player player;
        try {
            player = form.readPlayer();
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
            return;
        }
        runInBackground("POST /api/players", () -> client.addPlayer(player), saved ->
                loadPlayers(saved.id(), "POST /api/players  →  added " + saved.name() + " with id " + saved.id()));
    }

    private void updatePlayer() {
        Player player;
        try {
            player = form.readPlayer();
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
            return;
        }
        String request = "PUT /api/players/" + player.id();
        runInBackground(request, () -> client.updatePlayer(player.id(), player), saved ->
                loadPlayers(saved.id(), request + "  →  saved " + saved.name()));
    }

    private void deletePlayer() {
        Player player = form.getCurrent();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + player.name() + " (id " + player.id() + ") from the squad?");
        confirm.setHeaderText("Delete player");
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        String request = "DELETE /api/players/" + player.id();
        runInBackground(request, () -> {
            client.deletePlayer(player.id());
            return player;
        }, deleted -> {
            form.clear();
            loadPlayers(null, request + "  →  deleted " + deleted.name());
        });
    }

    private void selectInTable(int id) {
        for (Player player : table.getItems()) {
            if (player.id() == id) {
                table.getSelectionModel().select(player);
                table.scrollTo(player);
                return;
            }
        }
    }

    private <T> void runInBackground(String request, Supplier<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(event -> {
            finishTask();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            finishTask();
            showStatus(request + "  →  " + task.getException().getMessage(), true);
        });

        runningTasks++;
        progress.setVisible(true);
        showStatus(request + " ...", false);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void finishTask() {
        runningTasks--;
        progress.setVisible(runningTasks > 0);
    }

    private void showStatus(String text, boolean error) {
        status.setText(text);
        status.getStyleClass().remove("error");
        if (error) {
            status.getStyleClass().add("error");
        }
    }
}
