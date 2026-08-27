package mainpack1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class UI {
    GamePanel gp;
    Font font = new Font("Arial", Font.PLAIN, 28);
    public boolean messageOn = false;
    public String[] currentDialogue;
    public int dialogueIndex = 0;
    private String displayText = "";
    private int charIndex = 0;
    private int tick = 0;
    private boolean full = false;

    public UI(GamePanel gp){ this.gp = gp; }

    public void showDialogue(String[] lines){
        if(lines==null || lines.length==0){ messageOn=false; return; }
        currentDialogue = lines;
        dialogueIndex = 0;
        charIndex = 0;
        displayText = "";
        full = false;
        messageOn = true;
    }

    public void nextDialogue(){
        if(!messageOn) return;
        if(!full){
            displayText = currentDialogue[dialogueIndex];
            charIndex = displayText.length();
            full = true;
            return;
        }
        dialogueIndex++;
        if(dialogueIndex >= currentDialogue.length){
            messageOn = false;
            currentDialogue = null;
            displayText = "";
            return;
        }
        charIndex = 0;
        displayText = "";
        full = false;
    }

    public void update(){
        if(!messageOn || currentDialogue==null || full) return;
        tick++;
        if(tick >= 2){
            tick = 0;
            String fullText = currentDialogue[dialogueIndex];
            if(charIndex < fullText.length()){
                charIndex++;
                displayText = fullText.substring(0,charIndex);
            }else{
                full = true;
            }
        }
    }

    public void draw(Graphics2D g2){
        if(!messageOn || currentDialogue==null) return;
        int x = gp.tileSize*2;
        int y = gp.tileSize*8;
        int w = gp.screenWidth - gp.tileSize*4;
        int h = gp.tileSize*3;

        g2.setColor(Color.black);
        g2.fillRoundRect(x,y,w,h,20,20);
        g2.setColor(Color.white);
        g2.drawRoundRect(x,y,w,h,20,20);

        g2.setFont(font);
        int textX = x+30;
        int textY = y+50;
        int maxWidth = w-60;
        for(String line : wrap(displayText, g2, maxWidth)){
            g2.drawString(line, textX, textY);
            textY += 34;
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        if(full){
            g2.drawString("Press O", x+w-120, y+h-20);
        }else{
            g2.drawString("...", x+w-60, y+h-20);
        }
    }

    private java.util.List<String> wrap(String text, Graphics2D g2, int max){
        java.util.List<String> out = new java.util.ArrayList<>();
        if(text==null) return out;
        String[] words = text.split(" ");
        String line="";
        for(String w: words){
            String test = line + w + " ";
            if(g2.getFontMetrics().stringWidth(test) > max && !line.isEmpty()){
                out.add(line.trim());
                line = w + " ";
            }else{
                line = test;
            }
        }
        if(!line.isEmpty()) out.add(line.trim());
        return out;
    }
}