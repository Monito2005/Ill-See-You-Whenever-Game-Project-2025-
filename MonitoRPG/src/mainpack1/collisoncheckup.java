package mainpack1;

import entity.entity;

public class collisoncheckup {
    gamepannel gp;

    public collisoncheckup(gamepannel gp) {
        this.gp = gp;
    }

    public void checkTile(entity entity) {
        int entityLeftWorldX = entity.worldx + entity.solidArea.x;
        int entityRightWorldX = entity.worldx + entity.solidArea.x + entity.solidArea.width;
        int entityTopWorldY = entity.worldy + entity.solidArea.y;
        int entityBottomWorldY = entity.worldy + entity.solidArea.y + entity.solidArea.height;

        int entityLeftCol = entityLeftWorldX / gp.tileSize;
        int entityRightCol = entityRightWorldX / gp.tileSize;
        int entityTopRow = entityTopWorldY / gp.tileSize;
        int entityBottomRow = entityBottomWorldY / gp.tileSize;

        int tileNum1, tileNum2;

        switch (entity.direction) {
            case "up":
                entityTopRow = (entityTopWorldY - entity.speed) / gp.tileSize;
                if (entityLeftCol < 0 || entityLeftCol >= gp.maxWorldCol || entityTopRow < 0 || entityTopRow >= gp.maxWorldRow
                        || entityRightCol < 0 || entityRightCol >= gp.maxWorldCol) {
                    entity.collisionOn = true;
                } else {
                    tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                    tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                    if (gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
                        entity.collisionOn = true;
                    }
                }
                break;
            case "down":
                entityBottomRow = (entityBottomWorldY + entity.speed) / gp.tileSize;
                if (entityLeftCol < 0 || entityLeftCol >= gp.maxWorldCol || entityBottomRow < 0 || entityBottomRow >= gp.maxWorldRow
                        || entityRightCol < 0 || entityRightCol >= gp.maxWorldCol) {
                    entity.collisionOn = true;
                } else {
                    tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                    tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                    if (gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
                        entity.collisionOn = true;
                    }
                }
                break;
            case "left":
                entityLeftCol = (entityLeftWorldX - entity.speed) / gp.tileSize;
                if (entityLeftCol < 0 || entityLeftCol >= gp.maxWorldCol || entityTopRow < 0 || entityTopRow >= gp.maxWorldRow
                        || entityBottomRow < 0 || entityBottomRow >= gp.maxWorldRow) {
                    entity.collisionOn = true;
                } else {
                    tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                    tileNum2 = gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                    if (gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
                        entity.collisionOn = true;
                    }
                }
                break;
            case "right":
                entityRightCol = (entityRightWorldX + entity.speed) / gp.tileSize;
                if (entityRightCol < 0 || entityRightCol >= gp.maxWorldCol || entityTopRow < 0 || entityTopRow >= gp.maxWorldRow
                        || entityBottomRow < 0 || entityBottomRow >= gp.maxWorldRow) {
                    entity.collisionOn = true;
                } else {
                    tileNum1 = gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                    tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                    if (gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
                        entity.collisionOn = true;
                    }
                }
                break;
            default:
                break;
        }
    }

}


