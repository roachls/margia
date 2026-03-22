/**
 * The Margia module
 */
module margia {
    exports org.roach.margia.main;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires java.desktop;
    requires java.measure;
    requires java.prefs;
    requires jcommander;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.pushingpixels.radiance.theming;
    requires org.slf4j;
    requires tech.units.indriya;
    requires tech.uom.lib.common;
}