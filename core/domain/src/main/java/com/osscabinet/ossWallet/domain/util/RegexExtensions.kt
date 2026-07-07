package com.osscabinet.ossWallet.domain.util

/**
 * Parses a string into a Regex, supporting literal format: /pattern/flags
 * Flags supported:
 * i -> IGNORE_CASE (Case-insensitive)
 * m -> MULTILINE (^ and $ match start/end of lines)
 * s -> DOTALL (. matches newlines)
 * x -> COMMENTS (Permits whitespace and comments)
 * d -> UNIX_LINES (Only \n is recognized as line terminator)
 * c -> CANON_EQ (Enables canonical equivalence)
 * l -> LITERAL (Enables literal parsing of the pattern)
 */
fun String.toSmartRegex(defaultOptions: Set<RegexOption> = setOf(RegexOption.IGNORE_CASE)): Regex {
    val literalPattern = """^/(.+)/([imsxdcl]*)$""".toRegex()
    val match = literalPattern.matchEntire(this)

    return if (match != null) {
        val pattern = match.groupValues[1]
        val flags = match.groupValues[2]
        val options = mutableSetOf<RegexOption>()

        if ('i' in flags) options.add(RegexOption.IGNORE_CASE)
        if ('m' in flags) options.add(RegexOption.MULTILINE)
        if ('s' in flags) options.add(RegexOption.DOT_MATCHES_ALL)
        if ('x' in flags) options.add(RegexOption.COMMENTS)
        if ('d' in flags) options.add(RegexOption.UNIX_LINES)
        if ('c' in flags) options.add(RegexOption.CANON_EQ)
        if ('l' in flags) options.add(RegexOption.LITERAL)

        Regex(pattern, options)
    } else {
        // Fallback to default if not in literal format
        Regex(this, defaultOptions)
    }
}
