package dev.munchaholic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.DisplayUnit;
import dev.munchaholic.core.RollMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Keeps en_us and ru_ru in step with each other and with the code. Server text is sent with
 * {@code translatableWithFallback}, so a vanilla client shows the fallback: it has to be the en_us text, byte for byte.
 */
class LangFileTest {
	/** Every key of ARCHITECTURE §8 except the vanilla ({@code V}) ones. */
	static final String[] REQUIRED_KEYS = {
			"modmenu.summaryTranslation.munchaholic",
			"modmenu.descriptionTranslation.munchaholic",
			"munchaholic.createWorld.toggle",
			"munchaholic.createWorld.toggle.tooltip",
			"munchaholic.createWorld.rollMode",
			"munchaholic.createWorld.rollMode.tooltip.random",
			"munchaholic.createWorld.rollMode.tooltip.recipes",
			"munchaholic.rollMode.random",
			"munchaholic.rollMode.recipes",
			"munchaholic.command.on",
			"munchaholic.command.off",
			"munchaholic.command.status.on",
			"munchaholic.command.status.off",
			"munchaholic.command.mode.set",
			"munchaholic.command.mode.status",
			"munchaholic.command.keepOnDeath.on",
			"munchaholic.command.keepOnDeath.off",
			"munchaholic.command.keepOnDeath.status.on",
			"munchaholic.command.keepOnDeath.status.off",
			"munchaholic.command.stats.header",
			"munchaholic.command.stats.line",
			"munchaholic.command.stats.none",
			"munchaholic.command.reset.single",
			"munchaholic.command.reset.multiple",
			"munchaholic.command.reset.recipes.single",
			"munchaholic.command.reset.recipes.multiple",
			"munchaholic.command.notify.sound",
			"munchaholic.command.notify.message",
			"munchaholic.message.rolled",
			"munchaholic.message.now",
			"munchaholic.message.capped",
			"munchaholic.message.nothing",
			"munchaholic.message.reset",
			"munchaholic.message.recipesReset",
			"munchaholic.message.lostOnDeath",
			"munchaholic.unit.percent",
			"munchaholic.unit.points",
			"munchaholic.tooltip.recipe",
			"munchaholic.tooltip.undiscovered",
			"munchaholic.direction.up",
			"munchaholic.direction.down",
			"munchaholic.settings.title",
			"munchaholic.settings.notifySound",
			"munchaholic.settings.notifySound.tooltip",
			"munchaholic.settings.notifyMessage",
			"munchaholic.settings.notifyMessage.tooltip"};

	/** Source roots scanned for keys and fallbacks (Gradle runs tests in the project directory). */
	static final Path[] SOURCE_ROOTS = {Path.of("src/main/java"), Path.of("src/client/java")};

	/** A {@code "munchaholic.…"} literal that names a file ({@code munchaholic.json}), not a translation key. */
	static final Pattern FILE_NAME = Pattern.compile(".*\\.(?:json|json5|png|ogg|nbt|mcmeta|properties|toml|txt|dat)$");

	private static Map<String, String> lang;
	private static Map<String, String> ru;
	private static List<Literal> literals;
	private static List<FallbackCall> fallbackCalls;

	@BeforeAll
	static void load() throws IOException {
		lang = read("en_us");
		ru = read("ru_ru");
		literals = new ArrayList<>();
		fallbackCalls = new ArrayList<>();
		for (Path root : SOURCE_ROOTS) {
			assertTrue(Files.isDirectory(root), "source root not found: " + root.toAbsolutePath());
			try (Stream<Path> files = Files.walk(root)) {
				for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
					scan(file.toString().replace('\\', '/'), Files.readString(file, StandardCharsets.UTF_8));
				}
			}
		}
	}

	// ---------------------------------------------------------------- lang file parsing

	/** Reads a lang file strictly: a flat object of non-null strings, no duplicate keys, nothing after it. */
	private static Map<String, String> read(String code) throws IOException {
		String name = code + ".json";
		try (InputStream in = LangFileTest.class.getResourceAsStream("/assets/munchaholic/lang/" + name)) {
			assertNotNull(in, name + " not on the test classpath");
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8); JsonReader json = new JsonReader(reader)) {
				return readFlat(name, json);
			}
		}
	}

	static Map<String, String> readFlat(String name, JsonReader json) throws IOException {
		Map<String, String> out = new LinkedHashMap<>();
		json.beginObject();
		while (json.hasNext()) {
			String key = json.nextName();
			assertEquals(JsonToken.STRING, json.peek(), name + ": value of " + key + " is not a string");
			String value = json.nextString();
			assertFalse(out.containsKey(key), name + ": duplicate key " + key);
			out.put(key, value);
		}
		json.endObject();
		assertEquals(JsonToken.END_DOCUMENT, json.peek(), name + ": content after the top-level object");
		return out;
	}

	@Test
	void duplicateKeysAreDetected() throws IOException {
		try (JsonReader json = new JsonReader(new java.io.StringReader("{\"a\": \"1\", \"a\": \"2\"}"))) {
			AssertionError error = null;
			try {
				readFlat("test.json", json);
			} catch (AssertionError e) {
				error = e;
			}
			assertNotNull(error, "a duplicate key must fail");
		}
	}

	// ---------------------------------------------------------------- placeholders

	/** Format placeholders (%s, %1$s, %d, …) of a lang value, sorted; %% is a literal percent sign. */
	static List<String> placeholders(String value) {
		Matcher m = Pattern.compile("%(?:(\\d+)\\$)?([a-zA-Z%])").matcher(value);
		List<String> out = new ArrayList<>();
		int next = 1;
		while (m.find()) {
			if (m.group(2).equals("%")) continue;
			// unnumbered %s count as positional, so "%s %s" and "%2$s %1$s" compare equal
			out.add((m.group(1) != null ? m.group(1) : String.valueOf(next++)) + "$" + m.group(2));
		}
		out.sort(null);
		return out;
	}

	/** Number of literal {@code %%}. */
	static int literalPercents(String value) {
		Matcher m = Pattern.compile("%(?:\\d+\\$)?[sd]|%%").matcher(value);
		int count = 0;
		while (m.find()) {
			if (m.group().equals("%%")) count++;
		}
		return count;
	}

	/** A {@code %} that isn't {@code %s}, {@code %d}, {@code %n$s}, {@code %n$d} or {@code %%}; null if there is none. */
	static String strayPercent(String value) {
		String rest = value.replaceAll("%(?:\\d+\\$)?[sd]|%%", "");
		int i = rest.indexOf('%');
		return i < 0 ? null : rest.substring(i, Math.min(rest.length(), i + 4));
	}

	@Test
	void placeholderHelperSanity() {
		assertEquals(List.of("1$s", "2$s"), placeholders("✦ %1$s → %2$s"));
		assertEquals(List.of("1$s", "2$s"), placeholders("%2$s a %1$s"));
		assertEquals(List.of("1$s", "2$s"), placeholders("%s a %s"));
		assertEquals(List.of("1$s"), placeholders("Sound: %s (100%%)"));
		assertEquals(List.of("1$d"), placeholders("%d"));
		assertEquals(List.of(), placeholders("none"));
		assertEquals(1, literalPercents("%s%%"));
		assertEquals(0, literalPercents("%s"));
		assertEquals(null, strayPercent("%1$s %s %d %% %2$d"));
		assertEquals("% of", strayPercent("50% off"));
		assertEquals("%x", strayPercent("%x"));
	}

	@Test
	void russianHasExactlyTheEnglishKeys() {
		assertEquals(new TreeSet<>(lang.keySet()), new TreeSet<>(ru.keySet()), "ru_ru.json key set differs from en_us.json");
	}

	@Test
	void russianPlaceholdersMatchEnglish() {
		for (String key : lang.keySet()) {
			if (!ru.containsKey(key)) continue; // reported by russianHasExactlyTheEnglishKeys
			assertEquals(placeholders(lang.get(key)), placeholders(ru.get(key)), "placeholders of " + key);
			assertEquals(literalPercents(lang.get(key)), literalPercents(ru.get(key)), "number of %% in " + key);
		}
	}

	@Test
	void noStrayPercentSigns() {
		for (Map.Entry<String, Map<String, String>> file : files().entrySet()) {
			for (Map.Entry<String, String> e : file.getValue().entrySet()) {
				assertEquals(null, strayPercent(e.getValue()), file.getKey() + ": stray % in " + e.getKey());
			}
		}
	}

	@Test
	void noEmptyValues() {
		for (Map.Entry<String, Map<String, String>> file : files().entrySet()) {
			for (Map.Entry<String, String> e : file.getValue().entrySet()) {
				assertFalse(e.getValue().isBlank(), file.getKey() + ": blank " + e.getKey());
			}
		}
	}

	// ---------------------------------------------------------------- the contract (§8)

	@Test
	void requiredKeysPresent() {
		for (String key : REQUIRED_KEYS) {
			assertTrue(lang.containsKey(key), "en_us missing " + key);
		}
		assertEquals(new TreeSet<>(List.of(REQUIRED_KEYS)), new TreeSet<>(lang.keySet()), "en_us has keys outside §8");
	}

	@Test
	void noVanillaOrColorCodes() {
		for (Map.Entry<String, Map<String, String>> file : files().entrySet()) {
			for (Map.Entry<String, String> e : file.getValue().entrySet()) {
				assertFalse(e.getKey().startsWith("attribute.name."), file.getKey() + ": vanilla key " + e.getKey());
				assertFalse(e.getKey().startsWith("gamerule."), file.getKey() + ": game rule key " + e.getKey());
				assertFalse(e.getValue().contains("§"), file.getKey() + ": color code in " + e.getKey() + " (colors go in code)");
			}
		}
		assertFalse(lang.get("munchaholic.createWorld.toggle.tooltip").contains("/gamerule"), "tooltip mentions /gamerule");
	}

	@Test
	void russianSpecifics() {
		assertEquals("Режим Munchaholic", ru.get("munchaholic.createWorld.toggle"));
		assertTrue(ru.get("munchaholic.createWorld.toggle.tooltip").contains("/munchaholic on|off"), "ru toggle tooltip names the command");
		for (RollMode mode : RollMode.values()) {
			String key = "munchaholic.createWorld.rollMode.tooltip." + mode.id();
			assertTrue(ru.get(key).contains("/munchaholic mode"), "ru " + key + " names the command");
			assertTrue(lang.get(key).contains("/munchaholic mode random|recipes"), key + " names the command");
		}
		assertTrue(lang.get("munchaholic.createWorld.toggle.tooltip").contains("/munchaholic on|off"), "toggle tooltip names the command");
		// Mode names and UI labels must really be translated (the mod name alone is fine to keep)
		for (String key : new String[] {"munchaholic.rollMode.random", "munchaholic.rollMode.recipes", "munchaholic.createWorld.rollMode",
				"munchaholic.settings.notifySound", "munchaholic.settings.notifyMessage", "munchaholic.message.now"}) {
			assertFalse(lang.get(key).equals(ru.get(key)), "untranslated " + key);
		}
	}

	@Test
	void messageShapes() {
		for (Map.Entry<String, Map<String, String>> file : files().entrySet()) {
			Map<String, String> values = file.getValue();
			for (String key : new String[] {"munchaholic.message.rolled", "munchaholic.message.capped"}) {
				String v = values.get(key);
				for (int i = 1; i <= 4; i++) {
					assertTrue(v.contains("%" + i + "$s"), file.getKey() + ": " + key + " lacks %" + i + "$s: " + v);
				}
				assertTrue(v.startsWith("✦ %1$s → "), file.getKey() + ": " + key + " keeps ✦ and →");
			}
			assertTrue(values.get("munchaholic.message.nothing").startsWith("✦ %s → "), file.getKey() + ": message.nothing keeps ✦ and →");
			assertTrue(values.get("munchaholic.unit.percent").contains("%s%%"), file.getKey() + ": unit.percent keeps %s%%");
			assertEquals("▲", values.get("munchaholic.direction.up"), file.getKey());
			assertEquals("▼", values.get("munchaholic.direction.down"), file.getKey());
		}
	}

	/** Keys built at run time from a prefix documented in §8 ({@code rollMode.<id>}, tooltips per mode, arrows, units). */
	@Test
	void dynamicKeysPresent() {
		for (RollMode mode : RollMode.values()) {
			assertTrue(lang.containsKey(mode.translationKey()), "missing " + mode.translationKey());
			assertTrue(lang.containsKey("munchaholic.createWorld.rollMode.tooltip." + mode.id()), "missing roll mode tooltip for " + mode.id());
		}
		for (Direction direction : Direction.values()) {
			String key = "munchaholic.direction." + direction.name().toLowerCase(Locale.ROOT);
			assertTrue(lang.containsKey(key), "missing " + key);
		}
		for (String name : new String[] {"sound", "message"}) {
			assertTrue(lang.containsKey("munchaholic.command.notify." + name), "missing notify key " + name);
		}
		// DisplayUnit carries its own translatableWithFallback pair
		for (DisplayUnit unit : DisplayUnit.values()) {
			assertEquals(lang.get(unit.translationKey()), unit.fallback(), "fallback of " + unit + " (" + unit.translationKey() + ")");
		}
	}

	// ---------------------------------------------------------------- the code (§9.1 checks 7 and 8)

	/** Every {@code translatableWithFallback("key", "fallback", …)} uses an en_us key, and the fallback is its en_us text. */
	@Test
	void codeFallbacksMatchEnglish() {
		for (FallbackCall call : fallbackCalls) {
			assertTrue(lang.containsKey(call.key), call.where + ": key not in en_us: " + call.key);
			if (call.fallback != null) {
				assertEquals(lang.get(call.key), call.fallback, call.where + ": fallback of " + call.key + " differs from en_us");
			}
		}
	}

	/**
	 * Every {@code "munchaholic.…"} literal in code is an en_us key. A literal ending in {@code .} is a prefix that
	 * the code completes at run time: at least one key must start with it. File names ({@code munchaholic.json}) are
	 * not keys.
	 */
	@Test
	void codeKeysExist() {
		for (Literal literal : literals) {
			String s = literal.value;
			if (!s.startsWith("munchaholic.") || FILE_NAME.matcher(s).matches()) continue;
			if (!s.matches("[A-Za-z0-9_.]+")) continue; // not key-shaped (a sentence, a path, a format)
			if (s.endsWith(".")) {
				assertTrue(lang.keySet().stream().anyMatch(k -> k.startsWith(s) && k.length() > s.length()),
						literal.where + ": no en_us key starts with " + s);
			} else {
				assertTrue(lang.containsKey(s), literal.where + ": key not in en_us: " + s);
			}
		}
	}

	@Test
	void sourceScannerSanity() {
		List<Literal> lits = new ArrayList<>();
		List<FallbackCall> calls = new ArrayList<>();
		String src = """
				// "munchaholic.comment.line"
				/* "munchaholic.comment.block" */
				class X {
					char q = '"';
					String url = "https://x/y"; // "munchaholic.after"
					Object a = Component.translatableWithFallback("munchaholic.a", "A \\"q\\" \\u2726 %s", x);
					Object b = Component.translatableWithFallback(
							"munchaholic.b",
							"B1" + "B2", y);
					Object c = Component.translatableWithFallback("munchaholic.c", FALLBACK);
					Object d = Component.translatableWithFallback(KEY, "D");
					Object e = Component.translatableWithFallback("munchaholic.e", "E" + name);
				}
				""";
		scanInto("X.java", src, lits, calls);
		List<String> values = lits.stream().map(l -> l.value).toList();
		assertFalse(values.contains("munchaholic.comment.line"), "comments are skipped");
		assertFalse(values.contains("munchaholic.comment.block"), "comments are skipped");
		assertFalse(values.contains("munchaholic.after"), "comments after a URL literal are skipped");
		assertTrue(values.contains("https://x/y"));
		assertEquals(4, calls.size(), calls.toString());
		assertEquals("munchaholic.a", calls.get(0).key);
		assertEquals("A \"q\" ✦ %s", calls.get(0).fallback);
		assertEquals("munchaholic.b", calls.get(1).key);
		assertEquals("B1B2", calls.get(1).fallback);
		assertEquals("munchaholic.c", calls.get(2).key);
		assertEquals(null, calls.get(2).fallback);
		assertEquals("munchaholic.e", calls.get(3).key);
		assertEquals(null, calls.get(3).fallback, "a fallback built at run time can't be checked");
	}

	// ---------------------------------------------------------------- a tiny Java lexer

	record Literal(String value, String where) {}

	record FallbackCall(String key, String fallback, String where) {}

	/** Token kinds: 's' string literal (value unescaped), 'i' identifier or number, 'p' punctuation. */
	record Token(char kind, String text, int line) {}

	private static void scan(String file, String source) {
		scanInto(file, source, literals, fallbackCalls);
	}

	static void scanInto(String file, String source, List<Literal> lits, List<FallbackCall> calls) {
		List<Token> tokens = tokenize(file, source);
		for (Token t : tokens) {
			if (t.kind == 's') lits.add(new Literal(t.text, file + ":" + t.line));
		}
		for (int i = 0; i + 2 < tokens.size(); i++) {
			if (tokens.get(i).kind != 'i' || !tokens.get(i).text.equals("translatableWithFallback")) continue;
			if (!isPunct(tokens.get(i + 1), "(") || tokens.get(i + 2).kind != 's') continue;
			Token key = tokens.get(i + 2);
			String where = file + ":" + key.line;
			int j = i + 3;
			if (j >= tokens.size() || !isPunct(tokens.get(j), ",")) {
				calls.add(new FallbackCall(key.text, null, where));
				continue;
			}
			// the fallback: a chain of literals joined by '+', ending at ',' or ')'
			StringBuilder fallback = new StringBuilder();
			boolean constant = true;
			j++;
			while (true) {
				if (j >= tokens.size() || tokens.get(j).kind != 's') {
					constant = false;
					break;
				}
				fallback.append(tokens.get(j).text);
				j++;
				if (j < tokens.size() && isPunct(tokens.get(j), "+")) {
					j++;
					continue;
				}
				if (j >= tokens.size() || !(isPunct(tokens.get(j), ",") || isPunct(tokens.get(j), ")"))) constant = false;
				break;
			}
			calls.add(new FallbackCall(key.text, constant ? fallback.toString() : null, where));
		}
	}

	private static boolean isPunct(Token t, String p) {
		return t.kind == 'p' && t.text.equals(p);
	}

	static List<Token> tokenize(String file, String s) {
		List<Token> out = new ArrayList<>();
		int i = 0;
		int line = 1;
		int n = s.length();
		while (i < n) {
			char c = s.charAt(i);
			if (c == '\n') {
				line++;
				i++;
			} else if (Character.isWhitespace(c)) {
				i++;
			} else if (s.startsWith("//", i)) {
				while (i < n && s.charAt(i) != '\n') i++;
			} else if (s.startsWith("/*", i)) {
				int end = s.indexOf("*/", i + 2);
				if (end < 0) fail(file + ":" + line + ": unterminated comment");
				line += count(s, i, end, '\n');
				i = end + 2;
			} else if (s.startsWith("\"\"\"", i)) {
				int end = i + 3;
				while (true) {
					end = s.indexOf("\"\"\"", end);
					if (end < 0) fail(file + ":" + line + ": unterminated text block");
					if (s.charAt(end - 1) != '\\') break;
					end++;
				}
				int start = line;
				line += count(s, i, end, '\n');
				out.add(new Token('s', unescape(file, start, s.substring(s.indexOf('\n', i) + 1, end).stripIndent()), start));
				i = end + 3;
			} else if (c == '"' || c == '\'') {
				int j = i + 1;
				StringBuilder raw = new StringBuilder();
				while (j < n && s.charAt(j) != c) {
					if (s.charAt(j) == '\n') fail(file + ":" + line + ": unterminated literal");
					if (s.charAt(j) == '\\' && j + 1 < n) raw.append(s.charAt(j++));
					raw.append(s.charAt(j++));
				}
				if (j >= n) fail(file + ":" + line + ": unterminated literal");
				if (c == '"') out.add(new Token('s', unescape(file, line, raw.toString()), line));
				else out.add(new Token('p', "'", line));
				i = j + 1;
			} else if (Character.isJavaIdentifierStart(c) || Character.isDigit(c)) {
				int j = i;
				while (j < n && Character.isJavaIdentifierPart(s.charAt(j))) j++;
				out.add(new Token('i', s.substring(i, j), line));
				i = j;
			} else {
				out.add(new Token('p', String.valueOf(c), line));
				i++;
			}
		}
		return out;
	}

	private static int count(String s, int from, int to, char c) {
		int k = 0;
		for (int i = from; i < to; i++) {
			if (s.charAt(i) == c) k++;
		}
		return k;
	}

	/** Java escapes: {@code \" \' \\ \n \t \r \b \f \s \}uXXXX (any number of u's) and octal. */
	static String unescape(String file, int line, String raw) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c != '\\') {
				sb.append(c);
				continue;
			}
			if (++i >= raw.length()) fail(file + ":" + line + ": dangling backslash");
			char e = raw.charAt(i);
			switch (e) {
				case 'n' -> sb.append('\n');
				case 't' -> sb.append('\t');
				case 'r' -> sb.append('\r');
				case 'b' -> sb.append('\b');
				case 'f' -> sb.append('\f');
				case 's' -> sb.append(' ');
				case '\n' -> { } // text block line continuation
				case 'u' -> {
					while (i < raw.length() && raw.charAt(i) == 'u') i++;
					if (i + 4 > raw.length()) fail(file + ":" + line + ": bad \\u escape");
					sb.append((char) Integer.parseInt(raw.substring(i, i + 4), 16));
					i += 3;
				}
				default -> {
					if (e >= '0' && e <= '7') {
						int j = i;
						while (j < raw.length() && j < i + 3 && raw.charAt(j) >= '0' && raw.charAt(j) <= '7') j++;
						sb.append((char) Integer.parseInt(raw.substring(i, j), 8));
						i = j - 1;
					} else {
						sb.append(e); // \" \' \\
					}
				}
			}
		}
		return sb.toString();
	}

	private static Map<String, Map<String, String>> files() {
		Map<String, Map<String, String>> out = new LinkedHashMap<>();
		out.put("en_us", lang);
		out.put("ru_ru", ru);
		return out;
	}
}
