package xyz.lilyflower.catj;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.lilyflower.catj.util.BookParser;
import xyz.lilyflower.catj.util.RuntimeClassGenerator;

public class CrimesAgainstTheJVM implements ClientModInitializer {
	private static final KeyBinding EXECUTE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.catj.execute",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"category.catj.catj"
	));

	@SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
	public void onInitializeClient() {
		File configDir = new File("config/catj/");
		if (!configDir.exists()) {
			configDir.mkdir();
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				while (EXECUTE.wasPressed()) {
					ItemStack stack = client.player.getStackInHand(Hand.OFF_HAND);
					if (stack.getItem() instanceof WrittenBookItem) {
						WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

						if ((content.author() != null && Objects.equals(content.author(), client.player.getName().getString())) || FabricLoader.getInstance().isDevelopmentEnvironment()) {
							String list = BookParser.parse(stack);
							ArrayList<File> files = new ArrayList<>();

							list.lines().forEach(line -> {
								files.add(new File("config/catj/" + line));
							});

							for (File file : files) {
                                try {
                                    List<String> lines = Files.readAllLines(file.toPath());
									StringBuilder parsed = new StringBuilder();
									for (String line : lines) {
										parsed.append(line).append("\n");
									}

									try {
										Class<?> clazz = RuntimeClassGenerator.generate(parsed.toString());

										if (clazz != null) {
											try {
												Constructor<?> constructor = clazz.getConstructor();
												constructor.newInstance();
											} catch (NoSuchMethodException exception) {
												if (exception.getMessage().contains(".<init>")) {
													client.player.sendMessage(Text.literal("Loaded class " + clazz.getSimpleName().formatted(Formatting.GREEN)), true);
												}
											} catch (VerifyError error) {
												client.player.sendMessage(Text.literal("Invalid bytecode! Reason:").formatted(Formatting.RED));
												client.player.sendMessage(Text.literal(error.getMessage()));
											} catch (Throwable throwable) {
												client.player.sendMessage(Text.literal("Execution failed! Reason: " + throwable.getClass().getCanonicalName() + ": " + throwable.getMessage()).formatted(Formatting.RED));
												throwable.printStackTrace();
											}
										}
									} catch (Throwable throwable) {
										client.player.sendMessage(Text.literal("Compilation failed! Reason: " + throwable.getClass().getCanonicalName() + ": " + throwable.getMessage()).formatted(Formatting.RED));
										throwable.printStackTrace();
									}

                                } catch (IOException e) {
									client.player.sendMessage(Text.literal("Could not read file " + file.getName() + " (does it exist?)."));
                                }
                            }


						} else {
							client.player.sendMessage(Text.literal("⚠").formatted(Formatting.DARK_RED).append(Text.literal(" WARNING ").formatted(Formatting.BOLD, Formatting.DARK_RED)).append(Text.literal("⚠").formatted(Formatting.DARK_RED)));
							client.player.sendMessage(Text.literal("Your player entity did not write this book!").formatted(Formatting.RED));
							client.player.sendMessage(Text.literal("Refusing to execute for security reasons.").formatted(Formatting.RED));
							client.player.sendMessage(Text.literal("⚠").formatted(Formatting.DARK_RED).append(Text.literal(" WARNING ").formatted(Formatting.BOLD, Formatting.DARK_RED)).append(Text.literal("⚠").formatted(Formatting.DARK_RED)));
						}
					}
				}
			}
		});
	}
}