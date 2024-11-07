package xyz.lilyflower.catj.util.bytecode;

import java.lang.reflect.Field;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InstructionParser {

    public static AbstractInsnNode parse(String bytecode) {
        String insn = bytecode.replaceAll(" .*", "");
        String arguments = bytecode.replaceAll(insn + " ", "");
        try {
            Field field = Opcodes.class.getField(insn.toUpperCase());
            Object value = field.get(null);
            if (value instanceof Integer) {
                int opcode = (int) value;
                switch (opcode) {
                    case Opcodes.LDC: {
                        return new LdcInsnNode(arguments);
                    }

                    case Opcodes.GETSTATIC:
                    case Opcodes.PUTSTATIC:
                    case Opcodes.GETFIELD:
                    case Opcodes.PUTFIELD: {
                        String[] args = arguments.split(" ");
                        String owner = args[0].replaceAll("\\..*", "");
                        String name = args[0].replaceAll(".*\\.", "");

                        return new FieldInsnNode(opcode, owner, name, args[1]);
                    }

                    case Opcodes.INVOKESPECIAL:
                    case Opcodes.INVOKESTATIC:
                    case Opcodes.INVOKEVIRTUAL:
                    case Opcodes.INVOKEINTERFACE: {
                        String[] method = parseMethodSignature(arguments);
                        return new MethodInsnNode(opcode, method[0], method[1], method[2]);
                    }

                    case Opcodes.ALOAD:
                    case Opcodes.ILOAD:
                    case Opcodes.LLOAD:
                    case Opcodes.DLOAD:
                    case Opcodes.FLOAD:
                    case Opcodes.ASTORE:
                    case Opcodes.ISTORE:
                    case Opcodes.LSTORE:
                    case Opcodes.DSTORE:
                    case Opcodes.FSTORE:
                    case Opcodes.RET: {
                        return new VarInsnNode(opcode, Integer.parseInt(arguments));
                    }

                    case Opcodes.NEWARRAY: {
                        return new IntInsnNode(opcode, parsePrimitiveType(arguments));
                    }

                    case Opcodes.BIPUSH:
                    case Opcodes.SIPUSH: {
                        return new IntInsnNode(opcode, Integer.parseInt(arguments));
                    }

                    case Opcodes.ANEWARRAY:
                    case Opcodes.INSTANCEOF:
                    case Opcodes.CHECKCAST:
                    case Opcodes.NEW: {
                        return new TypeInsnNode(opcode, arguments);
                    }

                    default: {
                        return new InsnNode(opcode);
                    }
                }
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String[] parseMethodSignature(String signature) {
        String[] parsed = new String[3];

        String owner = signature.replaceAll("\\..*", "");
        String description = signature.replaceAll(".*\\(", "(");
        String method = signature
                .replace(owner + ".", "")
                .replace(description, "");

        parsed[0] = owner;
        parsed[1] = method;
        parsed[2] = description;

        return parsed;
    }

    private static int parsePrimitiveType(String type) {
        return switch (type) {
            case "I" -> Opcodes.T_INT;
            case "B" -> Opcodes.T_BYTE;
            case "C" -> Opcodes.T_CHAR;
            case "F" -> Opcodes.T_FLOAT;
            case "D" -> Opcodes.T_DOUBLE;
            case "J" -> Opcodes.T_LONG;
            case "S" -> Opcodes.T_SHORT;
            case "Z" -> Opcodes.T_BOOLEAN;
            default -> 0;
        };

    }
}
