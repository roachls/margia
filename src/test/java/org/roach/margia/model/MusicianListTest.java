package org.roach.margia.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.roach.margia.controller.Musician;

class MusicianListTest {
    
    @BeforeEach
    void setup() {
        MusicianList.getInstance().addMusician(Musician.newInstance());
    }
    
    @AfterEach
    void tearDown() {
        MusicianList.getInstance().clear();
    }

    @Test
    void testGetInstance() {
        var inst1 = MusicianList.getInstance();
        assertSame(inst1, MusicianList.getInstance());
    }

    @Test
    void testRestoreFromStorage() {
        MusicianList.getInstance().clear();
        
        Map<Integer, MusicianOptions> options = new HashMap<>();
        var mo1 = new MusicianOptions();
        mo1.setChannel(3);
        mo1.setDeviceName("teletubbies");
        mo1.setId(1);
        mo1.setKeyName(Key.BbMajor.getName());
        mo1.setListening(false);
        mo1.setMuted(true);
        mo1.setRange(new NoteRange(Octave.O0));
        mo1.getPeerIds().add(2);
        options.put(1, mo1);

        var mo2 = new MusicianOptions();
        mo2.setChannel(4);
        mo2.setDeviceName("teletubbies");
        mo2.setId(2);
        mo2.setKeyName(Key.BbMajor.getName());
        mo2.setListening(true);
        mo2.setMuted(false);
        mo2.setRange(new NoteRange(Octave.O1));
        options.put(2, mo2);
        
        MusicianList.getInstance().restoreFromStorage(options);
        assertEquals(2, MusicianList.getInstance().numMusicians());
        var musicians = MusicianList.getInstance().getMusicians();
        assertEquals(2, musicians.size());
        assertTrue(musicians.containsKey(1));
        var mus1 = musicians.get(1);
        assertEquals(3, mus1.getChannel());
        assertEquals(mo1, mus1.getOptions());
        assertTrue(musicians.containsKey(2));
        var mus2 = musicians.get(2);
        assertEquals(4, mus2.getChannel());
        assertEquals(mo2, mus2.getOptions());
        assertTrue(mo1.getPeerIds().contains(2));
    }

    @Test
    void testGetMusicians() {
        assertEquals(1, MusicianList.getInstance().getMusicians().size());
    }

    @Test
    void testAddMusician() {
        MusicianList.getInstance().addMusician(Musician.newInstance());
        assertEquals(2, MusicianList.getInstance().getMusicians().size());
        assertEquals(2, MusicianList.getInstance().numMusicians());
    }

    @Test
    void testRemoveMusicianMusician() {
        var m = MusicianList.getInstance().getMusicians().values().iterator().next();
        MusicianList.getInstance().removeMusician(m);
        assertTrue(MusicianList.getInstance().getMusicians().isEmpty());
        assertEquals(0, MusicianList.getInstance().numMusicians());
    }

    @Test
    void testRemoveMusicianInt() {
        var id = MusicianList.getInstance().getMusicians().keySet().iterator().next();
        MusicianList.getInstance().removeMusician(id);
        assertTrue(MusicianList.getInstance().getMusicians().isEmpty());
        assertEquals(0, MusicianList.getInstance().numMusicians());
    }

    @Test
    void testNumMusicians() {
        assertEquals(1, MusicianList.getInstance().numMusicians());
    }

}
