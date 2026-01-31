package org.roach.margia.storage.params;

/**
 * description of a boolean parameter
 * 
 * @param paramName   storage name of the parameter
 * @param displayName display name of the parameter
 */
public record BooleanParamDescription(String paramName, String displayName) implements SettableParamDescription {
    // no implementation
}