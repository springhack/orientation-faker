/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.android.orientationfaker

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import net.mm2d.log.Log
import java.net.URL


/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object AdMob {
    private const val APP_ID = "ca-app-pub-3057634395460859~4653069539"
    private const val UNIT_ID_SETTINGS = "ca-app-pub-3057634395460859/5509364941"
    private const val PUBLISHER_ID = "pub-3057634395460859"
    private const val PRIVACY_POLICY_URL = "https://github.com/ohmae/OrientationFaker/blob/develop/PRIVACY-POLICY.md"

    fun initialize(context: Context) {
        MobileAds.initialize(context, APP_ID)
    }

    fun makeAdView(context: Context): View {
        val adView = AdView(context).apply {
            adSize = AdSize.SMART_BANNER
            adUnitId = UNIT_ID_SETTINGS
        }
        Handler().post {
            checkConsent(context)
                    .subscribe { it -> loadAd(adView, it) }
        }
        return adView
    }

    private fun loadAd(adView: AdView, consent: ConsentStatus?) {
        when (consent) {
            ConsentStatus.NON_PERSONALIZED -> {
                val request = AdRequest.Builder().build()
                adView.loadAd(request)
            }
            ConsentStatus.PERSONALIZED -> {
                val request = AdRequest.Builder()
                        .addNetworkExtrasBundle(
                                AdMobAdapter::class.java,
                                Bundle().apply { putString("npa", "1") })
                        .build()
                adView.loadAd(request)
            }
            else -> {
            }
        }
    }

    private fun checkConsent(context: Context): Single<ConsentStatus> {
        val consentInformation = ConsentInformation.getInstance(context)
        if (!consentInformation.isRequestLocationInEeaOrUnknown) {
            return Single.just(ConsentStatus.PERSONALIZED)
        }
        val subject = SingleSubject.create<ConsentStatus>()
        consentInformation
                .requestConsentInfoUpdate(arrayOf(PUBLISHER_ID), object : ConsentInfoUpdateListener {
                    override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
                        when (consentStatus) {
                            ConsentStatus.NON_PERSONALIZED, ConsentStatus.PERSONALIZED
                            -> subject.onSuccess(consentStatus)
                            ConsentStatus.UNKNOWN, null
                            -> showConsentForm(context, subject)
                        }
                    }

                    override fun onFailedToUpdateConsentInfo(reason: String?) {
                        subject.onSuccess(ConsentStatus.UNKNOWN)
                    }
                })
        return subject
    }

    private fun showConsentForm(context: Context, subject: SingleSubject<ConsentStatus>) {
        val privacyUrl = URL(PRIVACY_POLICY_URL)
        var form: ConsentForm? = null
        form = ConsentForm.Builder(context, privacyUrl)
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormLoaded() {
                        form?.show()
                    }

                    override fun onConsentFormOpened() {
                    }

                    override fun onConsentFormClosed(
                            consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
                        subject.onSuccess(consentStatus ?: ConsentStatus.UNKNOWN)
                    }

                    override fun onConsentFormError(errorDescription: String?) {
                        Log.e("error:$errorDescription")
                        subject.onSuccess(ConsentStatus.UNKNOWN)
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()
        form?.load()
    }
}
