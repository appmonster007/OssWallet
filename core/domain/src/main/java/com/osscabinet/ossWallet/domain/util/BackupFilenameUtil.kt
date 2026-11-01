package com.osscabinet.ossWallet.domain.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Filename-safe timestamp, e.g. "20261101_143022_+0530" — numeric offset, no colon. */
fun backupFilenameTimestamp(): String =
    ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_Z"))
