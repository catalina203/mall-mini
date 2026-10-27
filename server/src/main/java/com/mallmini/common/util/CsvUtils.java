package com.mallmini.common.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static void write(HttpServletResponse response,
                             String filename,
                             List<String> headers,
                             List<? extends List<?>> rows) throws IOException {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println(headers.stream().map(CsvUtils::escape).reduce((a, b) -> a + "," + b).orElse(""));
        for (List<?> row : rows) {
            writer.println(row.stream().map(CsvUtils::escape).reduce((a, b) -> a + "," + b).orElse(""));
        }
        writer.flush();
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
