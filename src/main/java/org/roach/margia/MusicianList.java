package org.roach.margia;

import java.util.*;

import org.roach.margia.timing.Storable;

/**
 * A list of musicians that is able to restore itself from a YAML file
 */
public class MusicianList implements Storable {
    private final List<Musician> musicians = new ArrayList<>();
    private final Map<String, Object> storableProperties = new HashMap<>();
    static final String MUSICIANS_PROPERTY = "musicians";
    private static MusicianList instance;
    
    private MusicianList() {
        Options.getInstance().put("musicians", storableProperties);
    }
    
    /**
     * @return the singleton instance of {@link MusicianList}
     */
    public static MusicianList getInstance() {
        if (instance == null)
            instance = new MusicianList();
        return instance;
    }
    
    @Override
    public Map<String, Object> storableProperties() {
        return this.storableProperties;
    }

    @SuppressWarnings("hiding")
    @Override
    public void restoreFromStorage(Map<String, Object> storableProperties) {
        if (storableProperties == null)
            return;
        this.storableProperties.clear();
        this.musicians.clear();
        this.storableProperties.putAll(storableProperties);
        
        var map = new HashMap<Integer, Musician>();
        var peerIdsMap = new HashMap<Integer, List<Integer>>();
        for (var musicianEntry : storableProperties.entrySet()) {
            var id = Integer.parseInt(musicianEntry.getKey());
            @SuppressWarnings("unchecked")
            var params = (Map<String, Object>) musicianEntry.getValue();
            @SuppressWarnings("unchecked")
            var peerIds = (List<Integer>) params.get(Musician.PEER_IDS_PROPERTY);
            var m = new Musician();
            m.restoreFromStorage(params);

            peerIdsMap.put(id, peerIds);
            map.put(id, m);
        }
        for (var peerIdEntry : peerIdsMap.entrySet()) {
            var m = map.get(peerIdEntry.getKey());
            var peerIds = peerIdEntry.getValue();
            for (var peerId : peerIds) {
                var otherMusician = map.get(peerId);
                m.addPeer(otherMusician);
            }
        }
        this.musicians.addAll(map.values());
    }

    public List<Musician> getMusicians() {
        return Collections.unmodifiableList(this.musicians);
    }
    
    public void addMusician(Musician musician) {
        musicians.add(musician);
    }
    
    public void removeMusician(Musician musician) {
        musicians.remove(musician);
    }
    
    public int numMusicians() {
        return musicians.size();
    }
}
