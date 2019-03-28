package com.quizplanner.quizPlanner.exchange

import android.content.Context
import com.quizplanner.quizPlanner.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

class CustomTrust(context: Context, logInterceptor: HttpLoggingInterceptor?) {
    val client: OkHttpClient

    init {

        val keyStore = KeyStore.getInstance("BKS")

        val input = context.resources.openRawResource(R.raw.mykst)
        input.use { it ->
            keyStore.load(it, context.getString(R.string.mystore_password).toCharArray())
        }

        val trustManager = AdditionalKeyStoresTrustManager(arrayOf(keyStore))

        val sslSocketFactory: SSLSocketFactory
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), null)
            sslSocketFactory = sslContext.socketFactory
        } catch (ex: GeneralSecurityException) {
            throw RuntimeException(ex)
        }

        val builder = OkHttpClient.Builder()
                //  .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
                .sslSocketFactory(sslSocketFactory, trustManager)
        builder.hostnameVerifier { hostname, session ->
            val hv = HttpsURLConnection.getDefaultHostnameVerifier()
            hv.verify(context.getString(R.string.api_url), session)
        }
        if (logInterceptor != null) {
            builder.addInterceptor(logInterceptor)
        }

        client = builder.build()
    }
}

//---------------------------------------------------------------------------------------------------


class AdditionalKeyStoresTrustManager(keyStores: Array<KeyStore>) : X509TrustManager {

    private var x509TrustManagers = ArrayList<X509TrustManager>()

    init {
        val factories = ArrayList<TrustManagerFactory>()
        try {
            val original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            original.init(null as KeyStore?)
            factories.add(original)

            for (keyStore in keyStores) {
                val additionalCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                additionalCerts.init(keyStore)
                factories.add(additionalCerts)
            }

        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        for (tmf in factories) {
            for (tm in tmf.trustManagers) {
                if (tm is X509TrustManager) {
                    x509TrustManagers.add(tm)
                }
            }
        }

        if (x509TrustManagers.isEmpty()) {
            throw RuntimeException("Couldn't find any X509TrustManagers")
        }

    }

    /*
     Delegate to the default trust manager.
    */
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        val defaultX509TrustManager = x509TrustManagers[0]
        defaultX509TrustManager.checkClientTrusted(chain, authType)
    }

    /*
      Loop over the trustmanagers until we find one that accepts our server
    */
    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        for (tm in x509TrustManagers) {
            try {
                tm.checkServerTrusted(chain, authType)
                return
            } catch (e: CertificateException) {
                // ignore
            }

        }
        throw CertificateException()
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val list = ArrayList<X509Certificate>()
        for (tm in x509TrustManagers) {
            list.addAll(tm.acceptedIssuers)
        }
        return list.toArray(arrayOfNulls(list.size))
    }
}