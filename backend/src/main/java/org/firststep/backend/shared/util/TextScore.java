package org.firststep.backend.shared.util;

import java.util.List;
import java.util.Locale;

public final class TextScore {

    private TextScore() {}

    public static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    public static int match(String query, String field) {
        String q = lower(query);
        if (q.isBlank() || field == null || field.isBlank()) return 0;
        String f = lower(field);
        return f.contains(q) ? 5 : 0;
    }

    public static int match(String query, List<String> fields) {
        String q = lower(query);
        if (q.isBlank() || fields == null) return 0;
        for (String f : fields) {
            int s = match(q, f);
            if (s > 0) return s;
        }
        return 0;
    }

    public static int match(String query, String[] fields) {
        String q = lower(query);
        if (q.isBlank() || fields == null) return 0;
        for (String f : fields) {
            int s = match(q, f);
            if (s > 0) return s;
        }
        return 0;
    }
}
