package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.data.Formats
import tn.isetrades.isetcare.model.Role

/** Suivi : mes réclamations (demandeur), mes missions (technicien) ou tout (admin). */
class SuiviFragment : Fragment(R.layout.fragment_list) {

    private lateinit var store: DataStore
    private lateinit var adapter: RowAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = DataStore.get(requireContext())
        adapter = RowAdapter { row ->
            startActivity(
                Intent(requireContext(), ReclamationActivity::class.java).putExtra(EXTRA_ID, row.id)
            )
        }
        val list = view.findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val view = view ?: return
        val user = store.currentUser() ?: return

        val (titre, sousTitre) = when (user.role) {
            Role.TECHNICIEN -> "Mes missions" to "Réclamations qui vous sont affectées."
            Role.ADMINISTRATEUR -> "Réclamations" to "Touchez une réclamation pour affecter un technicien."
            else -> "Mes réclamations" to "Suivez la prise en charge de vos signalements."
        }
        view.findViewById<TextView>(R.id.listTitle).text = titre
        view.findViewById<TextView>(R.id.listSubtitle).text = sousTitre

        val rows = store.reclamationsFor(user).map { r ->
            val e = store.equipement(r.equipementId)
            val cible = if (e != null) "${e.code} · ${e.salle}" else "Équipement supprimé"
            Row(
                r.id,
                "${r.numero} · $cible",
                "${r.description}\n${Formats.jour(r.dateCreation)} · ${r.categorie} · priorité ${r.priorite.label}",
                r.statut.label,
                r.statut.couleur
            )
        }
        adapter.submit(rows)

        val empty = view.findViewById<TextView>(R.id.empty)
        empty.text = "Aucune réclamation pour le moment."
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }
}
