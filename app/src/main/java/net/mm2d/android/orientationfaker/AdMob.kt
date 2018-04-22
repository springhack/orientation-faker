/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.android.orientationfaker

import android.content.Context
import android.os.Handler
import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object AdMob {
    private const val APP_ID = "ca-app-pub-3057634395460859~4653069539"
    private const val UNIT_ID_SETTINGS = "ca-app-pub-3057634395460859/5509364941"

    fun initialize(context: Context) {
        MobileAds.initialize(context, APP_ID)
    }

    fun makeAdView(context: Context): View {
        val adView = AdView(context).apply {
            adSize = AdSize.SMART_BANNER
            adUnitId = AdMob.UNIT_ID_SETTINGS
        }
        Handler().post {
            adView.loadAd(AdRequest.Builder().build())
        }
        return adView
    }
}
