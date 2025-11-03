package com.example.devmobile.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

/**
 * Client singleton pour configurer Retrofit et la connexion à l'API.
 */
public class RetrofitClient {

    // L'URL de base de votre backend (Express/Node.js)
    // **METTEZ À JOUR CETTE URL** (Utilisez l'adresse IP locale de votre machine + le port, ex: http://192.168.1.5:3000/api/)
    private static final String BASE_URL = "http://10.0.2.2:5000/api/"; // 10.0.2.2 = localhost de l'hôte vu depuis l'émulateur

    private static RetrofitClient instance;
    private Retrofit retrofit;

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Retourne l'instance singleton du RetrofitClient.
     */
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    /**
     * Crée et retourne l'instance du service d'authentification.
     */
    public AuthService getAuthService() {
        return retrofit.create(AuthService.class);
    }

    // Ajoutez d'autres services ici (ex: getProductService())

    public AnnonceService getAnnonceService() {
        return retrofit.create(AnnonceService.class);
    }

    public UtilisateurService getUtilisateurService() {
        return retrofit.create(UtilisateurService.class);
    }
}
