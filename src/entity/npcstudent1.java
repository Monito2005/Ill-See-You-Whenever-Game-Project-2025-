package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import mainpack1.GamePanel;

public class npcstudent1 extends entity {

    private String[] dialogues = {
        "Hello! I'm Humberto.",
        "Nice to meet you!",
        "I like walking here."
    };

    // Patrol state
    private boolean baseInit = false;
    private int baseX, baseY;
    // limit area: ±2 tiles from spawn
    private final int PATROL_RADIUS_TILES = 2;
    private int targetX, targetY;
    private int waitFrames = 0;

    // Simple 4-direction loop within 2-tile area
    private int patrolStep = 0; // 0=up,1=right,2=down,3=left

    // Anim
    private int spriteCounter = 0;
    private int spriteNum = 1;

    // Move throttling
    private int moveCooldown = 0;
    private final int moveInterval = 3;

    // Conversation payload for ConversationSystem
    public NPC getConversationNPC(){
        return new NPC(
            "Humberto",
            new String[]{
                "Hey... you look new.",
                "It's okay to take it slow.",
                "I'll be around if you want to talk."
            }
        );
    }

    public npcstudent1(GamePanel gp){
        super(gp);
        speed = 1;           // keep slow speed
        direction = "down";
        solidArea = new Rectangle(8,16,32,32);
        loadImages();
    }

    private void loadImages(){
        idle  = load("res/npcstudent1/humbertoidle.png");
        down1 = load("res/npcstudent1/humbertofoward1.png");
        down2 = load("res/npcstudent1/humbertofoward2.png");
        up1   = load("res/npcstudent1/humbertowalkback1.png");
        up2   = load("res/npcstudent1/humbertowalkback2.png");
        left1 = load("res/npcstudent1/humbertoL.png");
        left2 = load("res/npcstudent1/humbertowalkL.png");
        right1= load("res/npcstudent1/humbertoR.png");
        right2= load("res/npcstudent1/humbertowalkR.png");
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
            setNextPatrolTarget(true);
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

                int dx = Integer.compare(targetX, worldx);
                int dy = Integer.compare(targetY, worldy);

                // Face move direction
                if(Math.abs(dx) >= Math.abs(dy)) direction = dx > 0 ? "right" : "left";
                else direction = dy > 0 ? "down" : "up";

                // Step toward target (no teleport)
                if(dx != 0){
                    int oldX = worldx;
                    worldx += dx * speed;
                    collisionOn = false;
                    gp.cChecker.checkTile(this);
                    if(collisionOn) worldx = oldX;
                }
                if(dy != 0){
                    int oldY = worldy;
                    worldy += dy * speed;
                    collisionOn = false;
                    gp.cChecker.checkTile(this);
                    if(collisionOn) worldy = oldY;
                }

                // Arrived (one-tile leg done)
                if(Math.abs(worldx - targetX) <= speed && Math.abs(worldy - targetY) <= speed){
                    worldx = targetX;
                    worldy = targetY;
                    waitFrames = 30 + (int)(Math.random()*30);
                    setNextPatrolTarget(false);
                }

                // Hard clamp inside patrol box
                if(!insidePatrolBox(worldx, worldy)){
                    worldx = Math.max(baseX - PATROL_RADIUS_TILES * gp.tileSize, Math.min(worldx, baseX + PATROL_RADIUS_TILES * gp.tileSize));
                    worldy = Math.max(baseY - PATROL_RADIUS_TILES * gp.tileSize, Math.min(worldy, baseY + PATROL_RADIUS_TILES * gp.tileSize));
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

    // Set next target: up, right, down, left; one tile per step within ±2 tiles
    private void setNextPatrolTarget(boolean firstPick){
        int ts = gp.tileSize;
        int minX = baseX - PATROL_RADIUS_TILES * ts;
        int maxX = baseX + PATROL_RADIUS_TILES * ts;
        int minY = baseY - PATROL_RADIUS_TILES * ts;
        int maxY = baseY + PATROL_RADIUS_TILES * ts;

        if(firstPick){
            patrolStep = 0;
            targetX = baseX;
            targetY = baseY;
            return;
        }

        switch(patrolStep){
            case 0: // up
                targetX = worldx;
                targetY = Math.max(minY, worldy - ts);
                break;
            case 1: // right
                targetX = Math.min(maxX, worldx + ts);
                targetY = worldy;
                break;
            case 2: // down
                targetX = worldx;
                targetY = Math.min(maxY, worldy + ts);
                break;
            default: // left
                targetX = Math.max(minX, worldx - ts);
                targetY = worldy;
                break;
        }
        patrolStep = (patrolStep + 1) % 4;
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
}