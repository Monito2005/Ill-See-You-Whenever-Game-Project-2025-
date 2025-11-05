import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GamePannel extends JPanel implements Runnable{
    // decide screen settings and set variables
    final int originalTileSize = 16; // 16*16 pixels
    final int scale = 3;

    final int tileSize = originalTileSize * scale; //48 * 48 tile
    final int maxscreencol = 16;
    final int maxscreenrow = 12;
    final int screenWidth = tileSize * maxscreencol;
    final int screenheight = tileSize * maxscreenrow;
    Thread gameThread;

    // offscreen buffer to keep a fixed logical resolution
    private BufferedImage screenImage;
    private boolean fullscreen = false;

    // player position and movement flags
    public int playerX;
    public int playerY;
    public int playerSpeed = 2;

    public boolean up = false;
    public boolean down = false;
    public boolean left = false;
    public boolean right = false;


    public GamePannel(){
        this.setPreferredSize(new Dimension(screenWidth, screenheight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.setFocusable(true);
        this.requestFocusInWindow();

        // initial player position
        playerX = tileSize * 2;
        playerY = tileSize * 2;

        // create offscreen image at logical resolution
        screenImage = new BufferedImage(screenWidth, screenheight, BufferedImage.TYPE_INT_ARGB);

        // attach key handler
        this.addKeyListener(new KeyHandler(this));
    }

    public void startGameThread(){
        
        gameThread = new Thread(this);
        gameThread.start();

    }

    @Override
    public void run() {
        // simple fixed-loop
        int fps = 60;
        double drawInterval = 1000000000.0 / fps;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while(gameThread != null){
            // System.out.println("The game is running");
            update();

            repaint();

            try{
                double remaining = nextDrawTime - System.nanoTime();
                remaining = remaining / 1000000;

                if(remaining < 0) remaining = 0;

                Thread.sleep((long)remaining);

                nextDrawTime += drawInterval;
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    public void update(){
        // update player position based on flags
        if(up) playerY -= playerSpeed;
        if(down) playerY += playerSpeed;
        if(left) playerX -= playerSpeed;
        if(right) playerX += playerSpeed;

        // clamp to panel bounds
        if(playerX < 0) playerX = 0;
        if(playerY < 0) playerY = 0;
        if(playerX > screenWidth - tileSize) playerX = screenWidth - tileSize;
        if(playerY > screenheight - tileSize) playerY = screenheight - tileSize;
    }
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;

        // draw everything to the offscreen logical-resolution image first
        Graphics2D off = screenImage.createGraphics();
        // ensure clean background
        off.setColor(getBackground());
        off.fillRect(0, 0, screenImage.getWidth(), screenImage.getHeight());

        // draw game scene onto offscreen image
        off.setColor(Color.white);
        off.fillRect(playerX, playerY, tileSize, tileSize);
        off.dispose();

        // draw the offscreen image scaled to the current component size
        // use integer scaling (nearest neighbor) to avoid stretching and preserve pixel art
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int panelW = getWidth();
        int panelH = getHeight();

        // compute integer scale factor (>=1) that fits the logical resolution into the panel
        int scaleX = panelW / screenImage.getWidth();
        int scaleY = panelH / screenImage.getHeight();
        int intScale = Math.max(1, Math.min(scaleX, scaleY));

        int drawW = screenImage.getWidth() * intScale;
        int drawH = screenImage.getHeight() * intScale;

        // center (letterbox) when the panel size doesn't match exact integer multiple
        int drawX = (panelW - drawW) / 2;
        int drawY = (panelH - drawH) / 2;

        g2.drawImage(screenImage, drawX, drawY, drawW, drawH, null);

        g2.dispose();
    }

    // Toggle fullscreen by disposing and changing undecorated + extended state on the parent frame
    public void toggleFullscreen(){
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        if(!(w instanceof javax.swing.JFrame)) return;
        javax.swing.JFrame frame = (javax.swing.JFrame) w;

        // flip state
        fullscreen = !fullscreen;

        // Changing undecorated requires disposing the frame
        frame.dispose();

        if(fullscreen){
            frame.setUndecorated(true);
            frame.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
        }else{
            frame.setUndecorated(false);
            frame.setExtendedState(javax.swing.JFrame.NORMAL);
            // restore size to logical size (scaled by nothing) -> pack will use preferred size
            frame.pack();
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        // after re-showing, ensure this panel has focus for key input
        this.requestFocusInWindow();
    }
}
