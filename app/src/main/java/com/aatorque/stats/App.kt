package com.aatorque.stats

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.bigquery.BigqueryScopes
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import org.acra.config.mailSender

@HiltAndroidApp
class App : Application() {
    var googleCredential: GoogleAccountCredential? = null
        private set
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        /**
         * Logging level for HTTP requests/responses.
         *
         *
         *
         * To turn on, set to [Level.CONFIG] or [Level.ALL] and run this from command line:
         *
         *
         * <pre>
         * adb shell setprop log.tag.HttpTransport DEBUG
        </pre> *
         */
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
    }

    override fun attachBaseContext(base:Context) {
        super.attachBaseContext(base)

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            toast {
                text = "App crashed. Tap to report."
            }
            mailSender {
                //required
                mailTo = "zgronick+zcrz@gmzil.com".replace("z", "a")
                //defaults to true
                reportAsFile = false
                //defaults to ACRA-report.stacktrace
                reportFileName = "Crash.txt"
            }
        }
    }

    companion object {
        const val PREF_ACCOUNT_NAME = "accountName"
        private fun getStackTrace(ex: Throwable): String {
            val sw = StringWriter()
            ex.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }
}