package tn.isetrades.isetcare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats
import tn.isetrades.isetcare.model.Reclamation
import tn.isetrades.isetcare.model.Role
import tn.isetrades.isetcare.model.StatutReclamation

/**
 * M05 · Ma réclamation : analyse IA et suivi Nouveau -> En cours -> Résolu.
 * L'administrateur y affecte un technicien, le technicien y ouvre son intervention.
 */
class ReclamationActivity : AppCompatActivity() {

    private lateinit var store: DataStore
    private var reclamationId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        store = DataStore.get(this)
        reclamationId = intent.getLongExtra(EXTRA_ID, 0L)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        afficher()
    }

    private fun afficher() {
        val r = store.reclamation(reclamationId)
        val user = store.currentUser()
        if (r == null || user == null) {
            finish()
            return
        }
        val e = store.equipement(r.equipementId)
        val tech = store.utilisateur(r.technicienId)
        val demandeur = store.utilisateur(r.demandeurId)

        findViewById<TextView>(R.id.title).text =
            if (user.role.isDemandeur()) "Ma réclamation" else "Réclamation"
        findViewById<TextView>(R.id.subtitle).text = when (r.statut) {
            StatutReclamation.RESOLU -> "Statut actuel : Résolu."
            StatutReclamation.EN_COURS -> "Statut actuel : En cours. Vous serez informé de la résolution."
            else -> "Statut actuel : Nouveau. En attente d'affectation à un technicien."
        }

        val fields = findViewById<LinearLayout>(R.id.fields)
        fields.removeAllViews()
        fields.addField(r.numero, "${e?.code ?: "?"} · ${r.description}")
        if (e != null) fields.addField("Équipement · Localisation", "${e.nom} · ${e.salle}")
        if (!user.role.isDemandeur() && demandeur != null) {
            fields.addField("Déclarée par", "${demandeur.nom} · ${demandeur.role.label}")
        }
        fields.addField(
            "Analyse IA",
            "Catégorie : ${r.categorie}\nPriorité : ${r.priorite.label} · " +
                "Confiance : ${Formats.pourcent(r.confiance)}",
            r.priorite.couleur
        )

        // Suivi chronologique
        fields.addField("${Formats.jour(r.dateCreation)} · Nouveau", "Réclamation enregistrée")
        if (tech != null) {
            fields.addField(
                "${Formats.jour(r.dateAffectation)} · En cours",
                "Technicien affecté : ${tech.prenom()}"
            )
        }
        for (i in store.interventionsReclamation(r.id)) {
            fields.addField(
                "${Formats.jour(i.dateFin)} · Intervention",
                "Diagnostic : ${i.diagnostic}\nAction : ${i.action}\nCoût : ${Formats.dt(i.cout)}"
            )
        }
        if (r.statut == StatutReclamation.RESOLU) {
            fields.addField("Résolu", "Clôturée le ${Formats.date(r.dateResolution)}",
                StatutReclamation.RESOLU.couleur)
        } else {
            fields.addField("Résolu", "En attente de clôture")
        }

        afficherPhoto(r)

        // Action principale selon le rôle (règles métier)
        val principal = findViewById<MaterialButton>(R.id.btnPrimary)
        principal.visibility = View.GONE
        if (r.statut != StatutReclamation.RESOLU) {
            if (user.role == Role.ADMINISTRATEUR) {
                principal.text = if (tech == null) "Affecter un technicien" else "Changer de technicien"
                principal.visibility = View.VISIBLE
                principal.setOnClickListener { choisirTechnicien(r) }
            } else if (user.role == Role.TECHNICIEN && r.technicienId == user.id) {
                principal.text = "Saisir mon intervention"
                principal.visibility = View.VISIBLE
                principal.setOnClickListener {
                    startActivity(
                        Intent(this, InterventionActivity::class.java).putExtra(EXTRA_ID, r.id)
                    )
                }
            }
        }

        val fiche = findViewById<MaterialButton>(R.id.btnSecondary)
        if (e != null) {
            fiche.text = "Voir l'équipement"
            fiche.visibility = View.VISIBLE
            fiche.setOnClickListener {
                startActivity(Intent(this, EquipementActivity::class.java).putExtra(EXTRA_ID, e.id))
            }
        } else {
            fiche.visibility = View.GONE
        }
    }

    private fun afficherPhoto(r: Reclamation) {
        val label = findViewById<TextView>(R.id.imageLabel)
        val image = findViewById<ImageView>(R.id.image)
        label.visibility = View.GONE
        image.visibility = View.GONE
        val uri = r.photoUri ?: return
        try {
            image.setImageURI(Uri.parse(uri))
            if (image.drawable != null) {
                label.text = "Photo jointe"
                label.visibility = View.VISIBLE
                image.visibility = View.VISIBLE
            }
        } catch (ex: Exception) {
            // Photo devenue inaccessible : on n'affiche rien.
        }
    }

    private fun choisirTechnicien(r: Reclamation) {
        val techniciens = store.techniciens()
        val noms = techniciens.map { "${it.nom} · ${it.specialite}" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Affecter ${r.numero}")
            .setItems(noms) { _, index ->
                if (store.affecter(r.id, techniciens[index].id)) {
                    Toast.makeText(this, "Technicien affecté et notifié.", Toast.LENGTH_SHORT).show()
                }
                afficher()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
