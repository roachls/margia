package org.roach.margia.storage.params;

/**
 * @param propertyName storage name of property
 * @param displayName  display name of property
 * @param type         {@link Class} of property
 * @param minValue     minimum value (only applicable if numeric type)
 * @param maxValue     maximum value (only applicable if numeric type)
 * @param step         step for UI spinners (only applicable if numeric type)
 */
public record NumericParamDescription(String propertyName, String displayName, Class<? extends Number> type,
        Double minValue, Double maxValue, Double step) implements SettableParamDescription {
    // no implementation
}