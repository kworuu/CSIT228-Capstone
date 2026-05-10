package com.example.dashboard_barangay;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MapCache {

    private static final String CACHE_FILE_PATH = "map_cache.png";

    public static void saveMapImage(WritableImage image) {
        if (image == null) {
            return;
        }

        File file = new File(CACHE_FILE_PATH);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        try {
            ImageIO.write(bufferedImage, "png", file);
            System.out.println("Map image saved to cache.");
        } catch (IOException e) {
            System.err.println("Failed to save map image to cache: " + e.getMessage());
        }
    }

    public static Image loadMapImage() {
        File file = new File(CACHE_FILE_PATH);
        if (file.exists()) {
            try {
                return new Image(file.toURI().toString());
            } catch (Exception e) {
                System.err.println("Failed to load map image from cache: " + e.getMessage());
            }
        }
        return null;
    }
}
