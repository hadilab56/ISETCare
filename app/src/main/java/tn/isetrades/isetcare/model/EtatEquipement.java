package tn.isetrades.isetcare.model;

public enum EtatEquipement {
    FONCTIONNEL("Fonctionnel", 0xFF1B7F6B),
    EN_PANNE("En panne", 0xFFD64545),
    EN_MAINTENANCE("En maintenance", 0xFFE08A00),
    HORS_SERVICE("Hors service", 0xFF6B7A88);

    public final String label;
    public final int couleur;

    EtatEquipement(String label, int couleur) {
        this.label = label;
        this.couleur = couleur;
    }
}
