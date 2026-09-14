package edu.up.isgc.cg.raytracer.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON reader/writer — just enough to save/load a scene as a
 * plain, human-readable text file (see {@link SceneIO}) without pulling in a JSON
 * library. Values are plain Java objects: {@code Map<String, Object>} for a JSON object,
 * {@code List<Object>} for an array, and {@code String}/{@code Double}/{@code Boolean}/
 * {@code null} for the leaves.
 *
 * @author Claude (Anthropic)
 */
public final class Json {
    private Json() {}

    // ---- Writing ----

    /**
     * Write string.
     *
     * @param value a {@code Map}, {@code List}, {@code String}, {@code Number},
     *              {@code Boolean} or {@code null}
     * @return the pretty-printed JSON text
     */
    public static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(value, builder, 0);
        return builder.toString();
    }

    private static void writeValue(Object value, StringBuilder out, int indent) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String string) {
            writeString(string, out);
        } else if (value instanceof Number number) {
            out.append(number);
        } else if (value instanceof Boolean bool) {
            out.append(bool);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(map, out, indent);
        } else if (value instanceof List<?> list) {
            writeArray(list, out, indent);
        } else {
            writeString(value.toString(), out);
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out, int indent) {
        if (map.isEmpty()) { out.append("{}"); return; }
        out.append("{\n");
        int remaining = map.size();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            indent(out, indent + 1);
            writeString(String.valueOf(entry.getKey()), out);
            out.append(": ");
            writeValue(entry.getValue(), out, indent + 1);
            if (--remaining > 0) out.append(",");
            out.append("\n");
        }
        indent(out, indent);
        out.append("}");
    }

    private static void writeArray(List<?> list, StringBuilder out, int indent) {
        if (list.isEmpty()) { out.append("[]"); return; }
        out.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(out, indent + 1);
            writeValue(list.get(i), out, indent + 1);
            if (i < list.size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, indent);
        out.append("]");
    }

    private static void indent(StringBuilder out, int level) {
        out.append("  ".repeat(level));
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    // ---- Reading ----

    /**
     * Parse object.
     *
     * @param text the JSON text
     * @return a {@code Map}, {@code List}, {@code String}, {@code Double}, {@code Boolean} or {@code null}
     */
    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        return value;
    }

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("Expected ',' or '}' at " + pos);
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw new IllegalArgumentException("Expected ',' or ']' at " + pos);
            }
            return list;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char escaped = next();
                    switch (escaped) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = text.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> sb.append(escaped);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (text.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (text.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            throw new IllegalArgumentException("Invalid literal at " + pos);
        }

        Object parseNull() {
            if (text.startsWith("null", pos)) { pos += 4; return null; }
            throw new IllegalArgumentException("Invalid literal at " + pos);
        }

        Double parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < text.length() && isNumberChar(peek())) pos++;
            return Double.parseDouble(text.substring(start, pos));
        }

        private boolean isNumberChar(char c) {
            return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
        }

        void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }

        char peek() { return text.charAt(pos); }

        char next() { return text.charAt(pos++); }

        void expect(char c) {
            if (next() != c) throw new IllegalArgumentException("Expected '" + c + "' at " + (pos - 1));
        }
    }
}
