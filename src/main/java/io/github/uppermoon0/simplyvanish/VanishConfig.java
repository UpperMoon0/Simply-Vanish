package io.github.uppermoon0.simplyvanish;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Startup-only, administrator-owned data. Invalid input aborts startup, never silently unhides. */
public record VanishConfig(Set<UUID> hiddenUuids, boolean observerMode) {
    public VanishConfig(Set<UUID> hiddenUuids) { this(hiddenUuids, true); }
    public VanishConfig { hiddenUuids = Set.copyOf(hiddenUuids); }
    public static VanishConfig parse(String input) {
        Set<UUID> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        boolean observerMode = true;
        try (com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(input))) {
            reader.setLenient(false);
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (!keys.add(key)) throw new IllegalArgumentException("Duplicate config key");
                if (key.equals("observerMode")) {
                    if (reader.peek() != com.google.gson.stream.JsonToken.BOOLEAN) throw new IllegalArgumentException("observerMode must be a boolean");
                    observerMode = reader.nextBoolean();
                } else if (key.equals("hiddenUuids")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        if (reader.peek() != com.google.gson.stream.JsonToken.STRING)
                            throw new IllegalArgumentException("Each hiddenUuids entry must be a UUID string");
                        String text = reader.nextString();
                        UUID uuid = UUID.fromString(text);
                        if (!uuid.toString().equalsIgnoreCase(text)) throw new IllegalArgumentException("UUID must use canonical 8-4-4-4-12 form");
                        if (!ids.add(uuid)) throw new IllegalArgumentException("Duplicate UUID");
                    }
                    reader.endArray();
                } else throw new IllegalArgumentException("Unknown config key");
            }
            reader.endObject();
            if (!keys.contains("hiddenUuids")) throw new IllegalArgumentException("Missing hiddenUuids key");
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing JSON data");
        } catch (IOException | IllegalStateException e) {
            throw new IllegalArgumentException("Malformed vanish config", e);
        }
        return new VanishConfig(ids, observerMode);
    }
    public static VanishConfig load(Path path) {
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                try { Files.writeString(path, "{\n  \"hiddenUuids\": [],\n  \"observerMode\": true\n}\n", StandardOpenOption.CREATE_NEW); }
                catch (FileAlreadyExistsException raced) { /* Another writer won; read, never overwrite. */ }
            }
            return parse(Files.readString(path));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot safely load Simply Vanish config: " + path + ". Fix the file before restarting.", e);
        }
    }
}
