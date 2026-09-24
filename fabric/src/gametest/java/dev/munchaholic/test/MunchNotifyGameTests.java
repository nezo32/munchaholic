package dev.munchaholic.test;

import static dev.munchaholic.test.TestSupport.defaults;
import static dev.munchaholic.test.TestSupport.eat;
import static dev.munchaholic.test.TestSupport.key;
import static dev.munchaholic.test.TestSupport.mockPlayer;

import java.util.List;

import dev.munchaholic.Feedback;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.RollOutcome;
import dev.munchaholic.net.RecipesPayload;
import dev.munchaholic.net.RolledPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Roll feedback (D14): vanilla clients get an overlay message and a sound packet from the server, clients with the mod
 * get only {@code munchaholic:rolled} and apply their own settings. Message keys, argument order and colors.
 */
public class MunchNotifyGameTests {
	/** Gravity DOWN is a buff (D5): 0 → -1 step, now 90%. */
	private static final RollOutcome.Applied GRAVITY_DOWN =
			new RollOutcome.Applied(Caps.GRAVITY, Direction.DOWN, 0, -1, Caps.GRAVITY.playerBase());
	/** Scale DOWN is a debuff: 0 → -1 step, now 92%. */
	private static final RollOutcome.Applied SCALE_DOWN =
			new RollOutcome.Applied(Caps.SCALE, Direction.DOWN, 0, -1, Caps.SCALE.playerBase());
	private static final RollOutcome.Capped SCALE_CAPPED =
			new RollOutcome.Capped(Caps.SCALE, Direction.DOWN, -10, Caps.SCALE.playerBase());

	/** A fresh bread stack (not a static field: item components are bound only after registries load). */
	private static ItemStack bread() {
		return new ItemStack(Items.BREAD);
	}

	private static TranslatableContents tc(Component c) {
		if (!(c.getContents() instanceof TranslatableContents t)) throw new AssertionError("not translatable: " + c);
		return t;
	}

	private static Component arg(Component c, int i) {
		Object a = tc(c).getArgs()[i];
		if (!(a instanceof Component ac)) throw new AssertionError("arg " + i + " is not a component: " + a);
		return ac;
	}

	private static TextColor color(ChatFormatting f) {
		return TextColor.fromLegacyFormat(f);
	}

	@GameTest
	public void vanillaClientGetsOverlayAndSound(GameTestHelper h) {
		for (RollOutcome o : List.<RollOutcome>of(GRAVITY_DOWN, SCALE_DOWN, SCALE_CAPPED)) {
			TestSupport.Mock mock = mockPlayer(h);
			h.assertTrue(!ServerPlayNetworking.canSend(mock.player(), RolledPayload.TYPE), "mock player has no munchaholic channel");
			Feedback.send(mock.player(), bread(), o);
			List<Object> out = mock.drain();
			Component expected = Feedback.message(bread(), o);
			List<ClientboundSystemChatPacket> overlays = TestSupport.Mock.overlays(out);
			h.assertValueEqual(overlays.size(), 1, "overlay packets for " + o + "; outbound=" + out);
			h.assertValueEqual(overlays.get(0).content(), expected, "overlay content");
			List<ClientboundSoundEntityPacket> sounds = out.stream().filter(m -> m instanceof ClientboundSoundEntityPacket)
					.map(m -> (ClientboundSoundEntityPacket) m).toList();
			h.assertValueEqual(sounds.size(), 1, "sound packets; outbound=" + out);
			ClientboundSoundEntityPacket sound = sounds.get(0);
			float pitch = o == GRAVITY_DOWN ? Feedback.PITCH_BUFF : o == SCALE_DOWN ? Feedback.PITCH_DEBUFF : Feedback.PITCH_NEUTRAL;
			h.assertTrue(Math.abs(sound.getPitch() - pitch) < 1e-4, "pitch " + sound.getPitch() + " for " + o);
			h.assertTrue(Math.abs(sound.getVolume() - Feedback.VOLUME) < 1e-4, "volume " + sound.getVolume());
			h.assertValueEqual(sound.getSound().value(), Feedback.SOUND, "sound event");
			h.assertValueEqual(TestSupport.Mock.payloads(out, RolledPayload.class).size(), 0, "rolled payloads");
		}
		h.succeed();
	}

	@GameTest
	public void moddedClientGetsPayloadOnly(GameTestHelper h) {
		TestSupport.Mock mock = mockPlayer(h);
		Feedback.send(mock.player(), bread(), SCALE_DOWN, true);
		List<Object> out = mock.drain();
		List<RolledPayload> payloads = TestSupport.Mock.payloads(out, RolledPayload.class);
		h.assertValueEqual(payloads.size(), 1, "rolled payloads; outbound=" + out);
		h.assertValueEqual(payloads.get(0).message(), Feedback.message(bread(), SCALE_DOWN), "payload message");
		h.assertValueEqual(payloads.get(0).tone(), Feedback.TONE_DEBUFF, "payload tone");
		h.assertTrue(TestSupport.Mock.overlays(out).isEmpty(), "no overlay packet; outbound=" + out);
		h.assertTrue(out.stream().noneMatch(m -> m instanceof ClientboundSoundEntityPacket), "no sound packet; outbound=" + out);
		h.succeed();
	}

	/** Real eating by a vanilla client: exactly one overlay (the roll) and one chime. */
	@GameTest
	public void eatingNotifiesOnce(GameTestHelper h) {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		eat(h, mock.player(), Items.BREAD);
		List<Object> out = mock.drain();
		List<ClientboundSystemChatPacket> overlays = TestSupport.Mock.overlays(out);
		h.assertValueEqual(overlays.size(), 1, "overlays; outbound=" + out);
		h.assertValueEqual(key(overlays.get(0).content()), "munchaholic.message.rolled", "rolled key");
		long chimes = out.stream().filter(m -> m instanceof ClientboundSoundEntityPacket s && s.getSound().value() == Feedback.SOUND).count();
		h.assertValueEqual(chimes, 1L, "chime packets");
		h.assertTrue(TestSupport.Mock.payloads(out, RecipesPayload.class).isEmpty(), "no recipes payload in Random mode");
		h.succeed();
	}

	@GameTest
	public void rolledPayloadCodecRoundTrip(GameTestHelper h) {
		Component msg = Feedback.message(bread(), GRAVITY_DOWN);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
		try {
			RolledPayload.CODEC.encode(buf, new RolledPayload(msg, Feedback.TONE_BUFF));
			RolledPayload decoded = RolledPayload.CODEC.decode(buf);
			h.assertValueEqual(decoded.message(), msg, "decoded message");
			h.assertValueEqual(decoded.tone(), Feedback.TONE_BUFF, "decoded tone");
			TranslatableContents t = tc(decoded.message());
			h.assertValueEqual(t.getKey(), "munchaholic.message.rolled", "key");
			h.assertValueEqual(t.getFallback(), "✦ %1$s → %2$s %3$s %4$s", "fallback");
			h.assertValueEqual(buf.readableBytes(), 0, "bytes left unread");
		} finally {
			buf.release();
		}
		h.assertValueEqual(RolledPayload.TYPE.id().toString(), "munchaholic:rolled", "channel id");
		h.succeed();
	}

	/** Food GOLD, attribute name WHITE, change GREEN for a buff / RED for a debuff, "now" GRAY, outer LIGHT_PURPLE. */
	@GameTest
	public void messageArgumentsAndColors(GameTestHelper h) {
		Component buff = Feedback.message(bread(), GRAVITY_DOWN);
		h.assertValueEqual(tc(buff).getArgs().length, 4, "4 args");
		h.assertValueEqual(buff.getStyle().getColor(), color(ChatFormatting.LIGHT_PURPLE), "outer color");
		h.assertValueEqual(arg(buff, 0).getStyle().getColor(), color(ChatFormatting.GOLD), "food color");
		h.assertValueEqual(arg(buff, 0).getString(), bread().getHoverName().getString(), "food name");
		h.assertValueEqual(tc(arg(buff, 1)).getKey(), "attribute.name.gravity", "attribute key (vanilla)");
		h.assertValueEqual(arg(buff, 2).getStyle().getColor(), color(ChatFormatting.GREEN), "gravity DOWN is a buff");
		h.assertValueEqual(arg(buff, 2).getString(), "-10%", "gravity change text");
		h.assertValueEqual(arg(buff, 3).getStyle().getColor(), color(ChatFormatting.GRAY), "now color");
		h.assertValueEqual(tc(arg(buff, 3)).getKey(), "munchaholic.message.now", "now key");
		h.assertValueEqual(arg(buff, 3).getString(), "(now 90%)", "now text (English fallback)");

		Component debuff = Feedback.message(bread(), SCALE_DOWN);
		h.assertValueEqual(arg(debuff, 2).getStyle().getColor(), color(ChatFormatting.RED), "scale DOWN is a debuff");
		h.assertValueEqual(arg(debuff, 2).getString(), "-8%", "scale change text");
		h.assertValueEqual(arg(debuff, 3).getString(), "(now 92%)", "scale now text");
		h.assertValueEqual(debuff.getString(), "✦ " + bread().getHoverName().getString() + " → Scale -8% (now 92%)",
				"whole message (server has only the fallbacks and vanilla attribute names)");

		h.assertValueEqual(Feedback.tone(GRAVITY_DOWN), Feedback.TONE_BUFF, "tone buff");
		h.assertValueEqual(Feedback.tone(SCALE_DOWN), Feedback.TONE_DEBUFF, "tone debuff");
		h.assertTrue(Feedback.pitch(Feedback.TONE_BUFF) == Feedback.PITCH_BUFF, "pitch buff");
		h.assertTrue(Feedback.pitch(Feedback.TONE_DEBUFF) == Feedback.PITCH_DEBUFF, "pitch debuff");
		h.assertTrue(Feedback.pitch(Feedback.TONE_NEUTRAL) == Feedback.PITCH_NEUTRAL, "pitch neutral");
		h.assertTrue(Feedback.pitch(42) == Feedback.PITCH_NEUTRAL, "unknown tone -> neutral pitch");
		h.succeed();
	}

	@GameTest
	public void cappedAndNothingMessages(GameTestHelper h) {
		Component capped = Feedback.message(bread(), SCALE_CAPPED);
		h.assertValueEqual(tc(capped).getKey(), "munchaholic.message.capped", "capped key");
		h.assertValueEqual(tc(capped).getFallback(), "✦ %1$s → %2$s %3$s (at the limit: %4$s)", "capped fallback");
		h.assertValueEqual(arg(capped, 2).getStyle().getColor(), color(ChatFormatting.GRAY), "capped change is gray");
		h.assertValueEqual(arg(capped, 3).getString(), "20%", "capped limit text");
		h.assertValueEqual(Feedback.tone(SCALE_CAPPED), Feedback.TONE_NEUTRAL, "capped tone");

		Component nothing = Feedback.message(bread(), RollOutcome.Nothing.INSTANCE);
		h.assertValueEqual(tc(nothing).getKey(), "munchaholic.message.nothing", "nothing key");
		h.assertValueEqual(tc(nothing).getFallback(), "✦ %s → nothing left to change", "nothing fallback");
		h.assertValueEqual(Feedback.tone(RollOutcome.Nothing.INSTANCE), Feedback.TONE_NEUTRAL, "nothing tone");
		h.succeed();
	}

	/** Feedback.SOUND is a registered sound event (the vanilla-client path wraps it as a holder). */
	@GameTest
	public void soundIsRegistered(GameTestHelper h) {
		h.assertTrue(BuiltInRegistries.SOUND_EVENT.getKey(Feedback.SOUND) != null, "Feedback.SOUND registered");
		h.succeed();
	}
}
