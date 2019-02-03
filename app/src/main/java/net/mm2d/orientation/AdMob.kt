/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation

import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import net.mm2d.android.orientationfaker.BuildConfig
import net.mm2d.log.Logger
import java.net.URL

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object AdMob {
    private const val APP_ID = "ca-app-pub-3057634395460859~4653069539"
    private const val UNIT_ID_SETTINGS = "ca-app-pub-3057634395460859/5509364941"
    private const val UNIT_ID_DETAILED = "ca-app-pub-3057634395460859/9578179809"
    private const val PUBLISHER_ID = "pub-3057634395460859"
    private const val PRIVACY_POLICY_URL =
        "https://github.com/ohmae/orientation-faker/blob/develop/PRIVACY-POLICY.md"
    private var checked: Boolean = false
    private var isInEeaOrUnknown: Boolean = false
    private var consentStatus: ConsentStatus? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context, APP_ID)
    }

    fun makeSettingsAdView(context: Context): AdView {
        return AdView(context).apply {
            adSize = AdSize.SMART_BANNER
            adUnitId = UNIT_ID_SETTINGS
        }
    }

    fun makeDetailedAdView(context: Context): AdView {
        return AdView(context).apply {
            adSize = AdSize.SMART_BANNER
            adUnitId = UNIT_ID_DETAILED
        }
    }

    fun loadAd(activity: FragmentActivity, adView: AdView) {
        Handler().post {
            loadAndConfirmConsentState(activity)
                .subscribe { status ->
                    if (activity.isResumed()) {
                        loadAd(adView, status)
                    }
                }
        }
    }

    fun isInEeaOrUnknown(): Boolean {
        return checked && isInEeaOrUnknown
    }

    fun updateConsent(activity: FragmentActivity) {
        showConsentForm(activity)
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

    private fun loadAndConfirmConsentState(activity: FragmentActivity): Single<ConsentStatus> {
        val subject = SingleSubject.create<ConsentStatus>()
        if (notifyOrConfirm(activity, subject)) {
            return subject
        }
        val consentInformation = ConsentInformation.getInstance(activity)
        if (BuildConfig.DEBUG) {
            consentInformation.debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
        }
        consentInformation.requestConsentInfoUpdate(
            arrayOf(PUBLISHER_ID),
            object : ConsentInfoUpdateListener {
                override fun onConsentInfoUpdated(status: ConsentStatus?) {
                    checked = true
                    consentStatus = status
                    isInEeaOrUnknown = consentInformation.isRequestLocationInEeaOrUnknown
                    notifyOrConfirm(activity, subject)
                }

                override fun onFailedToUpdateConsentInfo(reason: String?) {
                    subject.onSuccess(ConsentStatus.UNKNOWN)
                }
            })
        return subject
    }

    private fun notifyOrConfirm(
        activity: FragmentActivity,
        subject: SingleSubject<ConsentStatus>
    ): Boolean {
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
                showConsentForm(activity, subject)
            }
        }
        return true
    }

    private fun showConsentForm(
        activity: FragmentActivity,
        subject: SingleSubject<ConsentStatus>? = null
    ) {
        val privacyUrl = URL(PRIVACY_POLICY_URL)
        var form: ConsentForm? = null
        val listener = object : ConsentFormListener() {
            override fun onConsentFormLoaded() {
                if (activity.isResumed()) {
                    form?.show()
                } else {
                    subject?.onSuccess(ConsentStatus.UNKNOWN)
                }
            }

            override fun onConsentFormOpened() {
            }

            override fun onConsentFormClosed(status: ConsentStatus?, userPrefersAdFree: Boolean?) {
                consentStatus = status
                subject?.onSuccess(status ?: ConsentStatus.UNKNOWN)
            }

            override fun onConsentFormError(errorDescription: String?) {
                Logger.e { "error:$errorDescription" }
                subject?.onSuccess(ConsentStatus.UNKNOWN)
            }
        }
        form = ConsentForm.Builder(activity, privacyUrl)
            .withListener(listener)
            .withPersonalizedAdsOption()
            .withNonPersonalizedAdsOption()
            .build()
        form?.load()
    }

    private fun FragmentActivity.isResumed(): Boolean = lifecycle.currentState == State.RESUMED
}
