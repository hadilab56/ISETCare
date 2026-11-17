package tn.isetrades.isetcare.model;

public class MaintenancePreventive {
    public long id;
    public long equipementId;
    public long technicienId;
    public String titre;
    public long datePlanifiee;
    public int periodiciteJours;
    public int rappelAvantJours = 3;
    public boolean rappelEnvoye;
    /** 0 tant que la maintenance n'est pas réalisée. */
    public long dateRealisation;
    public String rapport;

    public boolean realisee() {
        return dateRealisation > 0;
    }
}
