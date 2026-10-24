package io.github.uppermoon0.vanishguard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Only synthetic UUID fixtures are used here, never player/account identifiers. */
class VanishConfigTest {
    private static final String FIRST_TEXT = "12345678-9abc-4def-8123-456789abcdef";
    private static final String SECOND_TEXT = "00000000-0000-0000-0000-000000000001";
    private static final UUID FIRST = UUID.fromString(FIRST_TEXT);
    private static final UUID SECOND = UUID.fromString(SECOND_TEXT);

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsExplicitEmptyConfiguration() {
        VanishConfig config = VanishConfig.parse("{\"hiddenUuids\":[]}");
        assertEquals(Set.of(), config.hiddenUuids());
        assertTrue(config.observerMode(), "Observer mode must default on when omitted");
    }

    @ParameterizedTest(name = "accepts observer flag: {0}")
    @ValueSource(booleans = {true, false})
    void acceptsExplicitObserverModeBeforeOrAfterRequiredKey(boolean enabled) {
        String hidden = "\"hiddenUuids\":[\"" + FIRST_TEXT + "\"]";
        VanishConfig first = VanishConfig.parse("{\"observerMode\":" + enabled + "," + hidden + "}");
        VanishConfig last = VanishConfig.parse("{" + hidden + ",\"observerMode\":" + enabled + "}");
        assertEquals(enabled, first.observerMode());
        assertEquals(Set.of(FIRST), first.hiddenUuids());
        assertEquals(first, last);
    }

    @Test
    void oneArgumentConstructorDefaultsObserverModeOn() {
        assertTrue(new VanishConfig(Set.of(FIRST)).observerMode());
    }

    @ParameterizedTest(name = "rejects non-boolean observer mode: {0}")
    @ValueSource(strings = {"null", "0", "1", "\"true\"", "\"false\"", "[]", "{}"})
    void rejectsNonBooleanObserverMode(String value) {
        assertThrows(RuntimeException.class,
            () -> VanishConfig.parse("{\"hiddenUuids\":[],\"observerMode\":" + value + "}"));
        assertThrows(RuntimeException.class,
            () -> VanishConfig.parse("{\"observerMode\":" + value + ",\"hiddenUuids\":[]}"));
    }

    @Test
    void observerModeDoesNotMakeHiddenUuidsOptional() {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse("{\"observerMode\":true}"));
        assertThrows(RuntimeException.class, () -> VanishConfig.parse("{\"observerMode\":false}"));
    }

    @ParameterizedTest(name = "rejects duplicate observer key: {0}")
    @ValueSource(strings = {
        "{\"hiddenUuids\":[],\"observerMode\":true,\"observerMode\":false}",
        "{\"hiddenUuids\":[],\"observerMode\":true,\"observerMode\":true}",
        "{\"observerMode\":false,\"hiddenUuids\":[],\"observerMode\":true}",
        "{\"observerMode\":true,\"observerMode\":false,\"hiddenUuids\":[]}"
    })
    void rejectsDuplicateObserverModeEvenIfValuesAgree(String input) {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(input));
    }

    @Test
    void rejectsEscapedDuplicateObserverModeKey() {
        String input = "{\"hiddenUuids\":[],\"observerMode\":true,\"observer\\u004dode\":false}";
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(input));
    }

    @Test
    void explicitObserverFlagIsPartOfImmutableStartupSnapshot() throws IOException {
        Path path = Files.writeString(temporaryDirectory.resolve("vanishguard.json"),
            "{\"hiddenUuids\":[],\"observerMode\":false}");
        VanishConfig startup = VanishConfig.load(path);
        Files.writeString(path, "{\"hiddenUuids\":[],\"observerMode\":true}");
        assertFalse(startup.observerMode());
        assertTrue(VanishConfig.load(path).observerMode());
        assertFalse(startup.observerMode());
    }

    @Test
    void acceptsCanonicalUuidCaseVariantsAndNormalizesToUuidIdentity() {
        VanishConfig lower = VanishConfig.parse(json(FIRST_TEXT));
        VanishConfig upper = VanishConfig.parse(json(FIRST_TEXT.toUpperCase(java.util.Locale.ROOT)));
        VanishConfig mixed = VanishConfig.parse(json("12345678-9AbC-4dEf-8123-456789aBcDeF"));
        assertEquals(Set.of(FIRST), lower.hiddenUuids());
        assertEquals(lower, upper);
        assertEquals(lower, mixed);
    }

    @Test
    void acceptsMultipleDistinctCanonicalUuids() {
        assertEquals(Set.of(FIRST, SECOND), VanishConfig.parse(json(FIRST_TEXT, SECOND_TEXT)).hiddenUuids());
    }

    @Test
    void acceptsOrdinaryJsonWhitespace() {
        assertEquals(Set.of(FIRST), VanishConfig.parse(" \n\t{ \"hiddenUuids\" : [ \n\"" + FIRST_TEXT + "\" ] }\r\n").hiddenUuids());
    }

    @ParameterizedTest(name = "rejects noncanonical UUID: {0}")
    @ValueSource(strings = {
        "", "not-a-uuid", "1-1-1-1-1", "0-0-0-0-1",
        "12345678-9abc-4def-8123-456789abcde",
        "1234567-9abc-4def-8123-456789abcdef",
        "123456789-9abc-4def-8123-456789abcdef",
        "123456789abc4def8123456789abcdef",
        "12345678_9abc_4def_8123_456789abcdef",
        "12345678-9abc-4def-8123-456789abcdeg",
        "{12345678-9abc-4def-8123-456789abcdef}",
        "urn:uuid:12345678-9abc-4def-8123-456789abcdef",
        " 12345678-9abc-4def-8123-456789abcdef",
        "12345678-9abc-4def-8123-456789abcdef ",
        "+2345678-9abc-4def-8123-456789abcdef"
    })
    void rejectsMalformedOrShortenedUuids(String value) {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(json(value)));
    }

    @ParameterizedTest(name = "rejects invalid schema: {0}")
    @MethodSource("invalidSchemaDocuments")
    void rejectsMissingUnknownAndWronglyTypedValues(String input) {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(input));
    }

    static Stream<String> invalidSchemaDocuments() {
        return Stream.of(
            "{}", "[]", "null", "true", "false", "42", "\"text\"",
            "{\"hiddenUUIDs\":[]}",
            "{\"hiddenUuids\":[],\"unknown\":true}",
            "{\"hiddenUuids\":[],\"reload\":false}",
            "{\"hiddenUuids\":null}",
            "{\"hiddenUuids\":\"" + FIRST_TEXT + "\"}",
            "{\"hiddenUuids\":{}}", "{\"hiddenUuids\":true}", "{\"hiddenUuids\":0}",
            "{\"hiddenUuids\":[null]}", "{\"hiddenUuids\":[false]}",
            "{\"hiddenUuids\":[1]}", "{\"hiddenUuids\":[1.5]}",
            "{\"hiddenUuids\":[[]]}", "{\"hiddenUuids\":[{}]}",
            "{\"hiddenUuids\":[\"" + FIRST_TEXT + "\",null]}"
        );
    }

    @ParameterizedTest(name = "rejects malformed JSON: {0}")
    @MethodSource("malformedJsonDocuments")
    void rejectsMalformedJsonRatherThanTreatingItAsEmpty(String input) {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(input));
    }

    static Stream<String> malformedJsonDocuments() {
        return Stream.of(
            "", " ", "{", "{\"hiddenUuids\":", "{\"hiddenUuids\":[}",
            "{\"hiddenUuids\":[]} trailing", "{\"hiddenUuids\":[]}{}",
            "{hiddenUuids:[]}", "{'hiddenUuids':[]}",
            "// comment\n{\"hiddenUuids\":[]}",
            "/* comment */{\"hiddenUuids\":[]}",
            "{\"hiddenUuids\":[],}"
        );
    }

    @Test
    void rejectsDuplicateUuidEntries() {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(json(FIRST_TEXT, FIRST_TEXT)));
    }

    @Test
    void rejectsDuplicateUuidEntriesRegardlessOfLetterCase() {
        assertThrows(RuntimeException.class,
            () -> VanishConfig.parse(json(FIRST_TEXT, FIRST_TEXT.toUpperCase(java.util.Locale.ROOT))));
    }

    @Test
    void rejectsDuplicateJsonKeysInsteadOfSilentlyUnhidingPlayers() {
        String input = "{\"hiddenUuids\":[\"" + FIRST_TEXT + "\"],\"hiddenUuids\":[]}";
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(input));
    }

    @Test
    void rejectsDuplicateJsonKeysEvenWithIdenticalValues() {
        assertThrows(RuntimeException.class, () -> VanishConfig.parse("{\"hiddenUuids\":[],\"hiddenUuids\":[]}"));
    }

    @Test
    void rejectsEscapedDuplicateJsonKeys() {
        String input = "{\"hiddenUuids\":[\"" + FIRST_TEXT + "\"],\"hidden\\u0055uids\":[]}";
        assertThrows(RuntimeException.class, () -> VanishConfig.parse(input));
    }

    @Test
    void createsEmptyDefaultOnlyWhenPathDoesNotExistIncludingParentDirectories() throws IOException {
        Path path = temporaryDirectory.resolve("config/nested/vanishguard.json");
        assertFalse(Files.exists(path));
        VanishConfig configuration = VanishConfig.load(path);
        assertEquals(Set.of(), configuration.hiddenUuids());
        assertTrue(configuration.observerMode());
        assertTrue(Files.isRegularFile(path));
        assertEquals(configuration, VanishConfig.parse(Files.readString(path)));
        assertEquals(Set.of(), VanishConfig.parse(Files.readString(path)).hiddenUuids());
    }

    @Test
    void reloadOfExistingDefaultDoesNotRewriteFile() throws IOException {
        Path path = temporaryDirectory.resolve("vanishguard.json");
        VanishConfig.load(path);
        byte[] before = Files.readAllBytes(path);
        FileTime marker = FileTime.fromMillis(1_234_567_890_000L);
        Files.setLastModifiedTime(path, marker);
        VanishConfig.load(path);
        assertArrayEquals(before, Files.readAllBytes(path));
        assertEquals(marker, Files.getLastModifiedTime(path));
    }

    @Test
    void loadsExistingConfigurationWithoutChangingBytesOrModificationTime() throws IOException {
        Path path = temporaryDirectory.resolve("vanishguard.json");
        String content = " \n{ \"hiddenUuids\": [ \"" + FIRST_TEXT.toUpperCase(java.util.Locale.ROOT) + "\" ] }\n";
        Files.writeString(path, content);
        FileTime marker = FileTime.fromMillis(1_234_567_890_000L);
        Files.setLastModifiedTime(path, marker);
        assertEquals(Set.of(FIRST), VanishConfig.load(path).hiddenUuids());
        assertEquals(content, Files.readString(path));
        assertEquals(marker, Files.getLastModifiedTime(path));
    }

    @ParameterizedTest(name = "existing invalid file is not replaced: {0}")
    @ValueSource(strings = {"", " ", "{}", "{\"hiddenUuids\":null}", "{\"hiddenUuids\":[\"1-1-1-1-1\"]}", "broken JSON"})
    void invalidExistingFileAbortsStartupAndIsNeverOverwritten(String content) throws IOException {
        Path path = temporaryDirectory.resolve("vanishguard.json");
        Files.writeString(path, content);
        FileTime marker = FileTime.fromMillis(1_234_567_890_000L);
        Files.setLastModifiedTime(path, marker);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> VanishConfig.load(path));
        assertTrue(failure.getMessage().contains(path.toString()));
        assertNotNull(failure.getCause());
        assertEquals(content, Files.readString(path));
        assertEquals(marker, Files.getLastModifiedTime(path));
    }

    @Test
    void malformedUtf8FileAbortsStartupWithoutOverwritingBytes() throws IOException {
        Path path = temporaryDirectory.resolve("vanishguard.json");
        byte[] malformedUtf8 = {(byte) 0xc3, (byte) 0x28};
        Files.write(path, malformedUtf8);
        assertThrows(IllegalStateException.class, () -> VanishConfig.load(path));
        assertArrayEquals(malformedUtf8, Files.readAllBytes(path));
    }

    @Test
    void directoryInPlaceOfConfigAbortsStartupAndPreservesItsContents() throws IOException {
        Path path = Files.createDirectory(temporaryDirectory.resolve("vanishguard.json"));
        Path sentinel = Files.writeString(path.resolve("sentinel.txt"), "do not overwrite");
        assertThrows(IllegalStateException.class, () -> VanishConfig.load(path));
        assertTrue(Files.isDirectory(path));
        assertEquals("do not overwrite", Files.readString(sentinel));
    }

    @Test
    void parentThatIsAFileAbortsStartupWithoutReplacingParent() throws IOException {
        Path parent = Files.writeString(temporaryDirectory.resolve("config"), "sentinel");
        assertThrows(IllegalStateException.class, () -> VanishConfig.load(parent.resolve("vanishguard.json")));
        assertEquals("sentinel", Files.readString(parent));
    }

    @Test
    void unreadableExistingFileAbortsStartupRatherThanFallingBackToEmpty() throws IOException {
        assumeTrue(Files.getFileStore(temporaryDirectory).supportsFileAttributeView("posix"));
        Path path = Files.writeString(temporaryDirectory.resolve("vanishguard.json"), json(FIRST_TEXT));
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(path);
        try {
            Files.setPosixFilePermissions(path, Set.of());
            assumeTrue(!Files.isReadable(path), "Privileged test process can read mode-000 files");
            assertThrows(IllegalStateException.class, () -> VanishConfig.load(path));
        } finally {
            Files.setPosixFilePermissions(path, originalPermissions);
        }
        assertEquals(json(FIRST_TEXT), Files.readString(path));
    }

    @Test
    void missingConfigInUnwritableDirectoryAbortsRatherThanReturningEmpty() throws IOException {
        assumeTrue(Files.getFileStore(temporaryDirectory).supportsFileAttributeView("posix"));
        Path parent = Files.createDirectory(temporaryDirectory.resolve("config"));
        Path path = parent.resolve("vanishguard.json");
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(parent);
        try {
            Files.setPosixFilePermissions(parent, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
            assumeTrue(!Files.isWritable(parent), "Privileged test process can write a read-only directory");
            assertThrows(IllegalStateException.class, () -> VanishConfig.load(path));
            assertFalse(Files.exists(path));
        } finally {
            Files.setPosixFilePermissions(parent, originalPermissions);
        }
    }

    @Test
    void constructorDefensivelyCopiesInputSet() {
        Set<UUID> mutableInput = new HashSet<>(Set.of(FIRST));
        VanishConfig configuration = new VanishConfig(mutableInput);
        mutableInput.clear();
        mutableInput.add(SECOND);
        assertEquals(Set.of(FIRST), configuration.hiddenUuids());
    }

    @Test
    void returnedSetCannotBeMutated() {
        Set<UUID> ids = VanishConfig.parse(json(FIRST_TEXT)).hiddenUuids();
        assertThrows(UnsupportedOperationException.class, () -> ids.add(SECOND));
        assertThrows(UnsupportedOperationException.class, () -> ids.remove(FIRST));
        assertThrows(UnsupportedOperationException.class, ids::clear);
        java.util.Iterator<UUID> iterator = ids.iterator();
        assertEquals(FIRST, iterator.next());
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals(Set.of(FIRST), ids);
    }

    @Test
    void emptyReturnedSetCannotBePopulated() {
        assertThrows(UnsupportedOperationException.class,
            () -> VanishConfig.parse("{\"hiddenUuids\":[]}").hiddenUuids().add(FIRST));
    }

    @Test
    void constructorRejectsNullCollectionOrEntries() {
        assertThrows(NullPointerException.class, () -> new VanishConfig(null));
        Set<UUID> ids = new HashSet<>();
        ids.add(null);
        assertThrows(NullPointerException.class, () -> new VanishConfig(ids));
    }

    @Test
    void loadedConfigurationIsStartupSnapshotAndDoesNotReloadWhenFileChanges() throws IOException {
        Path path = Files.writeString(temporaryDirectory.resolve("vanishguard.json"), json(FIRST_TEXT));
        VanishConfig startup = VanishConfig.load(path);
        Files.writeString(path, json(SECOND_TEXT));
        assertEquals(Set.of(FIRST), startup.hiddenUuids());
        VanishConfig explicitLaterLoad = VanishConfig.load(path);
        assertEquals(Set.of(SECOND), explicitLaterLoad.hiddenUuids());
        assertEquals(Set.of(FIRST), startup.hiddenUuids());
        Files.writeString(path, "malformed");
        assertEquals(Set.of(FIRST), startup.hiddenUuids());
        Files.delete(path);
        assertEquals(Set.of(FIRST), startup.hiddenUuids());
        assertFalse(Files.exists(path), "Accessing a loaded configuration must not recreate or reload its file");
    }

    private static String json(String... values) {
        return "{\"hiddenUuids\":[" + Stream.of(values).map(value -> "\"" + value + "\"")
            .collect(java.util.stream.Collectors.joining(",")) + "]}";
    }
}
