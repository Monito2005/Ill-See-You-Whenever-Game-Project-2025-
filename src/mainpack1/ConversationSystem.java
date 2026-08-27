package mainpack1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import entity.NPC;
import entity.entity;

public class ConversationSystem {

    GamePanel gp;
    public boolean inConversation = false;
    private NPC currentNPC;
    // Replace npcstudent1-typed reference with a generic entity
    private entity currentNPCEntity;

    // Overworld convo phases: 0 opening, 1 npc line, 2 responses, 3 npc react, 6 transition-to-battle
    private int phase = 0;
    private int selected = 0;        // kept for minimal changes, not used for battle choice now
    private int responseIndex = 0;

    private String battleLog = "";

    // Simple transition timer to battle
    private int transitionFrames = 0;
    private final int maxTransitionFrames = 30;

    // Hook to your battle system
    private BattleSystem battle;
    // Proxy mode: after transition, delegate to battle but keep this UI alive
    private boolean proxyBattle = false;

    // Patrol fields
    private boolean patrolActive = false;
    private int patrolOriginX = 0;
    private int patrolOriginY = 0;
    private int patrolDir = 0;
    private int patrolTimer = 0;
    private final int patrolChangeInterval = 50; // frames between direction changes

    private static class Response {
        String text;
        int affinityDelta;
        Response(String text, int delta){
            this.text = text; this.affinityDelta = delta;
        }
    }
    private Response[] responses;

    // Long-form overworld scripts per persona (extend playtime in overworld)
    private String[] owHumberto = new String[]{
        "Humberto: 'Hey... you look new.'",
        "He glances at the horizon. 'This place takes time.'",
        "You ask about the path ahead.",
        "Humberto: 'Slow steps. That's how you get somewhere.'",
        "He listens more than he speaks.",
        "Humberto: 'Pressure isn't the enemy. Rushing is.'",
        "You talk about fitting in.",
        "Humberto: 'You'll find your pace. Let it be yours.'",
        "A few students pass by. He nods calmly.",
        "Humberto: 'You're not alone in this.'",
        "Silence stretches a comfortable pause.",
        "Humberto: 'If you need a walk, I'll be around.'"
    };
    private String[] owAndrew = new String[]{
        "Andrew: 'You seem lost.'",
        "He scans you head to toe. 'Stay sharp.'",
        "You mention your goals.",
        "Andrew: 'Goals are fine. Results matter.'",
        "He steps closer. 'People are watching. Be better.'",
        "You ask for advice.",
        "Andrew: 'Earn respect. Don't beg for it.'",
        "He smirks. 'Talk is cheap. Show me substance.'",
        "You hold your ground.",
        "Andrew: 'Good. Finally some spine.'",
        "He turns away, testing you with silence.",
        "Andrew: 'If you're serious, prove it.'"
    };
    // Add: Delia long-form overworld script
    private String[] owDelia = new String[]{
        "Mysterious Girl: 'Oh... hi.'",
        "She tucks a strand of hair. 'I didn't expect you to stop.'",
        "You keep the tone light.",
        "Mysterious Girl: 'It's easier when you're around.'",
        "She glances up, then back down. 'Maybe we could... talk again?'",
        "You suggest a walk some time.",
        "Mysterious Girl: 'I'd like that. No rush.'",
        "A small smile, then quiet.",
        "Mysterious Girl: 'Thanks for being kind.'",
        "She nods. 'See you around?'"
    };
    // Add: Guero long-form overworld script (cocky but friendly, relatable)
    private String[] owGuero = new String[]{
        "Guero: 'You look like you've seen this before.'",
        "He grins. 'We got similar roots, huh?'",
        "You mention your old school.",
        "Guero: 'Same halls. Same grind.'",
        "He nudges your shoulder. 'You'll be okay out here.'",
        "You ask if he really thinks so.",
        "Guero: 'Yeah. You got this. Just breathe.'",
        "He looks around. 'Keep your head up.'",
        "A relaxed silence sits between you.",
        "Guero: 'Catch me later. We’ll talk more.'"
    };

    // Persona-specific responses
    private Response[] responsesDelia = new Response[]{
        new Response("We can take our time.", +3),
        new Response("I enjoy talking with you.", +3),
        new Response("Maybe we could walk later.", +2),
        new Response("No pressure. When you're ready.", +2)
    };
    private Response[] responsesAndrew = new Response[]{
        new Response("I'm here to prove myself.", +1),
        new Response("Pressure makes me focused.", +2),
        new Response("Respect is earned, not begged.", +1),
        new Response("Say what you want.", -1)
    };

    // Track extended overworld script
    private int longDialogueIndex = -1;
    private boolean playingLongDialogue = false;

    public ConversationSystem(GamePanel gp){
        this.gp = gp;
        this.battle = new BattleSystem(gp);
        initResponses();
    }

    private void playTone(double frequency, int durationMs, float volume){
        if(gp.se != null) gp.se.playTone(frequency, durationMs, volume);
    }

    private void playDialogueTone(){
        playTone(520, 70, 0.16f);
    }

    private void initResponses(){
        responses = new Response[]{
            new Response("Yes. I feel unseen too.", +2),
            new Response("I try to be honest about it.", +3),
            new Response("Tell me more about you.", +2),
            new Response("...I'd rather not talk.", -2)
        };
    }

    // Change the signature to accept any NPC entity
    public void startConversation(NPC npc, entity ent){
        currentNPC = npc;
        currentNPCEntity = ent;
        inConversation = true;
        playDialogueTone();
        phase = 0;
        selected = 0;
        responseIndex = 0;
        // If this NPC is blocked (player lost before), show rejection and do not battle
        if(BattleSystem.isBlocked(currentNPC.name)){
            battleLog = currentNPC.name + ": 'Weird. Go away.'";
            // Immediately show and close on input without transition
            transitionFrames = 0;
            return;
        }
        // If this NPC is already befriended, show friendly dialog and do not battle
        if(BattleSystem.isBefriended(currentNPC.name)){
            battleLog = currentNPC.name + ": 'Hey! That talk meant a lot. Let's hang out later.'";
            transitionFrames = 0;
            // Initialize patrol immediately for friendly idle behavior
            beginPatrolIfPossible();
            return;
        }
        // Choose long script and responses by persona
        String name = (npc != null && npc.name != null) ? npc.name : "";
        playingLongDialogue = true;
        longDialogueIndex = 0;
        if(name.equalsIgnoreCase("Andrew")){
            battleLog = owAndrew[0];
            responses = responsesAndrew;
        } else if(name.equalsIgnoreCase("Delia") || name.equalsIgnoreCase("Mysterious Girl")){
            battleLog = owDelia[0];
            responses = responsesDelia;
        } else if(name.equalsIgnoreCase("Guero")){
            battleLog = owGuero[0];
            // keep default empathetic responses
            initResponses();
        } else {
            battleLog = owHumberto[0];
            initResponses();
        }
        transitionFrames = 0;
        return;
    }

    private String nextLongLine(String name){
        String[] script;
        if(name.equalsIgnoreCase("Andrew")) script = owAndrew;
        else if(name.equalsIgnoreCase("Delia") || name.equalsIgnoreCase("Mysterious Girl")) script = owDelia;
        else if(name.equalsIgnoreCase("Guero")) script = owGuero;
        else script = owHumberto;

        if(longDialogueIndex >= 0 && longDialogueIndex < script.length){
            return script[longDialogueIndex];
        }
        return "";
    }

    public void handleInput(KeyEvent e){
        if(!inConversation) return;

        // If proxying battle, forward inputs
        if(proxyBattle){
            battle.handleInput(e);
            return;
        }

        // If blocked, allow a single close
        if(BattleSystem.isBlocked(currentNPC.name)){
            if(e.getKeyCode()==KeyEvent.VK_O || e.getKeyCode()==KeyEvent.VK_ENTER || e.getKeyCode()==KeyEvent.VK_P || e.getKeyCode()==KeyEvent.VK_ESCAPE){
                end();
            }
            return;
        }

        // If befriended, friendly close
        if(BattleSystem.isBefriended(currentNPC.name)){
            if(e.getKeyCode()==KeyEvent.VK_O || e.getKeyCode()==KeyEvent.VK_ENTER){
                end();
            }
            return;
        }

        int c = e.getKeyCode();

        // Extended overworld dialogue playback (unified via nextLongLine)
        if(playingLongDialogue){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                longDialogueIndex++;
                String name = (currentNPC != null && currentNPC.name != null) ? currentNPC.name : "";
                String next = nextLongLine(name);
                if(next != null && !next.isEmpty()){
                    battleLog = next;
                    playDialogueTone();
                }else{
                    // finished long dialogue; proceed to normal flow
                    playingLongDialogue = false;
                    phase = 1;
                    battleLog = (name.isEmpty() ? "NPC" : name) + " meets your eyes in silence.";
                }
            }else if(c==KeyEvent.VK_P || c==KeyEvent.VK_ESCAPE){
                // allow skip if desired
                playingLongDialogue = false;
                phase = 1;
                String name = (currentNPC != null && currentNPC.name != null) ? currentNPC.name : "NPC";
                battleLog = name + " meets your eyes in silence.";
            }
            return;
        }

        if(phase==0){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                phase = 1;
                // Simple, confident introverted tone
                battleLog = currentNPC.name + " meets your eyes in silence.";
                playDialogueTone();
            }else if(c==KeyEvent.VK_P || c==KeyEvent.VK_ESCAPE){
                end();
            }
            return;
        }

        if(phase==1){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                phase = 2;
                battleLog = "How do you respond?";
            }
            return;
        }

        if(phase==2){
            // Choose response
            if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP){
                responseIndex = (responseIndex - 1 + responses.length) % responses.length;
            }else if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN){
                responseIndex = (responseIndex + 1) % responses.length;
            }else if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                battleLog = "You: '" + responses[responseIndex].text + "'";
                phase = 3;
            }
            return;
        }

        if(phase==3){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                battleLog = currentNPC.name + " stands firm. The tension rises.";
                beginBattleTransition(); // auto transition after reaction
            }
            return;
        }

        if(phase==6){
            // Transition in progress; ignore inputs
            return;
        }
    }

    private void beginBattleTransition(){
        transitionFrames = maxTransitionFrames;
        phase = 6;
    }

    public void update(){
        if(!inConversation){
            // Run patrol in overworld when active
            if(patrolActive){
                updatePatrolMovement();
            }
            return;
        }

        if(proxyBattle){
            battle.update();
            // When battle ends, close the conversation proxy
            if(!battle.inBattle){
                // If befriended, start patrol before closing UI
                if(currentNPC != null && BattleSystem.isBefriended(currentNPC.name)){
                    beginPatrolIfPossible();
                }
                end();
            }
            return;
        }

        // Skip transition if blocked or befriended (no battle)
        if(BattleSystem.isBlocked(currentNPC.name) || BattleSystem.isBefriended(currentNPC.name)) return;

        if(phase==6){
            transitionFrames--;
            if(transitionFrames <= 0){
                // Start battle and switch to proxy mode with the actual entity (Humberto or Andrew)
                battle.startBattle(currentNPC, currentNPCEntity);
                proxyBattle = true;
            }
        }
    }

    private void beginPatrolIfPossible(){
        if(currentNPCEntity == null || gp == null) return;
        Integer wx = getEntityCoord(currentNPCEntity, "worldX");
        Integer wy = getEntityCoord(currentNPCEntity, "worldY");
        if(wx == null || wy == null){
            // Coordinates not available; disable patrol
            patrolActive = false;
            return;
        }
        patrolOriginX = wx;
        patrolOriginY = wy;
        patrolActive = true;
        patrolDir = 0;
        patrolTimer = 0;
    }

    private void updatePatrolMovement(){
        if(currentNPCEntity == null || gp == null || !patrolActive) return;

        Integer wx = getEntityCoord(currentNPCEntity, "worldX");
        Integer wy = getEntityCoord(currentNPCEntity, "worldY");
        if(wx == null || wy == null){
            patrolActive = false;
            return;
        }

        int ts = gp.tileSize;
        int minX = patrolOriginX - 2*ts;
        int maxX = patrolOriginX + 2*ts;
        int minY = patrolOriginY - 2*ts;
        int maxY = patrolOriginY + 2*ts;

        int x = wx;
        int y = wy;
        int speed = 1;

        switch(patrolDir){
            case 0: y -= speed; if(y <= minY){ y = minY; patrolDir = 1; patrolTimer = 0; } break;
            case 1: y += speed; if(y >= maxY){ y = maxY; patrolDir = 0; patrolTimer = 0; } break;
            case 2: x -= speed; if(x <= minX){ x = minX; patrolDir = 3; patrolTimer = 0; } break;
            case 3: x += speed; if(x >= maxX){ x = maxX; patrolDir = 2; patrolTimer = 0; } break;
        }

        setEntityCoord(currentNPCEntity, "worldX", x);
        setEntityCoord(currentNPCEntity, "worldY", y);

        patrolTimer++;
        if(patrolTimer >= patrolChangeInterval){
            patrolTimer = 0;
            patrolDir = (patrolDir + 1) % 4;
        }
    }

    // Safe coordinate getter via reflection; returns null if field missing/inaccessible
    private Integer getEntityCoord(Object entity, String fieldName){
        try{
            Field f = entity.getClass().getField(fieldName);
            f.setAccessible(true);
            Object v = f.get(entity);
            if(v instanceof Integer) return (Integer) v;
        }catch(Exception ignored){}
        return null;
    }

    // Safe coordinate setter via reflection
    private void setEntityCoord(Object entity, String fieldName, int value){
        try{
            Field f = entity.getClass().getField(fieldName);
            f.setAccessible(true);
            f.set(entity, value);
        }catch(Exception ignored){}
    }

    public void draw(Graphics2D g2){
        if(!inConversation){
            // No convo UI; patrol drawing happens in your main renderer that draws entities.
            return;
        }

        if(proxyBattle){
            battle.draw(g2);
            return;
        }

        // Overworld-style dialog UI
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);

        // NPC sprite (smaller in overworld convo)
        if(currentNPCEntity != null && currentNPCEntity.idle != null){
            int spriteW = gp.tileSize * 3;
            int spriteH = gp.tileSize * 3;
            int spriteX = gp.screenWidth/2 - spriteW/2;
            int spriteY = 40;
            g2.drawImage(currentNPCEntity.idle, spriteX, spriteY, spriteW, spriteH, null);
        }

        // Dialog panel
        int boxW = gp.screenWidth - 100;
        int boxH = 190;
        int boxX = 50;
        int boxY = gp.screenHeight - boxH - 32;

        g2.setColor(new Color(30,30,60,220));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setColor(new Color(150,100,200));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // Title
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        g2.drawString(currentNPC.name, boxX+20, boxY+32);

        // Wrapped dialogue
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(new Color(220,200,255));
        int textAreaX = boxX + 20;
        int textAreaY = boxY + 62;
        int textAreaW = boxW - 40;
        int lineHeight = 24;
        drawWrapped(g2, battleLog, textAreaX, textAreaY, textAreaW, lineHeight, 4);

        // Friendly or blocked instruction vs normal flow
        if(!playingLongDialogue){
            if(BattleSystem.isBlocked(currentNPC.name) || BattleSystem.isBefriended(currentNPC.name)){
                drawBottomHint(g2, boxX, boxY, boxW, boxH, "O: close", null);
            } else {
                if(phase==0){
                    drawBottomHint(g2, boxX, boxY, boxW, boxH, "O: continue", "P: cancel");
                }else if(phase==1){
                    drawBottomHint(g2, boxX, boxY, boxW, boxH, "O: continue", null);
                }else if(phase==2){
                g2.setFont(new Font("Arial", Font.BOLD, 17));
                g2.setColor(new Color(200,180,255));
                g2.drawString("How do you respond?", boxX+20, boxY + 104);

                // Bounded, scrollable response list
                int startY = boxY + 134;
                int itemSpacing = 24;
                int availableHeight = boxY + boxH - 36 - startY;
                int maxVisible = Math.max(1, availableHeight / itemSpacing);

                int firstIndex = Math.max(0, Math.min(responseIndex - (maxVisible - 1), responses.length - maxVisible));
                int lastIndex = Math.min(responses.length, firstIndex + maxVisible);

                // Scroll indicators
                if(firstIndex > 0){
                    g2.setColor(new Color(180,160,220));
                    g2.drawString("▲", boxX + boxW - 26, startY - 6);
                }
                if(lastIndex < responses.length){
                    g2.setColor(new Color(180,160,220));
                    g2.drawString("▼", boxX + boxW - 26, startY + itemSpacing * (maxVisible) - 6);
                }

                for(int i = firstIndex; i < lastIndex; i++){
                    int y = startY + (i - firstIndex) * itemSpacing;
                    if(i==responseIndex){
                        g2.setColor(Color.YELLOW);
                        drawWrapped(g2, "> " + responses[i].text, boxX+30, y, boxW - 50, itemSpacing - 4, 1);
                    }else{
                        g2.setColor(new Color(180,160,220));
                        drawWrapped(g2, responses[i].text, boxX+50, y, boxW - 70, itemSpacing - 4, 1);
                    }
                }
                }else if(phase==3){
                    drawBottomHint(g2, boxX, boxY, boxW, boxH, "O: proceed to battle", null);
                }
            }
        }

        // Fade-to-white transition overlay
        if(phase==6 && transitionFrames > 0){
            int a = (int)(255 * (transitionFrames / (float)maxTransitionFrames));
            g2.setColor(new Color(255,255,255, Math.max(0, Math.min(255, a))));
            g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        }

        // Long dialogue uses its own hint so it cannot overlap the normal hint.
        if(playingLongDialogue){
            drawBottomHint(g2, boxX, boxY, boxW, boxH, "O: next", "P: skip");
        }
    }

    private void drawBottomHint(Graphics2D g2, int boxX, int boxY, int boxW, int boxH, String leftText, String rightText){
        int y = boxY + boxH - 18;
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(new Color(180,160,220));

        if(leftText != null && !leftText.isEmpty()){
            g2.drawString(leftText, boxX + 20, y);
        }

        if(rightText != null && !rightText.isEmpty()){
            int rightTextWidth = g2.getFontMetrics().stringWidth(rightText);
            g2.drawString(rightText, boxX + boxW - rightTextWidth - 20, y);
        }
    }

    // Draws text wrapped to a maximum width, capped at maxLines.
    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight, int maxLines){
        if(text == null || text.isEmpty()) return;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int linesDrawn = 0;
        for(String w : words){
            String candidate = line.length()==0 ? w : line + " " + w;
            if(g2.getFontMetrics().stringWidth(candidate) <= maxWidth){
                line = new StringBuilder(candidate);
            }else{
                g2.drawString(line.toString(), x, y + linesDrawn * lineHeight);
                linesDrawn++;
                if(linesDrawn >= maxLines) return;
                line = new StringBuilder(w);
            }
        }
        if(line.length() > 0 && linesDrawn < maxLines){
            g2.drawString(line.toString(), x, y + linesDrawn * lineHeight);
        }
    }

    private void end(){
        inConversation = false;
        proxyBattle = false;
        currentNPC = null;
        // Preserve entity if patrolActive exists elsewhere; otherwise clear
        currentNPCEntity = null;
    }
}
