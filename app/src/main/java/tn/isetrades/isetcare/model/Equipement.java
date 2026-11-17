package tn.isetrades.isetcare.model;

/** Fiche numérique d'un équipement. Le champ code est le contenu de son QR Code. */
public class Equipement {
    public long id;
    public String code;
    public String nom;
    public String type;
    public String marque;
    public String modele;
    public String inventaire;
    public String numeroSerie;
    public String salle;
    public EtatEquipement etat = EtatEquipement.FONCTIONNEL;
    public long dateAcquisition;
    /** Coût d'acquisition, utilisé comme coût de remplacement estimé (DT). */
    public double coutAcquisition;

    public Equipement() {
    }

    public Equipement(long id, String code, String nom, String type, String marque, String modele,
                      String inventaire, String numeroSerie, String salle,
                      long dateAcquisition, double coutAcquisition) {
        this.id = id;
        this.code = code;
        this.nom = nom;
        this.type = type;
        this.marque = marque;
        this.modele = modele;
        this.inventaire = inventaire;
        this.numeroSerie = numeroSerie;
        this.salle = salle;
        this.dateAcquisition = dateAcquisition;
        this.coutAcquisition = coutAcquisition;
    }
}
