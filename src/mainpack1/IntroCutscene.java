package mainpack1;

import java.awt.*;
import java.util.Random;

public class IntroCutscene {

    private final GamePanel gp;

    // Shorter dialogue (adjust order if desired)
    private final String[] lines = {
        "You moved to college.",
        "Boxes. Strangers. New hallways.",
        "Everything is loud. Everything is new.",
        "Your head hurts from the overstimulation.",
        "Too many faces. Too many names.",
        "Too many expectations.",
        "Everyone seems to know who they are.",
        "You're still figuring it out.",
        "What if they don't like you?",
        "What if you say something wrong?",
        "What if you can't keep up?",
        "The fear is real.",
        "And so is the exhaustion.",
        "I think I'll be okay though.",
        "Just need time to adjust.",
        "It's weird being here alone.",
        "Everyone else seems so confident.",
        "But maybe that's just what they show.",
        "Maybe they're scared too.",
        "I'm trying to show up anyway.",
        "Even when my head hurts.",
        "Even when I want to hide.",
        "Because staying invisible feels worse.",
        "I don't know if I'm doing this right.",
        "But I'm trying.",
        "Thanks for checking in on me.",
        "It means more than you know.",
        "Okay then.",
        "I'll see you whenever."
    };

    // Timing
    private static final int INITIAL_DELAY = 80;
    private static final int PER_CHAR_DELAY = 5;
    private static final int HOLD_FRAMES = 150;
    private static final int FADE_FRAMES = 100;

    private int frame = 0;
    private int lineIndex = -1;
    private int lineStartFrame = -1;
    private boolean finished = false;
    private boolean musicStarted = false;
    private int fadeOut = 0;
    private int fadeIn = 220;

    // Player ghost
    private int spriteX;
    private int spriteY;
    private float spriteAlpha = 0.18f;
    private int walkTick = 0;

    // Stars
    private static class Star { float x,y,a,v,t; }
    private final Star[] stars = new Star[70];
    private final Random rng = new Random();

    public IntroCutscene(GamePanel gp){
        this.gp = gp;
        spriteX = -gp.tileSize * 2;
        spriteY = gp.screenHeight/2 - gp.tileSize;
        initStars();
    }

    private void initStars(){
        for(int i=0;i<stars.length;i++){
            Star s = new Star();
            s.x = rng.nextFloat()*gp.screenWidth;
            s.y = rng.nextFloat()*gp.screenHeight;
            s.v = 0.04f + rng.nextFloat()*0.12f;
            s.a = 0.15f + rng.nextFloat()*0.35f;
            s.t = rng.nextFloat()* (float)Math.PI * 2f;
            stars[i] = s;
        }
    }

    public void skip(){
        if(finished) return;
        finished = true;
        fadeOut = 40;
    }

    private int typeDuration(int idx){
        return lines[idx].length() * PER_CHAR_DELAY;
    }

    public void update(){
        if(!musicStarted){
            gp.stopMusic();
            gp.music.setFilePath("res/sound/introgame.wav");
            gp.music.play();
            musicStarted = true;
        }

        if(finished){
            fadeOut += 8;
            if(fadeOut >= 255) gp.enterMenu();
            return;
        }

        frame++;

        if(fadeIn > 0){
            fadeIn -= 4;
            if(fadeIn < 0) fadeIn = 0;
        }

        // Advance dialogue
        if(lineIndex < 0 && frame >= INITIAL_DELAY){
            nextLine();
        } else if(lineIndex >= 0 && lineIndex < lines.length){
            int elapsed = frame - lineStartFrame;
            int typeEnd = typeDuration(lineIndex);
            int holdEnd = typeEnd + HOLD_FRAMES;
            int fadeEnd = holdEnd + FADE_FRAMES;
            if(elapsed > fadeEnd){
                nextLine();
            }
        }

        if(lineIndex >= lines.length && !finished){
            finished = true;
        }

        // Player drift
        walkTick++;
        if(walkTick % 7 == 0){
            spriteX += 1;
            if(spriteAlpha < 0.55f) spriteAlpha += 0.002f;
        }

        // Stars
        for(Star s : stars){
            s.y += s.v;
            if(s.y > gp.screenHeight){
                s.y = -3;
                s.x = rng.nextFloat()*gp.screenWidth;
                s.v = 0.04f + rng.nextFloat()*0.12f;
            }
            s.t += 0.02f;
        }
    }

    private void nextLine(){
        lineIndex++;
        lineStartFrame = frame;
    }

    public void draw(Graphics2D g2){
        // Background gradient
        g2.setColor(Color.BLACK);
        g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        Paint old = g2.getPaint();
        RadialGradientPaint rg = new RadialGradientPaint(
                new Point(gp.screenWidth/2, gp.screenHeight/2),
                gp.screenWidth/1.3f,
                new float[]{0f,0.6f,1f},
                new Color[]{
                        new Color(30,6,42),
                        new Color(55,16,82),
                        new Color(14,3,26)
                }
        );
        g2.setPaint(rg);
        g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        g2.setPaint(old);

        // Stars
        for(Star s : stars){
            float tw = (float)(Math.sin(s.t) * 0.5 + 0.5);
            float a = s.a * (0.5f + tw);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f,a)));
            g2.setColor(new Color(210,170,255));
            g2.fillRect((int)s.x,(int)s.y,2,2);
        }
        g2.setComposite(AlphaComposite.SrcOver);

        // Player sprite
        Image walkFrame = null;
        try{
            walkFrame = gp.player.getRightWalkFrame((frame/28)%2);
        }catch(Exception ignored){}
        if(walkFrame == null) walkFrame = gp.player.idle;
        if(walkFrame != null){
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, spriteAlpha));
            g2.drawImage(walkFrame, spriteX, spriteY, gp.tileSize, gp.tileSize, null);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // Current line
        if(lineIndex >= 0 && lineIndex < lines.length){
            String full = lines[lineIndex];
            int elapsed = frame - lineStartFrame;
            int typeEnd = typeDuration(lineIndex);
            int holdEnd = typeEnd + HOLD_FRAMES;

            int visChars = (elapsed <= typeEnd)
                    ? Math.min(full.length(), elapsed / PER_CHAR_DELAY)
                    : full.length();
            String vis = full.substring(0, visChars);

            // Alpha fade out phase
            float alpha = 1f;
            if(elapsed > holdEnd){
                float f = (float)(elapsed - holdEnd) / FADE_FRAMES;
                alpha = Math.max(0f, 1f - f);
            }

            g2.setFont(new Font("Consolas", Font.PLAIN, 24));
            int w = g2.getFontMetrics().stringWidth(vis);
            int x = gp.screenWidth/2 - w/2;
            int y = gp.screenHeight/2 - gp.tileSize*2;

            // Glow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.35f));
            g2.setColor(new Color(130,60,210));
            g2.drawString(vis, x+2, y+2);

            // Text
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.95f));
            g2.setColor(new Color(220,190,255));
            g2.drawString(vis, x, y);

            // Cursor
            if(visChars < full.length() && (frame/25)%2==0){
                int cw = g2.getFontMetrics().stringWidth(vis);
                g2.fillRect(x + cw + 8, y - 18, 14, 26);
            }

            g2.setComposite(AlphaComposite.SrcOver);
        }

        // Skip hint after third line
        if(lineIndex >= 2 && !finished){
            float a = Math.min(1f, (frame - lineStartFrame)/120f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2.setColor(new Color(190,160,240));
            g2.drawString("Skip: O / Enter / Esc", 18, gp.screenHeight - 28);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // Global fades
        if(fadeIn > 0){
            g2.setColor(new Color(0,0,0, fadeIn));
            g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        }
        if(finished){
            fadeOut += 4;
            g2.setColor(new Color(0,0,0, Math.min(255, fadeOut)));
            g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        }
    }
}