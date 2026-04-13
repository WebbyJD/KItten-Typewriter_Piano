package utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourcesLoad {
    private ResourcesLoad() {}

    public static BufferedImage loadImage(String fileName) {
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