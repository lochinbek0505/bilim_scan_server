package uz.falconmobile.bilim_scan.config; // O'zingizning paketingizga moslang

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Component
public class NetworkDiscoveryService {

    // application.properties dagi portni oladi, topolmasa 4257 ni ishlatadi
    @Value("${server.port:4257}")
    private int serverPort;

    // UDP so'rovlar eshitiladigan port (Buni mijoz dasturlar ham bilishi kerak)
    private static final int DISCOVERY_PORT = 8888;
    
    // Mijoz qidirayotganda yuborishi kerak bo'lgan parol/xabar
    private static final String DISCOVER_MESSAGE = "DISCOVER_BILIMSCAN";
    
    // Server qaytaradigan javob xabari
    private static final String RESPONSE_MESSAGE = "BILIMSCAN_HERE";

    @EventListener(ApplicationReadyEvent.class)
    public void startDiscoveryListener() {
        Thread discoveryThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                socket.setBroadcast(true);
                System.out.println("UDP Kashf qilish (Discovery) xizmati " + DISCOVERY_PORT + "-portda ishga tushdi...");

                byte[] receiveBuffer = new byte[1024];

                while (true) {
                    // 1. Tarmoqdan kelayotgan UDP paketlarni kutish
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);

                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                    // 2. Agar xabar biz kutgan so'rov bo'lsa, unga javob qaytarish
                    if (DISCOVER_MESSAGE.equals(message)) {
                        String clientIp = receivePacket.getAddress().getHostAddress();
                        System.out.println("Mijoz (" + clientIp + ") serverni qidirmoqda. Javob yuborilyapti...");

                        // Javob: "BILIMSCAN_HERE:4257" ko'rinishida bo'ladi
                        String responseStr = RESPONSE_MESSAGE + ":" + serverPort;
                        byte[] sendData = responseStr.getBytes();

                        // 3. Javobni so'rov yuborgan mijozning IP va portiga qaytarish
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData, sendData.length,
                                receivePacket.getAddress(), receivePacket.getPort()
                        );
                        socket.send(sendPacket);
                    }
                }
            } catch (Exception e) {
                System.err.println("UDP Discovery xizmatida xatolik: " + e.getMessage());
            }
        });

        // Daemon qilsak, Spring Boot to'xtaganda bu orqa fon jarayoni ham avtomat to'xtaydi
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }
}