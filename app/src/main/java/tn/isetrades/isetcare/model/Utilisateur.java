package tn.isetrades.isetcare.model;

public class Utilisateur {
    public long id;
    public String nom;
    public String email;
    public String motDePasse;
    public Role role;
    /** Renseignée pour les techniciens uniquement. */
    public String specialite;

    public Utilisateur() {
    }

    public Utilisateur(long id, String nom, String email, String motDePasse, Role role, String specialite) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.specialite = specialite;
    }

    public String prenom() {
        int i = nom.indexOf(' ');
        return i > 0 ? nom.substring(0, i) : nom;
    }
}
