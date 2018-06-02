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
    private var checked: Boolean = false
    private var isInEeaOrUnknown: Boolean = false
    private var consentStatus: ConsentStatus? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context, APP_ID)
    }

    fun makeAdView(context: Context): AdView {
        return AdView(context).apply {
            adSize = AdSize.SMART_BANNER
            adUnitId = UNIT_ID_SETTINGS
        }
    }

    fun loadAd(context: Context, adView: AdView) {
        Handler().post {
            loadAndConfirmConsentState(context)
                    .subscribe { it -> loadAd(adView, it) }
        }
    }

    fun isInEeaOrUnknown(): Boolean {
        return checked && isInEeaOrUnknown
    }

    fun updateConsent(context: Context) {
        showConsentForm(context, null)
    }

    private fun loadAd(adView: AdView, consent: ConsentStatus?) {
        when (consent) {
            ConsentStatus.NON_PERSONALIZED -> {
                val request = AdRequest.Builder().build()
                adView.loadAd(request)
            }
            ConsentStatus.PERSONALIZED -> {
                val param = Bundle().apply { putString("npa", "1") }
                val request = AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, param)
                        .build()
                adView.loadAd(request)
            }
            else -> {
            }
        }
    }

    fun loadAndConfirmConsentState(context: Context): Single<ConsentStatus> {
        val subject = SingleSubject.create<ConsentStatus>()
        if (notifyOrConfirm(context, subject)) {
            return subject
        }
        val consentInformation = ConsentInformation.getInstance(context)
        consentInformation.requestConsentInfoUpdate(arrayOf(PUBLISHER_ID), object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(status: ConsentStatus?) {
                checked = true
                consentStatus = status
                isInEeaOrUnknown = consentInformation.isRequestLocationInEeaOrUnknown
                notifyOrConfirm(context, subject)
            }

            override fun onFailedToUpdateConsentInfo(reason: String?) {
                subject.onSuccess(ConsentStatus.UNKNOWN)
            }
        })
        return subject
    }

    private fun notifyOrConfirm(context: Context, subject: SingleSubject<ConsentStatus>): Boolean {
        if (!checked) {
            return false
        }
        if (!isInEeaOrUnknown) {
            subject.onSuccess(ConsentStatus.PERSONALIZED)
            return true
        }
        val status = consentStatus
        when (status) {
            ConsentStatus.NON_PERSONALIZED,
            ConsentStatus.PERSONALIZED -> {
                subject.onSuccess(status)
            }
            ConsentStatus.UNKNOWN,
            null -> {
                showConsentForm(context, subject)
            }
        }
        return true
    }

    private fun showConsentForm(context: Context, subject: SingleSubject<ConsentStatus>?) {
        val privacyUrl = URL(PRIVACY_POLICY_URL)
        var form: ConsentForm? = null
        val listener = object : ConsentFormListener() {
            override fun onConsentFormLoaded() {
                form?.show()
            }

            override fun onConsentFormOpened() {
            }

            override fun onConsentFormClosed(status: ConsentStatus?, userPrefersAdFree: Boolean?) {
                consentStatus = status
                subject?.onSuccess(status ?: ConsentStatus.UNKNOWN)
            }

            override fun onConsentFormError(errorDescription: String?) {
                Log.e("error:$errorDescription")
                subject?.onSuccess(ConsentStatus.UNKNOWN)
            }
        }
        form = ConsentForm.Builder(context, privacyUrl)
                .withListener(listener)
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()
        form?.load()
    }
}
