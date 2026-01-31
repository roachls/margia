package org.roach.margia.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a musical key along with a range of allowed notes
 */
@SuppressWarnings({ "javadoc", "java:S2386" })
public interface Key {
    RandomHolder randomHolder = new RandomHolder();

    class RandomHolder {
        private Random random = new Random();
        private long seed;

        void setRandomSeed(long seed) {
            this.seed = seed;
            random.setSeed(seed);
        }

        void reset() {
            this.random = new Random();
            random.setSeed(seed);
        }
    }

    static final List<Integer> MAJOR_INTERVALS = List.of(2, 2, 1, 2, 2, 2, 1);
    static final List<Integer> PENTATONIC_INTERVALS = List.of(2, 2, 3, 2, 3);
    static final List<Integer> CHROMATIC_INTERVALS = List.of(1);

    static final String MAJOR_INTERVAL_KEY = "MAJOR_INTERVALS";
    static final String PENTATONIC_KEY = "PENTATONIC_INTERVALS";
    static final String CHROMATIC_KEY = "CHROMATIC";

    // @formatter:off
    static final Map<String, List<Integer>> AVAILABLE_BASIS = Map.of(
            MAJOR_INTERVAL_KEY, MAJOR_INTERVALS,
            PENTATONIC_KEY, PENTATONIC_INTERVALS,
            CHROMATIC_KEY, CHROMATIC_INTERVALS
    );
    static final Map<List<Integer>, String> BASIS_MAP = Map.of(
            MAJOR_INTERVALS, MAJOR_INTERVAL_KEY,
            PENTATONIC_INTERVALS, PENTATONIC_KEY,
            CHROMATIC_INTERVALS, CHROMATIC_KEY
            );
    // @formatter:on

    static Key fromOptions(String keyName) {
        return BUILTIN_KEYS.get(keyName);
    }

    /**
     * @param seed random seed to use
     */
    static void setRandomSeed(long seed) {
        randomHolder.setRandomSeed(seed);
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
    static Key generateKey(final String name, final String basis, final int startingNote) {
        var intervals = AVAILABLE_BASIS.get(basis);
        var n = startingNote;
        var intervalNum = 0;
        var list = new ArrayList<Integer>();
        while (n <= 127) {
            list.add(n);
            n += intervals.get(intervalNum);
            intervalNum++;
            intervalNum %= intervals.size();
        }
        return new Key() {

            @Override
            public List<Integer> notes() {
                return list;
            }

            @Override
            public String getName() { return name; }

            @Override
            public String getBasis() { return basis; }

        };
    }

    /**
     * @param intervals intervals making up the key
     * @param octaves   a list of octaves; need not be in order, and need not
     *                  include all intervening octaves. For example, including O2
     *                  and O6 will also include all notes from O3, O4, and O5.
     * @return a key
     */
    Key CMajor = generateKey("C Major", MAJOR_INTERVAL_KEY, 0);
    Key DbMajor = CMajor.transposeUp("Db Major", 1);
    Key DMajor = CMajor.transposeUp("D Major", 2);
    Key EbMajor = CMajor.transposeUp("Eb Major", 3);
    Key EMajor = CMajor.transposeUp("E Major", 4);
    Key FMajor = CMajor.transposeUp("F Major", 5);
    Key GbMajor = CMajor.transposeUp("Gb Major", 6);
    Key GMajor = CMajor.transposeUp("G Major", 7);
    Key AbMajor = CMajor.transposeUp("Ab Major", 8);
    Key AMajor = CMajor.transposeUp("A Major", 9);
    Key BbMajor = CMajor.transposeUp("Bb Major", 10);
    Key BMajor = CMajor.transposeUp("B Major", 11);

    Key CPentatonic = generateKey("C Pentatonic", PENTATONIC_KEY, 0);

    Key Chromatic = generateKey("Chromatic", CHROMATIC_KEY, 0);

    static final Map<String, Key> BUILTIN_KEYS = List
            .of(CMajor, DbMajor, DMajor, EbMajor, EMajor, FMajor, GbMajor, GMajor, AbMajor, AMajor, BbMajor, BMajor,
                    CPentatonic, Chromatic)
            .stream().collect(Collectors.toMap(Key::getName, k -> k, (_, k2) -> k2, TreeMap::new));

    List<Integer> notes();

    default int randomNote() {
        var noteNum = randomHolder.random.nextInt(notes().size());
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
        int nStart = start;
        var list = this.notes();
        int startIndex = list.indexOf(nStart);
        while (startIndex == -1) {
            nStart--;
            startIndex = list.indexOf(nStart);
        }
        int endIndex = (startIndex + interval - 1) % list.size();
        return list.get(endIndex);
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
        int nStart = start;
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

    default Key transposeUp(String name, int interval) {
        return generateKey(name, this.getBasis(), interval);
    }

    default String getName() { return BASIS_MAP.get(notes()); }

    String getBasis();

    static void reset() {
        randomHolder.reset();
    }
}
