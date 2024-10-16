package tanks.tankson;

import basewindow.IModel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static jdk.internal.org.jline.utils.Colors.s;
import static tanks.tankson.TankBytes.Delim.NULL;

public class TankBytes {
    protected enum Delim {
        NULL,
        MAP,
        ARRAY,
        STRING,
        DOUBLE,
        TRUE,
        FALSE
    }

    protected static class ParserState {
        protected int i;
        protected List<Byte> bs;

        protected ParserState(List<Byte> bs) {
            this.bs = bs;
            this.i = 0;
        }

        protected Delim get_symb() {
            return Delim.values()[bs.get(i)];
        }

        protected Object parse() {
            switch (get_symb()) {
                case NULL:
                    return read_null();
                case TRUE:
                    return read_bool();
                case FALSE:
                    return read_bool();
                case DOUBLE:
                    return read_double();
                case STRING:
                    return read_string();
                case ARRAY:
                    return read_list();
                case MAP:
                    return read_map();
                default:
                    throw new RuntimeException("Unknown Byte found" + get_symb());
            }
        }

        protected Object read_null() {
            if (get_symb() == NULL) {
                return null;
            } else {
                throw new RuntimeException("Cannot read null, non-null byte found!");
            }
        }

        protected boolean read_bool() {
            if (get_symb() == Delim.TRUE) {
                return true;
            } else if (get_symb() == Delim.FALSE) {
                return false;
            } else {
                throw new RuntimeException("Cannot read bool, Byte is not True or False!");
            }
        }

        protected double read_double() {
            if (get_symb() == Delim.DOUBLE) {
                Byte[] bs2 = bs.subList(i+1,i+9).toArray(new Byte[0]);
                byte[] b = new byte[bs2.length];
                for (int j = 0; j < b.length; j++)
                    b[j] = bs2[j];
                i += 9;
                return ByteBuffer.wrap(b).getDouble();
            } else {
                throw new RuntimeException("Cannot read double when no double found!");
            }
        }

        protected int read_size() {
            Byte[] bs2 = bs.subList(i,i+4).toArray(new Byte[0]);
            byte[] b = new byte[bs2.length];
            for (int j = 0; j < b.length; j++)
                b[j] = bs2[j];
            i += 4;
            return ByteBuffer.wrap(b).getInt();
        }

        protected String read_string() {
            if (get_symb() == Delim.STRING) {
                i += 1;
                int size = read_size();
                List<Byte> string = bs.subList(i, i+size);
                byte[] s = new byte[string.size()];
                for (int i = 0; i < string.size(); i++)
                    s[i] = string.get(i);
                i += size;
                return new String(s);
            } else {
                throw new RuntimeException("Cannot read string when no string found!");
            }
        }

        protected ArrayList read_list() {
            if (get_symb() == Delim.ARRAY) {
                i += 1;
                int size = read_size();
                ArrayList<Object> a = new ArrayList<>();
                for (int j = 0; j < size; j++) {
                    a.add(parse());
                }
                return a;
            } else {
                throw new RuntimeException("Cannot read array when no array found!");
            }
        }

        protected Map read_map() {
            if (get_symb() == Delim.MAP) {
                Map m = new HashMap<String, Object>();
                i += 1;
                int size = read_size();
                for (int j = 0; j < size; j ++) {
                    m.put(parse(),parse());
                }
                return m;
            } else {
                throw new RuntimeException("Cannot read map when no map found!");
            }
        }

    }

    public static Byte[] bake (Object o) {
        return bakeList(o).toArray(new Byte[0]);
    }

    protected static List<Byte> byteInt (int l) {
        byte[] number = ByteBuffer.allocate(4).putInt(l).array();
        List<Byte> n = new ArrayList<>();
        for (byte b : number)
            n.add(b);
        return n;
    }

    public static List<Byte> bakeList (Object o) {
        if (o instanceof String || o instanceof Enum || o instanceof IModel) {
            byte[] string = o.toString().getBytes(StandardCharsets.UTF_8);
            List<Byte> s = new ArrayList<>();
            s.add((byte) Delim.STRING.ordinal());
            s.addAll(byteInt(o.toString().length()));
            for (byte b : string)
                s.add(b);
            return s;
        } else if (o instanceof Number) {
            byte[] number = ByteBuffer.allocate(8).putDouble(((Number) o).doubleValue()).array();
            List<Byte> n = new ArrayList<>();
            n.add((byte) Delim.DOUBLE.ordinal());
            for (byte b : number)
                n.add(b);
            return n;
        } else if (o instanceof Boolean) {
            List<Byte> b = new ArrayList<>();
            if ((Boolean) o) {
                b.add((byte) Delim.TRUE.ordinal());
            } else {
                b.add((byte) Delim.FALSE.ordinal());
            }
            return b;
        } else if (o == null) {
            List<Byte> n = new ArrayList<>();
            n.add((byte) NULL.ordinal());
            return n;
        } else if (o instanceof AbstractCollection) {
            List<Byte> c = new ArrayList<>();
            c.add((byte) Delim.ARRAY.ordinal());
            c.addAll(byteInt(((AbstractCollection) o).size()));
            for (Object el : (AbstractCollection<?>) o) {
                c.addAll(bakeList(el));
            }
            c.add((byte) Delim.ARRAY.ordinal());
            return c;
        } else if (o instanceof Map) {
            List<Byte> m = new ArrayList<>();
            m.add((byte) Delim.MAP.ordinal());
            Map<?, ?> h = ((Map<?, ?>) o);
            ArrayList<String> keys = new ArrayList<String>((Collection<? extends String>) h.keySet());
            m.addAll(byteInt(keys.size()));
            if (keys.remove("name"))
                keys.add(0, "name");

            if (keys.remove("obj_type"))
                keys.add(0, "obj_type");

            for (String el: keys)
            {
                m.addAll(bakeList(el));
                m.addAll(bakeList(h.get(el)));
            }
            return m;
        } else {
            throw new RuntimeException("Failed to turn object to bytes: " + o);
        }
    }

    public static Object eat (Byte[] b) {
        return new ParserState(Arrays.asList(b)).parse();
    }
}
