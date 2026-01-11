package org.roach.margia;

import java.util.*;

/**
 * A list of musicians that is able to restore itself from a YAML file
 */
@SuppressWarnings("java:S6548")
public class MusicianList implements Storable {
    private final List<Musician> musicians = new ArrayList<>();
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

    @Override
    public List<Map<String, Object>> storableProperties() {
        return this.musicians.stream().map(Musician::storableProperties).toList();
    }

    @Override
    public void restoreFromStorage(Object storableProperties) {
        if (storableProperties == null)
            return;
        this.musicians.clear();
        @SuppressWarnings("unchecked")
        var propertyList = (List<Map<String, Object>>) storableProperties;

        var map = new HashMap<Integer, Musician>();
        var peerIdsMap = new HashMap<Integer, List<Integer>>();
        for (var params : propertyList) {
            var id = (int) params.get(Musician.ID_PROPERTY);
            var m = new Musician();
            m.restoreFromStorage(params);
            if (params.containsKey(Musician.PEER_IDS_PROPERTY)) {
                @SuppressWarnings("unchecked")
                var peerIds = (List<Integer>) params.get(Musician.PEER_IDS_PROPERTY);
                peerIdsMap.put(id, peerIds);
            }
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
        this.musicians.addAll(map.values());
    }

    /**
     * @return a list of stored {@link Musician Musicians}
     */
    public List<Musician> getMusicians() { return Collections.unmodifiableList(this.musicians); }

    /**
     * @param musician {@link Musician} to add
     */
    public void addMusician(Musician musician) {
        musicians.add(musician);
    }

    /**
     * @param musician {@link Musician} to remove
     */
    public void removeMusician(Musician musician) {
        musicians.remove(musician);
    }

    /**
     * @return number of {@link Musician Musicians}
     */
    public int numMusicians() {
        return musicians.size();
    }
}
