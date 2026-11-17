package tn.isetrades.isetcare.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    /** 10.0.2.2 = la machine de développement vue depuis l'émulateur Android. */
    public static final String BASE_URL = "http://10.0.2.2:8080/";

    private static IsetCareApi api;

    private ApiClient() {
    }

    public static synchronized IsetCareApi get() {
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(IsetCareApi.class);
        }
        return api;
    }
}
