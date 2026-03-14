package org.roach.margia.util;

import java.util.*;
import java.util.stream.Collectors;

import org.roach.margia.storage.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with files
 */
public class MargiaFileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MargiaFileUtils.class);

    private MargiaFileUtils() {
        // static methods only
    }

    /**
     * @return an unmodifiable list of recent files (could be empty)
     */
    public static List<String> getRecentsFromPersistence() {
        var recentFilesString = Persistence.getInstance().getString("recent_files", "");
        if (recentFilesString.isBlank())
            return Collections.emptyList();
        var recentFilesArray = recentFilesString.split(";");
        var list = Arrays.asList(recentFilesArray);
        LOGGER.atDebug().setMessage("Retrieved recents: {}").addArgument(() -> list).log();
        return list;
    }

    /**
     * @param recentFile the full path of the most recent file to be saved or opened
     */
    public static void saveRecentsToPersistence(String recentFile) {
        var list = new ArrayList<>(getRecentsFromPersistence());
        if (!list.contains(recentFile))
            list.add(recentFile);
        var str = list.stream().collect(Collectors.joining(";"));
        LOGGER.atDebug().setMessage("Saving recents: {}").addArgument(() -> list).log();
        Persistence.getInstance().saveProperty("recent_files", str);
    }
}
