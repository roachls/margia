package org.roach.margia.storage.params;

import java.util.List;

/**
 * description of a storable String parameter with a list of possible values
 * 
 * @param paramName      storage name of parameter
 * @param displayName    display name of parameter
 * @param possibleValues possible values of parameter
 */
public record StringListParamDescription(String paramName, String displayName, List<String> possibleValues)
        implements SettableParamDescription {
    // no implementation
}