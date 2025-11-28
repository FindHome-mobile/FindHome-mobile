package com.example.devmobile.api;

import android.content.Context;
import android.content.SharedPreferences;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

/**
 * Client singleton pour configurer Retrofit et la connexion à l'API.
 */
public class RetrofitClient {

    // L'URL de base de votre backend (Express/Node.js)
    // **METTEZ À JOUR CETTE URL** (Utilisez l'adresse IP locale de votre machine + le port, ex: http://192.168.1.5:3000/api/)
    //172.20.10.2
    private static final String BASE_URL = "http://172.20.10.2:5000/api/"; // Adresse IP réseau locale pour téléphone réel

    private static RetrofitClient instance;
    private Retrofit retrofit;
    private Context context;

    public static void setContext(Context ctx) {
        instance = null; // Reset instance to recreate with context
        getInstance().context = ctx;
    }

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();

                    // Ajouter l'authentification si disponible
                    if (context != null) {
                        SharedPreferences prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                        String userId = prefs.getString("USER_ID", null);
                        if (userId != null && !userId.isEmpty()) {
                            requestBuilder.addHeader("user-id", userId);
                            android.util.Log.d("RetrofitClient", "Ajout header user-id: " + userId + " pour URL: " + original.url());
                        } else {
                            android.util.Log.w("RetrofitClient", "user-id non trouvé dans SharedPreferences");
                        }
                    } else {
                        android.util.Log.w("RetrofitClient", "Context null, impossible d'ajouter user-id");
                    }

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
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

    public DemandeProprietaireService getDemandeProprietaireService() {
        return retrofit.create(DemandeProprietaireService.class);
    }

    public FavoriService getFavoriService() {
        return retrofit.create(FavoriService.class);
    }

    public String getBaseUrl() {
        return BASE_URL;
    }
}