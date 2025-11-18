package entity;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class entity {
    public int worldx, worldy;
    public int speed;
    public BufferedImage idle, up1, up2, down1, down2, left1, left2, right1, right2;
    public String direction;
    public Rectangle solidArea;
    public boolean collisionOn = false;
}
