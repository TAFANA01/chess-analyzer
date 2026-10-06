package com.chessanalyzer.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = buildMapper();

    private JsonUtil() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) throws IOException {
        return MAPPER.writeValueAsString(value);
    }

    public static void writeToFile(Object value, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, value);
    }

    public static <T> T readFromFile(File file, Class<T> type) throws IOException {
        return MAPPER.readValue(file, type);
    }
}
