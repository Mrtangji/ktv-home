package com.homektv.web;

import com.homektv.domain.SongFile;
import com.homektv.repo.SongFileRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * 媒体流（P1.17，详设§11.1）。
 * 支持 HTTP Range，使 MKV/MP4 等大文件秒开、可 seek；TV 端 ExoPlayer 流式拉取。
 * file_id = song_files.id。
 *
 * 用 StreamingResponseBody + RandomAccessFile 手动流字节，完全掌控
 * Content-Type / Content-Range / Content-Length，不受 ResourceRegion 转换器
 * 的 Content-Type 白名单限制（其默认仅接受 octet-stream，video/mp4 会被拒）。
 */
@RestController
@RequestMapping("/api")
public class StreamController {

    private static final int BUF = 64 * 1024;

    private final SongFileRepository fileRepo;

    public StreamController(SongFileRepository fileRepo) {
        this.fileRepo = fileRepo;
    }

    @GetMapping("/stream/{fileId}")
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable Long fileId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        SongFile sf = fileRepo.findById(fileId).orElse(null);
        if (sf == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(sf.getFilePath());
        if (!file.exists() || !file.isFile()) {
            // 文件丢失：交由上层标记 file_missing（P2.5），这里返回 404
            return ResponseEntity.notFound().build();
        }

        long length = file.length();
        MediaType contentType = mediaTypeFor(sf.getFormat(), file.getName());

        // 无 Range：整段返回，声明 Accept-Ranges 以便客户端后续 seek
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(contentType)
                    .contentLength(length)
                    .body(writeRegion(file, 0, length));
        }

        // 有 Range：解析首个区间，返回 206 Partial Content
        long start;
        long end;
        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            HttpRange range = ranges.get(0);
            start = range.getRangeStart(length);
            end = range.getRangeEnd(length);
        } catch (IllegalArgumentException e) {
            return rangeNotSatisfiable(length);
        }
        // 起点越界 → 416（不依赖底层抛异常，避免被框架映射成 400）
        if (start >= length || start < 0 || end < start) {
            return rangeNotSatisfiable(length);
        }

        long count = end - start + 1;

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length)
                .contentType(contentType)
                .contentLength(count)
                .body(writeRegion(file, start, count));
    }

    private ResponseEntity<StreamingResponseBody> rangeNotSatisfiable(long length) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + length)
                .build();
    }

    /** 从 offset 起流出 count 字节。 */
    private StreamingResponseBody writeRegion(File file, long offset, long count) {
        return out -> {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(offset);
                byte[] buf = new byte[BUF];
                long remaining = count;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buf.length, remaining);
                    int read = raf.read(buf, 0, toRead);
                    if (read == -1) break;
                    out.write(buf, 0, read);
                    remaining -= read;
                }
                out.flush();
            }
        };
    }

    /** 按容器格式/扩展名映射 Content-Type，未知回退 octet-stream。 */
    private MediaType mediaTypeFor(String format, String name) {
        String f = format == null ? "" : format.toLowerCase();
        String n = name.toLowerCase();
        if (f.contains("matroska") || n.endsWith(".mkv")) return MediaType.parseMediaType("video/x-matroska");
        if (f.contains("mp4") || n.endsWith(".mp4") || n.endsWith(".m4v")) return MediaType.parseMediaType("video/mp4");
        if (n.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        if (n.endsWith(".avi")) return MediaType.parseMediaType("video/x-msvideo");
        if (f.contains("mpeg") || n.endsWith(".mpg") || n.endsWith(".mpeg")) return MediaType.parseMediaType("video/mpeg");
        if (n.endsWith(".flac")) return MediaType.parseMediaType("audio/flac");
        if (n.endsWith(".mp3")) return MediaType.parseMediaType("audio/mpeg");
        if (n.endsWith(".m4a") || n.endsWith(".aac")) return MediaType.parseMediaType("audio/mp4");
        if (n.endsWith(".wav")) return MediaType.parseMediaType("audio/wav");
        if (n.endsWith(".ogg")) return MediaType.parseMediaType("audio/ogg");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
