package tn.isetrades.isetcare.model;

public class NotificationItem {
    public long id;
    public long utilisateurId;
    /** Affectation, Statut, Rappel de maintenance, Résolution, Nouvelle réclamation */
    public String type;
    public String message;
    public long date;
    /** 0 si la notification n'est pas liée à une réclamation. */
    public long reclamationId;
    public long equipementId;
    public boolean lu;
}
