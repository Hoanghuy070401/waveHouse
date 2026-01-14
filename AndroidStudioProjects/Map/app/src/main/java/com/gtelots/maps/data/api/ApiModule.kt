package  com.gtelots.maps.data.api


import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSource
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.nio.charset.Charset
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        val baseUrl = "https://maps.ots.vn/"

        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String,
                ) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String,
                ) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })
            val builder = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    //  }
                })
                .addInterceptor(object : Interceptor {
                    @Throws(IOException::class)
                    override fun intercept(chain: Interceptor.Chain): Response {
                        val request: Request = chain.request()
                        try {
                            val response: Response = chain.proceed(request)

                            if (response.code == 401) {

                            } else if (response.code != 200) {
                                val responseBody = response.body
                                val source: BufferedSource? = responseBody?.source()
                                source?.request(Long.MAX_VALUE) // Buffer the entire body.
                                val buffer = source?.buffer
                                val message = buffer?.clone()?.readString(Charset.forName("UTF-8"))
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }

                            val apiVer = response.header("api-version")
                            if (apiVer != null) {
//                                sharedPrefsHelper.setString("api-version", apiVer)
                            }
                            return response
                        } catch (e: Exception) {
                            return Response.Builder()
                                .request(request)
                                .protocol(Protocol.HTTP_2)
                                .code(999)
                                .message("Không kết nối được đến server")
                                .body("{${e}}".toResponseBody(null)).build()
                        }
                    }
                })

            return builder.build()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }
//    @Provides
//    @Singleton
//    fun provideOkHttpClient(
//    ):Gson{
//        return Gson()
//    }


}