package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import mainpack1.GamePanel;

public class npcstudent2 extends entity {

    // Andrew's overworld small-talk (distinct tone)
    private String[] dialogues = {
        "Andrew: You seem lost.",
        "Stay sharp. People are watching.",
        "Don't waste my time."
    };

    // Patrol state
    private boolean baseInit = false;
    private int baseX, baseY;
    private final int PATROL_RADIUS_TILES = 2;  // ±2 tiles => 4 tiles across
    private int targetX, targetY;
    private int waitFrames = 0;

    // Anim
    private int spriteCounter = 0;
    private int spriteNum = 1;

    // Move throttling: step only every 3 frames (slower)
    private int moveCooldown = 0;
    private final int moveInterval = 3;

    // Conversation payload for ConversationSystem (distinct persona)
    public NPC getConversationNPC(){
        return new NPC(
            "Andrew",
            new String[]{
                "You're trying hard to fit in, huh?",
                "If you want respect, earn it.",
                "Talk is cheap. Show me substance."
            }
        );
    }

    public npcstudent2(GamePanel gp){
        super(gp);
        speed = 1;           // keep slow speed
        direction = "down";
        solidArea = new Rectangle(8,16,32,32);
        loadImages();
    }

    private void loadImages(){
        idle  = load("res/npc2/andrewIdle.png");
        down1 = load("res/npc2/andrewwalk1.png");
        down2 = load("res/npc2/andrewwalk2.png");
        up1   = load("res/npc2/andrewup1.png");
        up2   = load("res/npc2/andrewback2.png");
       

        // Optional: if idle exists, backfill any missing frames to avoid nulls
        if(idle != null){
            if(down1==null) down1=idle; if(down2==null) down2=idle;
            if(up1==null)   up1=idle;   if(up2==null)   up2=idle;
            if(left1==null) left1=idle; if(left2==null) left2=idle;
            if(right1==null) right1=idle; if(right2==null) right2=idle;
        }
    }

    private BufferedImage load(String path){
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path.replace("res/",""))) {
            if(is!=null){
                BufferedImage img = ImageIO.read(is);
                if(img!=null) return img;
            }
        } catch(Exception ignored){}
        try {
            java.io.File f = new java.io.File(path);
            if(f.exists()) return ImageIO.read(f);
        } catch(Exception ignored){}
        try {
            String base = System.getProperty("user.dir");
            java.io.File f2 = new java.io.File(base, path);
            if(f2.exists()) return ImageIO.read(f2);
        } catch(Exception ignored){}
        return null;
    }

    @Override
    public void update(){
        if(!baseInit){
            baseX = worldx;
            baseY = worldy;
            pickNewTarget(true);
            baseInit = true;
        }

        // Wait at waypoint
        if(waitFrames > 0){
            waitFrames--;
        } else {
            // Only move on interval to slow down further
            if(moveCooldown > 0){
                moveCooldown--;
            } else {
                moveCooldown = moveInterval;

                // Only vertical movement; lock X to baseX/targetX
                targetX = baseX; // ensure horizontal target is the spawn X
                int dy = Integer.compare(targetY, worldy);

                // Face vertical direction only
                direction = dy > 0 ? "down" : "up";

                // Step Y only
                if(dy != 0){
                    int oldY = worldy;
                    worldy += dy * speed;
                    collisionOn = false;
                    gp.cChecker.checkTile(this);
                    if(collisionOn) worldy = oldY;
                }

                // Arrived?
                if(Math.abs(worldy - targetY) <= speed){
                    worldy = targetY;
                    waitFrames = 40 + (int)(Math.random()*40);
                    pickNewTarget(false);
                }
            }
        }

        // Slow walk animation
        spriteCounter++;
        if(spriteCounter > 22){
            spriteNum = (spriteNum==1)?2:1;
            spriteCounter = 0;
        }
    }

    // Only pick a Y target within patrol radius; keep X fixed
    private void pickNewTarget(boolean firstPick){
        if(firstPick){
            targetX = baseX;   // lock X
            targetY = baseY;   // start at spawn Y
            return;
        }
        int rPx = PATROL_RADIUS_TILES * gp.tileSize;
        int minTileY = (baseY - rPx) / gp.tileSize;
        int maxTileY = (baseY + rPx) / gp.tileSize;

        int ty = minTileY + (int)(Math.random() * (maxTileY - minTileY + 1));

        targetX = baseX;            // lock X
        targetY = ty * gp.tileSize; // change only Y
    }

    // Fixed patrol box check around spawn
    private boolean insidePatrolBox(int x, int y){
        int rPx = PATROL_RADIUS_TILES * gp.tileSize;
        return x >= baseX - rPx && x <= baseX + rPx && y >= baseY - rPx && y <= baseY + rPx;
    }

    @Override
    public String[] interact(){
        return dialogues;
    }

    @Override
    public void draw(Graphics2D g2){
        BufferedImage image = idle;
        switch(direction){
            case "up"    -> image = (spriteNum==1?up1:up2);
            case "down"  -> image = (spriteNum==1?down1:down2);
            case "left"  -> image = (spriteNum==1?left1:left2);
            case "right" -> image = (spriteNum==1?right1:right2);
        }
        if(image == null) image = idle;

        int playerScreenX = gp.player.screenX; // player centers the camera
        int playerScreenY = gp.player.screenY;
        int screenX = worldx - gp.player.worldx + playerScreenX;
        int screenY = worldy - gp.player.worldy + playerScreenY;
        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);

        // Arrow indicator above head (blinking, slight bob)
        int arrowWidth = Math.max(8, gp.tileSize / 4);
        int arrowHeight = Math.max(6, gp.tileSize / 6);
        int bob = (int)(Math.sin(System.nanoTime() * 1e-9 * 6) * 2); // small bobbing
        int ax = screenX + gp.tileSize / 2;
        int ay = screenY - 6 + bob;

        int[] xs = { ax, ax - arrowWidth/2, ax + arrowWidth/2 };
        int[] ys = { ay, ay + arrowHeight, ay + arrowHeight };
        // blink
        boolean visible = (System.currentTimeMillis() / 400) % 2 == 0;
        if(visible){
            g2.setColor(new java.awt.Color(255, 255, 120));
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(new java.awt.Color(200, 180, 60));
            g2.drawPolygon(xs, ys, 3);
        }
    }

    // Optional: simple launch diagnostic for scripts to verify classes load
    public static String launchProbe() {
        return "npcstudent2:ok";
    }
}