package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats

/** M03 · Fiche équipement : identité, état, QR Code, historique et avis de l'IA. */
class EquipementActivity : AppCompatActivity() {

    private lateinit var store: DataStore
    private var equipementId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        store = DataStore.get(this)
        equipementId = intent.getLongExtra(EXTRA_ID, 0L)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        afficher()
    }

    private fun afficher() {
        val e = store.equipement(equipementId)
        val user = store.currentUser()
        if (e == null || user == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.title).text = "Fiche équipement"
        findViewById<TextView>(R.id.subtitle).text =
            "${e.code} · équipement identifié par son QR Code."

        val fields = findViewById<LinearLayout>(R.id.fields)
        fields.removeAllViews()
        fields.addField("Nom · Type", "${e.nom} · ${e.type}")
        fields.addField("Marque · Modèle", "${e.marque} · ${e.modele}")
        fields.addField("Inventaire · Localisation", "${e.inventaire} · ${e.salle}")
        fields.addField("N° de série", e.numeroSerie)
        fields.addField("État", e.etat.label, e.etat.couleur)
        fields.addField(
            "Acquisition · Coût de remplacement",
            "${Formats.date(e.dateAcquisition)} · ${Formats.dt(e.coutAcquisition)}"
        )

        // Historique et aide à la décision
        val ev = store.evaluer(e.id)
        fields.addField(
            "Historique",
            "${ev.pannes12Mois} panne(s) sur 12 mois · ${ev.interventions} intervention(s)\n" +
                "Coût cumulé des réparations : ${Formats.dt(ev.coutCumule)}"
        )
        fields.addField(
            "Recommandation de l'IA",
            "${ev.decision.label} — ${ev.motif}",
            ev.decision.couleur
        )
        for (r in store.reclamationsEquipement(e.id).take(5)) {
            fields.addField(
                "${r.numero} · ${Formats.date(r.dateCreation)} · ${r.statut.label}",
                r.description
            )
        }

        // QR Code à imprimer et coller sur l'équipement
        val label = findViewById<TextView>(R.id.imageLabel)
        val image = findViewById<ImageView>(R.id.image)
        try {
            image.setImageBitmap(BarcodeEncoder().encodeBitmap(e.code, BarcodeFormat.QR_CODE, 400, 400))
            label.text = "QR Code de l'équipement"
            label.visibility = View.VISIBLE
            image.visibility = View.VISIBLE
        } catch (ex: Exception) {
            label.visibility = View.GONE
            image.visibility = View.GONE
        }

        // Seuls le professeur et le personnel déclarent une panne.
        val signaler = findViewById<MaterialButton>(R.id.btnPrimary)
        if (user.role.isDemandeur()) {
            signaler.text = "Signaler une panne"
            signaler.visibility = View.VISIBLE
            signaler.setOnClickListener {
                startActivity(Intent(this, SignalerActivity::class.java).putExtra(EXTRA_ID, e.id))
            }
        } else {
            signaler.visibility = View.GONE
        }
    }
}
