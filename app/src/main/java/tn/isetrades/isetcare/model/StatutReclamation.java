package tn.isetrades.isetcare.model;

/** Nouveau -> En cours -> Résolu */
public enum StatutReclamation {
    NOUVEAU("Nouveau", 0xFF2F6FED),
    EN_COURS("En cours", 0xFFE08A00),
    RESOLU("Résolu", 0xFF1B7F6B);

    public final String label;
    public final int couleur;

    StatutReclamation(String label, int couleur) {
        this.label = label;
        this.couleur = couleur;
    }
}
