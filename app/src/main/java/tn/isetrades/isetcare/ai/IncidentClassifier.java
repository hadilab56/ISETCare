package tn.isetrades.isetcare.ai;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

import tn.isetrades.isetcare.model.AnalyseIA;
import tn.isetrades.isetcare.model.Priorite;


public final class IncidentClassifier {

    private static final Map<String, String[]> MOTS = new LinkedHashMap<>();

    static {
        MOTS.put("Réseau", new String[]{"wifi", "wi-fi", "internet", "connexion", "reseau",
                "routeur", "ethernet", "point d'acces", "debit"});
        MOTS.put("Informatique", new String[]{"pc", "ordinateur", "ecran", "clavier", "souris",
                "demarre", "windows", "bloque", "lent", "unite centrale", "disque", "session"});
        MOTS.put("Audiovisuel", new String[]{"projecteur", "videoprojecteur", "datashow", "hdmi",
                "vga", "television", "tv", "image", "telecommande", "voyant"});
        MOTS.put("Impression", new String[]{"imprimante", "imprime", "papier", "toner",
                "cartouche", "bourrage", "scanner", "photocopieuse", "encre"});
        MOTS.put("Climatisation", new String[]{"clim", "climatiseur", "climatisation", "froid",
                "chaud", "fuite d'eau", "filtre", "temperature"});
        MOTS.put("Électricité", new String[]{"prise", "courant", "electrique", "electricite",
                "eclairage", "lumiere", "neon", "disjoncteur", "interrupteur", "etincelle"});
    }

    private static final String[] URGENT = {"fumee", "brule", "etincelle", "odeur", "feu",
            "danger", "urgent", "examen", "plus de connexion", "ne demarre", "ne s'allume",
            "coupe", "court-circuit", "tous les", "toute la salle"};
    private static final String[] MINEUR = {"lent", "bruit", "parfois", "rayure", "leger",
            "de temps en temps", "un peu", "sale"};

    private IncidentClassifier() {
    }

    public static AnalyseIA analyser(String description, String typeEquipement) {
        String texte = normaliser(description);
        String type = normaliser(typeEquipement);

        String meilleure = "Autre";
        double meilleurScore = 0;
        double total = 0;
        for (Map.Entry<String, String[]> e : MOTS.entrySet()) {
            double score = 0;
            for (String mot : e.getValue()) {
                if (contientMot(texte, mot)) score += 1;
                if (!type.isEmpty() && contientMot(type, mot)) score += 1.5;
            }
            total += score;
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleure = e.getKey();
            }
        }

        Priorite priorite = Priorite.MOYENNE;
        if (contientUn(texte, URGENT)) {
            priorite = Priorite.ELEVEE;
        } else if (contientUn(texte, MINEUR)) {
            priorite = Priorite.FAIBLE;
        }

        // Une coupure réseau touche toute une salle : jamais en dessous de « Élevée ».
        if ("Réseau".equals(meilleure) && priorite == Priorite.MOYENNE) {
            priorite = Priorite.ELEVEE;
        }

        double confiance = total == 0 ? 0.40 : 0.55 + 0.42 * (meilleurScore / total);
        return new AnalyseIA(meilleure, priorite, Math.min(confiance, 0.97));
    }

    private static boolean contientUn(String texte, String[] mots) {
        for (String m : mots) {
            if (texte.contains(m)) return true;
        }
        return false;
    }

    /** Vrai si le mot apparaît entouré de séparateurs (évite "pc" dans "epcot"). */
    private static boolean contientMot(String texte, String mot) {
        int i = texte.indexOf(mot);
        while (i >= 0) {
            boolean debut = i == 0 || !Character.isLetterOrDigit(texte.charAt(i - 1));
            int fin = i + mot.length();
            boolean suite = fin >= texte.length() || !Character.isLetterOrDigit(texte.charAt(fin))
                    || mot.length() >= 5;
            if (debut && suite) return true;
            i = texte.indexOf(mot, i + 1);
        }
        return false;
    }

    /** Minuscules sans accents. */
    static String normaliser(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replace('’', '\'');
    }
}
