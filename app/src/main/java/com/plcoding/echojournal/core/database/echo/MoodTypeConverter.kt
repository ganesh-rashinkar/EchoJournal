package com.plcoding.echojournal.core.database.echo

import androidx.room.TypeConverter
import com.plcoding.echojournal.echos.domain.echo.Mood

class MoodTypeConverter {
    @TypeConverter
    fun fromMood(mood: Mood): String{
        return mood.name
    }

    @TypeConverter
    fun toMood(moodName:String): Mood{
        return Mood.valueOf(moodName)
    }
}