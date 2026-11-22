package org.roach.margia.view;

import java.io.IOException;

import org.roach.margia.model.MidiOptions;
import org.roach.margia.storage.Options;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * The panel for choosing MIDI options for a musician
 */
public class MidiPanel extends VBox {

    @FXML
    CheckBox external;
    @FXML
    CheckBox sendMidiTimecode;
    @FXML
    CheckBox sendPanMessage;
    @FXML
    Spinner<Integer> panController;
    @FXML
    CheckBox panWithRelativeLocations;
    @FXML
    CheckBox sendVerticalPanMessage;
    @FXML
    Spinner<Integer> verticalPanController;
    @FXML
    CheckBox verticalPanWithRelativeLocations;

    @FXML
    private Label panControllerLabel;
    @FXML
    private Label verticalPanControllerLabel;

    /**
     * Construct a new {@link MidiPanel}
     */
    public MidiPanel() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MidiPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MidiPanel.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        var midiOpts = Options.getInstance().getMidiOptions();

        external.setSelected(midiOpts.isUsingExternalMidi());
        panController.getValueFactory().setValue(MidiOptions.DEFAULT_PAN_CONTROLLER);
        verticalPanController.getValueFactory().setValue(MidiOptions.DEFAULT_VERTICAL_PAN_CONTROLLER);

        external.selectedProperty().addListener((_, _, newVal) -> midiOpts.setUsingExternalMidi(newVal));
        sendMidiTimecode.selectedProperty().addListener((_, _, newVal) -> midiOpts.setSendingMidiTimecode(newVal));
        sendPanMessage.selectedProperty().addListener((_, _, newVal) -> midiOpts.setSendPanMessage(newVal));
        panController.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                midiOpts.setPanController(newVal);
            }
        });

        panWithRelativeLocations.selectedProperty()
                .addListener((_, _, newVal) -> midiOpts.setPanWithRelativeLocations(newVal));

        sendVerticalPanMessage.selectedProperty()
                .addListener((_, _, newVal) -> midiOpts.setSendVerticalPanMessage(newVal));

        verticalPanController.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                midiOpts.setVerticalPanController(newVal);
            }
        });

        verticalPanWithRelativeLocations.selectedProperty()
                .addListener((_, _, newVal) -> midiOpts.setVerticalPanWithRelativeLocations(newVal));

        // Declarative Disable Bindings
        var externalNotSelected = external.selectedProperty().not();
        var panDisabled = externalNotSelected.or(sendPanMessage.selectedProperty().not());
        var verticalPanDisabled = externalNotSelected.or(sendVerticalPanMessage.selectedProperty().not());

        sendMidiTimecode.disableProperty().bind(externalNotSelected);
        sendPanMessage.disableProperty().bind(externalNotSelected);
        sendVerticalPanMessage.disableProperty().bind(externalNotSelected);

        panController.disableProperty().bind(panDisabled);
        panControllerLabel.disableProperty().bind(panDisabled);
        panWithRelativeLocations.disableProperty().bind(panDisabled);

        verticalPanController.disableProperty().bind(verticalPanDisabled);
        verticalPanControllerLabel.disableProperty().bind(verticalPanDisabled);
        verticalPanWithRelativeLocations.disableProperty().bind(verticalPanDisabled);
    }

    @Override
    public String toString() {
        return "MIDI";
    }
}
