package ua.auk.hw12.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ua.auk.hw12.model.Player;
import ua.auk.hw12.model.Position;

public class PlayerForm extends VBox {

    private final TextField nameField = new TextField();
    private final ComboBox<Position> positionBox = new ComboBox<>(FXCollections.observableArrayList(Position.values()));
    private final TextField numberField = new TextField();
    private final TextField nationalityField = new TextField();
    private final TextField goalsField = new TextField();
    private final Label modeLabel = new Label();
    private final Button addButton = requestButton("Add as new player", "POST");
    private final Button saveButton = requestButton("Save changes", "PUT");
    private final Button deleteButton = requestButton("Delete player", "DELETE");
    private final Button clearButton = new Button("Clear form");
    private final ObjectProperty<Player> current = new SimpleObjectProperty<>();

    public PlayerForm() {
        super(14);
        getStyleClass().add("player-form");
        setPrefWidth(330);
        setMinWidth(330);

        Label title = new Label("Player details");
        title.getStyleClass().add("form-title");
        modeLabel.getStyleClass().add("form-mode");

        nameField.setId("nameField");
        positionBox.setId("positionBox");
        numberField.setId("numberField");
        nationalityField.setId("nationalityField");
        goalsField.setId("goalsField");
        addButton.setId("addButton");
        saveButton.setId("saveButton");
        deleteButton.setId("deleteButton");
        clearButton.setId("clearButton");

        nameField.setPromptText("e.g. Petro Melnyk");
        positionBox.setPromptText("Choose a position");
        positionBox.setMaxWidth(Double.MAX_VALUE);
        numberField.setPromptText("1 - 99");
        nationalityField.setPromptText("e.g. Ukraine");
        goalsField.setPromptText("0");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(10);
        addRow(fields, 0, "Name", nameField);
        addRow(fields, 1, "Position", positionBox);
        addRow(fields, 2, "Shirt number", numberField);
        addRow(fields, 3, "Nationality", nationalityField);
        addRow(fields, 4, "Goals", goalsField);

        addButton.getStyleClass().add("accent");
        deleteButton.getStyleClass().add("danger");
        for (Button button : new Button[]{addButton, saveButton, deleteButton, clearButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        saveButton.disableProperty().bind(current.isNull());
        deleteButton.disableProperty().bind(current.isNull());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox buttons = new VBox(8, addButton, saveButton, deleteButton, clearButton);

        getChildren().addAll(new VBox(2, title, modeLabel), fields, buttons, spacer);
        current.addListener((observable, oldPlayer, player) -> updateModeLabel());
        updateModeLabel();
    }

    private static Button requestButton(String text, String method) {
        Label methodTag = new Label(method);
        methodTag.getStyleClass().add("method-tag");
        Button button = new Button(text, methodTag);
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setGraphicTextGap(10);
        return button;
    }

    private void addRow(GridPane grid, int row, String text, Region field) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        label.setMinWidth(Region.USE_PREF_SIZE);
        GridPane.setHgrow(field, Priority.ALWAYS);
        grid.addRow(row, label, field);
    }

    private void updateModeLabel() {
        Player player = current.get();
        if (player == null) {
            modeLabel.setText("New player - fill in the fields and press Add");
        } else {
            modeLabel.setText("Editing player #" + player.id() + " - " + player.name());
        }
    }

    public void show(Player player) {
        current.set(player);
        nameField.setText(player.name());
        positionBox.setValue(player.position());
        numberField.setText(String.valueOf(player.shirtNumber()));
        nationalityField.setText(player.nationality());
        goalsField.setText(String.valueOf(player.goals()));
    }

    public void clear() {
        current.set(null);
        nameField.clear();
        positionBox.setValue(null);
        numberField.clear();
        nationalityField.clear();
        goalsField.clear();
    }

    public Player readPlayer() {
        int shirtNumber = readNumber(numberField, "Shirt number");
        int goals = readNumber(goalsField, "Goals");
        int id = current.get() == null ? 0 : current.get().id();
        return new Player(id, nameField.getText().trim(), positionBox.getValue(), shirtNumber,
                nationalityField.getText().trim(), goals);
    }

    private int readNumber(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    public Player getCurrent() {
        return current.get();
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }

    public Button getClearButton() {
        return clearButton;
    }
}
