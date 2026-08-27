package entity;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import mainpack1.GamePanel;

public class entity {
    public GamePanel gp;
    public int worldx, worldy, speed;
    public String direction = "down";
    public BufferedImage idle, up1, up2, down1, down2, left1, left2, right1, right2;
    public Rectangle solidArea = new Rectangle(0,0,48,48);
    public boolean collisionOn = false;

    // Ensure base constructor exists
    public entity(mainpack1.GamePanel gp){
        this.gp = gp;
    }

    // Allow GamePanel to call update/draw on entity references
    public void update(){ /* no-op; override in subclasses */ }

    public void draw(java.awt.Graphics2D g2){ /* no-op; override in subclasses */ }

    public String[] interact(){
        return new String[]{"..."};
    }
}
