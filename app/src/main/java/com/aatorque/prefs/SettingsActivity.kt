package com.aatorque.prefs

import android.Manifest
import android.accounts.AccountManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.aatorque.stats.App
import com.aatorque.stats.R

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var mCredential: GoogleAccountCredential? = null
    private var mCurrentAuthorizationIntent: Intent? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val app = application as App
        mCredential = app.googleCredential
        handleIntent()
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment, SettingsFragment())
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val intent = intent
        if (intent.hasExtra(EXTRA_AUTHORIZATION_INTENT) && mCurrentAuthorizationIntent == null) {
            mCurrentAuthorizationIntent = intent.getParcelableExtra(EXTRA_AUTHORIZATION_INTENT)
            mCurrentAuthorizationIntent?.let { startActivityForResult(it, REQUEST_AUTHORIZATION) }
        }
    }

    override fun onResume() {
        super.onResume()
        val permissionsToRequest: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(this, PERMISSION_CAR_VENDOR_EXTENSION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(PERMISSION_CAR_VENDOR_EXTENSION)
        }
        if (!permissionsToRequest.isEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS)
            return
        }

        checkLocationPermissions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_AUTHORIZATION -> {
                mCurrentAuthorizationIntent = null
                if (resultCode != RESULT_OK) {
                    chooseAccountIfNeeded()
                }
            }

            REQUEST_ACCOUNT_PICKER -> if (resultCode == RESULT_OK && data != null && data.extras != null) {
                val accountName = data.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    mCredential!!.selectedAccountName = accountName
                    val settings = PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = settings.edit()
                    editor.putString(App.PREF_ACCOUNT_NAME, accountName)
                    editor.apply()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            REQUEST_LOCATION_PERMISSION -> if (grantResults.size == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    applicationContext,
                    R.string.location_permission_denied_toast, Toast.LENGTH_LONG
                ).show()
                val settings = PreferenceManager.getDefaultSharedPreferences(this)
                val editor = settings.edit()
                editor.putBoolean(PREF_LOCATION, false)
                editor.apply()
                editor.commit()
            }
        }
    }



    fun checkLocationPermissions() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean(PREF_LOCATION, false)) {
            val permissionsToRequest: MutableList<String> = ArrayList()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }

    fun chooseAccount() {
        if (!checkAccountsPermission()) {
            return
        }
        startActivityForResult(mCredential!!.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
    }

    fun chooseAccountIfNeeded() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!checkAccountsPermission()) {
            return
        }
        if (mCredential!!.selectedAccountName == null) {
            chooseAccount()
        }
    }

    private fun checkAccountsPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@SettingsActivity, arrayOf(Manifest.permission.GET_ACCOUNTS),
                REQUEST_ACCOUNTS_PERMISSION
            )
            return false
        }
        return true
    }



    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = Bundle()
        args.putCharSequence("title", pref.title)
        args.putString("prefix", pref.key)
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!
        )
        fragment.arguments = args
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.settings_fragment, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }


    companion object {
        private const val TAG = "SettingsActivity"
        private const val REQUEST_PERMISSIONS = 0
        private const val REQUEST_ACCOUNTS_PERMISSION = 1
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 2
        private const val REQUEST_AUTHORIZATION = 3
        private const val REQUEST_ACCOUNT_PICKER = 4
        private const val REQUEST_LOCATION_PERMISSION = 5
        const val EXTRA_AUTHORIZATION_INTENT = "authorizationRequest"
        private const val PERMISSION_CAR_VENDOR_EXTENSION =
            "com.google.android.gms.permission.CAR_VENDOR_EXTENSION"
        const val PREF_LOCATION = "useGoogleGeocoding"
    }
}

