package xyz.lilyflower.catj.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class RuntimeClassGenerator {
    public static Class<?> generate(String bytecode) {
        Stream<String> lines = bytecode.lines();
        InsnList instructions = new InsnList();

        AtomicReference<String> className = new AtomicReference<>(UUID.randomUUID().toString());
        AtomicReference<String> methodDesc = new AtomicReference<>("()V");
        AtomicBoolean entrypoint = new AtomicBoolean(false);

        AtomicReference<String> superclass = new AtomicReference<>("java/lang/Object");
        ArrayList<String> interfaces = new ArrayList<>();

        lines.forEach(line -> {
            if (line.equals("ENTRYPOINT")) {
                entrypoint.set(true);
            } else if (line.startsWith("SUPER ")) {
                superclass.set(line.replaceAll("SUPER ", ""));
            } else if (line.startsWith("IFACE ")) {
                interfaces.add(line.replaceAll("IFACE ", ""));
            } else if (line.startsWith("NAME ")) {
                className.set(line.replaceAll("NAME ", ""));
            } else if (line.startsWith("DESC ")) {
                methodDesc.set(line.replaceAll("DESC ", ""));
            } else if (!line.isBlank()) {
                instructions.add(InstructionParser.parse(line));
            }
        });

        if (entrypoint.get() && superclass.get().equals("java/lang/Object")) {
            InsnList temp = new InsnList();
            temp.add(InstructionParser.parse("ALOAD 0"));
            temp.add(InstructionParser.parse("INVOKESPECIAL java/lang/Object.<init>()V"));
            temp.add(instructions);
            instructions.clear();
            instructions.add(temp);
        }

        ClassNode node = new ClassNode(Opcodes.ASM8);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        node.name = className.get();
        node.superName = superclass.get();
        node.interfaces.addAll(interfaces);

        MethodNode method = new MethodNode();
        method.instructions = instructions;
        method.name = entrypoint.get() ? "<init>" : "run";
        method.desc = entrypoint.get() ? "()V" : methodDesc.get();
        method.exceptions = new ArrayList<>();
        method.access = entrypoint.get() ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

        node.methods.add(method);
        node.access = Opcodes.ACC_PUBLIC;
        node.version = Opcodes.V21;
        try {
            node.accept(writer);
        } catch (Exception exception) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.of("Invalid bytecode! Reason: " + exception.getMessage()));
            }
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try (FileOutputStream out = new FileOutputStream(File.createTempFile("generated_", ".class"))) {
                out.write(writer.toByteArray());
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return CriminalClassloader.INSTANCE.load(node.name, writer.toByteArray());
    }
}