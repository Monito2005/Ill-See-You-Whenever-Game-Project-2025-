import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.jar.JarEntry;

import javax.swing.JPanel;

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

    g2.setColor(Color.white);

    // draw player rectangle at playerX/playerY
    g2.fillRect(playerX, playerY, tileSize, tileSize);

        g2.dispose();
    }
}
