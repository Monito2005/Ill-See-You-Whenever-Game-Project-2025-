package mainpack1;

import entity.entity;

public class CollisionChecker {
    GamePanel gp;
    public CollisionChecker(GamePanel gp){ this.gp = gp; }

    public void checkTile(entity e){
        int solidAreaLeftX = e.worldx + e.solidArea.x;
        int solidAreaRightX = e.worldx + e.solidArea.x + e.solidArea.width;
        int solidAreaTopY = e.worldy + e.solidArea.y;
        int solidAreaBottomY = e.worldy + e.solidArea.y + e.solidArea.height;

        int col1 = solidAreaLeftX / gp.tileSize;
        int col2 = solidAreaRightX / gp.tileSize;
        int row1 = solidAreaTopY / gp.tileSize;
        int row2 = solidAreaBottomY / gp.tileSize;

        // Clamp to map bounds
        col1 = Math.max(0, Math.min(col1, gp.maxWorldCol - 1));
        col2 = Math.max(0, Math.min(col2, gp.maxWorldCol - 1));
        row1 = Math.max(0, Math.min(row1, gp.maxWorldRow - 1));
        row2 = Math.max(0, Math.min(row2, gp.maxWorldRow - 1));

        for(int row = row1; row <= row2; row++){
            for(int col = col1; col <= col2; col++){
                // FIX: use mapTileNum[col][row] to match TileManager storage
                int tileNum = gp.tileM.mapTileNum[col][row];
                
                if(tileNum >= 0 && tileNum < gp.tileM.tile.length && gp.tileM.tile[tileNum] != null){
                    if(gp.tileM.tile[tileNum].collision){
                        e.collisionOn = true;
                    }
                }
            }
        }
    }

    private void collide(entity e, int c1, int r1, int c2, int r2){
        if(outOfBounds(c1,r1) || outOfBounds(c2,r2)) return;
        int t1 = gp.tileM.mapTileNum[c1][r1];
        int t2 = gp.tileM.mapTileNum[c2][r2];
        if(gp.tileM.tile[t1].collision || gp.tileM.tile[t2].collision){
            e.collisionOn = true;
        }
    }

    private boolean outOfBounds(int c, int r){
        return c<0 || r<0 || c>=gp.maxWorldCol || r>=gp.maxWorldRow;
    }
}


