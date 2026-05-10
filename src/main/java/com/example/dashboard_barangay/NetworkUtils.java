package com.example.dashboard_barangay;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkUtils {
    public static boolean isInternetAvailable() {
        try (Socket socket = new Socket()) {
            // Connect to a reliable host (Google's public DNS) with a timeout.
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            return true;
        } catch (IOException e) {
            return false; // Connection failed
        }
    }
}
