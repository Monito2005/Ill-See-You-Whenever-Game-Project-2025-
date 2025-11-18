package mainpack1;

import entity.player;
import tile.tilemanager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class gamepannel extends JPanel implements Runnable{
// Make setting for the screen
final int originalTileSize = 16; //16*16
final int scale = 3; 

public final int tileSize = originalTileSize * scale;
public final int maxScreenCol = 16;
public final int maxScreenRow = 12;
public final int screenWidth = tileSize * maxScreenCol;
public final int screenHeight = tileSize * maxScreenRow;

//world settings
public final int maxWorldCol = 50;
public final int maxWorldRow = 50;
public final int worldWidth = tileSize * maxWorldCol;
public final int worldHeight = tileSize * maxWorldRow; 


//fps
int FPS = 60;

tilemanager tileM = new tilemanager(this);

keyhandler keyH = new keyhandler();
Thread gameThread;
public collisoncheckup  collisionCheck = new collisoncheckup(this);
public player player = new player(this,keyH);


//set positon
int playerx = 100;
int playery = 100;
int playerSpeed = 4;


public gamepannel() {

    this.setPreferredSize(new Dimension(screenWidth, screenHeight));
    this.setBackground(Color.BLACK);
    this.setDoubleBuffered(true);
    // Ensure the panel is focusable and requests focus so it receives key events
    this.setFocusable(true);
    this.addKeyListener(keyH);
    // request focus when the panel is shown this helps ensure key events are delivered
    this.requestFocusInWindow();
}

public void startGameThread() {
    gameThread = new Thread(this);
    gameThread.start();
    // also request focus when the game thread starts 
    this.requestFocusInWindow();

}

@Override
public void run() {

double drawInterval = 1000000000/FPS;
double delta = 0;
long lastTime = System.nanoTime();
long currentTime;


while(gameThread != null){

currentTime = System.nanoTime();

delta  += (currentTime - lastTime)/ drawInterval;

lastTime = currentTime;

if(delta >= 1){


update();
repaint();
delta--;

}

}
    throw new UnsupportedOperationException("Unimplemented method 'run'");
}
public void update() {
player. update();

}
public void paintComponent(Graphics g){

    super.paintComponent(g);

    Graphics2D g2 = (Graphics2D)g;
    
    tileM.draw(g2);

    player.draw(g2);   

    g2.dispose();

}
}
