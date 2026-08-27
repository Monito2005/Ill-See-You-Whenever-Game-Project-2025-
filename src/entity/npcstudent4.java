package entity;

import mainpack1.GamePanel;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class npcstudent4 extends entity {

	// Cocky but friendly overworld dialogue
	private String[] dialogues = new String[]{
		"Guero: 'You look like you've seen this kind of place before.'",
		"Guero: 'We got similar roots, huh?'",
		"Guero: 'You'll be okay out here. Trust me.'"
	};

	// Simple animation counters (kept for potential idle blink later)
	private int spriteCounter = 0;
	private int spriteNum = 1;

	public npcstudent4(GamePanel gp){
		super(gp);
		speed = 0;               // stationary
		direction = "down";
		solidArea = new Rectangle(8,16,32,32);
		loadImages();
	}

	public NPC getConversationNPC(){
		return new NPC(
			"Guero",
			new String[]{
				"Cocky? Maybe. But I mean well.",
				"We had similar schools similar fights.",
				"You'll be okay out here. I promise."
			}
		);
	}

	private void loadImages(){
		// Only idle is needed for a stationary character
		idle = load("res/npc4/el guero.png");
		// Ensure all other directions fall back to idle to avoid nulls
		down1 = down2 = up1 = up2 = left1 = left2 = right1 = right2 = idle;
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
		// Stationary: no movement, no patrol.
		// Keep a minimal idle animation toggle if needed in future.
		spriteCounter++;
		if(spriteCounter > 60){ // slower idle tick
			spriteNum = (spriteNum==1)?2:1;
			spriteCounter = 0;
		}
	}

	@Override
	public String[] interact(){ return dialogues; }

	@Override
	public void draw(Graphics2D g2){
		// Always render idle for a stationary character
		BufferedImage image = idle;
		if(image == null) image = idle;

		int playerScreenX = gp.player.screenX;
		int playerScreenY = gp.player.screenY;
		int screenX = worldx - gp.player.worldx + playerScreenX;
		int screenY = worldy - gp.player.worldy + playerScreenY;
		g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);

		// Arrow indicator above head (blinking)
		int arrowWidth = Math.max(8, gp.tileSize / 4);
		int arrowHeight = Math.max(6, gp.tileSize / 6);
		int bob = (int)(Math.sin(System.nanoTime() * 1e-9 * 6) * 2);
		int ax = screenX + gp.tileSize / 2;
		int ay = screenY - 6 + bob;
		int[] xs = { ax, ax - arrowWidth/2, ax + arrowWidth/2 };
		int[] ys = { ay, ay + arrowHeight, ay + arrowHeight };
		boolean visible = (System.currentTimeMillis() / 400) % 2 == 0;
		if(visible){
			g2.setColor(new java.awt.Color(255, 255, 120));
			g2.fillPolygon(xs, ys, 3);
			g2.setColor(new java.awt.Color(200, 180, 60));
			g2.drawPolygon(xs, ys, 3);
		}
	}
}
