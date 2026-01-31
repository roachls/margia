package org.roach.margia.controller;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;

class MidiControllerTest {
    Logger logger = LogManager.getLogger(getClass());

    @Test
    @Disabled("Only for local use with a DAW")
    @SuppressWarnings({ "java:S2699", "java:S2925" })
    void testPlayNote() throws InterruptedException {
        Options.getInstance().getMusicOptions().setTempo(60);
        Options.getInstance().getMidiOptions().setUseExternalMidi(false);
        var controller = MidiController.getInstance();
        controller.scanForMidiOutputDevices();
        for (var note = 48; note < 84; note++) {
            logger.atInfo().log("Playing {}", note);
            var chord = new Chord(Set.of(note, note + 4, note + 7), 1, 70);
            controller.playChord(MusicianOptions.ALL_BUSSES, 0, chord);
            controller.playChordsThisTick();
            Thread.sleep(250);
        }
        controller.close();
    }

}
