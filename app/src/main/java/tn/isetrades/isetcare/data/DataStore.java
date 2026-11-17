package tn.isetrades.isetcare.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import tn.isetrades.isetcare.ai.IncidentClassifier;
import tn.isetrades.isetcare.ai.ReplacementAdvisor;
import tn.isetrades.isetcare.model.AnalyseIA;
import tn.isetrades.isetcare.model.Equipement;
import tn.isetrades.isetcare.model.EtatEquipement;
import tn.isetrades.isetcare.model.EvaluationRemplacement;
import tn.isetrades.isetcare.model.Intervention;
import tn.isetrades.isetcare.model.MaintenancePreventive;
import tn.isetrades.isetcare.model.NotificationItem;
import tn.isetrades.isetcare.model.Reclamation;
import tn.isetrades.isetcare.model.Role;
import tn.isetrades.isetcare.model.StatutReclamation;
import tn.isetrades.isetcare.model.Utilisateur;

/**
 * Source de données de l'application et règles métier d'ISETCare.
 *
 * Les données sont gardées sur le téléphone (JSON dans les SharedPreferences) pour que
 * l'application soit utilisable sans serveur. Chaque méthode publique correspond à un
 * endpoint de l'API REST Spring Boot (voir network/IsetCareApi) et pourra y être branchée.
 */
public final class DataStore {

    private static final String PREFS = "isetcare";
    private static final String KEY = "state_v1";
    private static final long JOUR = 24L * 60 * 60 * 1000;
    private static final long HEURE = 60L * 60 * 1000;

    private static DataStore instance;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private State s;

    /** Tout l'état persistant de l'application. */
    public static class State {
        public List<Utilisateur> utilisateurs = new ArrayList<>();
        public List<Equipement> equipements = new ArrayList<>();
        public List<Reclamation> reclamations = new ArrayList<>();
        public List<Intervention> interventions = new ArrayList<>();
        public List<MaintenancePreventive> maintenances = new ArrayList<>();
        public List<NotificationItem> notifications = new ArrayList<>();
        public long nextId = 1000;
        public int nextRec = 43;
        public long sessionUserId = 0;
    }

    private DataStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY, null);
        if (json != null) {
            try {
                s = gson.fromJson(json, State.class);
            } catch (RuntimeException e) {
                s = null;
            }
        }
        if (s == null || s.utilisateurs == null || s.utilisateurs.isEmpty()) {
            s = new State();
            seed();
            save();
        }
    }

    public static synchronized DataStore get(Context context) {
        if (instance == null) instance = new DataStore(context);
        return instance;
    }

    private void save() {
        prefs.edit().putString(KEY, gson.toJson(s)).apply();
    }

    // ------------------------------------------------------------------ Session

    /** POST /api/auth/login */
    public synchronized Utilisateur login(String email, String motDePasse) {
        for (Utilisateur u : s.utilisateurs) {
            if (u.email.equalsIgnoreCase(email.trim()) && u.motDePasse.equals(motDePasse)) {
                s.sessionUserId = u.id;
                save();
                return u;
            }
        }
        return null;
    }

    public synchronized void logout() {
        s.sessionUserId = 0;
        save();
    }

    public synchronized Utilisateur currentUser() {
        return utilisateur(s.sessionUserId);
    }

    public synchronized Utilisateur utilisateur(long id) {
        for (Utilisateur u : s.utilisateurs) {
            if (u.id == id) return u;
        }
        return null;
    }

    public synchronized List<Utilisateur> techniciens() {
        return parRole(Role.TECHNICIEN);
    }

    private List<Utilisateur> parRole(Role role) {
        List<Utilisateur> out = new ArrayList<>();
        for (Utilisateur u : s.utilisateurs) {
            if (u.role == role) out.add(u);
        }
        return out;
    }

    // --------------------------------------------------------------- Équipements

    /** GET /api/equipements */
    public synchronized List<Equipement> equipements() {
        return new ArrayList<>(s.equipements);
    }

    /** GET /api/equipements/{id} */
    public synchronized Equipement equipement(long id) {
        for (Equipement e : s.equipements) {
            if (e.id == id) return e;
        }
        return null;
    }

    /** Recherche par contenu du QR Code (code de l'équipement, ex. PC-024). */
    public synchronized Equipement findEquipementByCode(String code) {
        if (code == null) return null;
        String c = code.trim();
        for (Equipement e : s.equipements) {
            if (e.code.equalsIgnoreCase(c) || e.inventaire.equalsIgnoreCase(c)) return e;
        }
        return null;
    }

    // -------------------------------------------------------------- Réclamations

    /**
     * GET /api/reclamations — le demandeur voit les siennes, le technicien celles qui
     * lui sont affectées, l'administrateur toutes. Les plus récentes d'abord.
     */
    public synchronized List<Reclamation> reclamationsFor(Utilisateur u) {
        List<Reclamation> out = new ArrayList<>();
        if (u == null) return out;
        for (Reclamation r : s.reclamations) {
            boolean visible = u.role == Role.ADMINISTRATEUR
                    || (u.role == Role.TECHNICIEN && r.technicienId == u.id)
                    || (u.role.isDemandeur() && r.demandeurId == u.id);
            if (visible) out.add(r);
        }
        out.sort((a, b) -> Long.compare(b.dateCreation, a.dateCreation));
        return out;
    }

    public synchronized List<Reclamation> reclamationsEquipement(long equipementId) {
        List<Reclamation> out = new ArrayList<>();
        for (Reclamation r : s.reclamations) {
            if (r.equipementId == equipementId) out.add(r);
        }
        out.sort((a, b) -> Long.compare(b.dateCreation, a.dateCreation));
        return out;
    }

    /** GET /api/reclamations/{id} */
    public synchronized Reclamation reclamation(long id) {
        for (Reclamation r : s.reclamations) {
            if (r.id == id) return r;
        }
        return null;
    }

    /**
     * POST /api/reclamations — crée la réclamation, lance l'analyse IA, passe
     * l'équipement « En panne » et prévient les administrateurs.
     */
    public synchronized Reclamation creerReclamation(long equipementId, String description,
                                                     String photoUri) {
        Utilisateur u = currentUser();
        Equipement e = equipement(equipementId);
        if (u == null || e == null) return null;

        AnalyseIA a = IncidentClassifier.analyser(description, e.type + " " + e.nom);
        Reclamation r = new Reclamation();
        r.id = s.nextId++;
        r.numero = String.format(Locale.US, "REC-%03d", s.nextRec++);
        r.equipementId = e.id;
        r.demandeurId = u.id;
        r.description = description.trim();
        r.photoUri = photoUri;
        r.categorie = a.categorie;
        r.priorite = a.priorite;
        r.confiance = a.confiance;
        r.dateCreation = System.currentTimeMillis();
        s.reclamations.add(r);

        e.etat = EtatEquipement.EN_PANNE;
        for (Utilisateur admin : parRole(Role.ADMINISTRATEUR)) {
            notifier(admin.id, "Nouvelle réclamation", r.numero + " : " + e.code + " · "
                    + e.salle + " (priorité " + r.priorite.label + ").", r.id, e.id);
        }
        save();
        return r;
    }

    /** PUT /api/reclamations/{id}/affectation — réservé à l'administrateur. */
    public synchronized boolean affecter(long reclamationId, long technicienId) {
        Utilisateur u = currentUser();
        Reclamation r = reclamation(reclamationId);
        Utilisateur tech = utilisateur(technicienId);
        if (u == null || u.role != Role.ADMINISTRATEUR || r == null || tech == null) return false;
        if (r.statut == StatutReclamation.RESOLU) return false;

        r.technicienId = tech.id;
        r.dateAffectation = System.currentTimeMillis();
        r.statut = StatutReclamation.EN_COURS;
        Equipement e = equipement(r.equipementId);
        String cible = e == null ? "" : " (" + e.code + " · " + e.salle + ")";
        notifier(tech.id, "Affectation", r.numero + " vous a été affectée" + cible + ".", r.id, r.equipementId);
        notifier(r.demandeurId, "Affectation", r.numero + " a été affectée à " + tech.prenom() + ".", r.id, r.equipementId);
        notifier(r.demandeurId, "Statut", r.numero + " est en cours de traitement.", r.id, r.equipementId);
        save();
        return true;
    }

    // ------------------------------------------------------------- Interventions

    /**
     * POST /api/interventions — un technicien ne traite que les réclamations qui lui
     * sont affectées. Si la panne est résolue, la réclamation est clôturée.
     */
    public synchronized boolean enregistrerIntervention(long reclamationId, String diagnostic,
                                                        String action, double cout,
                                                        boolean resolu, long dateDebut) {
        Utilisateur u = currentUser();
        Reclamation r = reclamation(reclamationId);
        if (u == null || r == null || u.role != Role.TECHNICIEN || r.technicienId != u.id) return false;

        Intervention i = new Intervention();
        i.id = s.nextId++;
        i.reclamationId = r.id;
        i.equipementId = r.equipementId;
        i.technicienId = u.id;
        i.diagnostic = diagnostic.trim();
        i.action = action.trim();
        i.cout = cout;
        i.resolu = resolu;
        i.dateDebut = dateDebut;
        i.dateFin = System.currentTimeMillis();
        s.interventions.add(i);

        Equipement e = equipement(r.equipementId);
        if (resolu) {
            r.statut = StatutReclamation.RESOLU;
            r.dateResolution = i.dateFin;
            if (e != null) e.etat = EtatEquipement.FONCTIONNEL;
            notifier(r.demandeurId, "Résolution", r.numero + " a été résolue.", r.id, r.equipementId);
        } else {
            if (e != null) e.etat = EtatEquipement.EN_MAINTENANCE;
            notifier(r.demandeurId, "Statut", r.numero + " : intervention en cours, pièce ou suite à prévoir.", r.id, r.equipementId);
        }
        save();
        return true;
    }

    public synchronized List<Intervention> interventionsReclamation(long reclamationId) {
        List<Intervention> out = new ArrayList<>();
        for (Intervention i : s.interventions) {
            if (i.reclamationId == reclamationId) out.add(i);
        }
        return out;
    }

    // ------------------------------------------------------------------------ IA

    /** Recommandation Réparer / Surveiller / Remplacer pour un équipement. */
    public synchronized EvaluationRemplacement evaluer(long equipementId) {
        Equipement e = equipement(equipementId);
        long ilYaUnAn = System.currentTimeMillis() - 365 * JOUR;
        int pannes = 0;
        for (Reclamation r : s.reclamations) {
            if (r.equipementId == equipementId && r.dateCreation >= ilYaUnAn) pannes++;
        }
        int interventions = 0;
        double cout = 0;
        for (Intervention i : s.interventions) {
            if (i.equipementId == equipementId) {
                interventions++;
                cout += i.cout;
            }
        }
        return ReplacementAdvisor.evaluer(pannes, interventions, cout,
                e == null ? 0 : e.coutAcquisition);
    }

    // ---------------------------------------------------- Maintenance préventive

    /** Le technicien voit son agenda, les autres rôles voient tout le planning. */
    public synchronized List<MaintenancePreventive> maintenancesFor(Utilisateur u) {
        List<MaintenancePreventive> out = new ArrayList<>();
        if (u == null) return out;
        for (MaintenancePreventive m : s.maintenances) {
            if (u.role != Role.TECHNICIEN || m.technicienId == u.id) out.add(m);
        }
        out.sort((a, b) -> {
            if (a.realisee() != b.realisee()) return a.realisee() ? 1 : -1;
            return Long.compare(a.datePlanifiee, b.datePlanifiee);
        });
        return out;
    }

    public synchronized MaintenancePreventive maintenance(long id) {
        for (MaintenancePreventive m : s.maintenances) {
            if (m.id == id) return m;
        }
        return null;
    }

    /** Clôture une maintenance et planifie la suivante selon la périodicité. */
    public synchronized boolean realiserMaintenance(long id, String rapport) {
        Utilisateur u = currentUser();
        MaintenancePreventive m = maintenance(id);
        if (u == null || m == null || m.realisee() || m.technicienId != u.id) return false;

        m.dateRealisation = System.currentTimeMillis();
        m.rapport = rapport == null ? "" : rapport.trim();
        if (m.periodiciteJours > 0) {
            MaintenancePreventive suivante = new MaintenancePreventive();
            suivante.id = s.nextId++;
            suivante.equipementId = m.equipementId;
            suivante.technicienId = m.technicienId;
            suivante.titre = m.titre;
            suivante.periodiciteJours = m.periodiciteJours;
            suivante.rappelAvantJours = m.rappelAvantJours;
            suivante.datePlanifiee = m.dateRealisation + m.periodiciteJours * JOUR;
            s.maintenances.add(suivante);
        }
        save();
        return true;
    }

    /** Crée les rappels des maintenances qui approchent (une seule fois chacune). */
    private void genererRappels() {
        long now = System.currentTimeMillis();
        boolean modifie = false;
        for (MaintenancePreventive m : s.maintenances) {
            if (m.realisee() || m.rappelEnvoye) continue;
            if (m.datePlanifiee - m.rappelAvantJours * JOUR <= now) {
                Equipement e = equipement(m.equipementId);
                String code = e == null ? "équipement" : e.code;
                String msg = "Entretien " + code + " prévu le " + Formats.date(m.datePlanifiee) + ".";
                notifier(m.technicienId, "Rappel de maintenance", msg, 0, m.equipementId);
                for (Utilisateur admin : parRole(Role.ADMINISTRATEUR)) {
                    notifier(admin.id, "Rappel de maintenance", msg, 0, m.equipementId);
                }
                m.rappelEnvoye = true;
                modifie = true;
            }
        }
        if (modifie) save();
    }

    // ------------------------------------------------------------- Notifications

    public synchronized List<NotificationItem> notificationsFor(Utilisateur u) {
        genererRappels();
        List<NotificationItem> out = new ArrayList<>();
        if (u == null) return out;
        for (NotificationItem n : s.notifications) {
            if (n.utilisateurId == u.id) out.add(n);
        }
        out.sort((a, b) -> Long.compare(b.date, a.date));
        return out;
    }

    public synchronized int nonLues(Utilisateur u) {
        int n = 0;
        for (NotificationItem item : notificationsFor(u)) {
            if (!item.lu) n++;
        }
        return n;
    }

    public synchronized void marquerLue(long notificationId) {
        for (NotificationItem n : s.notifications) {
            if (n.id == notificationId) n.lu = true;
        }
        save();
    }

    private NotificationItem notifier(long utilisateurId, String type, String message,
                                      long reclamationId, long equipementId) {
        NotificationItem n = new NotificationItem();
        n.id = s.nextId++;
        n.utilisateurId = utilisateurId;
        n.type = type;
        n.message = message;
        n.date = System.currentTimeMillis();
        n.reclamationId = reclamationId;
        n.equipementId = equipementId;
        s.notifications.add(n);
        return n;
    }

    // --------------------------------------------------- Données de démonstration

    private void seed() {
        long now = System.currentTimeMillis();

        Utilisateur prof = new Utilisateur(1, "Sami Ben Ali", "sami.benali@isetrades.tn", "prof123", Role.PROFESSEUR, null);
        Utilisateur pers = new Utilisateur(2, "Leila Trabelsi", "leila.trabelsi@isetrades.tn", "pers123", Role.PERSONNEL, null);
        Utilisateur amine = new Utilisateur(3, "Amine Gharbi", "amine.gharbi@isetrades.tn", "tech123", Role.TECHNICIEN, "Informatique");
        Utilisateur karim = new Utilisateur(4, "Karim Mansour", "karim.mansour@isetrades.tn", "tech123", Role.TECHNICIEN, "Électricité et climatisation");
        Utilisateur admin = new Utilisateur(5, "Nadia Jaziri", "admin@isetrades.tn", "admin123", Role.ADMINISTRATEUR, null);
        s.utilisateurs.add(prof);
        s.utilisateurs.add(pers);
        s.utilisateurs.add(amine);
        s.utilisateurs.add(karim);
        s.utilisateurs.add(admin);

        Equipement pc24 = new Equipement(10, "PC-024", "Ordinateur de salle", "PC", "Dell", "OptiPlex 7090", "INV-2024-024", "DL7090-55812", "Salle B12", now - 700 * JOUR, 2400);
        Equipement pc31 = new Equipement(11, "PC-031", "Ordinateur de laboratoire", "PC", "HP", "ProDesk 400 G7", "INV-2023-031", "HP400-77120", "Labo TI2", now - 1000 * JOUR, 2100);
        Equipement vid12 = new Equipement(12, "VID-012", "Vidéoprojecteur", "Vidéoprojecteur", "Epson", "EB-X06", "INV-2022-012", "EP-20458", "Salle B203", now - 1300 * JOUR, 1500);
        Equipement clim6 = new Equipement(13, "CLIM-006", "Climatiseur", "Climatiseur", "LG", "DualCool 18000", "INV-2021-006", "LG18-30977", "Salle B12", now - 1700 * JOUR, 2800);
        Equipement prn1 = new Equipement(14, "PRN-001", "Imprimante", "Imprimante", "HP", "LaserJet Pro M404", "INV-2020-001", "HPLJ-11873", "Administration", now - 2100 * JOUR, 700);
        Equipement tv3 = new Equipement(15, "TV-003", "Télévision", "Télévision", "Samsung", "Crystal UHD 55", "INV-2023-003", "SM55-64120", "Salle de réunion", now - 900 * JOUR, 1900);
        s.equipements.add(pc24);
        s.equipements.add(pc31);
        s.equipements.add(vid12);
        s.equipements.add(clim6);
        s.equipements.add(prn1);
        s.equipements.add(tv3);

        // Imprimante souvent en panne : la somme des réparations dépasse son prix.
        String[] pannes = {"Bourrage papier répété", "Toner non reconnu", "L'imprimante n'imprime plus",
                "Traces noires sur les pages", "Erreur papier au démarrage", "Bourrage papier dans le bac 2"};
        double[] couts = {90, 160, 180, 120, 140, 160};
        for (int k = 0; k < pannes.length; k++) {
            seedRec(20 + k, prn1, pers, pannes[k], now - (300 - k * 50) * JOUR, amine, true,
                    "Usure du mécanisme d'entraînement", "Remplacement de pièces", couts[k]);
        }

        seedRec(35, vid12, prof, "Le vidéoprojecteur affiche une image jaune en HDMI.", now - 9 * JOUR,
                amine, true, "Câble HDMI défectueux", "Remplacement du câble HDMI", 35);

        // Réclamation des maquettes : REC-042, affectée à Amine, en cours.
        Reclamation r42 = seedRec(42, pc24, prof, "L'ordinateur ne démarre plus.", now - 3 * JOUR,
                amine, false, null, null, 0);
        r42.dateAffectation = now - JOUR;
        pc24.etat = EtatEquipement.EN_PANNE;

        seedMaintenance(clim6, karim, "Nettoyage des filtres", 2, 9, 120);
        seedMaintenance(pc24, amine, "Contrôle et nettoyage", 4, 10, 180);
        seedMaintenance(vid12, amine, "Contrôle optique", 7, 14, 180);
        seedMaintenance(prn1, amine, "Nettoyage et contrôle du toner", 20, 9, 90);

        long n = now - JOUR;
        seedNotif(amine.id, "Affectation", "REC-042 vous a été affectée (PC-024 · Salle B12).", n, r42.id, pc24.id);
        seedNotif(prof.id, "Affectation", "REC-042 a été affectée à Amine.", n, r42.id, pc24.id);
        seedNotif(prof.id, "Statut", "REC-042 est en cours de traitement.", n + 1000, r42.id, pc24.id);
        seedNotif(admin.id, "Nouvelle réclamation", "REC-042 : PC-024 · Salle B12 (priorité Élevée).", now - 3 * JOUR, r42.id, pc24.id);
        Reclamation r35 = null;
        for (Reclamation r : s.reclamations) {
            if ("REC-035".equals(r.numero)) r35 = r;
        }
        if (r35 != null) {
            seedNotif(prof.id, "Résolution", "REC-035 a été résolue.", r35.dateResolution, r35.id, vid12.id);
        }
    }

    private Reclamation seedRec(int numero, Equipement e, Utilisateur demandeur, String description,
                                long creation, Utilisateur tech, boolean resolu,
                                String diagnostic, String action, double cout) {
        AnalyseIA a = IncidentClassifier.analyser(description, e.type + " " + e.nom);
        Reclamation r = new Reclamation();
        r.id = s.nextId++;
        r.numero = String.format(Locale.US, "REC-%03d", numero);
        r.equipementId = e.id;
        r.demandeurId = demandeur.id;
        r.description = description;
        r.categorie = a.categorie;
        r.priorite = a.priorite;
        r.confiance = a.confiance;
        r.dateCreation = creation;
        r.technicienId = tech.id;
        r.dateAffectation = creation + JOUR;
        r.statut = resolu ? StatutReclamation.RESOLU : StatutReclamation.EN_COURS;
        s.reclamations.add(r);

        if (resolu) {
            Intervention i = new Intervention();
            i.id = s.nextId++;
            i.reclamationId = r.id;
            i.equipementId = e.id;
            i.technicienId = tech.id;
            i.diagnostic = diagnostic;
            i.action = action;
            i.cout = cout;
            i.resolu = true;
            i.dateDebut = creation + 2 * JOUR;
            i.dateFin = i.dateDebut + HEURE;
            r.dateResolution = i.dateFin;
            s.interventions.add(i);
        }
        return r;
    }

    /** Maintenance planifiée dans « dansJours » jours, à l'heure indiquée. */
    private void seedMaintenance(Equipement e, Utilisateur tech, String titre, int dansJours,
                                 int heure, int periodicite) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dansJours);
        cal.set(Calendar.HOUR_OF_DAY, heure);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        MaintenancePreventive m = new MaintenancePreventive();
        m.id = s.nextId++;
        m.equipementId = e.id;
        m.technicienId = tech.id;
        m.titre = titre;
        m.datePlanifiee = cal.getTimeInMillis();
        m.periodiciteJours = periodicite;
        s.maintenances.add(m);
    }

    private void seedNotif(long utilisateurId, String type, String message, long date,
                           long reclamationId, long equipementId) {
        NotificationItem n = notifier(utilisateurId, type, message, reclamationId, equipementId);
        n.date = date;
    }
}
