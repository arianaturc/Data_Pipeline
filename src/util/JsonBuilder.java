package util;

import java.util.*;

public class JsonBuilder {
    public static String toJson(Map<String, Object> map) {
        return toJson(map, 0);
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object obj, int indent) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof Map) {
            return mapToJson((Map<String, Object>) obj, indent);
        } else if (obj instanceof List) {
            return listToJson((List<Object>) obj, indent);
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
    }

    private static String mapToJson(Map<String, Object> map, int indent) {
        if (map.isEmpty()) return "{}";

        StringBuilder sb = new StringBuilder();
        String pad = "  ".repeat(indent + 1);
        String closePad = "  ".repeat(indent);

        sb.append("{\n");
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            sb.append(pad)
                    .append("\"").append(escapeJson(entry.getKey())).append("\": ")
                    .append(toJson(entry.getValue(), indent + 1));
            if (it.hasNext()) sb.append(",");
            sb.append("\n");
        }
        sb.append(closePad).append("}");
        return sb.toString();
    }

    private static String listToJson(List<Object> list, int indent) {
        if (list.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder();
        String pad = "  ".repeat(indent + 1);
        String closePad = "  ".repeat(indent);

        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append(pad).append(toJson(list.get(i), indent + 1));
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(closePad).append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    public static Map<String, Object> object() {
        return new LinkedHashMap<>();
    }


    public static List<Object> array() {
        return new ArrayList<>();
    }
}
