//package org.roach.margia;
//
//import org.roach.margia.messages.MusicianMessage;
//
///**
// * @param noteNum  the note to play
// */
//public record NoteInfo(int noteNum) implements MusicianMessage {
//
//    /**
//     * Sanity check params
//     * 
//     * @param noteNum  note number (0-127)
//     * @param velocity velocity (0-127)
//     */
//    public NoteInfo {
//        if (noteNum < -1)
//            noteNum = -1;
//        else if (noteNum > 127)
//            noteNum = 127;
//    }
//
//}
