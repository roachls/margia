package org.roach.margia.mains.params;

import org.roach.margia.Key;
import org.roach.margia.mains.MargiaParams;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

/**
 * common parameters for algorithms
 */
@SuppressWarnings("javadoc")
public abstract class AbstractAlgParams implements MargiaParams {

    @Parameter(names = { "-k", "--key" }, description = "Key of instruments", converter = KeyConverter.class)
    public Key key = Key.Chromatic;

    protected static class KeyConverter implements IStringConverter<Key> {

        @Override
        @SuppressWarnings("java:S106")
        public Key convert(String value) {
            try {
                return (Key) Key.class.getDeclaredField(value).get(null);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException _) {
                System.err.println("Error: Key " + value + " doesn't exist");
                return null;
            }
        }

    }

}
