package tn.isetrades.isetcare.model;

public class Reclamation {
    public long id;
    public String numero;
    public long equipementId;
    public long demandeurId;
    public String description;
    /** Photo facultative (URI du document choisi). */
    public String photoUri;
    public String canal = "MOBILE";

    // Résultat de l'analyse IA
    public String categorie;
    public Priorite priorite = Priorite.MOYENNE;
    public double confiance;

    public StatutReclamation statut = StatutReclamation.NOUVEAU;
    public long dateCreation;
    /** 0 tant qu'aucun technicien n'est affecté. */
    public long technicienId;
    public long dateAffectation;
    public long dateResolution;
}
