package org.roach.margia;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roach.margia.storage.Options;

class MidiControllerTest {
    Logger logger = LogManager.getLogger(getClass());

    @Test
    @Disabled("Only for local use with a DAW")
    @SuppressWarnings({ "java:S2699", "java:S2925" })
    void testPlayNote() throws InterruptedException {
        Options.getInstance().getMusicOptions().setTempo(60);
        Options.getInstance().getMidiOptions().setUseExternalMidi(true);
        var controller = MidiController.getInstance();
        controller.setMidiDevice(true);
        for (var note : Key.Chromatic.notes()) {
            logger.atInfo().log("Playing {}", note);
            controller.playNote(0, new NoteInfo(note, 70, 1));
            controller.playNotesThisTick();
            Thread.sleep(250);
        }
        controller.close();
    }

}
