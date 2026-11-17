package tn.isetrades.isetcare.model;

public class Intervention {
    public long id;
    public long reclamationId;
    public long equipementId;
    public long technicienId;
    public String diagnostic;
    public String action;
    public long dateDebut;
    public long dateFin;
    public boolean resolu;
    /** Coût de la réparation en DT. */
    public double cout;
}
