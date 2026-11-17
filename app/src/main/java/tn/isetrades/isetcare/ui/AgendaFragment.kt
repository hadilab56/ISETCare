package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats
import tn.isetrades.isetcare.model.Role

/** M08 · Mon agenda : maintenances préventives planifiées et rappels. */
class AgendaFragment : Fragment(R.layout.fragment_list) {

    private lateinit var store: DataStore
    private lateinit var adapter: RowAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = DataStore.get(requireContext())
        adapter = RowAdapter { row -> ouvrir(row.id) }
        val list = view.findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        rafraichir()
    }

    private fun rafraichir() {
        val view = view ?: return
        val user = store.currentUser() ?: return
        val technicien = user.role == Role.TECHNICIEN

        view.findViewById<TextView>(R.id.listTitle).text =
            if (technicien) "Mon agenda" else "Agenda de maintenance"
        view.findViewById<TextView>(R.id.listSubtitle).text =
            "Maintenances à venir. Les rappels précèdent les entretiens."

        val now = System.currentTimeMillis()
        val rows = store.maintenancesFor(user).map { m ->
            val e = store.equipement(m.equipementId)
            val tech = store.utilisateur(m.technicienId)
            val detail = buildString {
                append("${e?.code ?: "?"} · ${m.titre}")
                if (e != null) append("\n${e.salle}")
                if (!technicien && tech != null) append(" · ${tech.prenom()}")
                if (m.periodiciteJours > 0) append(" · tous les ${m.periodiciteJours} j")
            }
            val (badge, couleur) = when {
                m.realisee() -> "Réalisée" to 0xFF1B7F6B.toInt()
                m.datePlanifiee < now -> "En retard" to 0xFFD64545.toInt()
                else -> "Planifiée" to 0xFF2F6FED.toInt()
            }
            Row(m.id, Formats.jourHeure(m.datePlanifiee), detail, badge, couleur)
        }
        adapter.submit(rows)

        val empty = view.findViewById<TextView>(R.id.empty)
        empty.text = "Aucune maintenance planifiée."
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Le technicien concerné clôture la maintenance ; les autres ouvrent la fiche. */
    private fun ouvrir(maintenanceId: Long) {
        val m = store.maintenance(maintenanceId) ?: return
        val user = store.currentUser() ?: return

        if (user.role == Role.TECHNICIEN && m.technicienId == user.id && !m.realisee()) {
            val rapport = EditText(requireContext())
            rapport.hint = "Rapport de maintenance"
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(m.titre)
                .setMessage("Marquer cette maintenance comme réalisée ?")
                .setView(rapport)
                .setPositiveButton("Réalisée") { _, _ ->
                    store.realiserMaintenance(m.id, rapport.text.toString())
                    rafraichir()
                }
                .setNeutralButton("Voir l'équipement") { _, _ -> ouvrirFiche(m.equipementId) }
                .setNegativeButton("Annuler", null)
                .show()
        } else {
            ouvrirFiche(m.equipementId)
        }
    }

    private fun ouvrirFiche(equipementId: Long) {
        startActivity(
            Intent(requireContext(), EquipementActivity::class.java).putExtra(EXTRA_ID, equipementId)
        )
    }
}
