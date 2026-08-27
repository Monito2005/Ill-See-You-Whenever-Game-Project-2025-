package mainpack1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class MainMenu {
    private final GamePanel gp;
    private int selection = 0;
    private final String[] items = {"Start", "Exit"};

    // Title BOOM animation
    private int boomFrames = 0;
    private boolean boomTriggered = false;

    public MainMenu(GamePanel gp){ this.gp = gp; }

    public void update(){
        // Run boom burst then transition to PLAY when done
        if(boomTriggered){
            if(boomFrames > 0){
                boomFrames--;
            } else {
                boomTriggered = false;
                gp.enterPlay();
            }
        }
    }

    public void move(int delta){
        selection = (selection + delta + items.length) % items.length;
    }

    public void confirm(){
        if(selection == 0){
            // Trigger BOOM burst; update() will switch to PLAY after burst
            boomTriggered = true;
            boomFrames = 20; // short burst length
        } else {
            System.exit(0);
        }
    }

    public void draw(Graphics2D g2){
        // Background
        g2.setColor(Color.BLACK);
        g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);

        // Title text
        String title = "I'll See You Whenever";

        // Boom effect: size/jitter during burst
        int baseSize = 46;
        int extra = boomFrames > 0 ? 16 : 0;
        int jitter = boomFrames > 0 ? 2 : 0;

        // Glow layer
        g2.setFont(new Font("Arial", Font.BOLD, baseSize + extra));
        g2.setColor(new Color(130,60,210));
        g2.drawString(title, 60 + jitter, 140 + jitter);

        // Main layer
        g2.setColor(Color.WHITE);
        g2.drawString(title, 60, 140);

        // Menu items
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        for(int i=0;i<items.length;i++){
            g2.setColor(i==selection?Color.YELLOW:Color.LIGHT_GRAY);
            g2.drawString((i==selection?"> ":"  ")+items[i],120,240+i*50);
        }

        // Footer
        g2.setFont(new Font("Arial", Font.PLAIN,16));
        g2.setColor(Color.GRAY);
        g2.drawString("W/S or Up/Down: Select   O/Enter: Confirm", 120, gp.screenHeight - 40);
    }
}