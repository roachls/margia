package org.roach.margia.timing;

import java.util.Map;

public interface Storable {
    Map<String, Object> storableProperties();
    
    void restoreFromStorage(Map<String, Object> storableProperties);
}
