package com.example.map_tiles;

public class ServerSmokeTest {
    public static void main(String[] args) throws Exception {
        int port = LocalTileServer.getInstance().start();
        System.out.println("Server running on http://localhost:" + port);
        System.out.println("Test URLs (Lahug area at z=15):");
        System.out.println("  http://localhost:" + port + "/15/27658/15440.png");
        System.out.println("  http://localhost:" + port + "/16/55316/30880.png");
        System.out.println("  http://localhost:" + port + "/16/55317/30880.png");
        System.out.println("  http://localhost:" + port + "/17/110632/61760.png");
        System.out.println("\nPress Enter to stop the server...");
        System.in.read();
        LocalTileServer.getInstance().stop();
    }
}
