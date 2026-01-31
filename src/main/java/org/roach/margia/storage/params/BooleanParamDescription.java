package org.roach.margia.storage.params;

/**
 * description of a boolean parameter
 * 
 * @param paramName    storage name of the parameter
 * @param displayName  display name of the parameter
 * @param defaultValue default value if not set in file
 */
public record BooleanParamDescription(String paramName, String displayName, boolean defaultValue)
        implements SettableParamDescription {
    // no implementation
}