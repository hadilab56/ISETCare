package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore

/** M02 · Scanner un QR Code (ou saisir le code à la main, utile sur émulateur). */
class ScanFragment : Fragment(R.layout.fragment_scan) {

    private lateinit var store: DataStore
    private lateinit var status: TextView
    private lateinit var code: EditText

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val contenu = result.contents
        if (contenu != null) {
            code.setText(contenu.trim())
            val e = store.findEquipementByCode(contenu)
            status.text = if (e != null) "${e.code} reconnu" else "QR Code inconnu : $contenu"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = DataStore.get(requireContext())
        status = view.findViewById(R.id.scanStatus)
        code = view.findViewById(R.id.code)

        val lancerScan = View.OnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Placez le QR Code de l'équipement dans le cadre")
            options.setBeepEnabled(false)
            options.setOrientationLocked(true)
            scanner.launch(options)
        }
        view.findViewById<View>(R.id.qrPanel).setOnClickListener(lancerScan)
        view.findViewById<View>(R.id.btnScan).setOnClickListener(lancerScan)

        view.findViewById<View>(R.id.btnOpen).setOnClickListener {
            val e = store.findEquipementByCode(code.text.toString())
            if (e == null) {
                Toast.makeText(requireContext(), "Équipement introuvable.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(requireContext(), EquipementActivity::class.java).putExtra(EXTRA_ID, e.id)
                )
            }
        }
    }
}
