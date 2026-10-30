package com.osscabinet.ossWallet.domain.service

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Decides whether the "Buy Me a Coffee" support popup should show on this app open: once
 * per "alternate Monday" window (weeks at an even distance from [ANCHOR_MONDAY]), on the
 * first open on or after that Monday.
 */
object BuyMeACoffeePromptScheduler {

    /** Week 0 for the alternating pattern. Any fixed Monday works; this one has no other meaning. */
    private val ANCHOR_MONDAY: LocalDate = LocalDate.of(2024, 1, 1)

    /** Sentinel for "never shown" — no real week start can produce this epoch day. */
    const val NEVER_SHOWN_EPOCH_DAY: Long = -1L

    private fun mostRecentMonday(today: LocalDate): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun isAlternateWeek(monday: LocalDate): Boolean =
        ChronoUnit.WEEKS.between(ANCHOR_MONDAY, monday) % 2 == 0L

    /** Epoch day of the current alternating-schedule window's Monday, for persisting "shown". */
    fun currentWindowEpochDay(today: LocalDate): Long = mostRecentMonday(today).toEpochDay()

    fun shouldShowPrompt(today: LocalDate, lastShownWindowEpochDay: Long): Boolean {
        val monday = mostRecentMonday(today)
        return isAlternateWeek(monday) && monday.toEpochDay() != lastShownWindowEpochDay
    }
}
