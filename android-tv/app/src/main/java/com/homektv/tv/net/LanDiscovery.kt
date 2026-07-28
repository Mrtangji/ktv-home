package com.homektv.tv.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

class LanDiscovery(context: Context) {
    enum class Stage { SAVED, MDNS, UDP, SUBNET }

    private val appContext = context.applicationContext
    private val scanner = LanScanner()

    suspend fun discover(
        savedHost: String? = null,
        onStage: ((Stage) -> Unit)? = null,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): String? {
        if (!savedHost.isNullOrBlank()) {
            onStage?.invoke(Stage.SAVED)
            if (scanner.validate(savedHost)) return savedHost
        }
        onStage?.invoke(Stage.MDNS)
        discoverMdns()?.let { return it }
        onStage?.invoke(Stage.UDP)
        discoverUdp()?.let { return it }
        onStage?.invoke(Stage.SUBNET)
        return scanner.scan(onProgress)
    }

    private suspend fun discoverMdns(): String? {
        val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifi?.createMulticastLock("home-ktv-discovery")?.apply {
            setReferenceCounted(false)
            acquire()
        }
        val result = CompletableDeferred<String?>()
        val scope = CoroutineScope(Dispatchers.IO)
        lateinit var listener: NsdManager.DiscoveryListener
        listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { result.complete(null) }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.startsWith(DiscoveryProtocol.SERVICE_TYPE)) return
                @Suppress("DEPRECATION")
                nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val address = serviceInfo.host?.hostAddress ?: return
                        val hostPort = "$address:${serviceInfo.port}"
                        scope.launch {
                            if (scanner.validate(hostPort)) result.complete(hostPort)
                        }
                    }
                })
            }
        }
        return try {
            nsd.discoverServices(DiscoveryProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            withTimeoutOrNull(MDNS_TIMEOUT_MS) { result.await() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { nsd.stopServiceDiscovery(listener) }
            if (multicastLock?.isHeld == true) multicastLock.release()
        }
    }

    private suspend fun discoverUdp(): String? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(UDP_TIMEOUT_MS + 300L) {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = UDP_TIMEOUT_MS.toInt()
                    val payload = DiscoveryProtocol.REQUEST.encodeToByteArray()
                    broadcastAddresses().forEach { address ->
                        socket.send(DatagramPacket(payload, payload.size, address, DiscoveryProtocol.UDP_PORT))
                    }
                    val buffer = ByteArray(1_024)
                    while (true) {
                        val response = DatagramPacket(buffer, buffer.size)
                        socket.receive(response)
                        val hostPort = DiscoveryProtocol.parseResponse(
                            response.data.decodeToString(response.offset, response.offset + response.length),
                            response.address.hostAddress ?: continue,
                        ) ?: continue
                        if (scanner.validate(hostPort)) return@runCatching hostPort
                    }
                    null
                }
            }.getOrNull()
        }
    }

    private fun broadcastAddresses(): Set<InetAddress> {
        val addresses = linkedSetOf(InetAddress.getByName("255.255.255.255"))
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                if (!network.isUp || network.isLoopback) continue
                network.interfaceAddresses.mapNotNullTo(addresses) { it.broadcast }
            }
        }
        return addresses
    }

    companion object {
        private const val MDNS_TIMEOUT_MS = 2_500L
        private const val UDP_TIMEOUT_MS = 1_500L
    }
}
