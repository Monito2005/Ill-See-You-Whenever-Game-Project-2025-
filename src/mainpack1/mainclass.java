package mainpack1;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Image;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.File;

public class mainclass {

    private static JFrame window;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            window = new JFrame("I'll See You Whenever");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            // Set app/window icon (place your icon at res/icon.png or packaging/icon.png)
            Image icon = loadIcon();
            if (icon != null) {
                window.setIconImage(icon);
            }

            GamePanel gp = new GamePanel();
            
            window.add(gp);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            gp.startGameThread();
            gp.requestFocusInWindow();
        });
    }

    // Try multiple locations to load an icon for the app window
    private static Image loadIcon() {
        String[] candidates = {
            "res/icon.png",
            "res/icon.jpg",
            "res/icon.ico",
            "packaging/icon.png",
            "packaging/icon.jpg",
            "packaging/icon.ico"
        };
        for (String path : candidates) {
            try {
                // classpath first (strip leading res/)
                String cp = path.startsWith("res/") ? path.substring(4) : path;
                try (InputStream is = mainclass.class.getClassLoader().getResourceAsStream(cp)) {
                    if (is != null) {
                        Image img = ImageIO.read(is);
                        if (img != null) return img;
                    }
                }
            } catch (Exception ignored) {}
            try {
                File f = new File(path);
                if (f.exists()) {
                    Image img = ImageIO.read(f);
                    if (img != null) return img;
                }
            } catch (Exception ignored) {}
            try {
                String base = System.getProperty("user.dir");
                File f2 = new File(base, path);
                if (f2.exists()) {
                    Image img = ImageIO.read(f2);
                    if (img != null) return img;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
