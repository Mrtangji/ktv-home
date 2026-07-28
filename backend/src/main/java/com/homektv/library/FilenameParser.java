package com.homektv.library;

/**
 * 文件名规则兜底解析（P1.2，详设§9.3）。
 * 标签缺失时，从文件名按常见模式解析歌手/歌名：
 *   「歌手 - 歌名」「歌名 - 歌手」「歌手－歌名」（全角）等。
 * 分隔符两侧的空白会被裁剪；无法拆分时归入未识别。
 */
public final class FilenameParser {

    private FilenameParser() {}

    /**
     * @param filename 文件名（可含扩展名）
     * @return 解析结果；无法识别时 recognized=false，title 回退为去扩展名的文件名
     */
    public static ParsedMeta parse(String filename) {
        return parse(filename, "artist_title");
    }

    public static ParsedMeta parse(String filename, String rule) {
        String base = stripExtension(filename);
        if (base.isBlank()) return ParsedMeta.unrecognized(base);

        // 统一常见分隔符为标准 " - "
        String normalized = base
                .replace('－', '-')   // 全角连字符
                .replace('—', '-')    // 破折号
                .replace('_', '-');

        // 用 " - " 或 "-" 分隔（优先带空格的）
        String[] parts = splitByDash(normalized);
        if (parts == null) {
            // 无分隔符：整个作为歌名，歌手未知
            return ParsedMeta.of(base.trim(), "");
        }

        String left = parts[0].trim();
        String right = parts[1].trim();
        if (left.isEmpty() || right.isEmpty()) {
            return ParsedMeta.of(base.trim(), "");
        }

        return "title_artist".equals(rule) ? ParsedMeta.of(left, right) : ParsedMeta.of(right, left);
    }

    private static String[] splitByDash(String s) {
        int idx = s.indexOf(" - ");
        if (idx >= 0) {
            return new String[]{ s.substring(0, idx), s.substring(idx + 3) };
        }
        idx = s.indexOf('-');
        if (idx > 0 && idx < s.length() - 1) {
            return new String[]{ s.substring(0, idx), s.substring(idx + 1) };
        }
        return null;
    }

    static String stripExtension(String filename) {
        if (filename == null) return "";
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
