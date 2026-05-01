package utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourcesLoad {
    // utility only, no instance needed
    private ResourcesLoad() {}

    public static BufferedImage loadImage(String fileName) {
        // all ui art is loaded from assets/images
        Path path = Paths.get("assets", "images", fileName);
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image format: " + path);
            }
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load image: " + path, e);
        }
    }
}