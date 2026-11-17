package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats

/** M06 · Notifications : affectation, statut, résolution et rappels de maintenance. */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var store: DataStore
    private lateinit var adapter: RowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        store = DataStore.get(this)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.listTitle).text = "Notifications"
        findViewById<TextView>(R.id.listSubtitle).text =
            "Affectation, changement de statut, résolution et rappels préventifs."

        adapter = RowAdapter { row -> ouvrir(row.id) }
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val user = store.currentUser()
        if (user == null) {
            finish()
            return
        }
        val rows = store.notificationsFor(user).map { n ->
            Row(
                n.id,
                "${n.type} · ${Formats.jour(n.date)}",
                n.message,
                if (n.lu) "" else "Nouveau",
                0xFF2F6FED.toInt()
            )
        }
        adapter.submit(rows)

        val empty = findViewById<TextView>(R.id.empty)
        empty.text = "Aucune notification."
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Ouvre la réclamation liée, ou la fiche de l'équipement pour un rappel. */
    private fun ouvrir(notificationId: Long) {
        val user = store.currentUser() ?: return
        val n = store.notificationsFor(user).firstOrNull { it.id == notificationId } ?: return
        store.marquerLue(n.id)
        if (n.reclamationId > 0) {
            startActivity(
                Intent(this, ReclamationActivity::class.java).putExtra(EXTRA_ID, n.reclamationId)
            )
        } else if (n.equipementId > 0) {
            startActivity(
                Intent(this, EquipementActivity::class.java).putExtra(EXTRA_ID, n.equipementId)
            )
        }
    }
}
