package com.anadcurukovic.berylux.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Moments are stored as milliseconds since 1970, game days as days since 1970. */
internal class TimeConverters {
    @TypeConverter
    fun instantToMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}
