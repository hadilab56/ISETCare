package tn.isetrades.isetcare.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import tn.isetrades.isetcare.model.Equipement;
import tn.isetrades.isetcare.model.Intervention;
import tn.isetrades.isetcare.model.Reclamation;
import tn.isetrades.isetcare.model.Utilisateur;

/**
 * Contrat de l'API REST Spring Boot d'ISETCare (partagée avec le site web React).
 *
 * L'application utilise pour l'instant DataStore (données locales). Quand le backend
 * sera prêt, chaque méthode de DataStore pourra appeler l'endpoint correspondant.
 */
public interface IsetCareApi {

    @POST("api/auth/login")
    Call<Utilisateur> login(@Body Map<String, String> identifiants);

    @GET("api/equipements")
    Call<List<Equipement>> equipements();

    @GET("api/equipements/{id}")
    Call<Equipement> equipement(@Path("id") long id);

    @GET("api/reclamations")
    Call<List<Reclamation>> reclamations();

    @GET("api/reclamations/{id}")
    Call<Reclamation> reclamation(@Path("id") long id);

    @POST("api/reclamations")
    Call<Reclamation> creerReclamation(@Body Reclamation reclamation);

    @PUT("api/reclamations/{id}/affectation")
    Call<Reclamation> affecter(@Path("id") long id, @Body Map<String, Long> technicien);

    @POST("api/interventions")
    Call<Intervention> creerIntervention(@Body Intervention intervention);

    /** Entrée : {"description": "..."} — Sortie : catégorie, priorité, confiance. */
    @POST("api/ia/analyser")
    Call<Map<String, Object>> analyser(@Body Map<String, String> description);
}
