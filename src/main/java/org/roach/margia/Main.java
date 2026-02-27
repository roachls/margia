package org.roach.margia;

import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.prefs.BackingStoreException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.pushingpixels.radiance.theming.api.RadianceSkin;
import org.pushingpixels.radiance.theming.api.RadianceThemingCortex;
import org.pushingpixels.radiance.theming.api.RadianceThemingSlices.FocusKind;
import org.pushingpixels.radiance.theming.api.skin.GeminiSkin;
import org.roach.margia.MainParams.UiType;
import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.ExternalTimingSource;
import org.roach.margia.controller.timing.InternalTimingSource;
import org.roach.margia.model.Key;
import org.roach.margia.model.MusicianList;
import org.roach.margia.storage.Options;
import org.roach.margia.util.DieRoller;
import org.roach.margia.view.MargiaWindow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Main entry point for Margia
 */
@SuppressWarnings("java:S106")
public class Main {

    /**
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.printf("[%s] %s%n", t.getName(), e.getMessage());
            e.printStackTrace();
        });

        var params = new MainParams();
        // @formatter:off
        var jCommanderBuilder = JCommander.newBuilder()
                .addObject(params);
        var jCommander = jCommanderBuilder.build();
        // @formatter:on
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jCommander.usage();
            return;
        }

        if (params.file != null) {
            try (var is = Files.newInputStream(params.file)) {
                Options.getInstance().load(is);
                Options.getInstance().setFilename(params.file);
                Options.getInstance().setSaveDir(params.file.getParent());
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

        var transport = Transport.instance();
        musicians.values().forEach(m -> transport.addPropertyListener(Transport.RESET_PROPERTY, m));
        transport.addPropertyListener(Transport.RESET_PROPERTY, _ -> {
            Key.reset();
            DieRoller.reset();
        });

        var timing = Options.getInstance().getMidiOptions().isUsingExternalTiming()
                ? new ExternalTimingSource(transport)
                : new InternalTimingSource(transport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timing.stop();
            transport.stop();
            MidiController.getInstance().close();
        }));

        if (params.ui == UiType.SWING) {
            SwingUtilities.invokeLater(() -> {
                RadianceSkin skin = new GeminiSkin();
                RadianceThemingCortex.GlobalScope.setSkin(skin);
                RadianceThemingCortex.GlobalScope.setFocusKind(FocusKind.NONE);
                JFrame.setDefaultLookAndFeelDecorated(true);
                var ui = new MargiaWindow(timing, transport);
                ui.setExtendedState(Frame.MAXIMIZED_BOTH);
                ui.setResizable(false);
                ui.setVisible(true);
            });
        } else {
            timing.start();
            try (var scanner = new Scanner(System.in)) {
                scanner.nextLine();
                System.exit(0);
            }
        }

    }

}
