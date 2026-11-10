package org.roach.margia.view;

import java.io.IOException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

public class MusicianOptionsPanel extends BorderPane {

    @FXML private Tab uiTab;
    @FXML private Tab musicTab;
    @FXML private Tab ruleTab;

    private final int id;
    private final MusicianUiPanel musicianUiPanel;
    private final MusicianMusicalOptionsPanel musicPanel;
    private final MusicianRuleOptionsPanel rulePanel;

    MusicianOptionsPanel(MusicianUiPanel uiPanel, MusicianMusicalOptionsPanel musicPanel,
            MusicianRuleOptionsPanel rulePanel) {
        this.id = musicPanel.id;
        this.musicianUiPanel = uiPanel;
        this.musicPanel = musicPanel;
        this.rulePanel = rulePanel;
        loadFxml();
    }

    public MusicianOptionsPanel(List<MusicianOptionsPanel> mups) {
        this.id = -1;
        this.musicianUiPanel = new MusicianUiPanel(mups.stream().map(m -> m.musicianUiPanel).toList());
        this.musicPanel = new MusicianMusicalOptionsPanel(mups.stream().map(m -> m.musicPanel).toList());
        this.rulePanel = new MusicianRuleOptionsPanel(mups.stream().map(m -> m.rulePanel).toList());
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MusicianOptionsPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MusicianOptionsPanel.fxml", e);
        }

        uiTab.setContent(musicianUiPanel);
        musicTab.setContent(musicPanel);
        ruleTab.setContent(rulePanel);
    }

    @Override
    public String toString() {
        return OptionsWindow.MUSICIAN + id;
    }
}
