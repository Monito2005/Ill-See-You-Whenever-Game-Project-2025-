package entity;

import java.awt.image.BufferedImage;

public class NPC {
    public String name;
    public int openness;      // higher = more guarded
    public int maxOpenness;
    public int charisma;
    public int resilience;
    public int exp;
    public BufferedImage image;

    // Optional: dialogue lines for conversations
    public String[] lines;

    public NPC(String name, int openness, int maxOpenness, int charisma, int resilience, int exp) {
        this.name = name;
        this.openness = openness;
        this.maxOpenness = maxOpenness;
        this.charisma = charisma;
        this.resilience = resilience;
        this.exp = exp;
    }

    // IMPLEMENTED: name + dialogue constructor
    public NPC(String name, String[] lines) {
        this.name = name;
        this.lines = lines;
        // default stats (tweak as needed)
        this.maxOpenness = 60;
        this.openness = maxOpenness;
        this.charisma = 10;
        this.resilience = 5;
        this.exp = 30;
    }

    public boolean isGuarded() {
        return openness > 0;
    }

    public int receiveSharing(int impact) {
        int actualImpact = Math.max(1, impact - resilience);
        openness -= actualImpact;
        if (openness < 0) openness = 0;
        return actualImpact;
    }

    public int response() {
        return resilience + (int)(Math.random() * (resilience / 2));
    }
}
