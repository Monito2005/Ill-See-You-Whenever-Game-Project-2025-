package entity;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.InputStream;
import java.nio.file.Paths;
import mainpack1.gamepannel;
import mainpack1.keyhandler;

public class player extends entity{
gamepannel gp;
keyhandler keyH;

public final int screenX;
public final int screenY;

// sprite images
private BufferedImage idle; // default idle sprite (monitoidle.png)
private BufferedImage upIdle; // back.png when facing up and idle
private BufferedImage leftIdle, rightIdle; // idle images for left/right facing
private BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
// use the base `direction` field from entity
private int spriteCounter = 0;
private int spriteNum = 1; // for normal 2-frame animations
private int idleCounter = 0; // counter to track how long player has been idle (in frames)

public player(gamepannel gp, keyhandler keyH) {

    this.gp = gp;
    this.keyH = keyH;

    screenX = gp.screenWidth / 2 - gp.tileSize / 2;
    screenY = gp.screenHeight / 2 - gp.tileSize / 2;

    solidArea = new java.awt.Rectangle();
    solidArea.x = 8;
    solidArea.y = 16;  
    solidArea.width = 32;
    solidArea.height = 32;

    setDefaultValues();
    getPlayerImage();   
}
 public void setDefaultValues() {

    worldx = gp.tileSize * 25;
    worldy = gp.tileSize * 25;
     speed = 4;
     direction = "down";
 }
public void getPlayerImage(){
    try {
        // try loading from classpath first, then several common filesystem locations
        idle   = loadImage("res/player/monitoidle.png");
        down1  = loadImage("res/player/walk.png");
        down2  = loadImage("res/player/walk2.png");
    // upIdle is the facing-back idle image; up1/up2 are the two walk frames (backL/backR)
    upIdle = loadImage("res/player/back.png");
    up1    = loadImage("res/player/backL.png");
    up2    = loadImage("res/player/backR.png");
    // left/right idle frames (use sideL/sideR for idle) and walking alternates use walk2 variants
    leftIdle = loadImage("res/player/sideL.png");
    left1    = loadImage("res/player/sideL.png");
    left2    = loadImage("res/player/walk2L.png");
    rightIdle = loadImage("res/player/sideR.png");
    right1   = loadImage("res/player/sideR.png");
    right2   = loadImage("res/player/Walk2R.png");
        
    } catch (IOException e) {
        e.printStackTrace();
    }
}

/**
 * Load an image with multiple fallbacks:
 * 1) classpath resource via getResourceAsStream
 * 2) file at workingDir/<relativePath>
 * 3) file at workingDir/MonitoRPG/<relativePath>
 * 4) file at workingDir/../MonitoRPG/<relativePath>
 */
private BufferedImage loadImage(String relativePath) throws IOException {
    // try classpath
    InputStream is = getClass().getClassLoader().getResourceAsStream(relativePath);
    if (is != null) {
        try {
            BufferedImage img = ImageIO.read(is);
            if (img != null) {
                System.out.println("Loaded image from classpath: " + relativePath);
                return img;
            }
        } finally {
            is.close();
        }
    }

    // try a few filesystem locations
    String userDir = System.getProperty("user.dir");
    String[] candidates = new String[] {
        Paths.get(userDir, relativePath).toString(),
        Paths.get(userDir, "MonitoRPG", relativePath).toString(),
        Paths.get(userDir, "MonitoRPG", "src", relativePath).toString(),
        Paths.get(userDir, "MonitoGame_Release", relativePath).toString(),
        Paths.get(userDir, "..", "MonitoRPG", relativePath).toString()
    };

    for (String p : candidates) {
        File f = new File(p);
        if (f.exists()) {
            BufferedImage img = ImageIO.read(f);
            if (img != null) {
                System.out.println("Loaded image from file: " + f.getAbsolutePath());
                return img;
            }
        }
    }

    // last attempt: try relative path as-is
    File f = new File(relativePath);
    if (f.exists()) {
        BufferedImage img = ImageIO.read(f);
        if (img != null) {
            System.out.println("Loaded image from relative file: " + f.getAbsolutePath());
            return img;
        }
    }

    // not found
    System.err.println("Could not load image: " + relativePath + " (tried classpath and " + candidates.length + " filesystem locations)");
    return null;
}

 public void update(){
    boolean moving = false;
    int moveX = 0;
    int moveY = 0;
    
    if (keyH.upPressed) {
        moveY -= speed;
        moving = true;
    }
    if (keyH.downPressed) {
        moveY += speed;
        moving = true;
    }
    if (keyH.leftPressed) {
        moveX -= speed;
        moving = true;
    }
    if (keyH.rightPressed) {
        moveX += speed;
        moving = true;
    }

  


    // Update direction based on movement (prioritize vertical, then horizontal)
    if (moving) {
        if (moveY < 0) {
            direction = "up";
        } else if (moveY > 0) {
            direction = "down";
        } else if (moveX < 0) {
            direction = "left";
        } else if (moveX > 0) {
            direction = "right";
        }
    }

    // Normalize diagonal movement to maintain consistent speed
    if (moveX != 0 && moveY != 0) {
        // moving diagonally: reduce movement by ~15% each axis to keep speed consistent
        moveX = (int)(moveX * 0.85);
        moveY = (int)(moveY * 0.85);
    }

    // Apply movement, then check collision and undo if collision detected.
    if (moveX != 0 || moveY != 0) {
        // Try horizontal movement first and revert if collision occurs (allows sliding along obstacles)
        if (moveX != 0) {
            worldx += moveX;
            collisionOn = false;
            gp.collisionCheck.checkTile(this);
            if (collisionOn) {
                worldx -= moveX;
            }
        }

        // Then try vertical movement and revert if collision occurs
        if (moveY != 0) {
            worldy += moveY;
            collisionOn = false;
            gp.collisionCheck.checkTile(this);
            if (collisionOn) {
                worldy -= moveY;
            }
        }
    }

    // sprite animation: toggle frame while moving
    if (moving) {
        spriteCounter++;
        if (spriteCounter > 12) { // change frame every ~12 updates
            // toggle between 1 and 2 for all directions
            if (spriteNum == 1) {
                spriteNum = 2;
            } else {
                spriteNum = 1;
            }
            spriteCounter = 0;
        }
        // reset idle counter when moving
        idleCounter = 0;
    } else {
        // not moving: hold current pose and increment idle counter
        spriteCounter = 0;
        idleCounter++;
        
        // after 4 seconds (60 FPS * 4 = 240 frames), return to idle pose
        if (idleCounter >= 240) {
            spriteNum = 1;
            idleCounter = 0;
        }
    }
}

    public void draw(Graphics2D g2){
    BufferedImage image = null;
    // If no movement keys are pressed, show the idle sprite (use back-facing idle when facing up)
    if (!keyH.upPressed && !keyH.downPressed && !keyH.leftPressed && !keyH.rightPressed) {
        // prefer a direction-specific idle sprite when facing left/right/up
        if (direction.equals("up") && upIdle != null) {
            image = upIdle; // show back.png when facing up
        } else if (direction.equals("left") && leftIdle != null) {
            image = leftIdle;
        } else if (direction.equals("right") && rightIdle != null) {
            image = rightIdle;
        } else {
            image = idle;
        }
    } else {
        // choose frame based on direction and spriteNum (1 or 2)
        switch (direction) {
            case "up":
                // toggle between back.png and backw.png when moving up
                image = (spriteNum == 1) ? up1 : up2;
                break;
            case "down":
                image = (spriteNum == 1) ? down1 : down2;
                break;
            case "left":
                image = (spriteNum == 1) ? left1 : left2;
                break;
            case "right":
                image = (spriteNum == 1) ? right1 : right2;
                break;
            default:
                image = idle;
                break;
        }
    }
    if (image != null) {
        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
    } else {
        // draw a visible placeholder so it's obvious something loaded (or not)
        g2.setColor(Color.MAGENTA);
        g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
    }
}
}
