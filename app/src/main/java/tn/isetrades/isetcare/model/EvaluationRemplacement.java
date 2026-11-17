package tn.isetrades.isetcare.model;

/** Aide à la décision : réparer, surveiller ou remplacer un équipement. */
public class EvaluationRemplacement {

    public enum Decision {
        REPARER("Réparer", 0xFF1B7F6B),
        SURVEILLER("Surveiller", 0xFFE08A00),
        REMPLACER("Remplacer", 0xFFD64545);

        public final String label;
        public final int couleur;

        Decision(String label, int couleur) {
            this.label = label;
            this.couleur = couleur;
        }
    }

    public final int pannes12Mois;
    public final int interventions;
    public final double coutCumule;
    public final double coutRemplacement;
    public final Decision decision;
    public final String motif;

    public EvaluationRemplacement(int pannes12Mois, int interventions, double coutCumule,
                                  double coutRemplacement, Decision decision, String motif) {
        this.pannes12Mois = pannes12Mois;
        this.interventions = interventions;
        this.coutCumule = coutCumule;
        this.coutRemplacement = coutRemplacement;
        this.decision = decision;
        this.motif = motif;
    }
}
