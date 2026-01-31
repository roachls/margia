package org.roach.margia.storage.params;

/**
 * description of an enum parameter
 * 
 * @param paramName storage name of parameter
 * @param displayName  display name of parameter
 * @param defaultValue default value of parameter
 */
public record EnumParamDescription(String paramName, String displayName, Enum<?> defaultValue)
        implements SettableParamDescription {
    // no implementation
}