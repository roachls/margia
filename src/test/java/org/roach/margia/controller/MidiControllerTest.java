package org.roach.margia.controller;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianOptions;
import org.roach.margia.storage.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MidiControllerTest {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @SuppressWarnings({ "java:S2699", "java:S2925" })
    void testPlayNote() throws InterruptedException {
        Options.getInstance().getMusicOptions().setTempo(60);
        Options.getInstance().getMidiOptions().setUsingExternalMidi(false);
        var mo = new MusicianOptions();
        mo.setDeviceName("Microsoft GS Wavetable Synth");
        Options.getInstance().getMusicians().put(1, mo);
        var controller = MidiController.getInstance();
        controller.scanForMidiOutputDevices();
        for (var note = 48; note < 84; note++) {
            logger.atInfo().setMessage("Playing {}").addArgument(note).log();
            var chord = new Chord(Set.of(note, note + 4, note + 7), 1, 70);
            controller.playChord("Microsoft GS Wavetable Synth", 0, chord);
            controller.playChordsThisTick();
            Thread.sleep(250);
        }
        controller.close();
    }

}
