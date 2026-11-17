package tn.isetrades.isetcare.model;

public enum Priorite {
    FAIBLE("Faible", 0xFF1B7F6B),
    MOYENNE("Moyenne", 0xFFE08A00),
    ELEVEE("Élevée", 0xFFD64545);

    public final String label;
    public final int couleur;

    Priorite(String label, int couleur) {
        this.label = label;
        this.couleur = couleur;
    }
}
