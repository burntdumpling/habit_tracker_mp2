package com.example.application.data.network.api

import com.example.application.data.network.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface HabitApiService {

    @GET("habits")
    suspend fun getHabits(
        @Query("status") status: String? = null
    ): List<HabitDto>

    @GET("habits/logs")
    suspend fun getHabitLogs(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): List<HabitLogDto>

    @GET("habits/calendar/range")
    suspend fun getCalendarRange(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String,
        @Query("status") status: String? = null
    ): CalendarRangeDto

    @GET("habits/calendar/day")
    suspend fun getCalendarDay(
        @Query("date") date: String,
        @Query("status") status: String? = null
    ): CalendarDayDto

    @GET("habits/{id}")
    suspend fun getHabit(
        @Path("id") id: Long
    ): HabitDto

    @POST("habits")
    suspend fun createHabit(
        @Body request: CreateHabitDto
    ): HabitDto

    @PATCH("habits/{id}")
    suspend fun updateHabit(
        @Path("id") id: Long,
        @Body request: CreateHabitDto
    ): HabitDto

    @DELETE("habits/{id}")
    suspend fun deleteHabit(
        @Path("id") id: Long
    )

    @PUT("habits/{id}/logs")
    suspend fun upsertHabitLog(
        @Path("id") habitId: Long,
        @Body request: UpsertHabitLogRequest
    ): HabitLogDto
}
