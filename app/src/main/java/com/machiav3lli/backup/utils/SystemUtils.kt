package com.machiav3lli.backup.utils

import android.content.Context
import android.content.pm.PackageManager
import com.machiav3lli.backup.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


object SystemUtils {

    fun Context.getApplicationIssuer() : String? {
        runCatching {
            val packageManager: PackageManager = getPackageManager()
            val packageName = BuildConfig.APPLICATION_ID

            val packageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = packageInfo.signatures
            val signature = signatures[0]
            val signatureBytes = signature.toByteArray()
            val cf = CertificateFactory.getInstance("X509")
            val x509Certificate: X509Certificate =
                cf.generateCertificate(ByteArrayInputStream(signatureBytes)) as X509Certificate
            var DN = x509Certificate.getIssuerDN().getName()
            val names = DN.split(",").map {
                val (field, value) = it.split("=", limit = 2)
                field to value
            }.toMap()
            var issuer = names["CN"]
            names["O"]?.let { if (issuer != it) issuer = "$issuer / $it"}
            return issuer ?: DN
        }
        return null
    }

    val numCores = Runtime.getRuntime().availableProcessors()

    suspend fun <T> runParallel(
        items: List<T>,
        scope: CoroutineScope = MainScope(),
        pool: CoroutineDispatcher = Dispatchers.IO,
        todo: (item: T) -> Unit
    ) {
        val list = items.toList()
        when (1) {

            // best,  8 threads, may hang with recursion
            0 -> list.stream().parallel().forEach { todo(it) }

            // slow,  7 threads with IO, most used once, one used 900 times
            0 -> runBlocking { list.asFlow().onEach { todo(it) }.flowOn(pool).collect {} }

            // slow,  1 thread with IO
            0 -> list.asFlow().onEach { todo(it) }.collect {}

            // slow, 19 threads with IO
            0 -> list.asFlow().map { scope.launch(pool) { todo(it) } }.collect { it.join() }

            // best, 63 threads with IO
            0 -> runBlocking { list.asFlow().collect { launch(pool) { todo(it) } } }

            // best, 66 threads with IO
            0 -> list.map { scope.launch(pool) { todo(it) } }.joinAll()

            // best, 63 threads with IO
            1 -> runBlocking { list.forEach { launch(pool) { todo(it) } } }
        }
    }

}
