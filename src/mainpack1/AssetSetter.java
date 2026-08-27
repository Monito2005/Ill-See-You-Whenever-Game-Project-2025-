package mainpack1;

import entity.npcstudent1;

public class AssetSetter {
    GamePanel gp;
    public AssetSetter(GamePanel gp){ this.gp = gp; }

    public void setNPC(){
        gp.npc[0] = new npcstudent1(gp);
        gp.npc[0].worldx = 11 * gp.tileSize;
        gp.npc[0].worldy = 25 * gp.tileSize;
    }
}
