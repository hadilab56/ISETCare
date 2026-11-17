package tn.isetrades.isetcare.model;

/** Résultat de l'analyse d'une réclamation : catégorie, priorité et confiance. */
public class AnalyseIA {
    public final String categorie;
    public final Priorite priorite;
    public final double confiance;

    public AnalyseIA(String categorie, Priorite priorite, double confiance) {
        this.categorie = categorie;
        this.priorite = priorite;
        this.confiance = confiance;
    }
}
