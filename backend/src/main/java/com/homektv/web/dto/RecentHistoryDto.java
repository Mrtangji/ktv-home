package com.homektv.web.dto;

import java.time.OffsetDateTime;

public record RecentHistoryDto(
        Long historyId,
        SongDto song,
        Long playedBy,
        String playedByNick,
        boolean mine,
        OffsetDateTime playedAt
) {}
