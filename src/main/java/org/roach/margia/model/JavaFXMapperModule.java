package org.roach.margia.model;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

/**
 * Glue code to get YAML mapper to read/write {@link Point2D}
 */
public class JavaFXMapperModule extends SimpleModule {
    /**
     * constructor
     */
    public JavaFXMapperModule() {
        // Add Serializer for Point2D
        addSerializer(Point3D.class, new JsonSerializer<>() {
            @Override
            public void serialize(Point3D point, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeStartObject();
                gen.writeNumberField("x", point.getX());
                gen.writeNumberField("y", point.getY());
                gen.writeNumberField("z", point.getZ());
                gen.writeEndObject();
            }
        });

        // Add Deserializer for Point3D
        addDeserializer(Point3D.class, new JsonDeserializer<>() {
            @Override
            public Point3D deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonNode node = p.getCodec().readTree(p);
                double x = node.has("x") ? node.get("x").asDouble() : 0.0;
                double y = node.has("y") ? node.get("y").asDouble() : 0.0;
                double z = node.has("z") ? node.get("z").asDouble() : 0.0;
                return new Point3D(x, y, z);
            }
        });

        // add serializer for Color
        addSerializer(Color.class, new JsonSerializer<>() {

            @Override
            public void serialize(Color value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    // Outputs hex with alpha, e.g., "0xff0000ff"
                    gen.writeString(value.toString());
                }
            }

        });

        // deserializer for Color
        addDeserializer(Color.class, new JsonDeserializer<>() {

            @Override
            public Color deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                String text = p.getText();
                if (text == null || text.isBlank()) {
                    return null;
                }
                return Color.web(text);
            }

        });
    }
}
