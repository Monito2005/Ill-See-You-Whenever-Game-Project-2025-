package tile;
import java.awt.Graphics2D;
import java.io.BufferedReader;

import javax.imageio.ImageIO;
import mainpack1.gamepannel;

public class tilemanager {
    gamepannel gp;
    public tile[] tile;
    public int mapTileNum[][];

    public tilemanager(gamepannel gp){
        this.gp = gp;
        tile = new tile[10];
        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];
        getTileImage();
            loadMap();
    }
    public void getTileImage(){
        try{
            tile[0] = new tile();
            tile[0].image = ImageIO.read(new java.io.File("MonitoRPG/res/tiles/grass.png"));

            tile[1] = new tile();
            tile[1].image = ImageIO.read(new java.io.File("MonitoRPG/res/tiles/dirt.png"));

            tile[2] = new tile();
            tile[2].image = ImageIO.read(new java.io.File("MonitoRPG/res/tiles/water.png"));
            tile[2].collision = true;

        }   catch(Exception e){
            e.printStackTrace();}
       
    }

    public void loadMap(){
        try{
            BufferedReader br = new BufferedReader(new java.io.FileReader("MonitoRPG/res/maps/map01.txt"));
            int row =0;
            String line;
            while((line = br.readLine()) != null && row < gp.maxWorldRow){
                line = line.replaceAll("\\P{Print}", "").trim();
                if(line.isEmpty()) continue;
                String numbers[] = line.split(" +");
                for(int col = 0; col < gp.maxWorldCol && col < numbers.length; col++){
                    if(numbers[col] != null && !numbers[col].isEmpty()){
                        int num = Integer.parseInt(numbers[col]);
                        mapTileNum[col][row] = num;
                    }
                }
                row++;
            }
            br.close();

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public void draw(Graphics2D g2){
       
        int worldcol =0;
        int worldrow =0;
      

       while (worldcol < gp.maxWorldCol && worldrow < gp.maxWorldRow){
            int tileNum = mapTileNum[worldcol][worldrow];

            int worldx = worldcol * gp.tileSize;
            int worldy = worldrow * gp.tileSize;
            int screenx = worldx - gp.player.worldx + gp.player.screenX;
            int screeny = worldy - gp.player.worldy + gp.player.screenY;

            if(worldx > gp.player.worldx - gp.player.screenX - gp.tileSize &&
               worldx < gp.player.worldx + gp.player.screenX + gp.tileSize &&
               worldy > gp.player.worldy - gp.player.screenY - gp.tileSize &&
               worldy < gp.player.worldy + gp.player.screenY + gp.tileSize){

        g2.drawImage(tile[tileNum].image, screenx, screeny, gp.tileSize, gp.tileSize, null);
            }
        worldcol++;
     

        if(worldcol == gp.maxWorldCol){
            worldcol =0;
 
            worldrow++;

        }
       
}
}
    }



