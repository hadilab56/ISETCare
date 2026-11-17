package tn.isetrades.isetcare.ai;

import tn.isetrades.isetcare.model.EvaluationRemplacement;
import tn.isetrades.isetcare.model.EvaluationRemplacement.Decision;


public final class ReplacementAdvisor {

    private ReplacementAdvisor() {
    }

    public static EvaluationRemplacement evaluer(int pannes12Mois, int interventions,
                                                 double coutCumule, double coutRemplacement) {
        double ratio = coutRemplacement > 0 ? coutCumule / coutRemplacement : 0;
        Decision decision;
        String motif;

        if (ratio >= 0.8 || pannes12Mois >= 5) {
            decision = Decision.REMPLACER;
            motif = ratio >= 0.8
                    ? "Les réparations deviennent plus coûteuses que le remplacement."
                    : "Pannes trop fréquentes : l'équipement n'est plus fiable.";
        } else if (ratio >= 0.4 || pannes12Mois >= 3) {
            decision = Decision.SURVEILLER;
            motif = "Pannes ou coûts en hausse : à surveiller avant la prochaine réparation.";
        } else {
            decision = Decision.REPARER;
            motif = "Peu de pannes et coûts de réparation faibles.";
        }
        return new EvaluationRemplacement(pannes12Mois, interventions, coutCumule,
                coutRemplacement, decision, motif);
    }
}
