package tanks.tankson;

import java.nio.ByteBuffer;

public class ObjectBuffer
{
    public enum Type {
        // Primatives
        NULL((byte) 0x00),
        FALSE((byte) 0x01),
        TRUE((byte) 0x02),
        INF((byte) 0xFF),

        // Integers
        CHAR((byte) 0xA1),
        SHORT((byte) 0xA2),
        INT((byte) 0xA3),
        LONG((byte) 0xA4),

        // Floats
        FP8((byte) 0xB0),
        FP16((byte) 0xB1),
        FLOAT((byte) 0xB2),
        DOUBLE((byte) 0xB3),

        //Variable Length
        STRING((byte) 0xC0),
        LIST((byte) 0xC1),

        //Objects
        OBJECT((byte) 0x0B);

        private final byte code;

        Type(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return this.code;
        }

        public static Type fromByte(byte code) {
            for (Type command : Type.values()) {
                if (command.code == code) {
                    return command;
                }
            }
            throw new IllegalArgumentException("Unknown command code: " + code);
        }
    }

    public static Type getType(long tag) {
        return Type.fromByte((byte) (tag & 0xFF));
    }

    public static long getId(long tag) {
        return tag >> 8;
    }

    public static long getTag(Type type, long id) {
        return (id << 8) | type.getCode();
    }

    public static byte[] toBytes(char c, long id) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(getTag(Type.CHAR, id));
        buffer.putChar(c);
        return buffer.array();
    }

    public static byte[] toBytes(short s, long id) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(getTag(Type.SHORT, id));
        buffer.putShort(s);
        return buffer.array();
    }

    public static byte[] toBytes(int i, long id) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(getTag(Type.INT, id));
        buffer.putInt(i);
        return buffer.array();
    }

    public static byte[] toBytes(long l, long id) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(getTag(Type.LONG, id));
        buffer.putLong(l);
        return buffer.array();
    }
}
