package tn.isetrades.isetcare.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tn.isetrades.isetcare.R

/** Clé d'Intent : identifiant de l'équipement ou de la réclamation à afficher. */
const val EXTRA_ID = "id"

/** Ajoute un bloc « libellé + valeur » comme sur les maquettes. */
fun ViewGroup.addField(label: String, value: String, valueColor: Int? = null) {
    val v = LayoutInflater.from(context).inflate(R.layout.item_field, this, false)
    v.findViewById<TextView>(R.id.fieldLabel).text = label
    val tv = v.findViewById<TextView>(R.id.fieldValue)
    tv.text = value
    if (valueColor != null) tv.setTextColor(valueColor)
    addView(v)
}

/** Une ligne de liste : titre, sous-titre et pastille colorée facultative. */
data class Row(
    val id: Long,
    val title: String,
    val subtitle: String,
    val badge: String = "",
    val badgeColor: Int = 0
)

class RowAdapter(private val onClick: (Row) -> Unit) : RecyclerView.Adapter<RowAdapter.VH>() {

    private var rows: List<Row> = emptyList()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.rowTitle)
        val subtitle: TextView = view.findViewById(R.id.rowSubtitle)
        val badge: TextView = view.findViewById(R.id.rowBadge)
    }

    fun submit(newRows: List<Row>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.title.text = row.title
        holder.subtitle.text = row.subtitle
        if (row.badge.isEmpty()) {
            holder.badge.visibility = View.GONE
        } else {
            holder.badge.visibility = View.VISIBLE
            holder.badge.text = row.badge
            holder.badge.background.mutate().setTint(row.badgeColor)
        }
        holder.itemView.setOnClickListener { onClick(row) }
    }
}
