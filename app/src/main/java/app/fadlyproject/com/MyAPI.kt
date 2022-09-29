package app.fadlyproject.com

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MyAPI {

    @Multipart
    @POST("process")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("desc") desc: RequestBody
    ): Call<UploadResponse>

    companion object {
        operator fun invoke(): MyAPI {
            return Retrofit.Builder()
                .baseUrl("http://167.172.72.26:1337/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MyAPI::class.java)
        }
    }
}