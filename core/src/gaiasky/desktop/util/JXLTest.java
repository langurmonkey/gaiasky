package gaiasky.desktop.util;

import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class JXLTest {
    public static void main(String[] args) {
        try {
            BufferedImage image = ImageIO.read(Paths.get("/home/tsagrista/Pictures/screen-i3-x11.jxl").toFile());
            image.getRGB()
            display(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JFrame frame;
    private static JLabel label;

    public static void display(BufferedImage image) {
        if (frame == null) {
            frame = new JFrame();
            frame.setTitle("stained_image");
            frame.setSize(image.getWidth(), image.getHeight());
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            label = new JLabel();
            label.setIcon(new ImageIcon(image));
            frame.getContentPane().add(label, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.pack();
            frame.setVisible(true);
        } else
            label.setIcon(new ImageIcon(image));
    }
}
