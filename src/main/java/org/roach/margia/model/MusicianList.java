package org.roach.margia.model;

import java.util.*;

import org.roach.margia.controller.Musician;

/**
 * A list of musicians that is able to restore itself from a YAML file
 */
@SuppressWarnings("java:S6548")
public class MusicianList {
    private final Map<Integer, Musician> musicians = new TreeMap<>();
    static final String MUSICIANS_PROPERTY = "musicians";
    private static MusicianList instance;

    /**
     * @return the singleton instance of {@link MusicianList}
     */
    public static MusicianList getInstance() {
        if (instance == null)
            instance = new MusicianList();
        return instance;
    }

    /**
     * Restore all musicians from file
     * 
     * @param musicianOptions properties of all musicians
     */
    public void restoreFromStorage(Map<Integer, MusicianOptions> musicianOptions) {
        if (musicianOptions == null)
            return;
        this.musicians.clear();

        var map = new HashMap<Integer, Musician>();
        var peerIdsMap = new HashMap<Integer, List<Integer>>();
        for (var paramEntry : musicianOptions.entrySet()) {
            var id = paramEntry.getKey();
            var m = Musician.restoreFromStorage(paramEntry.getValue());
            var peerIds = paramEntry.getValue().getPeerIds();
            peerIdsMap.put(id, peerIds);
            map.put(id, m);
        }
        for (var peerIdEntry : peerIdsMap.entrySet()) {
            var m = map.get(peerIdEntry.getKey());
            var peerIds = peerIdEntry.getValue();
            if (peerIds != null) {
                for (var peerId : peerIds) {
                    var otherMusician = map.get(peerId);
                    m.addPeer(otherMusician);
                }
            }
        }
        this.musicians.putAll(map);
    }

    /**
     * @return a list of stored {@link Musician Musicians}
     */
    public Map<Integer, Musician> getMusicians() { return Collections.unmodifiableMap(this.musicians); }

    /**
     * @param musician {@link Musician} to add
     */
    public void addMusician(Musician musician) {
        musicians.put(musician.getId(), musician);
    }

    /**
     * @param musician {@link Musician} to remove
     */
    public void removeMusician(Musician musician) {
        musicians.remove(musician.getId());
    }

    /**
     * @param id the ID of the {@link Musician} to remove
     * @return the removed {@link Musician}, or null if no such musician existed
     */
    public Musician removeMusician(int id) {
        return musicians.remove(id);
    }

    /**
     * @return number of {@link Musician Musicians}
     */
    public int numMusicians() {
        return musicians.size();
    }
}
