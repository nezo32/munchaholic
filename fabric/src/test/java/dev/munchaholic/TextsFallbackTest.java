package dev.munchaholic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.munchaholic.core.DisplayUnit;
import dev.munchaholic.core.RollMode;
import org.junit.jupiter.api.Test;

/**
 * The English fallbacks that are not {@code translatableWithFallback} literals (so LangFileTest's code scan can't see
 * them) still equal en_us, and Texts spells each unit with the same key and fallback as {@link DisplayUnit}.
 */
class TextsFallbackTest {
	private static final Path TEXTS = Path.of("src/main/java/dev/munchaholic/Texts.java");

	private static Map<String, String> english() throws IOException {
		try (InputStream in = TextsFallbackTest.class.getResourceAsStream("/assets/munchaholic/lang/en_us.json")) {
			assertNotNull(in, "en_us.json not on the test classpath");
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
			}
		}
	}

	@Test
	void englishRollModeMatchesEnUs() throws IOException {
		Map<String, String> lang = english();
		for (RollMode mode : RollMode.values()) {
			assertEquals(lang.get(mode.translationKey()), Texts.englishRollMode(mode), mode.id());
		}
	}

	@Test
	void textsUsesTheDisplayUnitKeyAndFallback() throws IOException {
		String source = Files.readString(TEXTS, StandardCharsets.UTF_8);
		for (DisplayUnit unit : DisplayUnit.values()) {
			String call = "translatableWithFallback(\"" + unit.translationKey() + "\", \"" + unit.fallback() + "\", number)";
			assertTrue(source.contains(call), "Texts.amount lacks " + call);
		}
	}
}
