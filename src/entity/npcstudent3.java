package entity;

import mainpack1.GamePanel;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class npcstudent3 extends entity {

	// Shy, nervous overworld dialogue (love interest, kept subtle)
	private String[] dialogues = new String[]{
		"Mysterious Girl: 'Oh... hi.'",
		"(She looks away, searching for words.)",
		"Mysterious Girl: 'I'm... a bit nervous.'",
		"Mysterious Girl: 'But I want to try talking.'"
	};

	// Simple animation counters
	private int spriteCounter = 0;
	private int spriteNum = 1;

	// Run-away state
	private boolean fleeing = false;
	private int fleeFrames = 0;

	public npcstudent3(GamePanel gp){
		super(gp);
		speed = 0;            // stationary
		direction = "down";
		solidArea = new Rectangle(8,16,32,32);
		loadImages();
	}

	// Conversation payload: name is unknown to player
	public NPC getConversationNPC(){
		return new NPC(
			"Mysterious Girl",
			new String[]{
				"Sorry, I'm... really bad at this.",
				"I want to talk, but my brain forgets words.",
				"Maybe, if we take it slow, I can try again."
			}
		);
	}

	// Trigger fleeing when interacted
	public void runAway(){
		if(!fleeing){
			fleeing = true;
			fleeFrames = 40; // ~0.6s at 60 FPS
			System.out.println("[npcstudent3] She runs away and disappears...");
		}
	}

	private void loadImages(){
		// Try a dedicated idle; adjust path as needed
		idle  = load("res/npc3/delia.png");
		// Optional alternates to avoid nulls
	

		// Backfill with idle if frames missing
		if(idle != null){
			if(down1==null) down1=idle; if(down2==null) down2=idle;
			if(up1==null)   up1=idle;   if(up2==null)   up2=idle;
			if(left1==null) left1=idle; if(left2==null) left2=idle;
			if(right1==null)right1=idle;if(right2==null)right2=idle;
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
		// No standard movement; only idle or flee
		if(fleeing && fleeFrames > 0){
			// move up quickly then finish
			direction = "up";
			int oldY = worldy;
			worldy -= 3; // faster than normal
			collisionOn = false;
			gp.cChecker.checkTile(this);
			if(collisionOn) worldy = oldY;
			fleeFrames--;
			// after flee finishes, entity should be removed by GamePanel
		}

		// No movement; only idle animation ticker
		spriteCounter++;
		if(spriteCounter > 24){
			spriteNum = (spriteNum==1)?2:1;
			spriteCounter = 0;
		}
	}

	@Override
	public String[] interact(){
		// Return shy lines; GamePanel will trigger runAway() and remove her
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

		int playerScreenX = gp.player.screenX;
		int playerScreenY = gp.player.screenY;
		int screenX = worldx - gp.player.worldx + playerScreenX;
		int screenY = worldy - gp.player.worldy + playerScreenY;
		g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
	}
}
