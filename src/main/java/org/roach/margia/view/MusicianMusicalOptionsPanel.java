package org.roach.margia.view;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.roach.margia.controller.MidiController;
import org.roach.margia.model.Key;
import org.roach.margia.model.MusicianOptions;
import org.roach.margia.model.NoteRange;

public class MusicianMusicalOptionsPanel extends VBox {
    private static final String MISMATCH_CONTROL_STYLE = "-fx-base: #ffcccc;";
    private static final String MISMATCH_EDITOR_STYLE = "-fx-control-inner-background: #ffcccc;";

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> key;
    @FXML private Spinner<Integer> rangeLow;
    @FXML private Spinner<Integer> rangeHi;
    @FXML private Spinner<Integer> channel;
    @FXML private ComboBox<String> device;

    final int id;
    private final MusicianOptions options;

    public MusicianMusicalOptionsPanel(MusicianOptions options) {
        this.id = options.getId();
        this.options = options;
        loadFxml();

        titleLabel.setText(OptionsWindow.MUSICIAN + id + " musical options");

        // Set initial values
        key.setValue(options.getKeyName());
        rangeLow.getValueFactory().setValue(options.getRange().low());
        rangeHi.getValueFactory().setValue(options.getRange().high());
        channel.getValueFactory().setValue(options.getChannel() + 1);
        device.setValue(options.getDeviceName());

        // Attach model listeners
        key.valueProperty().addListener((_, _, newVal) -> options.setKeyName(newVal));

        rangeLow.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                options.setRange(options.getRange().withLow(newVal));
            }
        });

        rangeHi.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                options.setRange(options.getRange().withHigh(newVal));
            }
        });

        channel.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                options.setChannel(newVal - 1);
            }
        });

        device.valueProperty().addListener((_, _, newVal) -> options.setDeviceName(newVal));
    }

    MusicianMusicalOptionsPanel(List<MusicianMusicalOptionsPanel> others) {
        this.id = -1; // unused
        this.options = new MusicianOptions();
        loadFxml();

        var idList = others.stream().map(p -> p.id).toList();
        var optionsList = others.stream().map(p -> p.options).toList();

        var firstKeyName = optionsList.get(0).getKeyName();
        var allKeysSame = optionsList.stream().map(MusicianOptions::getKeyName).allMatch(firstKeyName::equals);
        options.setKeyName(allKeysSame ? firstKeyName : "");

        var firstLow = optionsList.get(0).getRange().low();
        var allLowsSame = optionsList.stream().map(MusicianOptions::getRange).map(NoteRange::low)
                .allMatch(l -> firstLow == l);
        if (allLowsSame) {
            options.setRange(options.getRange().withLow(firstLow));
        }

        var firstHigh = optionsList.get(0).getRange().high();
        var allHighsSame = optionsList.stream().map(MusicianOptions::getRange).map(NoteRange::high)
                .allMatch(h -> firstHigh == h);
        if (allHighsSame) {
            options.setRange(options.getRange().withHigh(firstHigh));
        }

        var firstChannel = optionsList.get(0).getChannel();
        var allChannelsSame = optionsList.stream().map(MusicianOptions::getChannel)
                .allMatch(c -> firstChannel == c);
        if (allChannelsSame) {
            options.setChannel(firstChannel);
        }

        var firstDeviceName = optionsList.get(0).getDeviceName();
        var allDevicesNamesSame = optionsList.stream().map(MusicianOptions::getDeviceName)
                .allMatch(firstDeviceName::equals);
        options.setDeviceName(allDevicesNamesSame ? firstDeviceName : "");

        titleLabel.setText(OptionsWindow.MUSICIAN + idList + " musical options");

        // Key ComboBox
        setMismatchHighlight(key, !allKeysSame);
        key.setValue(options.getKeyName());
        key.valueProperty().addListener((_, _, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                return;
            }
            options.setKeyName(newVal);
            setMismatchHighlight(key, false);
            others.forEach(o -> o.key.setValue(newVal));
        });

        // Low Note Spinner
        rangeLow.getValueFactory().setValue(options.getRange().low());
        setMismatchHighlight(rangeLow, !allLowsSame);
        rangeLow.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                setMismatchHighlight(rangeLow, false);
                options.setRange(options.getRange().withLow(newVal));
                others.forEach(o -> o.rangeLow.getValueFactory().setValue(newVal));
            }
        });

        // High Note Spinner
        rangeHi.getValueFactory().setValue(options.getRange().high());
        setMismatchHighlight(rangeHi, !allHighsSame);
        rangeHi.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                setMismatchHighlight(rangeHi, false);
                options.setRange(options.getRange().withHigh(newVal));
                others.forEach(o -> o.rangeHi.getValueFactory().setValue(newVal));
            }
        });

        // MIDI Channel Spinner
        channel.getValueFactory().setValue(options.getChannel() + 1);
        setMismatchHighlight(channel, !allChannelsSame);
        channel.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                setMismatchHighlight(channel, false);
                options.setChannel(newVal - 1);
                others.forEach(o -> o.channel.getValueFactory().setValue(newVal));
            }
        });

        // MIDI Device ComboBox
        setMismatchHighlight(device, !allDevicesNamesSame);
        device.setValue(options.getDeviceName());
        device.valueProperty().addListener((_, _, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                return;
            }
            setMismatchHighlight(device, false);
            options.setDeviceName(newVal);
            others.forEach(o -> o.device.setValue(newVal));
        });
    }

    private void setMismatchHighlight(Control control, boolean mismatch) {
        String controlStyle = mismatch ? MISMATCH_CONTROL_STYLE : "";
        String editorStyle = mismatch ? MISMATCH_EDITOR_STYLE : "";
        control.setStyle(controlStyle);
        if (control instanceof ComboBox<?> combo && combo.getEditor() != null) {
            combo.getEditor().setStyle(editorStyle);
        } else if (control instanceof Spinner<?> spinner && spinner.getEditor() != null) {
            spinner.getEditor().setStyle(editorStyle);
        }
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MusicianMusicalOptionsPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MusicianMusicalOptionsPanel.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        key.getItems().add("");
        key.getItems().addAll(Key.BUILTIN_KEYS.keySet());

        var availableDevices = new TreeSet<String>();
        availableDevices.add("");
        availableDevices.addAll(MidiController.getInstance().getAvailableOutputDevices());
        device.getItems().addAll(availableDevices);
    }

    @Override
    public String toString() {
        return "Music";
    }
}
