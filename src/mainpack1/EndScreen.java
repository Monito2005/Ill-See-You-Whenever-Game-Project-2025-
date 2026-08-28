package mainpack1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public class EndScreen {
    private final GamePanel gp;

    // fade-in for the whole end screen
    private int fade = 255;
    private boolean ready = false;

    // outcomes snapshot
    private boolean befH, befA, befG, befM;
    private boolean blkH, blkA, blkG, blkM;
    private int confidence = 0;

    // Ending branch
    private boolean isBadEnding = false;

    // --- Credits state + typewriter ---
    private boolean viewingCredits = false;
    private boolean creditsCompleted = false;

    // Sectioned typewriter: sections are separated by empty lines ("")
    // TODO: Replace lines below with your actual credits
    private final String[] credits = {
        "I'll See You Whenever",
        "--- Developed by ---",
        "Gustavo Castillo",
        "",
        "--- ART ---",
        "Gustavo Castillo",
        "",
        "--- MUSIC ---",
        "Ziday ",
        "",
        "--- SPECIAL THANKS ---",
        "You",
        "",
        "Keep going, you will make everyone proud.",
        "",
        "--END--"

    };

    // Typewriter indices
    private int cSectionStart = 0;
    private int cLineIndex = 0;
    private int cCharIndex = 0;
    private int cTick = 0;
    private int cDelay = 0;
    private final int cSpeed = 8;          // was 6, slower typing
    private final int cSectionPause = 140; // was 120, longer pause
    private int creditsStartDelay = 30;    // small delay before credits begin

    // Epilogue exposition (typewriter) – good and bad variants
    private final String[] epilogueGood = new String[]{
        "You listen to the echoes of each conversation.",
        "You stood your ground. You opened up. You gave space.",
        "Some doors stayed shut. Others opened quietly.",
        "In that mix of silence and voice, something shifted.",
        "You realized you're going to be okay out here.",
        "You found your own pace your own presence.",
        "Purpose isn't a destination. It's what you carry forward."
    };
    private final String[] epilogueBad = new String[]{
        "You replay moments that were hard.",
        "Some talks slipped away. Some doors closed.",
        "It stung. It still matters.",
        "But you showed up. You tried. You learned.",
        "Confidence grows in imperfect steps.",
        "You're not finished you're becoming.",
        "Next time, you'll carry what you found today."
    };
    private String[] epilogue = epilogueGood;

    private int epiIndex = 0;
    private String currentEpi = "";
    private int twIndex = 0;
    private int twTick = 0;
    private final int twInterval = 5; // typewriter speed
    private boolean epilogueDone = false;

    // NEW: auto epilogue pacing and auto-finish handling
    private int epiPause = 0;               // per-line pause frames
    private final int epiLinePause = 90;    // ~0.75s at 60fps
    private boolean finished = false;       // once we auto-return to play
    // private int resultsTimer = 0;
    // private final int resultsDuration = 300; // ~5s results display

    // Starfield
    private static final int STAR_COUNT = 90;
    private int[] starX = new int[STAR_COUNT];
    private float[] starY = new float[STAR_COUNT];
    private float[] starSpeed = new float[STAR_COUNT];
    private Color[] starColor = new Color[STAR_COUNT];

    // Results interaction
    private boolean showingResults = false; // when credits completed

    public EndScreen(GamePanel gp){
        this.gp = gp;
    }

    public void enter(){
        if(ready) return;

        // snapshot results
        befH = BattleSystem.isBefriended("Humberto");
        befA = BattleSystem.isBefriended("Andrew");
        befG = BattleSystem.isBefriended("Guero");
        befM = BattleSystem.isBefriended("Mysterious Girl") || BattleSystem.isBefriended("Delia");

        blkH = BattleSystem.isBlocked("Humberto");
        blkA = BattleSystem.isBlocked("Andrew");
        blkG = BattleSystem.isBlocked("Guero");
        blkM = BattleSystem.isBlocked("Mysterious Girl") || BattleSystem.isBlocked("Delia");

        // confidence calculation
        confidence = 0;
        confidence += befH ? 25 : (blkH ? -10 : 0);
        confidence += befA ? 25 : (blkA ? -10 : 0);
        confidence += befG ? 25 : (blkG ? -10 : 0);
        confidence += befM ? 25 : (blkM ? -10 : 0);
        if(confidence < 0) confidence = 0;
        if(confidence > 100) confidence = 100;

        // bad ending if confidence < 70 or any battle was lost
        int losses = (blkH?1:0) + (blkA?1:0) + (blkG?1:0) + (blkM?1:0);
        isBadEnding = (confidence < 70) || (losses > 0);
        epilogue = isBadEnding ? epilogueBad : epilogueGood;

        // prepare first epilogue line
        epiIndex = 0;
        currentEpi = epilogue[epiIndex];
        twIndex = 0;
        twTick = 0;
        epilogueDone = false;

        // reset credits
        viewingCredits = false;
        creditsCompleted = false;
        cSectionStart = 0;
        cLineIndex = 0;
        cCharIndex = 0;
        cTick = 0;
        cDelay = 0;
        creditsStartDelay = 30; // reset

        // reset auto flow
        epiPause = 0;
        finished = false;
        // resultsTimer = 0;

        // Init stars
        java.util.Random r = new java.util.Random();
        for(int i=0;i<STAR_COUNT;i++){
            starX[i] = r.nextInt(gp.screenWidth);
            starY[i] = r.nextFloat()*gp.screenHeight;
            starSpeed[i] = 0.4f + r.nextFloat()*1.2f;
            int c = 180 + r.nextInt(75);
            starColor[i] = new Color(c, c, 255);
        }

        fade = 255;
        ready = true;
    }

    public void update(){
        if(!ready) return;
        // end screen fade-in
        if(fade > 0){ fade -= 10; if(fade < 0) fade = 0; }

        // Starfield update
        for(int i=0;i<STAR_COUNT;i++){
            starY[i] += starSpeed[i];
            if(starY[i] > gp.screenHeight){
                starY[i] = -5;
                starX[i] = (int)(Math.random()*gp.screenWidth);
                starSpeed[i] = 0.4f + (float)Math.random()*1.2f;
            }
        }

        // Strong safeguard: if epilogue finished, wait a short delay then start credits
        if(epilogueDone && !viewingCredits && !creditsCompleted && !showingResults){
            if(creditsStartDelay > 0){
                creditsStartDelay--;
            } else {
                viewingCredits = true;
                cSectionStart = 0;
                cLineIndex = 0;
                cCharIndex = 0;
                cTick = 0;
                cDelay = 0;
                advancePastBlanks(); // ensure we start on a non-empty line
            }
        }

        // Credits typewriter (auto)
        if(viewingCredits){
            if(cDelay > 0){
                cDelay--;
                if(cDelay == 0){
                    // move to next section (skip blank separator(s))
                    cSectionStart = cLineIndex + 1;
                    advancePastBlanks(); // skip any consecutive blanks
                    cCharIndex = 0;
                    cTick = 0;
                    // done?
                    if(cSectionStart >= credits.length){
                        viewingCredits = false;
                        creditsCompleted = true;
                        showingResults = true;
                    }
                }
                return;
            }
            cTick++;
            if(cTick > cSpeed){
                cTick = 0;
                if(cLineIndex < credits.length){
                    String line = credits[cLineIndex];
                    if(line == null) line = "";
                    if(cCharIndex < line.length()){
                        cCharIndex++;
                    } else {
                        // step to next line within current section until blank
                        int next = cLineIndex + 1;
                        if(next < credits.length && credits[next] != null && !credits[next].isEmpty()){
                            cLineIndex++;
                            cCharIndex = 0;
                        } else {
                            cDelay = cSectionPause; // pause before next section
                        }
                    }
                }
            }
            return;
        }

        // Epilogue typewriter (auto-advance lines)
        if(!epilogueDone){
            twTick++;
            if(twTick >= twInterval){
                twTick = 0;
                if(twIndex < currentEpi.length()){
                    twIndex++;
                } else {
                    // line finished: wait, then go next line
                    if(epiPause == 0){
                        epiPause = epiLinePause;
                    } else {
                        epiPause--;
                        if(epiPause == 0){
                            epiIndex++;
                            if(epiIndex < epilogue.length){
                                currentEpi = epilogue[epiIndex];
                                twIndex = 0;
                                twTick = 0;
                            } else {
                                epilogueDone = true;
                                // start credits automatically
                                viewingCredits = true;
                                cSectionStart = 0;
                                cLineIndex = 0;
                                cCharIndex = 0;
                                cDelay = 0;
                            }
                        }
                    }
                }
            }
            return;
        }

        // Results state only via input; no auto-exit
        // if(epilogueDone && creditsCompleted && !finished){
        //     resultsTimer++;
        //     if(resultsTimer >= resultsDuration){
        //         BattleSystem.resetProgress();
        //         gp.enterPlay();
        //         finished = true;
        //     }
        // }
    }

    public void handleInput(KeyEvent e){
        if(!ready) return;
        int code = e.getKeyCode();

        // Restart / Exit when results visible
        if(showingResults){
            if(code == KeyEvent.VK_ENTER || code == KeyEvent.VK_R){
                BattleSystem.resetProgress();
                gp.enterPlay();
            } else if(code == KeyEvent.VK_ESCAPE){
                System.exit(0);
            }
            return;
        }

        // Allow fast-forward current credits section
        if(viewingCredits){
            if(code == KeyEvent.VK_ENTER || code == KeyEvent.VK_O){
                int end = cLineIndex;
                while(end + 1 < credits.length && !credits[end+1].isEmpty()){
                    end++;
                }
                cLineIndex = end;
                cCharIndex = credits[cLineIndex].length();
                cDelay = 18; // short pause to next section
            }
            return;
        }

        // No manual epilogue skip; flow is automatic.
    }

    public void draw(Graphics2D g2){
        if(!ready) return;

        // Background
        Color bg = isBadEnding ? new Color(10,0,20) : Color.BLACK;
        g2.setColor(bg);
        g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);

        // Stars
        for(int i=0;i<STAR_COUNT;i++){
            g2.setColor(starColor[i]);
            g2.fillRect(starX[i], (int)starY[i], 2, 2);
        }

        // Title
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(Color.WHITE);
        String title = isBadEnding ? "THE END (REFLECTION)" : "THE END";
        int twTitle = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (gp.screenWidth - twTitle)/2, 60);

        // Credits view (ensure it draws)
        if(viewingCredits){
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(new Color(230,230,255));

            // Ensure we’re not on a blank start
            int startIdx = cSectionStart;
            while (startIdx < credits.length && (credits[startIdx] == null || credits[startIdx].isEmpty())) {
                startIdx++;
            }

            // compute current section height
            int sectionLines = 0;
            for(int i = startIdx; i < credits.length; i++){
                if(credits[i] == null || credits[i].isEmpty()) break;
                sectionLines++;
            }
            int lh = 40;
            int totalH = Math.max(1, sectionLines) * lh;
            int startY = Math.max(40, (gp.screenHeight - totalH)/2);

            for(int i=0;i<sectionLines;i++){
                int idx = startIdx + i;
                if(idx > cLineIndex) break;
                String line = credits[idx] == null ? "" : credits[idx];
                String drawLine = (idx == cLineIndex)
                        ? line.substring(0, Math.min(cCharIndex, line.length()))
                        : line;
                int w = g2.getFontMetrics().stringWidth(drawLine);
                g2.drawString(drawLine, (gp.screenWidth - w)/2, startY + i*lh);
            }

            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(180,160,220));
            g2.drawString("Press Enter to speed up section", 20, gp.screenHeight - 24);

            if(fade > 0){
                g2.setColor(new Color(0,0,0, fade));
                g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
            }
            return;
        }

        // Epilogue text
        if(!epilogueDone){
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.setColor(new Color(220,200,255));
            String visible = (twIndex > 0) ? currentEpi.substring(0, Math.min(twIndex, currentEpi.length())) : "";
            drawWrapped(g2, visible, 60, 140, gp.screenWidth - 120, 26, 4);
        }

        // Results (no auto exit; show restart/exit options)
        if(showingResults){
            int y = 140;
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(new Color(255,240,240));
            g2.drawString(isBadEnding ? "What you learned:" : "How you did:", 60, y); y += 34;

            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.setColor(new Color(220,200,255));
            g2.drawString("Humberto: " + outcome(befH, blkH), 80, y); y += 24;
            g2.drawString("Andrew: " + outcome(befA, blkA), 80, y); y += 24;
            g2.drawString("Guero: " + outcome(befG, blkG), 80, y); y += 24;
            g2.drawString("Mysterious Girl: " + outcome(befM, blkM), 80, y); y += 32;

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(255,240,240));
            g2.drawString(isBadEnding ? "Confidence (growing):" : "Confidence rating:", 60, y);
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.setColor(new Color(255,170,120));
            g2.drawString(confidence + "/100", 300, y); y += 36;

            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.setColor(new Color(200,180,255));
            String closer = isBadEnding
                    ? "It's okay if it wasn't perfect. You're still on your way."
                    : "You found your true self and purpose. Keep going.";
            g2.drawString(closer, 60, y); y += 40;

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(new Color(180,160,220));
            g2.drawString("Press ENTER/R to restart or ESC to exit", 60, y);
        }

        if(fade > 0){
            g2.setColor(new Color(0,0,0, fade));
            g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        }
    }

    private String outcome(boolean bef, boolean blk){
        if(bef) return "Befriended";
        if(blk) return "Blocked";
        return "Unresolved";
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight, int maxLines){
        if(text == null || text.isEmpty()) return;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int linesDrawn = 0;
        for(String w : words){
            String candidate = line.length()==0 ? w : line + " " + w;
            if(g2.getFontMetrics().stringWidth(candidate) <= maxWidth){
                line = new StringBuilder(candidate);
            }else{
                g2.drawString(line.toString(), x, y + linesDrawn * lineHeight);
                linesDrawn++;
                if(linesDrawn >= maxLines) return;
                line = new StringBuilder(w);
            }
        }
        if(line.length() > 0 && linesDrawn < maxLines){
            g2.drawString(line.toString(), x, y + linesDrawn * lineHeight);
        }
    }

    // Helper to skip blank separator lines
    private void advancePastBlanks() {
        while (cSectionStart < credits.length && (credits[cSectionStart] == null || credits[cSectionStart].isEmpty())) {
            cSectionStart++;
        }
        if (cLineIndex < cSectionStart) {
            cLineIndex = cSectionStart;
            cCharIndex = 0;
            cTick = 0;
        }
    }
}
