package api;
//Интерфейс для отправки запросов
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
public interface OpenAIService {
    @Headers({
            // Ключ API
            "Authorization: Bearer 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",
            "Content-Type: application/json"
    })
    @POST("v1/engines/davinci/completions")

    Call<ResponseBody> getResponse(@Body RequestBody requestBody);

}
