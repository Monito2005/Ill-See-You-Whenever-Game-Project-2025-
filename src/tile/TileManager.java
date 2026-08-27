package tile;

import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.FileReader;
import javax.imageio.ImageIO;
import java.io.File;
import mainpack1.GamePanel;

public class TileManager {
    GamePanel gp;
    public Tile[] tile;
    public int[][] mapTileNum;

    public TileManager(GamePanel gp){
        this.gp = gp;
        tile = new Tile[14];  // increase from 10 to 14
        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];
        getTileImage();
        loadMap("res/maps/map01.txt");
    }

    public void getTileImage(){
        try{
            tile[0] = new Tile();
            tile[0].image = loadTile("res/tiles/Tiles_00.png");
            tile[0].collision = true;

            tile[1] = new Tile();
            tile[1].image = loadTile("res/tiles/Tiles_01.png");
            tile[1].collision = false;

            tile[2] = new Tile();
            tile[2].image = loadTile("res/tiles/Tiles_02.png");
            tile[2].collision = false;

            tile[3] = new Tile();
            tile[3].image = loadTile("res/tiles/Tiles_03.png");
            tile[3].collision = true;

            tile[4] = new Tile();
            tile[4].image = ImageIO.read(new File("res/tiles/Tiles_04.png"));
            tile[4].collision = false;   // wall - blocks movement

            tile[5] = new Tile();
            tile[5].image = ImageIO.read(new File("res/tiles/Tiles_05.png"));
            tile[5].collision = false;   // grass - blocks movement

            tile[6] = new Tile();
            tile[6].image = ImageIO.read(new File("res/tiles/Tiles_06.png"));
            tile[6].collision = true;   // tree - blocks movement

            tile[7] = new Tile();
            tile[7].image = ImageIO.read(new File("res/tiles/Tiles_07.png"));
            tile[7].collision = true;   // tree - blocks movement
            
            tile[8] = new Tile();
            tile[8].image = ImageIO.read(new File("res/tiles/Tiles_08.png"));
            tile[8].collision = true;   // Tree - blocks movement

            tile[9] = new Tile();   
            tile[9].image = ImageIO.read(new File("res/tiles/Tiles_09.png"));
            tile[9].collision = true;   // Tree - blocks movement

            tile[10] = new Tile();
            tile[10].image = ImageIO.read(new File("res/tiles/Tiles_10.png"));
            tile[10].collision = false;   // Tree - blocks movement

            tile[11] = new Tile();
            tile[11].image = ImageIO.read(new File("res/tiles/Tiles_11.png"));
            tile[11].collision = false;   // Tree - blocks movement
            
            tile[12] = new Tile();
            tile[12].image = ImageIO.read(new File("res/tiles/Tiles_12.png"));
            tile[12].collision = false;   // Tree - blocks movement
            
            tile[13] = new Tile();
            tile[13].image = ImageIO.read(new File("res/tiles/Tiles_13.png"));
            tile[13].collision = true;   // Tree - blocks movement


        }catch(Exception e){
            System.out.println("Tile loading error: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback: fill uninitialized with tile[0]
        if(tile[0] != null && tile[0].image != null){
            for(int i=1; i<tile.length; i++){
                if(tile[i] == null){
                    tile[i] = new Tile();
                    tile[i].image = tile[0].image;
                    tile[i].collision = false;
                }
            }
        }
    }

    private java.awt.image.BufferedImage loadTile(String path){
        try{
            java.io.File f = new java.io.File(path);
            if(f.exists()){
                System.out.println("Loading: " + f.getAbsolutePath());
                return javax.imageio.ImageIO.read(f);
            }
            System.out.println("NOT FOUND: " + f.getAbsolutePath());
        }catch(Exception e){
            System.out.println("Error loading " + path + ": " + e.getMessage());
        }
        return null;
    }

    public void loadMap(String path){
        try(BufferedReader br = new BufferedReader(new FileReader(path))){
            int row=0;
            String line;
            while((line = br.readLine())!=null && row < gp.maxWorldRow){
                String[] nums = line.trim().split(",");  // change from " " to ","
                for(int col=0; col<gp.maxWorldCol && col<nums.length; col++){
                    int n;
                    try{ n = Integer.parseInt(nums[col].trim()); }catch(Exception ex){ n=0; }  // add .trim() to handle whitespace
                    if(n<0 || n>=tile.length || tile[n]==null) n=0;
                    mapTileNum[col][row] = n;
                }
                row++;
            }
        }catch(Exception e){
            // fallback fill
            for(int r=0;r<gp.maxWorldRow;r++)
                for(int c=0;c<gp.maxWorldCol;c++)
                    mapTileNum[c][r]=0;
        }
    }

    public void draw(Graphics2D g2){
        for(int col=0; col<gp.maxWorldCol; col++){
            for(int row=0; row<gp.maxWorldRow; row++){
                int num = mapTileNum[col][row];
                int worldX = col * gp.tileSize;
                int worldY = row * gp.tileSize;
                int screenX = worldX - gp.player.worldx + gp.player.screenX;
                int screenY = worldY - gp.player.worldy + gp.player.screenY;

                if(worldX + gp.tileSize > gp.player.worldx - gp.player.screenX &&
                   worldX - gp.tileSize < gp.player.worldx + gp.player.screenX &&
                   worldY + gp.tileSize > gp.player.worldy - gp.player.screenY &&
                   worldY - gp.tileSize < gp.player.worldy + gp.player.screenY){
                    g2.drawImage(tile[num].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                }
            }
        }
    }
}



