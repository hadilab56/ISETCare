package tn.isetrades.isetcare.model;

public enum Role {
    PROFESSEUR("Professeur"),
    PERSONNEL("Personnel"),
    TECHNICIEN("Technicien"),
    ADMINISTRATEUR("Administrateur");

    public final String label;

    Role(String label) {
        this.label = label;
    }

    /** Seuls le professeur et le personnel déclarent des pannes (règle métier). */
    public boolean isDemandeur() {
        return this == PROFESSEUR || this == PERSONNEL;
    }
}
