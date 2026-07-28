package com.homektv.discovery;

import com.homektv.config.AppProperties;
import jakarta.annotation.PreDestroy;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class LanDiscoveryService {
    public static final String SERVICE_TYPE = "_home-ktv._tcp.local.";
    public static final String DISCOVERY_REQUEST = "HOME_KTV_DISCOVER_V1";

    private static final Logger log = LoggerFactory.getLogger(LanDiscoveryService.class);
    private final AppProperties properties;
    private final List<JmDNS> registrations = new ArrayList<>();
    private ExecutorService udpExecutor;
    private DatagramSocket udpSocket;
    private int httpPort;

    public LanDiscoveryService(AppProperties properties) {
        this.properties = properties;
    }

    @EventListener
    public synchronized void onWebServerReady(WebServerInitializedEvent event) {
        if (!properties.getDiscovery().isEnabled() || httpPort != 0) return;
        httpPort = event.getWebServer().getPort();
        registerMdns();
        startUdpResponder();
    }

    private void registerMdns() {
        for (InetAddress address : siteLocalAddresses()) {
            try {
                JmDNS jmDNS = JmDNS.create(address, "home-ktv");
                Map<String, String> txt = Map.of(
                        "service", "home-ktv",
                        "protocol", "1",
                        "api", "/api"
                );
                ServiceInfo info = ServiceInfo.create(
                        SERVICE_TYPE,
                        properties.getDiscovery().getInstanceName(),
                        httpPort,
                        0,
                        0,
                        txt
                );
                jmDNS.registerService(info);
                registrations.add(jmDNS);
                log.info("mDNS discovery published on {}:{}", address.getHostAddress(), httpPort);
            } catch (IOException e) {
                log.warn("mDNS discovery unavailable on {}: {}", address, e.getMessage());
            }
        }
    }

    private void startUdpResponder() {
        int discoveryPort = properties.getDiscovery().getUdpPort();
        udpExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "home-ktv-udp-discovery");
            thread.setDaemon(true);
            return thread;
        });
        udpExecutor.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                udpSocket = socket;
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.bind(new InetSocketAddress(discoveryPort));
                log.info("UDP discovery listening on 0.0.0.0:{}", discoveryPort);
                byte[] buffer = new byte[512];
                while (!socket.isClosed()) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    socket.receive(request);
                    String message = new String(request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
                    if (!DISCOVERY_REQUEST.equals(message.trim())) continue;
                    byte[] response = responsePayload(httpPort, properties.getDiscovery().getInstanceName());
                    socket.send(new DatagramPacket(response, response.length, request.getAddress(), request.getPort()));
                }
            } catch (SocketException e) {
                if (udpSocket != null && !udpSocket.isClosed()) log.warn("UDP discovery stopped: {}", e.getMessage());
            } catch (IOException e) {
                log.warn("UDP discovery unavailable: {}", e.getMessage());
            }
        });
    }

    static byte[] responsePayload(int port, String name) {
        String safeName = name.replace("\\", "\\\\").replace("\"", "\\\"");
        return ("{\"service\":\"home-ktv\",\"protocolVersion\":1,\"name\":\"" +
                safeName + "\",\"port\":" + port + "}").getBytes(StandardCharsets.UTF_8);
    }

    private List<InetAddress> siteLocalAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                Enumeration<InetAddress> values = network.getInetAddresses();
                while (values.hasMoreElements()) {
                    InetAddress address = values.nextElement();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) addresses.add(address);
                }
            }
        } catch (SocketException e) {
            log.warn("Cannot enumerate network interfaces: {}", e.getMessage());
        }
        return addresses;
    }

    @PreDestroy
    public synchronized void close() {
        if (udpSocket != null) udpSocket.close();
        if (udpExecutor != null) udpExecutor.shutdownNow();
        registrations.forEach(jmDNS -> {
            try {
                jmDNS.unregisterAllServices();
                jmDNS.close();
            } catch (IOException ignored) {
            }
        });
        registrations.clear();
    }
}
