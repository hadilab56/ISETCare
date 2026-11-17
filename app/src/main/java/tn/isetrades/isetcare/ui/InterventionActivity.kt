package tn.isetrades.isetcare.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats

/** M07 · Mon intervention : diagnostic, action, résultat et coût. */
class InterventionActivity : AppCompatActivity() {

    private var debut = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intervention)
        val store = DataStore.get(this)
        val r = store.reclamation(intent.getLongExtra(EXTRA_ID, 0L))
        if (r == null) {
            finish()
            return
        }
        val e = store.equipement(r.equipementId)
        debut = savedInstanceState?.getLong("debut") ?: System.currentTimeMillis()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.mission).text =
            "${r.numero} · ${e?.code ?: "?"} · ${e?.salle ?: ""}\n${r.description}"
        findViewById<TextView>(R.id.debut).text = Formats.heureDate(debut)

        val diagnostic = findViewById<EditText>(R.id.diagnostic)
        val action = findViewById<EditText>(R.id.action)
        val cout = findViewById<EditText>(R.id.cout)
        val resolu = findViewById<SwitchMaterial>(R.id.resolu)

        findViewById<View>(R.id.btnSave).setOnClickListener {
            if (diagnostic.text.isBlank()) {
                diagnostic.error = "Diagnostic obligatoire."
                return@setOnClickListener
            }
            if (action.text.isBlank()) {
                action.error = "Action obligatoire."
                return@setOnClickListener
            }
            val montant = cout.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
            val ok = store.enregistrerIntervention(
                r.id, diagnostic.text.toString(), action.text.toString(),
                montant, resolu.isChecked, debut
            )
            val message = if (ok) "Intervention enregistrée." else "Cette réclamation ne vous est pas affectée."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("debut", debut)
    }
}
