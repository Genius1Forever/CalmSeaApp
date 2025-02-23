package network;

import api.OpenAIService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
//Класс содержит настройку Retrofit и создание объекта OpenAIService
public class RetrofitClient {

    private static final String BASE_URL = "https://api.openai.com/";
    private static Retrofit retrofit = null;

    // Метод для получения экземпляра Retrofit
    public static OpenAIService createService(String apiKey) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + apiKey)
                            .build();
                    return chain.proceed(request);
                })
                .build();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(OpenAIService.class);
    }

}
