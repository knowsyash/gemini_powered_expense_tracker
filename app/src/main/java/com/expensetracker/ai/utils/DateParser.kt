package com.expensetracker.ai.utils

import java.text.SimpleDateFormat
import java.util.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
object DateParser {

    /** Parse natural language date expressions and return the corresponding Date */
    fun parseDate(message: String): Date? {
        val normalizedMessage = message.lowercase()
        val calendar = Calendar.getInstance()

        return when {
            // Today
            normalizedMessage.contains("today") -> {
                Date()
            }

            // Yesterday
            normalizedMessage.contains("yesterday") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.time
            }

            // Last week (7 days ago)
            normalizedMessage.contains("last week") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.time
            }

            // This week (beginning of current week)
            normalizedMessage.contains("this week") -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.time
            }

            // Last month (1 month ago)
            normalizedMessage.contains("last month") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.time
            }

            // This month (beginning of current month)
            normalizedMessage.contains("this month") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }

            // Specific days ago (e.g., "2 days ago", "3 days ago")
            normalizedMessage.contains("days ago") -> {
                val daysPattern = "(\\d+)\\s+days?\\s+ago".toRegex()
                val match = daysPattern.find(normalizedMessage)
                if (match != null) {
                    val days = match.groupValues[1].toIntOrNull()
                    if (days != null) {
                        calendar.add(Calendar.DAY_OF_YEAR, -days)
                        calendar.time
                    } else null
                } else null
            }

            // Specific weeks ago (e.g., "2 weeks ago")
            normalizedMessage.contains("weeks ago") -> {
                val weeksPattern = "(\\d+)\\s+weeks?\\s+ago".toRegex()
                val match = weeksPattern.find(normalizedMessage)
                if (match != null) {
                    val weeks = match.groupValues[1].toIntOrNull()
                    if (weeks != null) {
                        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
                        calendar.time
                    } else null
                } else null
            }

            // Specific months ago (e.g., "2 months ago")
            normalizedMessage.contains("months ago") -> {
                val monthsPattern = "(\\d+)\\s+months?\\s+ago".toRegex()
                val match = monthsPattern.find(normalizedMessage)
                if (match != null) {
                    val months = match.groupValues[1].toIntOrNull()
                    if (months != null) {
                        calendar.add(Calendar.MONTH, -months)
                        calendar.time
                    } else null
                } else null
            }

            // Day of the week (e.g., "on monday", "last tuesday")
            normalizedMessage.contains("monday") || normalizedMessage.contains("mon") -> {
                getLastDayOfWeek(Calendar.MONDAY)
            }
            normalizedMessage.contains("tuesday") || normalizedMessage.contains("tue") -> {
                getLastDayOfWeek(Calendar.TUESDAY)
            }
            normalizedMessage.contains("wednesday") || normalizedMessage.contains("wed") -> {
                getLastDayOfWeek(Calendar.WEDNESDAY)
            }
            normalizedMessage.contains("thursday") || normalizedMessage.contains("thu") -> {
                getLastDayOfWeek(Calendar.THURSDAY)
            }
            normalizedMessage.contains("friday") || normalizedMessage.contains("fri") -> {
                getLastDayOfWeek(Calendar.FRIDAY)
            }
            normalizedMessage.contains("saturday") || normalizedMessage.contains("sat") -> {
                getLastDayOfWeek(Calendar.SATURDAY)
            }
            normalizedMessage.contains("sunday") || normalizedMessage.contains("sun") -> {
                getLastDayOfWeek(Calendar.SUNDAY)
            }
            else -> null
        }
    }

    /** Get the most recent occurrence of a specific day of the week */
    private fun getLastDayOfWeek(dayOfWeek: Int): Date {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)

        val daysToSubtract =
                if (currentDay >= dayOfWeek) {
                    currentDay - dayOfWeek
                } else {
                    7 - (dayOfWeek - currentDay)
                }

        if (daysToSubtract == 0) {
            // If it's the same day, assume they mean last week's occurrence
            calendar.add(Calendar.DAY_OF_YEAR, -7)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        }

        return calendar.time
    }

    /** Parse date range queries for spending analysis */
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    fun parseDateRange(query: String): Pair<Date, Date>? {
        val normalizedQuery = query.lowercase()

        return when {
            // Last week
            normalizedQuery.contains("last week") -> {
                val endDate = Calendar.getInstance()
                endDate.add(Calendar.DAY_OF_YEAR, -7)
                endDate.set(Calendar.HOUR_OF_DAY, 23)
                endDate.set(Calendar.MINUTE, 59)
                endDate.set(Calendar.SECOND, 59)

                val startDate = Calendar.getInstance()
                startDate.time = endDate.time
                startDate.add(Calendar.DAY_OF_YEAR, -6)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)

                Pair(startDate.time, endDate.time)
            }

            // This week
            normalizedQuery.contains("this week") -> {
                val startDate = Calendar.getInstance()
                startDate.set(Calendar.DAY_OF_WEEK, startDate.firstDayOfWeek)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)

                val endDate = Calendar.getInstance()
                endDate.set(Calendar.HOUR_OF_DAY, 23)
                endDate.set(Calendar.MINUTE, 59)
                endDate.set(Calendar.SECOND, 59)

                Pair(startDate.time, endDate.time)
            }

            // Last month
            normalizedQuery.contains("last month") -> {
                val startDate = Calendar.getInstance()
                startDate.add(Calendar.MONTH, -1)
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)

                val endDate = Calendar.getInstance()
                endDate.time = startDate.time
                endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH))
                endDate.set(Calendar.HOUR_OF_DAY, 23)
                endDate.set(Calendar.MINUTE, 59)
                endDate.set(Calendar.SECOND, 59)

                Pair(startDate.time, endDate.time)
            }

            // This month
            normalizedQuery.contains("this month") -> {
                val startDate = Calendar.getInstance()
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)

                val endDate = Calendar.getInstance()
                endDate.set(Calendar.HOUR_OF_DAY, 23)
                endDate.set(Calendar.MINUTE, 59)
                endDate.set(Calendar.SECOND, 59)

                Pair(startDate.time, endDate.time)
            }

            // Yesterday
            normalizedQuery.contains("yesterday") -> {
                val startDate = Calendar.getInstance()
                startDate.add(Calendar.DAY_OF_YEAR, -1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)

                val endDate = Calendar.getInstance()
                endDate.time = startDate.time
                endDate.set(Calendar.HOUR_OF_DAY, 23)
                endDate.set(Calendar.MINUTE, 59)
                endDate.set(Calendar.SECOND, 59)

                Pair(startDate.time, endDate.time)
            }

            // Today
            normalizedQuery.contains("today") -> {
                val startDate = Calendar.getInstance()
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)

                val endDate = Calendar.getInstance()
                endDate.set(Calendar.HOUR_OF_DAY, 23)
                endDate.set(Calendar.MINUTE, 59)
                endDate.set(Calendar.SECOND, 59)

                Pair(startDate.time, endDate.time)
            }

            // Last N days
            normalizedQuery.contains("last") && normalizedQuery.contains("days") -> {
                val pattern = "last\\s+(\\d+)\\s+days".toRegex()
                val match = pattern.find(normalizedQuery)
                if (match != null) {
                    val days = match.groupValues[1].toIntOrNull()
                    if (days != null) {
                        val endDate = Calendar.getInstance()
                        endDate.set(Calendar.HOUR_OF_DAY, 23)
                        endDate.set(Calendar.MINUTE, 59)
                        endDate.set(Calendar.SECOND, 59)

                        val startDate = Calendar.getInstance()
                        startDate.add(Calendar.DAY_OF_YEAR, -days)
                        startDate.set(Calendar.HOUR_OF_DAY, 0)
                        startDate.set(Calendar.MINUTE, 0)
                        startDate.set(Calendar.SECOND, 0)

                        Pair(startDate.time, endDate.time)
                    } else null
                } else null
            }
            else -> null
        }
    }

    /** Format date for display */
    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    /** Format date range for display */
    fun formatDateRange(startDate: Date, endDate: Date): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val sdfYear = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val startCal = Calendar.getInstance()
        startCal.time = startDate
        val endCal = Calendar.getInstance()
        endCal.time = endDate

        return if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)) {
            "${sdf.format(startDate)} - ${sdfYear.format(endDate)}"
        } else {
            "${sdfYear.format(startDate)} - ${sdfYear.format(endDate)}"
        }
    }
}
