package org.roach.margia.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.*;
import org.roach.margia.model.Key;
import org.roach.margia.model.MusicianList;
import org.roach.margia.storage.Options;
import org.roach.margia.util.DieRoller;
import org.roach.margia.util.MargiaFileUtils;
import org.roach.margia.view.MargiaWindow;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for Margia
 */
@SuppressWarnings("java:S106")
public class JavaFxMain extends Application {
    private static TimingSource timing;
    private static Transport transport;
    private static final Logger JAVAFX_LOGGER = Logger.getLogger("javafx");
    private static final Logger CSS_LOGGER = Logger.getLogger("javafx.css");
    
    /**
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.printf("[%s] %s%n", t.getName(), e.getMessage());
            e.printStackTrace();
        });
        
        JAVAFX_LOGGER.setLevel(Level.FINE);
        CSS_LOGGER.setLevel(Level.FINE);

        if (args.length >= 1) {
            Path filePath = Path.of(args[0]);
            try (var is = Files.newInputStream(filePath)) {
                Options.getInstance().load(is);
                Options.getInstance().setFilename(filePath);
                Options.getInstance().setSaveDir(filePath.getParent());
                MargiaFileUtils.saveRecentsToPersistence(filePath.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BackingStoreException e) {
                System.err.println("Error writing save directory to preferences: " + e.getMessage());
            }
        } else {
            MidiController.getInstance().scanForMidiInputDevices();
            MidiController.getInstance().scanForMidiOutputDevices();
        }
        var musicians = MusicianList.getInstance().getMusicians();

        Key.initFromOptions();
        DieRoller.initFromOptions();

        transport = Transport.instance();
        musicians.values().forEach(m -> transport.addPropertyListener(Transport.RESET_PROPERTY, m));
        transport.addPropertyListener(Transport.RESET_PROPERTY, _ -> {
            Key.reset();
            DieRoller.reset();
        });

        timing = Options.getInstance().getMidiOptions().isUsingExternalTiming()
                ? new ExternalTimingSource(transport)
                : new InternalTimingSource(transport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timing.stop();
            transport.stop();
            MidiController.getInstance().close();
        }));

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Construct and display MargiaWindow on the JavaFX Application Thread
        var ui = new MargiaWindow(timing, transport);
        ui.setMaximized(true);
        ui.show();
    }
}
