package tn.isetrades.isetcare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats

/** M04 · Signaler une panne : équipement, description, photo facultative, analyse IA. */
class SignalerActivity : AppCompatActivity() {

    private var photoUri: Uri? = null

    private val choisirPhoto =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                // Garde le droit de lecture pour réafficher la photo plus tard.
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (ex: SecurityException) {
                    // La photo reste lisible pendant cette session.
                }
                photoUri = uri
                val photo = findViewById<ImageView>(R.id.photo)
                photo.setImageURI(uri)
                photo.visibility = View.VISIBLE
                findViewById<TextView>(R.id.btnPhoto).text = "Changer la photo"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signaler)
        val store = DataStore.get(this)
        val e = store.equipement(intent.getLongExtra(EXTRA_ID, 0L))
        if (e == null) {
            finish()
            return
        }

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.equipement).text = "${e.code} · ${e.salle}"
        findViewById<View>(R.id.btnPhoto).setOnClickListener {
            choisirPhoto.launch(arrayOf("image/*"))
        }

        val description = findViewById<EditText>(R.id.description)
        findViewById<View>(R.id.btnSend).setOnClickListener {
            val texte = description.text.toString().trim()
            if (texte.length < 5) {
                description.error = "Décrivez le problème constaté."
                return@setOnClickListener
            }
            val r = store.creerReclamation(e.id, texte, photoUri?.toString())
            if (r == null) {
                finish()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(this)
                .setTitle("Analyse IA terminée")
                .setMessage(
                    "Réclamation ${r.numero} enregistrée.\n\n" +
                        "Catégorie : ${r.categorie}\n" +
                        "Priorité : ${r.priorite.label}\n" +
                        "Confiance : ${Formats.pourcent(r.confiance)}"
                )
                .setCancelable(false)
                .setPositiveButton("Suivre la réclamation") { _, _ ->
                    startActivity(
                        Intent(this, ReclamationActivity::class.java).putExtra(EXTRA_ID, r.id)
                    )
                    finish()
                }
                .show()
        }
    }
}
