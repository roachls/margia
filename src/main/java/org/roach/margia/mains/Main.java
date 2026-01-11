package org.roach.margia.mains;

import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.prefs.BackingStoreException;

import javax.swing.SwingUtilities;

import org.roach.margia.*;
import org.roach.margia.mains.MainParams.UiType;
import org.roach.margia.random.DieRoller;
import org.roach.margia.timing.InternalTimingSource;
import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.MargiaWindow;

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
        var algMap = new HashMap<String, Algorithm<?>>();
        var paramMap = new HashMap<String, MargiaParams>();

        var loader = ServiceLoader.load(Algorithm.class);
        loader.forEach(c -> algMap.put(c.command(), c));

        var params = new MainParams();
        // @formatter:off
        var jCommanderBuilder = JCommander.newBuilder()
                .addObject(params);
        for (var algEntry : algMap.entrySet()) {
            var algParams = algEntry.getValue().createParams();
            paramMap.put(algEntry.getKey(), algParams);
                jCommanderBuilder.addCommand(algEntry.getKey(), algParams);
        }
        var jCommander = jCommanderBuilder.build();
        // @formatter:on
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            if (e.getMessage().startsWith("Expected a command")) {
                System.err.println("Available commands:" + algMap.keySet());
            }
            jCommander.usage();
            return;
        }

        if (params.file != null) {
            try (var is = Files.newInputStream(params.file)) {
                Options.getInstance().load(is);
                Options.getInstance().setFilename(params.file);
                Options.getInstance().setSaveDir(params.file.getParent());
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (BackingStoreException e) {
                System.err.println("Error writing save directory to preferences: " + e.getMessage());
            }
        }
        var tempo = Options.getInstance().getOrDefaultAsInt(TimingSource.TEMPO_PROPERTY, params.tempo);

        var command = jCommander.getParsedCommand();
        Algorithm<?> algorithm = algMap.get(command);
        if (algorithm == null) {
            System.err.println("Unable to obtain algorithm: " + command);
            return;
        }
        var musicians = MusicianList.getInstance().getMusicians();

        Key.setRandomSeed(params.randomSeed);
        DieRoller.setSeed(params.randomSeed);

        var transport = new Transport(params.tempo);
        musicians.forEach(m -> transport.addPropertyListener(Transport.RESET_PROPERTY, m));
        transport.addPropertyListener(Transport.RESET_PROPERTY, _ -> {
            Key.reset();
            DieRoller.reset();
        });
        transport.setControlDawTiming(params.sendExternalMidi);

        var timing = new InternalTimingSource(transport, tempo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timing.stop();
            transport.stop();
            MidiController.getInstance().close();
        }));

        if (params.ui == UiType.SWING) {
            SwingUtilities.invokeLater(() -> {
                var ui = new MargiaWindow(algorithm.displayName(), timing, transport);
                ui.setExtendedState(Frame.MAXIMIZED_BOTH);
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
