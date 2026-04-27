package com.alberto.edubus.network

import com.alberto.edubus.model.TriviaResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Definimos la petición a la API
interface TriviaApiService {
    // Si pasamos amount=5, nos devuelve 5 preguntas
    @GET("api.php")
    suspend fun getPreguntas(
        @Query("amount") cantidad: Int,
        @Query("type") tipo: String = "multiple" // Queremos respuestas A, B, C, D
    ): TriviaResponse
}

// 2. Creamos el cliente estático (Singleton) para no saturar la memoria
object RetrofitClient {
    private const val BASE_URL = "https://opentdb.com/"

    val triviaApi: TriviaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TriviaApiService::class.java)
    }
}