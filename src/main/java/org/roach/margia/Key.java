package org.roach.margia;

import java.security.SecureRandom;
import java.util.*;

@SuppressWarnings("javadoc")
public interface Key {
    Random RANDOM = new SecureRandom();

    static final List<Integer> MAJOR_INTERVALS = List.of(2, 2, 1, 2, 2, 2, 1);
    static final List<Integer> PENTATONIC_INTERVALS = List.of(2, 2, 3, 2, 3);
    static final List<Integer> CHROMATIC_INTERVALS = List.of(1);

    /**
     * @param seed random seed to use
     */
    static void setRandomSeed(long seed) {
        RANDOM.setSeed(seed);
    }

    /**
     * Generate a key using the specified intervals starting at the given start note
     * and ending with the note within the key that is less then or equal to the
     * range hi
     * 
     * @param intervals    intervals making up the key
     * @param startingNote start note
     * @param rangeHi      end range
     * @return a key
     */
    static Key generateKey(List<Integer> intervals, int startingNote, int rangeHi) {
        var n = startingNote;
        var intervalNum = 0;
        var list = new ArrayList<Integer>();
        while (n <= rangeHi) {
            list.add(n);
            n += intervals.get(intervalNum);
            intervalNum++;
            intervalNum %= intervals.size();
        }
        return () -> list;
    }

    /**
     * @param intervals intervals making up the key
     * @param octaves   a list of octaves; need not be in order, and need not
     *                  include all intervening octaves. For example, including O2
     *                  and O6 will also include all notes from O3, O4, and O5.
     * @return a key
     */
    static Key generateKey(List<Integer> intervals, List<Octave> octaves) {
        if (octaves == null || octaves.isEmpty()) {
            throw new IllegalArgumentException("octaves list must not be null or empty");
        }
        var low = octaves.stream().map(Octave::getLow).min(Integer::compare).orElse(0);
        var high = octaves.stream().map(Octave::getHigh).max(Integer::compare).orElse(127);
        return generateKey(intervals, low, high);
    }

    static Key generateKey(List<Integer> intervals) {
        return generateKey(intervals, List.of(Octave.O_NEG2, Octave.O7));
    }

    Key CMajor = generateKey(MAJOR_INTERVALS, 0, 127);
    Key DbMajor = CMajor.transposeUp(1);
    Key DMajor = CMajor.transposeUp(2);
    Key EbMajor = CMajor.transposeUp(3);
    Key EMajor = CMajor.transposeUp(4);
    Key FMajor = CMajor.transposeUp(5);
    Key GbMajor = CMajor.transposeUp(6);
    Key GMajor = CMajor.transposeUp(7);
    Key AbMajor = CMajor.transposeUp(8);
    Key AMajor = CMajor.transposeUp(9);
    Key BbMajor = CMajor.transposeUp(10);
    Key BMajor = CMajor.transposeUp(11);

    Key CPentatonic = generateKey(PENTATONIC_INTERVALS, 0, 127);

    Key Chromatic = generateKey(CHROMATIC_INTERVALS, 0, 127);

    Key DRUMPAD = Chromatic.of(Octave.O1, Octave.O1);

    List<Integer> notes();

    default Key of(int rangeLow, int rangeHi) {
        var origNotes = this.notes();
        int indexOfNearestNoteToRangeLow = 0;
        while (origNotes.get(indexOfNearestNoteToRangeLow) < rangeLow) {
            indexOfNearestNoteToRangeLow++;
        }
        var indexOfNearestNoteToRangeHi = origNotes.size() - 1;
        while (origNotes.get(indexOfNearestNoteToRangeHi) > rangeHi) {
            indexOfNearestNoteToRangeHi--;
        }
        var indexLow = indexOfNearestNoteToRangeLow;
        var indexHi = indexOfNearestNoteToRangeHi;
        return () -> origNotes.subList(indexLow, indexHi + 1);
    }

    default Key of(Octave o1, Octave o2) {
        return of(o1.getLow(), o2.getHigh());
    }

    default int randomNote() {
        var noteNum = RANDOM.nextInt(notes().size());
        return notes().get(noteNum);
    }

    /**
     * Find the note the given number of indices up within the key; if it goes above
     * the top index, wrap around to the bottom
     * 
     * @param start    starting note
     * @param interval number of indices away
     * @return the note the given interval up
     */
    default int up(int start, int interval) {
        int nStart = adjustToKeyByOctaves(start);
        var list = this.notes();
        int startIndex = list.indexOf(nStart);
        while (startIndex == -1) {
            nStart--;
            startIndex = list.indexOf(nStart);
        }
        int endIndex = (startIndex + interval - 1) % list.size();
        return list.get(endIndex);
    }

    default int adjustToKeyByOctaves(int start) {
        int nStart = start;
        if (nStart > highestNote()) {
            while (nStart > highestNote()) {
                nStart -= 12;
            }
        } else if (nStart < lowestNote()) {
            while (nStart < lowestNote()) {
                nStart += 12;
            }
        }
        return nStart;
    }

    /**
     * Find the note the given number of indices away within the key; if it goes
     * below 0, wrap around to the top
     * 
     * @param start    starting note
     * @param interval number of indices away
     * @return the note the given interval down
     */
    default int down(int start, int interval) {
        int nStart = adjustToKeyByOctaves(start);
        var list = this.notes();
        int startIndex = list.indexOf(nStart);
        while (startIndex == -1) {
            nStart++;
            startIndex = list.indexOf(nStart);
        }
        int endIndex = (startIndex - (interval - 1));
        if (endIndex < 0)
            endIndex = list.size() - 1;
        endIndex %= list.size();
        return list.get(endIndex);
    }

    default int lowestNote() {
        return notes().get(0);
    }

    default int highestNote() {
        return notes().get(notes().size() - 1);
    }

    default int range() {
        return highestNote() - lowestNote();
    }

    default float noteToRange(int note) {
        if (note <= lowestNote())
            return 0f;
        if (note >= highestNote())
            return 1f;
        return (float) (note - lowestNote()) / range();
    }

    default Key transposeUp(int interval) {
        var newNotes = this.notes().stream().map(n -> n + interval).filter(n -> n <= 127).toList();
        return generateKey(newNotes);
    }
}
