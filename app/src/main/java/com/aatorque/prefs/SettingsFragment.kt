package com.aatorque.prefs

import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.aatorque.datastore.UserPreference
import com.aatorque.stats.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections

class SettingsFragment : PreferenceFragmentCompat() {
    lateinit var numScreensPref: EditTextPreference
    lateinit var dashboardsCat: PreferenceCategory
    lateinit var backgroundPref: ImageListPreference
    lateinit var themePref: ImageListPreference
    lateinit var fontPref: ImageListPreference
    lateinit var centerGaugeLargePref: CheckBoxPreference
    lateinit var rotaryInputPref: CheckBoxPreference
    lateinit var minMaxBelowPref: CheckBoxPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferencesName = null
        dashboardsCat = findPreference("dashboardsCat")!!
        numScreensPref = findPreference("dashboardCount")!!
        backgroundPref = findPreference("selectedBackground")!!
        themePref = findPreference("selectedTheme")!!
        fontPref = findPreference("selectedFont")!!
        centerGaugeLargePref = findPreference("centerGaugeLarge")!!
        rotaryInputPref = findPreference("rotaryInput")!!
        minMaxBelowPref = findPreference("minMaxBelow")!!
        themePref.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        fontPref.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        backgroundPref.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        numScreensPref.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        numScreensPref.setOnPreferenceChangeListener {
                _, newValue ->
            val intVal = (newValue as String).toInt()
            if (intVal in 1..10) {
                lifecycleScope.launch {
                    requireContext().dataStore.updateData { currentSettings ->
                        var bldr = currentSettings.toBuilder()
                        if (bldr.screensCount > intVal) {
                            val keeping = bldr.screensList.subList(0, intVal)
                            bldr = bldr.clearScreens().addAllScreens(keeping)
                        } else if (currentSettings.screensCount < intVal) {
                            val newItms = Collections.nCopies(
                                intVal - currentSettings.screensCount,
                                UserPreferenceSerializer.defaultScreen.build()
                            )
                            bldr = bldr.addAllScreens(newItms.toMutableList())
                            Timber.i("${newItms.size} added, ${intVal} specified")
                        }
                        return@updateData bldr.build()
                    }
                }
                return@setOnPreferenceChangeListener true
            }
            return@setOnPreferenceChangeListener false
        }

        themePref.setOnPreferenceChangeListener {
            _, newValue ->
            updateDatastorePref {
                it.setSelectedTheme(newValue as String)
            }
            return@setOnPreferenceChangeListener true
        }
        fontPref.setOnPreferenceChangeListener {
                preference, newValue ->
            updateDatastorePref {
                it.setSelectedFont(newValue as String)
            }
            Timber.i("Setting font $newValue")
            return@setOnPreferenceChangeListener true
        }
        backgroundPref.setOnPreferenceChangeListener {
                preference, newValue ->
            updateDatastorePref {
                it.setSelectedBackground(newValue as String)
            }
            return@setOnPreferenceChangeListener true
        }
        centerGaugeLargePref.setOnPreferenceChangeListener {
                preference, newValue ->
            updateDatastorePref {
                it.setCenterGaugeLarge(newValue as Boolean)
            }
            return@setOnPreferenceChangeListener true
        }
        rotaryInputPref.setOnPreferenceChangeListener {
                preference, newValue ->
            updateDatastorePref {
                it.setRotaryInput(newValue as Boolean)
            }
            return@setOnPreferenceChangeListener true
        }
        minMaxBelowPref.setOnPreferenceChangeListener{
            preference, newValue ->
            updateDatastorePref {
                it.setMinMaxBelow(newValue as Boolean)
            }
            return@setOnPreferenceChangeListener true
        }

        numScreensPref.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }

        lifecycleScope.launch {
            requireContext().dataStore.data.collect {
                themePref.value = it.selectedTheme
                fontPref.value = it.selectedFont
                backgroundPref.value = it.selectedBackground
                centerGaugeLargePref.isChecked = it.centerGaugeLarge
                rotaryInputPref.isChecked = it.rotaryInput
                minMaxBelowPref.isChecked = it.minMaxBelow
            }
        }
        lifecycleScope.launch {
            requireContext().dataStore.data.distinctUntilChangedBy{
                it.screensList.map { it.title }
            }.collect { userPreference ->
                numScreensPref.text = userPreference.screensCount.toString()
                dashboardsCat.removeAll()
                userPreference.screensList.forEachIndexed {
                        i, screen ->
                    dashboardsCat.addPreference(Preference(requireContext()).also {
                        it.title = requireContext().getString(
                            R.string.pref_data_element_settings,
                             i + 1
                        )
                        it.key = "dashboard_$i"
                        it.fragment = SettingsDashboard::class.java.canonicalName
                        it.summary = screen.title
                    })
                }
            }
        }
    }

    private fun updateDatastorePref(updateBuilder: (obj: UserPreference.Builder) -> UserPreference.Builder): Unit {
        GlobalScope.launch(Dispatchers.IO) {
            requireContext().dataStore.updateData { currentSettings ->
                updateBuilder(currentSettings.toBuilder()).build()
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as SettingsActivity).supportActionBar!!.subtitle = null
    }
}