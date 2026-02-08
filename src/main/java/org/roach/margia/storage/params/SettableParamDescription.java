package org.roach.margia.storage.params;

/**
 * A generic description of a storable, settable parameter
 */
public sealed interface SettableParamDescription
        permits IntegerParamDescription, BooleanParamDescription, StringListParamDescription, EnumParamDescription {
    // no implementation, marker interface only
}