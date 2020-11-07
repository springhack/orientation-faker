package net.mm2d.orientation.util

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import net.mm2d.android.orientationfaker.R
import net.mm2d.orientation.settings.Settings

object AbTest {
    fun loadAdMobSize() {
        Firebase.remoteConfig.also { config ->
            config.setConfigSettingsAsync(remoteConfigSettings {})
            config.setDefaultsAsync(R.xml.remote_config_defaults)
            config.fetchAndActivate().addOnSuccessListener {
                Settings.get().setAdMobSize(config.getString("ad_mob_size"))
            }
        }
    }
}

enum class AdMobSize {
    SMART_BANNER,
    BANNER,
    LARGE_BANNER,
    MEDIUM_RECTANGLE,
    ;

    companion object {
        fun of(value: String): AdMobSize =
            values().find { it.name == value } ?: SMART_BANNER
    }
}
