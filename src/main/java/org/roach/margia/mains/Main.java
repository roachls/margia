package org.roach.margia.mains;

import java.awt.Frame;
import java.util.HashMap;
import java.util.ServiceLoader;

import javax.swing.SwingUtilities;

import org.roach.margia.*;
import org.roach.margia.random.DieRoller;
import org.roach.margia.timing.InternalTimingSource;
import org.roach.margia.ui.MargiaWindow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Main entry point for Margia
 */
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

        MidiController controller;
        if (params.sendExternalMidi) {
            controller = new MidiController("loopMIDI Port", params.tempo);
        } else {
            controller = new MidiController(MidiController.DEFAULT_SYNTH, params.tempo);
        }

        var command = jCommander.getParsedCommand();
        Algorithm<?> algorithm = algMap.get(command);
        if (algorithm == null) {
            System.err.println("Unable to obtain algorithm: " + command);
            return;
        }
        var algParamGeneric = paramMap.get(command);
        var musicians = algorithm.initMusicians(params, algParamGeneric, controller);

        Key.setRandomSeed(params.randomSeed);
        DieRoller.setSeed(params.randomSeed);

        var transport = new Transport(musicians, params.tempo, controller);
        transport.setControlDawTiming(params.sendExternalMidi);

        var timing = new InternalTimingSource(transport, params.tempo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timing.stop();
            transport.stop();
            controller.close();
        }));

        SwingUtilities.invokeLater(() -> {
            var ui = new MargiaWindow(algorithm.displayName(), timing, transport, musicians);
            ui.setExtendedState(Frame.MAXIMIZED_BOTH);
            ui.setVisible(true);
        });

    }

}
