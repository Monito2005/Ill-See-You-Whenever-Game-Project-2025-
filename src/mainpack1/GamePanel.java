package mainpack1;

import entity.Player;
import entity.entity;
import entity.npcstudent1;
import entity.npcstudent2; // add: student 2
import entity.npcstudent3; // add: love interest
import entity.npcstudent4; // add: Guero
import tile.TileManager;
import entity.NPC;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    public enum GameState { INTRO, MENU, PLAY, CONVO, END }
    public GameState gameState = GameState.INTRO;

    private final int originalTileSize = 16;
    private final int scale = 3;
    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    private final int FPS = 60;

    public KeyHandler keyH = new KeyHandler();
    public UI ui = new UI(this);
    public ConversationSystem conversationSystem = new ConversationSystem(this);
    public MainMenu mainMenu = new MainMenu(this);
    public IntroCutscene intro = new IntroCutscene(this);
    public AssetSetter assetSetter = new AssetSetter(this);
    public CollisionChecker cChecker = new CollisionChecker(this);
    public sound music = new sound();
    public sound se = new sound();
    public TileManager tileM = new TileManager(this);
    public Player player = new Player(this, keyH);
    public entity[] npc = new entity[10];
    public EndScreen endScreen = new EndScreen(this);

    public float musicVolume = 0.4f;
    public float sfxVolume = 0.4f;

    private Thread gameThread;
    private int fadeAlpha = 255;
    private boolean fadingIn = true;
    // Debounce for interact
    private int interactCooldown = 0;

    // Gate message state
    private int gateMessageFrames = 0;
    private String gateMessageText = "";
    // Typewriter for gate message
    private int gateMessageCharIndex = 0;
    private int gateMessageTick = 0;
    private final int gateMessageTickInterval = 2; // smaller = faster typewriter

    public GamePanel(){
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.black);
        setDoubleBuffered(true);
        addKeyListener(keyH);
        setFocusable(true);
        keyH.gp = this;
        applyAudioSettings();
        startGameThread();
    }

    public void applyAudioSettings(){
        if(music != null) music.setMasterVolume(musicVolume);
        if(se != null) se.setMasterVolume(sfxVolume);
    }

    public void startIntro(){ gameState = GameState.INTRO; }
    public void enterMenu(){
        stopMusic();
        try{
            music.setFilePath("res/sound/menu1.wav");
            music.setMasterVolume(musicVolume);
            music.playLoop();
        }catch(Exception ignored){}
        gameState = GameState.MENU;
    }
    public void enterPlay(){
        stopMusic();
        try{
            music.setFilePath("res/sound/song1.wav");
            music.setMasterVolume(musicVolume);
            music.playLoop();
        }catch(Exception ignored){}
        gameState = GameState.PLAY;

        // Spawn player at map row 9, col 23
        player.worldx = 22 * tileSize;
        player.worldy = 9 * tileSize;

        if(npc == null || npc.length == 0) npc = new entity[10];

        // Humberto
        entity s1 = new npcstudent1(this);
        s1.worldx = 25 * tileSize;
        s1.worldy = 11 * tileSize;
        npc[0] = s1;

        // Andrew at row 29, column 16 (tile coords)
        entity s2 = new npcstudent2(this);
        s2.worldx = 16 * tileSize; // column
        s2.worldy = 29 * tileSize; // row
        npc[1] = s2;

        // Love interest at row 34, column 78 (tile coords)
        entity s3 = new npcstudent3(this);
        s3.worldx = 37 * tileSize; // column (fixed from 37)
        s3.worldy = 34 * tileSize; // row
        npc[2] = s3;

        // Spawn Guero (row 16, col 12)
        entity s4 = new npcstudent4(this);
        s4.worldx = 12 * tileSize; // column 12
        s4.worldy = 15 * tileSize; // row 16
        npc[3] = s4;
    }

    public void startConversationFromNPC(entity e){
        // accept npcstudent1, npcstudent2, and npcstudent3
        NPC payload = null;
        if (e instanceof npcstudent1 n1) {
            payload = n1.getConversationNPC();
        } else if (e instanceof npcstudent2 n2) {
            payload = n2.getConversationNPC();
        } else if (e instanceof npcstudent3 n3) {
            // Confidence gate: must have befriended Humberto and Andrew first
            boolean hasConfidence = BattleSystem.isBefriended("Humberto") && BattleSystem.isBefriended("Andrew");
            if(!hasConfidence){
                // Show in-game gate window for ~4 seconds with typewriter effect
                gateMessageText = "You pause...\nYou need more confidence before talking to the Mysterious Girl.\nBefriend Humberto and Andrew first.";
                gateMessageFrames = 240; // ≈ 4s at 60 FPS
                gateMessageCharIndex = 0; // reset typewriter
                gateMessageTick = 0;      // reset tick
                return;
            }
            payload = n3.getConversationNPC();
        } else if (e instanceof npcstudent4 n4) {
            payload = n4.getConversationNPC();
        } else {
            return;
        }

        if (conversationSystem != null && payload != null){
            conversationSystem.startConversation(payload, e);
            gameState = GameState.CONVO;
        }
    }

    public void playMusic(int i){ music.setFile(i); music.play(); music.loop(); }
    public void stopMusic(){
        if(music != null) music.stop();
    }
    public void playSE(int i){ se.setFile(i); se.play(); }

    public void startGameThread(){
        gameThread = new Thread(this,"GameLoop");
        gameThread.start();
    }

    @Override
    public void run(){
        double interval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long last = System.nanoTime();
        while(gameThread != null){
            long now = System.nanoTime();
            delta += (now - last)/interval;
            last = now;
            while(delta >= 1){
                update();
                repaint();
                delta--;
            }
            // Prevent busy-wait CPU spikes and keep pacing stable.
            // This helps maintain smoother frame times on laptops.
            try{
                Thread.sleep(1);
            }catch(InterruptedException ignored){
                Thread.currentThread().interrupt();
            }
        }
    }

    public void update(){
        // Only run fade for the MENU state
        if (gameState == GameState.MENU) {
            if (fadingIn) {
                fadeAlpha -= 8;
                if (fadeAlpha < 0) { fadeAlpha = 0; fadingIn = false; }
            }
        } else {
            // Ensure fade is off outside menu
            fadingIn = false;
            fadeAlpha = 0;
        }

        if(gameState == GameState.INTRO){
            if (intro != null) intro.update();
            return;
        }
        if(gameState == GameState.MENU){
            if (mainMenu != null) mainMenu.update();
            return;
        }
        if(gameState == GameState.CONVO){
            if (conversationSystem != null) {
                conversationSystem.update();
                if(!conversationSystem.inConversation){
                    gameState = GameState.PLAY;
                }
            } else {
                gameState = GameState.PLAY;
            }
            return;
        }

        // If in END, only update endScreen
        if(gameState == GameState.END){
            if(endScreen != null) endScreen.update();
            interactCooldown = 0;
            return;
        }

        // PLAY
        if(ui != null && ui.messageOn){
            ui.update();
        } else if(player != null){
            player.update();
        }

        if(npc != null){
            for(entity e: npc){
                if(e != null) e.update();
            }
        }

        handleInteract();

        // Decay gate message timer + typewriter advance
        if(gateMessageFrames > 0){
            gateMessageFrames--;
            gateMessageTick++;
            if(gateMessageTick >= gateMessageTickInterval){
                gateMessageTick = 0;
                // advance one character at a time up to full text length (including newlines)
                int totalLen = gateMessageText.length();
                if(gateMessageCharIndex < totalLen){
                    gateMessageCharIndex++;
                }
            }
        }

        // After handling gameplay, check for completion
        if(gameState == GameState.PLAY){
            if(allResolved()){
                gameState = GameState.END;
                if(endScreen != null) endScreen.enter();
                // start ending music here
                try{
                    stopMusic();
                    music.setFilePath("res/sound/credit1.wav"); // replace with your actual ending track
                    music.playLoop();
                }catch(Exception ignored){}
                return;
            }
        }
    }

    // Route inputs to EndScreen in END state
    public void dispatchEndInput(java.awt.event.KeyEvent e){
        if(gameState == GameState.END && endScreen != null){
            endScreen.handleInput(e);
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        try{
            if(gameState == GameState.INTRO){
                if(intro != null) intro.draw(g2);
                return;
            }
            if(gameState == GameState.MENU){
                if(mainMenu != null) mainMenu.draw(g2);
                return;
            }
            if(gameState == GameState.END){
                if(endScreen != null) endScreen.draw(g2);
                return;
            }

            // world
            if(tileM != null) tileM.draw(g2);
            if(npc != null){
                for(entity e : npc){
                    if(e != null) e.draw(g2);
                }
            }
            if(player != null) player.draw(g2);
            if(ui != null) ui.draw(g2);

            // convo overlay
            if(gameState == GameState.CONVO && conversationSystem != null){
                conversationSystem.draw(g2);
            } else {
                drawInteractPrompt(g2);
            }

            // Gate message overlay only in PLAY state
            if(gameState == GameState.PLAY && gateMessageFrames > 0){
                drawGateMessage(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    // Helper: are all core NPCs resolved (befriended or blocked)?
    private boolean allResolved(){
        boolean h = BattleSystem.isBefriended("Humberto") || BattleSystem.isBlocked("Humberto");
        boolean a = BattleSystem.isBefriended("Andrew")   || BattleSystem.isBlocked("Andrew");
        boolean g = BattleSystem.isBefriended("Guero")    || BattleSystem.isBlocked("Guero");
        boolean m = BattleSystem.isBefriended("Mysterious Girl") || BattleSystem.isBefriended("Delia")
                 || BattleSystem.isBlocked("Mysterious Girl")     || BattleSystem.isBlocked("Delia");
        return h && a && g && m;
    }

    private void drawInteractPrompt(Graphics2D g2){
        if(player == null || npc == null) return;
        entity nearest = null;
        int best = Integer.MAX_VALUE;
        int range = tileSize * 3; // proximity range

        for(entity e : npc){
            if(e == null) continue;
            int dx = e.worldx - player.worldx;
            int dy = e.worldy - player.worldy;
            int d2 = dx*dx + dy*dy;
            if(d2 < range*range && d2 < best){
                best = d2;
                nearest = e;
            }
        }
        if(nearest == null) return;

        int px = nearest.worldx - player.worldx + player.screenX + tileSize/2;
        int py = nearest.worldy - player.worldy + player.screenY - tileSize/2;

        String msg = "Press O to interact";
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        int w = g2.getFontMetrics().stringWidth(msg);
        int x = px - w/2;
        int y = py - 8;

        // shadow
        g2.setColor(new Color(0,0,0,160));
        g2.fillRoundRect(x-6, y-16, w+12, 22, 10, 10);
        // text
        g2.setColor(Color.WHITE);
        g2.drawString(msg, x, y);
    }

    // Small window overlay for gate feedback
    private void drawGateMessage(Graphics2D g2){
        int boxW = screenWidth - 220;
        int boxH = 140; // slightly taller for longer text
        int boxX = 110;
        int boxY = screenHeight/2 - boxH/2;

        g2.setColor(new Color(20, 10, 40, 230));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(220, 200, 255));
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);

        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        String title = "Confidence Needed";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, boxX + (boxW - tw)/2, boxY + 28);

        // Typewriter content
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(new Color(220,200,255));
        int textX = boxX + 20;
        int textY = boxY + 54;
        int textW = boxW - 40;
        int lineH = 18;

        String full = gateMessageText != null ? gateMessageText : "";
        String visible = (gateMessageCharIndex > 0 && gateMessageCharIndex <= full.length())
                ? full.substring(0, gateMessageCharIndex)
                : "";

        // wrap visible text (supports \n)
        int drawn = 0;
        for(String ln : visible.split("\n")){
            String[] words = ln.split("\\s+");
            StringBuilder cur = new StringBuilder();
            for(String w : words){
                String cand = cur.length()==0 ? w : cur + " " + w;
                if(g2.getFontMetrics().stringWidth(cand) <= textW){
                    cur = new StringBuilder(cand);
                }else{
                    g2.drawString(cur.toString(), textX, textY + drawn*lineH);
                    drawn++;
                    cur = new StringBuilder(w);
                }
            }
            if(cur.length()>0){
                g2.drawString(cur.toString(), textX, textY + drawn*lineH);
                drawn++;
            }
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(180,160,220));
        g2.drawString("Beat Humberto and Andrew to build confidence.", textX, boxY + boxH - 16);
    }

    private void handleInteract(){
        // block interact while gate message is visible or in END
        if(gateMessageFrames > 0) return;
        if(gameState != GameState.PLAY) return;

        // cooldown so we don’t restart conversation every frame
        if(interactCooldown > 0){ interactCooldown--; }

        // wait for press
        if(!keyH.oPressed) return;

        // consume press
        keyH.oPressed = false;
        if(interactCooldown > 0) return;
        interactCooldown = 10;

        // find nearby NPC with stricter facing priority
        int range = (int)(tileSize * 1.5);
        int range2 = range * range;
        int fx = 0, fy = 0;
        switch (player.direction) {
            case "up":    fy = -1; break;
            case "down":  fy =  1; break;
            case "left":  fx = -1; break;
            case "right": fx =  1; break;
            default:      fx = 0; fy = 0; break; // defensive default
        }

        entity bestFacing = null;
        int bestFacingDist2 = Integer.MAX_VALUE;
        entity bestAny = null;
        int bestAnyDist2 = Integer.MAX_VALUE;

        if(npc != null){
            for(entity e : npc){
                // include Humberto, Andrew, Mysterious Girl, and Guero
                if(!(e instanceof npcstudent1) && !(e instanceof npcstudent2) && !(e instanceof npcstudent3) && !(e instanceof npcstudent4)) continue;
                // ...existing distance and cone checks...
                int dx = e.worldx - player.worldx;
                int dy = e.worldy - player.worldy;
                int d2 = dx*dx + dy*dy;
                if(d2 > range2) continue;

                int dot = dx*fx + dy*fy;
                boolean inFront = dot > 0;

                if(inFront && d2 < bestFacingDist2){
                    bestFacingDist2 = d2;
                    bestFacing = e;
                }
                if(d2 < bestAnyDist2){
                    bestAnyDist2 = d2;
                    bestAny = e;
                }
            }
        }

        entity target = (bestFacing != null) ? bestFacing : bestAny;
        if(target == null) return;

        startConversationFromNPC(target);
    }
}