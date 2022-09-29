package app.fadlyproject.com

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import app.fadlyproject.com.R
import android.view.WindowManager
import androidx.core.content.ContextCompat
import android.content.Intent
import android.widget.Button
import app.fadlyproject.com.MainActivity

class OnboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboard)


        val window = this.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_200)
        val next = findViewById<Button>(R.id.next)
        next.setOnClickListener {
            startActivity(
                Intent(
                    this@OnboardActivity,
                    MainActivity::class.java
                )
            )
        }
    }
}