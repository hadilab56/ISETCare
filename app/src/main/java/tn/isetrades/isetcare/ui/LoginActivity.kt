package tn.isetrades.isetcare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import tn.isetrades.isetcare.R
import tn.isetrades.isetcare.data.DataStore

/** M01 · Connexion */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = DataStore.get(this)
        if (store.currentUser() != null) {
            openMain()
            return
        }
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val error = findViewById<TextView>(R.id.error)

        findViewById<View>(R.id.btnLogin).setOnClickListener {
            val user = store.login(email.text.toString(), password.text.toString())
            if (user == null) {
                error.text = "Adresse e-mail ou mot de passe incorrect."
                error.visibility = View.VISIBLE
            } else {
                openMain()
            }
        }

        // Comptes de démonstration : un par rôle.
        val demo = findViewById<LinearLayout>(R.id.demoAccounts)
        val comptes = listOf(
            Triple("Professeur", "sami.benali@isetrades.tn", "prof123"),
            Triple("Personnel", "leila.trabelsi@isetrades.tn", "pers123"),
            Triple("Technicien", "amine.gharbi@isetrades.tn", "tech123"),
            Triple("Administrateur", "admin@isetrades.tn", "admin123")
        )
        for ((role, mail, mdp) in comptes) {
            demo.addField(role, "$mail  ·  $mdp")
            demo.getChildAt(demo.childCount - 1).setOnClickListener {
                email.setText(mail)
                password.setText(mdp)
                error.visibility = View.GONE
            }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
