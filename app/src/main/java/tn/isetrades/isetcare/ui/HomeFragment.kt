package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore
import tn.isetrades.isetcare.model.EtatEquipement
import tn.isetrades.isetcare.model.Role
import tn.isetrades.isetcare.model.StatutReclamation

/** Accueil : indicateurs du rôle connecté et parc des équipements. */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var store: DataStore
    private lateinit var adapter: RowAdapter
    private var filtre = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = DataStore.get(requireContext())
        adapter = RowAdapter { row ->
            startActivity(
                Intent(requireContext(), EquipementActivity::class.java).putExtra(EXTRA_ID, row.id)
            )
        }
        val list = view.findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        view.findViewById<EditText>(R.id.search).doAfterTextChanged { text ->
            filtre = text?.toString().orEmpty()
            afficherEquipements()
        }
    }

    override fun onResume() {
        super.onResume()
        val view = view ?: return
        val user = store.currentUser() ?: return

        view.findViewById<TextView>(R.id.homeTitle).text = "Bonjour ${user.prenom()}"
        view.findViewById<TextView>(R.id.homeRole).text = when (user.role) {
            Role.TECHNICIEN -> "Technicien · ${user.specialite}"
            Role.ADMINISTRATEUR -> "Administrateur · vue globale du parc"
            else -> "${user.role.label} · scannez un équipement pour signaler une panne"
        }

        val recs = store.reclamationsFor(user)
        if (user.role == Role.ADMINISTRATEUR) {
            val equipements = store.equipements()
            stat(R.id.stat1, R.id.stat1Label, equipements.size, "Équipements")
            stat(R.id.stat2, R.id.stat2Label,
                equipements.count { it.etat != EtatEquipement.FONCTIONNEL }, "En panne")
            stat(R.id.stat3, R.id.stat3Label,
                recs.count { it.statut != StatutReclamation.RESOLU }, "Réclam. ouvertes")
        } else {
            val titre = if (user.role == Role.TECHNICIEN) "Missions" else "Réclamations"
            stat(R.id.stat1, R.id.stat1Label, recs.size, titre)
            stat(R.id.stat2, R.id.stat2Label,
                recs.count { it.statut != StatutReclamation.RESOLU }, "En cours")
            stat(R.id.stat3, R.id.stat3Label,
                recs.count { it.statut == StatutReclamation.RESOLU }, "Résolues")
        }
        afficherEquipements()
    }

    private fun stat(valueId: Int, labelId: Int, value: Int, label: String) {
        val view = view ?: return
        view.findViewById<TextView>(valueId).text = value.toString()
        view.findViewById<TextView>(labelId).text = label
    }

    private fun afficherEquipements() {
        val q = filtre.trim().lowercase()
        val rows = store.equipements()
            .filter { e ->
                q.isEmpty() || listOf(e.code, e.nom, e.type, e.marque, e.modele, e.salle)
                    .any { it.lowercase().contains(q) }
            }
            .map { e ->
                Row(e.id, "${e.code} · ${e.nom}", "${e.marque} ${e.modele} · ${e.salle}",
                    e.etat.label, e.etat.couleur)
            }
        adapter.submit(rows)
    }
}
