package mainpack1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Random;

public class MainMenu {
    private final GamePanel gp;
    private final Random random = new Random();
    private final int starCount = 60;
    private final int[] starX = new int[starCount];
    private final int[] starY = new int[starCount];
    private final int[] starSize = new int[starCount];
    private final int[] starSpeed = new int[starCount];
    private final float[] shootX = new float[5];
    private final float[] shootY = new float[5];
    private final float[] shootVX = new float[5];
    private final float[] shootVY = new float[5];
    private final int[] shootAlpha = new int[5];
    private final boolean[] shootActive = new boolean[5];
    private final float[] burstX = new float[20];
    private final float[] burstY = new float[20];
    private final float[] burstVX = new float[20];
    private final float[] burstVY = new float[20];
    private final int[] burstAlpha = new int[20];
    private final boolean[] burstActive = new boolean[20];
    private int selection = 0;
    private final String[] items = {"Start", "Exit"};

    private int boomFrames = 0;
    private boolean boomTriggered = false;

    public MainMenu(GamePanel gp){
        this.gp = gp;
        initBackground();
    }

    private void initBackground(){
        for(int i = 0; i < starCount; i++){
            starX[i] = random.nextInt(gp.screenWidth);
            starY[i] = random.nextInt(gp.screenHeight);
            starSize[i] = 1 + random.nextInt(2);
            starSpeed[i] = 1;
        }
    }

    private void updateBackground(){
        for(int i = 0; i < starCount; i++){
            starY[i] += starSpeed[i];
            if(starY[i] > gp.screenHeight + 10){
                starY[i] = -10;
                starX[i] = random.nextInt(gp.screenWidth);
            }
        }

        for(int i = 0; i < shootActive.length; i++){
            if(!shootActive[i]){
                if(random.nextInt(600) == 0){
                    spawnShootingStar(i);
                }
            } else {
                shootX[i] += shootVX[i];
                shootY[i] += shootVY[i];
                shootAlpha[i] -= 5;

                if(shootAlpha[i] <= 0 || shootX[i] < -50 || shootY[i] < -50 || shootX[i] > gp.screenWidth + 50 || shootY[i] > gp.screenHeight + 50){
                    shootActive[i] = false;
                    triggerBurst((int) shootX[i], (int) shootY[i]);
                }
            }
        }

        for(int i = 0; i < burstActive.length; i++){
            if(!burstActive[i]) continue;
            burstX[i] += burstVX[i];
            burstY[i] += burstVY[i];
            burstAlpha[i] -= 7;
            burstVX[i] *= 0.975f;
            burstVY[i] *= 0.975f;
            if(burstAlpha[i] <= 0){
                burstActive[i] = false;
            }
        }
    }

    private void triggerBurst(int x, int y){
        for(int i = 0; i < burstActive.length; i++){
            if(!burstActive[i]){
                burstX[i] = x;
                burstY[i] = y;
                double angle = (Math.PI * 2 * i) / burstActive.length;
                float speed = 0.8f + random.nextFloat() * 1.8f;
                burstVX[i] = (float)(Math.cos(angle) * speed);
                burstVY[i] = (float)(Math.sin(angle) * speed);
                burstAlpha[i] = 200;
                burstActive[i] = true;
            }
        }
    }

    private void spawnShootingStar(int index){
        int side = random.nextInt(4);
        if(side == 0){
            shootX[index] = -20; shootY[index] = random.nextInt(gp.screenHeight / 2); shootVX[index] = 1.2f + random.nextFloat() * 1.4f; shootVY[index] = 0.9f + random.nextFloat() * 1.1f;
        } else if(side == 1){
            shootX[index] = random.nextInt(gp.screenWidth); shootY[index] = -20; shootVX[index] = 0.8f + random.nextFloat() * 1.1f; shootVY[index] = 1.5f + random.nextFloat() * 1.4f;
        } else if(side == 2){
            shootX[index] = gp.screenWidth + 20; shootY[index] = random.nextInt(gp.screenHeight / 2); shootVX[index] = -(1.2f + random.nextFloat() * 1.4f); shootVY[index] = 0.9f + random.nextFloat() * 1.1f;
        } else {
            shootX[index] = random.nextInt(gp.screenWidth); shootY[index] = gp.screenHeight + 20; shootVX[index] = 0.8f + random.nextFloat() * 1.1f; shootVY[index] = -(1.5f + random.nextFloat() * 1.4f);
        }
        shootAlpha[index] = 200;
        shootActive[index] = true;
    }

    public void update(){
        updateBackground();
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
        gp.se.playTone(360, 45, 0.18f);
    }

    public void confirm(){
        gp.se.playTone(640, 90, 0.24f);
        if(selection == 0){
            boomTriggered = true;
            boomFrames = 20;
        } else {
            System.exit(0);
        }
    }

    public void draw(Graphics2D g2){
        g2.setColor(Color.BLACK);
        g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);

        drawBackgroundParticles(g2);

        String title = "I'll See You Whenever";
        int baseSize = 46;
        int extra = boomFrames > 0 ? 16 : 0;
        int jitter = boomFrames > 0 ? 2 : 0;

        g2.setFont(new Font("Arial", Font.BOLD, baseSize + extra));
        g2.setColor(new Color(130,60,210));
        g2.drawString(title, 60 + jitter, 140 + jitter);

        g2.setColor(Color.WHITE);
        g2.drawString(title, 60, 140);

        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        for(int i=0;i<items.length;i++){
            g2.setColor(i==selection?Color.YELLOW:Color.LIGHT_GRAY);
            g2.drawString((i==selection?"> ":"  ")+items[i],120,240+i*50);
        }

        g2.setFont(new Font("Arial", Font.PLAIN,16));
        g2.setColor(Color.GRAY);
        g2.drawString("W/S or Up/Down: Select   O/Enter: Confirm", 120, gp.screenHeight - 40);
    }

    private void drawBackgroundParticles(Graphics2D g2){
        for(int i = 0; i < starCount; i++){
            g2.setColor(new Color(255, 255, 255, 150));
            int s = starSize[i];
            g2.fillRect(starX[i], starY[i], s, s);
        }

        for(int i = 0; i < shootActive.length; i++){
            if(!shootActive[i]) continue;
            int alpha = Math.max(0, shootAlpha[i]);
            g2.setColor(new Color(180, 220, 255, alpha));

            int trailSteps = 8;
            int x1 = (int)shootX[i];
            int y1 = (int)shootY[i];
            for(int step = 0; step < trailSteps; step++){
                int px = (int)(shootX[i] - shootVX[i] * step * 2.0f);
                int py = (int)(shootY[i] - shootVY[i] * step * 2.0f);
                g2.fillRect(px, py, 4, 4);
            }

            g2.fillRect(x1, y1, 5, 5);
        }

        for(int i = 0; i < burstActive.length; i++){
            if(!burstActive[i]) continue;
            int alpha = Math.max(0, burstAlpha[i]);
            g2.setColor(new Color(255, 240, 180, alpha));
            g2.fillRect((int)burstX[i], (int)burstY[i], 4, 4);
        }
    }
}