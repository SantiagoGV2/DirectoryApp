package com.example.directoriodigital


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.directoriodigital.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inflar y establecer la vista usando View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Usar la toolbar directamente desde 'binding' y establecerla como ActionBar
        setSupportActionBar(binding.toolbar)

        // 3. Configurar NavController de la manera estándar
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Define las pantallas de nivel superior que no mostrarán una flecha de "atrás"
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_qr, R.id.navigation_folders
            )
        )

        // 4. Conectar el ActionBar con el NavController (esto maneja el título y el botón "atrás")
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 5. Conectar el BottomNavigationView con el NavController
        navView.setupWithNavController(navController)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}