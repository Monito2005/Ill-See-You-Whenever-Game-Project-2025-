package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import mainpack1.GamePanel;
import mainpack1.KeyHandler;

public class Player extends entity {

    private final KeyHandler keyH;
    public final int screenX, screenY;

    private int spriteCounter = 0;
    private int spriteNum = 0; // 0 = idle, 1/2 = walking

    // Optional extra frames (unused here but kept for compatibility)
    private BufferedImage down3, left3, right3, up3;

    private static final int WALK_FRAME_DELAY = 16;
    private static final int SPRINT_FRAME_DELAY = 10;
    private static final int BASE_SPEED = 1;
    private static final int SPRINT_SPEED = 2;

    // Overload that uses gp.keyH if available
    public Player(GamePanel gp){
        this(gp, gp != null ? gp.keyH : null);
    }

    public Player(GamePanel gp, KeyHandler keyH){
        super(gp);
        this.gp = gp;
        this.keyH = keyH;
        this.speed = BASE_SPEED;
        this.screenX = gp.screenWidth / 2 - gp.tileSize / 2;
        this.screenY = gp.screenHeight / 2 - gp.tileSize / 2;
        this.solidArea = new Rectangle(8, 16, 32, 32);
        setDefaultValues();
        loadImages();
    }

    private void setDefaultValues() {
        worldx = gp.tileSize * 23;
        worldy = gp.tileSize * 21;
        speed = BASE_SPEED;
        direction = "down";
    }

    private void loadImages() {
        // Down
        idle  = load("res/player/monitoidle.png");
        down1 = load("res/player/walk.png");
        down2 = load("res/player/walk2.png");
        if(down2 == null) down2 = down1;

        // Left
        left1 = load("res/player/sideL.png");
        left2 = load("res/player/walk2L.png");
        if(left2 == null) left2 = left1;

        // Right
        right1 = load("res/player/sideR.png");
        right2 = load("res/player/walk2R.png");
        if(right2 == null) right2 = right1;

        // Up
        BufferedImage backIdle = load("res/player/backidle.png");
        up1 = load("res/player/backL.png");
        up2 = load("res/player/backR.png");
        if(up1 == null) up1 = backIdle;
        if(up2 == null) up2 = up1;

        // Fallbacks
        if(idle == null) idle = down1 != null ? down1 : backIdle;
        if(down1 == null) down1 = idle;
        if(down2 == null) down2 = down1;
        if(left1 == null) left1 = idle;
        if(left2 == null) left2 = left1;
        if(right1 == null) right1 = idle;
        if(right2 == null) right2 = right1;
        if(up1 == null) up1 = idle;
        if(up2 == null) up2 = up1;
    }

    private BufferedImage load(String path) {
        try {
            java.io.File f = new java.io.File(path);
            if(f.exists()) return ImageIO.read(f);
        } catch(Exception ignored){}
        try {
            String base = System.getProperty("user.dir");
            java.io.File f2 = new java.io.File(base, path);
            if(f2.exists()) return ImageIO.read(f2);
        } catch(Exception ignored){}
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path.replace("res/",""))) {
            if(is != null) return ImageIO.read(is);
        } catch(Exception ignored){}
        return null;
    }

    @Override
    public void update(){
        mainpack1.KeyHandler kh = (this.keyH != null) ? this.keyH : gp.keyH;
        int dx = 0, dy = 0;
        if(kh != null){
            if(kh.upPressed)    dy -= 1;
            if(kh.downPressed)  dy += 1;
            if(kh.leftPressed)  dx -= 1;
            if(kh.rightPressed) dx += 1;
        }

        if(dx != 0 || dy != 0){
            int moveSpeed = (kh != null && kh.shiftPressed) ? SPRINT_SPEED : BASE_SPEED;
            int vx = dx * moveSpeed;
            int vy = dy * moveSpeed;

            // Reset collision before each move so wall checks are consistent.
            collisionOn = false;

            if(vx != 0){
                worldx += vx;
                gp.cChecker.checkTile(this);
                if(collisionOn) worldx -= vx;
            }

            if(vy != 0){
                worldy += vy;
                collisionOn = false;
                gp.cChecker.checkTile(this);
                if(collisionOn) worldy -= vy;
            }

            if(Math.abs(dx) >= Math.abs(dy)) direction = dx > 0 ? "right" : "left";
            else direction = dy > 0 ? "down" : "up";

            spriteCounter++;
            int frameDelay = (kh != null && kh.shiftPressed) ? SPRINT_FRAME_DELAY : WALK_FRAME_DELAY;
            if(spriteCounter > frameDelay){
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
            if(spriteNum == 0) spriteNum = 1;
        } else {
            spriteCounter = 0;
            spriteNum = 0;
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = idle;
        if (spriteNum == 0) {
            // Idle facing direction
            switch(direction) {
                case "up" -> image = up1;
                case "down" -> image = idle;
                case "left" -> image = left1;
                case "right" -> image = right1;
            }
        } else {
            switch (direction) {
                case "up" -> image = (spriteNum == 1 ? up1 : up2);
                case "down" -> image = (spriteNum == 1 ? down1 : down2);
                case "left" -> image = (spriteNum == 1 ? left1 : left2);
                case "right" -> image = (spriteNum == 1 ? right1 : right2);
            }
        }
        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
    }

    // Getter for intro cutscene
    public java.awt.image.BufferedImage getRightWalkFrame(int phase){
        return (phase % 2 == 0) ? right1 : right2;
    }
}
