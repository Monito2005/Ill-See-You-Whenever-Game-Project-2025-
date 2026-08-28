package mainpack1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import entity.NPC;
import entity.entity;

public class BattleSystem {

    GamePanel gp;
    public boolean inBattle = false;
    private NPC currentNPC;
    // accept any NPC entity (Humberto or Andrew)
    private entity currentNPCEntity;
    private BufferedImage cachedBattleSprite; // Cache sprite to avoid repeated loading

    private int phase = 0; // 0: intro, 1: player menu, 2: choose action, 3: NPC turn, 4: result, 5: end
    private int selected = 0;
    private int playerHP = 100;
    private int npcHP = 100;
    private int maxHP = 100;
    private int combo = 0;
    private boolean guarding = false;

    private String battleLog = "";
    private int actionCount = 0;

    private Action[] actions;
    private int actionIndex = 0;

    // Earthbound-style vars
    private float bgPhase = 0f;
    private float bgSpeed = 0.015f;
    private int transitionFrames = 0;
    private int maxTransitionFrames = 30;

    // Rolling HP display (odometer style)
    private int displayPlayerHP = 100;
    private int displayNpcHP = 100;
    private int rollStep = 2;

    // Retro palette
    private final Color EB_BG_DARK = new Color(30, 8, 48);
    private final Color EB_BG_LIGHT = new Color(58, 15, 92);
    private final Color EB_PANEL = new Color(16, 4, 32);
    private final Color EB_BORDER = new Color(220, 200, 255);
    private final Color EB_TEXT = new Color(255, 240, 240);
    private final Color EB_SUBTEXT = new Color(220, 200, 255);
    private final Color EB_SELECT = new Color(255, 255, 120);
    private final Color COLOR_DARK_GRAY = Color.DARK_GRAY;
    private final Color COLOR_RED = new Color(255, 120, 120);
    private final Color COLOR_SUBTEXT_OUTLINE = new Color(180, 160, 220);
    private final Color COLOR_CYAN = new Color(150, 220, 255);
    
    // Font cache
    private Font fontBold18;
    private Font fontBold16;
    private Font fontBold14;
    private Font fontBold13;
    private Font fontBold22;
    private Font fontPlain16;
    private Font fontPlain14;
    private Font fontPlain13;
    private Font fontPlain12;
    private Font fontPlain11;

    // Pre-battle convo (generic, unnamed NPC)
    private String[] preDialogue = {
        "They step forward, quiet but present.",
        "The silence says more than words.",
        "You feel the weight of the moment."
    };
    // Andrew-specific pre-battle convo
    private final String[] preDialogueAndrew = new String[]{
        "Andrew sizes you up with a sharp stare.",
        "He scoffs: 'Is that all you've got?'",
        "The air feels tense; he's testing you."
    };
    // Delia-specific pre-battle convo (more tense, personal)
    private final String[] preDialogueDelia = new String[]{
        "Delia looks nervous, but determined.",
        "She whispers: 'I... need to say this.'",
        "Her eyes lock onto yours. The moment matters."
    };
    // Guero-specific pre-battle convo (cocky but friendly)
    private final String[] preDialogueGuero = new String[]{
        "Guero grins: 'You think you can keep up?'",
        "He tilts his head: 'Relax. I got you.'",
        "'We came from similar halls. You'll be okay.'"
    };

    private int preIndex = 0;
    // phases: -1 pre-convo, 0 intro title, 1 menu, ...
    // Affinity to give meaning to choices
    private int affinity = 0;

    // Shake effects
    private int npcShakeFrames = 0;
    private int uiShakeFrames = 0;
    private int shakeMagnitude = 6;

    // Extra transition into battle screen
    private int battleStartTransition = 0;
    private int battleStartTransitionMax = 20;

    // Persistent outcome per NPC name
    private static final Set<String> BEFRIENDED = new HashSet<>();
    private static final Set<String> BLOCKED = new HashSet<>();

    public static boolean isBefriended(String name){ return BEFRIENDED.contains(name); }
    public static boolean isBlocked(String name){ return BLOCKED.contains(name); }
    public static void resetProgress(){
        BEFRIENDED.clear();
        BLOCKED.clear();
    }

    private static class Action {
        String text;
        int damage;
        String npcReaction;
        int affinityDelta;
        // new: follow-up branching
        String followUpPrompt;     // what NPC asks next after this choice
        Action[] followUps;        // next options after reaction
        Action(String text, int dmg, String reaction){ this(text, dmg, reaction, 0, null, null); }
        Action(String text, int dmg, String reaction, int affinityDelta){ this(text, dmg, reaction, affinityDelta, null, null); }
        Action(String text, int dmg, String reaction, int affinityDelta, String prompt, Action[] followUps){
            this.text=text; this.damage=dmg; this.npcReaction=reaction; this.affinityDelta=affinityDelta;
            this.followUpPrompt=prompt; this.followUps=followUps;
        }
    }

    // Questions per round when no explicit follow-up is provided
    private final String[] npcQuestions = new String[]{
        "He asks: 'What matters most to you right now?'",
        "He asks: 'How do you handle the pressure?'",
        "He asks: 'Where do you see yourself fitting in?'"
    };

    // Starfield
    private static class Star {
        float x, y, z;   // position and depth
        float speed;     // pixels per frame
        int size;        // drawn size
        Color color;
    }
    private Star[] stars;
    private int starCount = 120;

    // Keep an immutable base actions set for round 1
    private Action[] baseActions;
    // Current round actions presented to the player
    private Action[] currentActions;

    // Track round to pick default questions if no follow-up is provided
    private int roundIndex = 0;

    // Pacing controls to target ~20–30 minutes total play
    private int totalTurns = 0;
    private final int intermissionEvery = 6;   // story beat after every 6 turns
    private boolean inIntermission = false;
    private final float damagePacingFactor = 1.15f;
    // Procedural tone generation is expensive; throttle to avoid input hitches.
    private long nextToneNanos = 0L;
    private static final long TONE_COOLDOWN_NANOS = 120_000_000L;

    public BattleSystem(GamePanel gp){
        this.gp = gp;
        initActions();
        initFonts();
    }
    
    private void initFonts(){
        fontBold18 = new Font("Arial", Font.BOLD, 18);
        fontBold16 = new Font("Arial", Font.BOLD, 16);
        fontBold14 = new Font("Arial", Font.BOLD, 14);
        fontBold13 = new Font("Arial", Font.BOLD, 13);
        fontBold22 = new Font("Arial", Font.BOLD, 22);
        fontPlain16 = new Font("Arial", Font.PLAIN, 16);
        fontPlain14 = new Font("Arial", Font.PLAIN, 14);
        fontPlain13 = new Font("Arial", Font.PLAIN, 13);
        fontPlain12 = new Font("Arial", Font.PLAIN, 12);
        fontPlain11 = new Font("Arial", Font.PLAIN, 11);
    }

    private void playTone(double frequency, int durationMs, float volume){
        if(gp == null || gp.se == null) return;
        long now = System.nanoTime();
        if(now < nextToneNanos) return;
        nextToneNanos = now + TONE_COOLDOWN_NANOS;

        // Keep synthesized tones very short/quiet to reduce clip work.
        int safeDuration = Math.max(20, Math.min(durationMs, 35));
        float safeVolume = Math.max(0.05f, Math.min(volume, 0.12f));
        gp.se.playTone(frequency, safeDuration, safeVolume);
    }

    private void initActions(){
        // Humberto base options (empathetic)
        actions = new Action[]{
            new Action(
                "Share a fear", 14, "He listens intently. 'That's honest.'", +2,
                "He leans closer: 'When did you first feel that?'",
                new Action[]{
                    new Action("Recently, after joining here.", 10, "'Makes sense.'", +1),
                    new Action("Since long before this.", 12, "'Deep roots, then.'", +2),
                    new Action("I'd rather change the topic.", -5, "He stays silent.", -1)
                }
            ),
            new Action(
                "Be honest", 18, "He nods slowly. 'Truth matters.'", +3,
                "He asks: 'What truth are you avoiding?'",
                new Action[]{
                    new Action("That I want approval.", 10, "'We all do.'", +1),
                    new Action("That I'm afraid of failing.", 12, "'Failure teaches.'", +2),
                    new Action("I don't know yet.", 5, "'That's fine.'", 0)
                }
            ),
            new Action(
                "Ask about him", 11, "He opens up. 'Quiet doesn't mean weak.'", +2,
                "He asks back: 'What do you think silence is?'",
                new Action[]{
                    new Action("Space to think.", 10, "'Exactly.'", +2),
                    new Action("A shield.", 8, "'Sometimes.'", +1),
                    new Action("Something I struggle with.", 10, "'Then face it.'", +1)
                }
            ),
            new Action(
                "Say nothing", -4, "An awkward silence. He waits.", -2,
                "He raises an eyebrow: 'Will you speak?'",
                new Action[]{
                    new Action("Yes... I'm trying.", 8, "'Good.'", +1),
                    new Action("No. Not now.", -8, "He sighs.", -2)
                }
            )
        };
        baseActions = actions;
        currentActions = baseActions;
    }

    // Andrew’s harsher action set
    private Action[] makeAndrewActions(){
        return new Action[]{
            new Action(
                "Stand your ground", 12, "Andrew smirks: 'Finally some spine.'", +1,
                "He presses: 'What makes you think you belong?'",
                new Action[]{
                    new Action("I work for it.", 10, "'We'll see.'", +1),
                    new Action("I adapt fast.", 12, "'Prove it.'", 0),
                    new Action("None of your business.", -10, "He laughs: 'Thin skin.'", -2)
                }
            ),
            new Action(
                "Call him out", 18, "His eyes narrow: 'Bold move.'", +0,
                "He asks: 'Do you always deflect with bravado?'",
                new Action[]{
                    new Action("No. I face things.", 12, "'Convince me.'", +0),
                    new Action("Sometimes.", 8, "'Predictable.'", -1),
                    new Action("Better than folding.", 10, "'Debatable.'", -1)
                }
            ),
            new Action(
                "Expose a weakness", 22, "He pauses, then: 'Risky...'", +2,
                "He asks: 'Why show this to me?'",
                new Action[]{
                    new Action("Because truth is strong.", 12, "'Maybe.'", +1),
                    new Action("Because I'm done pretending.", 14, "'Good. Keep that.'", +2),
                    new Action("Because I need approval.", -12, "He scoffs: 'There it is.'", -3)
                }
            ),
            new Action(
                "Stay silent", -8, "He shakes his head: 'Weak.'", -3,
                "He taunts: 'Cat got your tongue?'",
                new Action[]{
                    new Action("Enough. I speak now.", 10, "'Finally.'", +0),
                    new Action("Say what you want.", -8, "He shrugs.", -1)
                }
            )
        };
    }

    // Delia’s hardest action set (subtle mutual interest, sincere tone)
    private Action[] makeDeliaActions(){
        return new Action[]{
            new Action(
                "Be sincere (no pressure)", 26, "Delia exhales: 'I appreciate that.'", +2,
                "She asks softly: 'Do you like this... us talking?'", new Action[]{
                    new Action("I do. It feels right.", 18, "She smiles. 'Me too.'", +3),
                    new Action("I’m curious to know you.", 16, "She nods. 'Same.'", +2),
                    new Action("We can keep it light.", 12, "She relaxes. 'That helps.'", +1)
                }
            ),
            new Action(
                "Offer a walk later", 28, "She hesitates, then: 'Maybe... yes.'", +2,
                "She asks: 'No rush?'", new Action[]{
                    new Action("No rush at all.", 16, "'Thank you.'", +2),
                    new Action("When you’re ready.", 14, "She nods. 'Okay.'", +2),
                    new Action("We can text first.", 14, "'That works.'", +1)
                }
            ),
            new Action(
                "Share a gentle truth", 30, "Delia listens: 'Thanks for saying that.'", +2,
                "She asks: 'Does this mean anything to you?'", new Action[]{
                    new Action("It might. I’d like to see.", 20, "She blushes. 'Same.'", +3),
                    new Action("I’m figuring it out.", 16, "She smiles. 'Me too.'", +2),
                    new Action("Let’s not label it yet.", 14, "She relaxes. 'Good idea.'", +1)
                }
            ),
            new Action(
                "Give space kindly", -4, "She breathes easier: 'Thank you for that.'", +1,
                "She asks: 'Will you still be around?'", new Action[]{
                    new Action("Yes. Always.", 14, "She smiles warmly.", +2),
                    new Action("I’ll check in.", 12, "She nods.", +1),
                    new Action("If you want me to.", 10, "She looks relieved.", +1)
                }
            )
        };
    }

    private Action[] makeGueroActions(){
        // Medium difficulty: moderate damage and balanced affinity
        return new Action[]{
            new Action(
                "Match his energy", 20, "Guero laughs: 'That's the spirit!'", +2,
                "He asks: 'Where'd you learn that grit?'", new Action[]{
                    new Action("Same kind of school as yours.", 14, "'Figures.'", +2),
                    new Action("From people like us.", 12, "'Respect.'", +1),
                    new Action("I’m still learning.", 10, "'We all are.'", +1)
                }
            ),
            new Action(
                "Be honest", 18, "He nods: 'No need to front.'", +2,
                "He asks: 'You good out here?'", new Action[]{
                    new Action("I will be.", 14, "'You will.'", +2),
                    new Action("Sometimes.", 12, "'You'll find your pace.'", +1),
                    new Action("Not yet.", 8, "'You'll get there.'", +1)
                }
            ),
            new Action(
                "Ask advice", 16, "Guero smirks: 'Alright, listen.'", +1,
                "He says: 'Keep your head up.'", new Action[]{
                    new Action("Got it.", 12, "'Good.'", +1),
                    new Action("Thanks.", 12, "'Anytime.'", +1),
                    new Action("I needed that.", 12, "'I know.'", +1)
                }
            ),
            new Action(
                "Stay chill", 0, "He grins: 'Cool is a choice.'", +0,
                "He asks: 'Ready to focus?'", new Action[]{
                    new Action("Yeah.", 12, "'Let's roll.'", +1),
                    new Action("Give me a sec.", 8, "'Take it.'", +0)
                }
            )
        };
    }

    public void startBattle(NPC npc, entity ent){
        currentNPC = npc;
        currentNPCEntity = ent;
        inBattle = true;
        phase = -1; // start in pre-conversation
        selected = 0;
        playerHP = maxHP;
        npcHP = maxHP;
        battleLog = "Battle start!";
        actionCount = 0;
        combo = 0;
        guarding = false;
        // Ensure previous branching resets
        roundIndex = 0;
        preIndex = 0;
        currentActions = (baseActions != null && baseActions.length > 0) ? baseActions : new Action[]{};
        actionIndex = 0;              // reset selection
        displayPlayerHP = playerHP;
        displayNpcHP = npcHP;
        bgPhase = 0f;
        transitionFrames = maxTransitionFrames;
        battleStartTransition = battleStartTransitionMax;
        initStarfield();

        // Persona switch based on NPC name
        if(npc != null && "Andrew".equalsIgnoreCase(npc.name)){
            preDialogue = preDialogueAndrew;
            actions = makeAndrewActions();
            baseActions = actions;
            currentActions = baseActions;
            // Make Andrew medium-long battle
            maxHP = 150; playerHP = maxHP; npcHP = maxHP;
            displayPlayerHP = playerHP; displayNpcHP = npcHP;
        }else if(npc != null && ("Delia".equalsIgnoreCase(npc.name) || "Mysterious Girl".equalsIgnoreCase(npc.name))){
            preDialogue = preDialogueDelia;
            actions = makeDeliaActions();
            baseActions = actions;
            currentActions = baseActions;
            // Hardest battle
            maxHP = 180; playerHP = maxHP; npcHP = maxHP;
            displayPlayerHP = playerHP; displayNpcHP = npcHP;
        }else if(npc != null && "Guero".equalsIgnoreCase(npc.name)){
            preDialogue = preDialogueGuero;
            actions = makeGueroActions();
            baseActions = actions;
            currentActions = baseActions;
            // Medium difficulty: a bit tougher than default
            maxHP = 125; playerHP = maxHP; npcHP = maxHP;
            displayPlayerHP = playerHP; displayNpcHP = npcHP;
        }else{
            // Humberto: longer than default
            initActions();
            maxHP = 130; playerHP = maxHP; npcHP = maxHP;
            displayPlayerHP = playerHP;
            displayNpcHP = npcHP;
        }

        // Reset pacing state
        totalTurns = 0;
        inIntermission = false;
        
        // Cache the sprite ONCE at battle start (instead of loading every frame)
        cachedBattleSprite = getBattleSprite(ent, npc);

        // Play battle music
        gp.stopMusic();
        try{
            gp.music.setFilePath("res/sound/battle1.wav");
            gp.music.playLoop();
        }catch(Exception ignored){}
    }

    private void initStarfield(){
        stars = new Star[starCount];
        for(int i=0;i<starCount;i++){
            Star s = new Star();
            s.x = (float)(Math.random() * gp.screenWidth);
            s.y = (float)(Math.random() * gp.screenHeight);
            s.z = (float)(Math.random() * 1.0f); // 0..1 depth
            s.speed = 1.5f + (float)(Math.random() * 3f) + s.z * 2f;
            s.size = 1 + (int)(s.z * 2);
            int brightness = 180 + (int)(s.z * 75);
            s.color = new Color(brightness, brightness, brightness);
            stars[i] = s;
        }
    }

    public void handleInput(KeyEvent e){
        if(!inBattle) return;
        int c = e.getKeyCode();

        // Pre-battle conversation
        if(phase==-1){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(520, 65, 0.16f);
                // advance dialogue or move to choice
                if(preIndex < preDialogue.length-1){
                    preIndex++;
                }else{
                    // simple choice: respond or stay silent affects affinity
                    phase = 0; // go to intro title
                }
            }else if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP){
                playTone(350, 45, 0.14f);
                // respond: positive affinity
                affinity += 2;
                phase = 0;
            }else if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN){
                playTone(350, 45, 0.14f);
                // stay silent: negative affinity
                affinity -= 1;
                phase = 0;
            }
            return;
        }

        if(phase==0){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                phase = 1;
                battleLog = currentNPC.name + ": 'I need to tell you something...'";
            }
            return;
        }

        if(phase==1){
            // clamp selection between 0 and 1
            if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP){ selected = 0; playTone(350, 45, 0.14f); }
            else if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN){ selected = 1; playTone(350, 45, 0.14f); }
            else if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(640, 80, 0.2f);
                phase = 2;
                actionIndex = 0; // reset cursor for action list
            }
            return;
        }

        if(phase==2){
            // navigate currentActions (guard against null/empty)
            if(currentActions == null || currentActions.length == 0){
                currentActions = (baseActions != null && baseActions.length > 0) ? baseActions : new Action[]{};
                actionIndex = 0;
            }
            if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP){
                actionIndex = (actionIndex - 1 + currentActions.length) % currentActions.length;
                playTone(350, 45, 0.14f);
            }else if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN){
                actionIndex = (actionIndex + 1) % currentActions.length;
                playTone(350, 45, 0.14f);
            }else if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(640, 80, 0.2f);
                performAction();
                phase = 3;
            }
            return;
        }

        if(phase==3){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(520, 65, 0.16f);
                phase = 4;
                // prepare next menu: ensure cursor in bounds
                if(currentActions == null || currentActions.length == 0){
                    currentActions = (baseActions != null && baseActions.length > 0) ? baseActions : new Action[]{};
                }
                actionIndex = Math.min(actionIndex, Math.max(0, currentActions.length-1));
            }
            return;
        }

        if(phase==4){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(640, 80, 0.2f);
                // Continue until someone reaches 0 HP (no turn-count limit)
                if(playerHP <= 0){
                    // Defeat: mark blocked and end immediately
                    BLOCKED.add(currentNPC.name);
                    battleLog = currentNPC.name + ": 'Weird. Go away.'";
                    phase = 6; // use end flow
                }else if(npcHP <= 0){
                    // Victory: to win screen
                    BEFRIENDED.add(currentNPC.name);
                    phase = 5;
                }else{
                    // next turn: return to phase 2 directly for smoother flow
                    phase = 2;
                    // if we returned to base actions, reset cursor
                    if(currentActions == baseActions) actionIndex = 0;
                }
            }
            return;
        }

        if(phase==5){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(760, 120, 0.24f);
                // Go to detailed Win Screen
                phase = 6;
            }
            return;
        }

        if(phase==6){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(420, 80, 0.18f);
                end();
            }
            return;
        }

        // New intermission phase: short beat to slow pacing
        if(phase==7){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER){
                playTone(520, 65, 0.16f);
                inIntermission = false;
                // Return to results flow to check win/continue
                phase = 4;
            }
            return;
        }
    }

    private void performAction(){
        // guard currentActions
        if(currentActions == null || currentActions.length == 0){
            currentActions = (baseActions != null && baseActions.length > 0) ? baseActions : new Action[]{};
            actionIndex = 0;
        }
        Action a = currentActions[actionIndex];
        battleLog = "You: " + a.text;

        boolean listening = selected == 1;
        int pacedDamage = Math.max(0, Math.round(a.damage * damagePacingFactor * (listening ? 0.85f : 1.0f)));
        boolean critical = pacedDamage > 0 && (++combo % 3 == 0);
        if(critical){
            pacedDamage = Math.round(pacedDamage * 1.75f);
            playTone(920, 150, 0.28f);
        }else if(pacedDamage > 0){
            playTone(680, 70, 0.2f);
        }else if(pacedDamage <= 0){
            combo = 0;
        }
        npcHP -= pacedDamage;
        if(npcHP < 0) npcHP = 0;
        affinity += a.affinityDelta;

        guarding = listening;
        if(listening){
            playerHP = Math.min(maxHP, playerHP + 6);
        }

        if(a.followUps != null && a.followUps.length > 0){
            currentActions = a.followUps;
            actionIndex = 0;
            roundIndex = Math.min(roundIndex + 1, Math.max(0, npcQuestions.length - 1));
        }else{
            currentActions = (baseActions != null && baseActions.length > 0) ? baseActions : new Action[]{};
            actionIndex = 0;
            roundIndex = Math.min(roundIndex + 1, Math.max(0, npcQuestions.length - 1));
        }

        // Trigger shakes
        npcShakeFrames = 10;

        int baseNpcDamage = 5 + (int)(Math.random()*8);
        int pacedNpcDamage = Math.max(1, Math.round(baseNpcDamage * (guarding ? 0.4f : 1.0f)));
        playerHP -= pacedNpcDamage;
        if(playerHP < 0) playerHP = 0;
        if(pacedNpcDamage > 0) uiShakeFrames = 10;

        String impact = critical ? " CRITICAL!" : "";
        String defense = listening ? " You recover 6 resolve and brace for the reply." : "";
        battleLog = "You: " + a.text + " [" + pacedDamage + " impact" + impact + "] " + a.npcReaction + " " +
                    (a.followUpPrompt != null ? a.followUpPrompt :
                        (roundIndex < npcQuestions.length ? npcQuestions[roundIndex] : "He watches you carefully.")) +
                    " They deal " + pacedNpcDamage + " resolve." + defense;

        // Turn count and intermission scheduling
        totalTurns++;
        if(playerHP > 0 && npcHP > 0 && (totalTurns % intermissionEvery == 0)){
            inIntermission = true;
            phase = 7; // new intermission phase
        } else {
            phase = 3;
        }
    }

    public void update(){
        if(!inBattle) return;
        // Animate wavy background
        bgPhase += bgSpeed;

        // Transition countdown
        if(transitionFrames > 0) transitionFrames--;
        if(battleStartTransition > 0) battleStartTransition--;

        // Decay shakes
        if(npcShakeFrames > 0) npcShakeFrames--;
        if(uiShakeFrames > 0) uiShakeFrames--;

        // Rolling HP for player
        if(displayPlayerHP < playerHP){
            displayPlayerHP = Math.min(playerHP, displayPlayerHP + rollStep);
        } else if(displayPlayerHP > playerHP){
            displayPlayerHP = Math.max(playerHP, displayPlayerHP - rollStep);
        }
        // Rolling HP for npc
        if(displayNpcHP < npcHP){
            displayNpcHP = Math.min(npcHP, displayNpcHP + rollStep);
        } else if(displayNpcHP > npcHP){
            displayNpcHP = Math.max(npcHP, displayNpcHP - rollStep);
        }

        // Starfield movement: fly downward subtly (can change direction)
        if(stars != null){
            for(Star s : stars){
                s.y += s.speed;
                // Wrap
                if(s.y > gp.screenHeight){
                    s.y = -5;
                    s.x = (float)(Math.random() * gp.screenWidth);
                    s.z = (float)(Math.random() * 1.0f);
                    s.speed = 1.5f + (float)(Math.random() * 3f) + s.z * 2f;
                    s.size = 1 + (int)(s.z * 2);
                    // Reuse cached colors instead of creating new ones
                    int brightness = 180 + (int)(s.z * 75);
                    if(brightness > 240) s.color = Color.WHITE;
                    else if(brightness > 220) s.color = new Color(brightness, brightness, brightness);
                    else s.color = Color.GRAY;
                }
            }
        }
    }

    public void draw(Graphics2D g2){
        if(!inBattle) return;

        // Black background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // Starfield
        if(stars != null){
            for(Star s : stars){
                g2.setColor(s.color);
                g2.fillRect((int)s.x, (int)s.y, s.size, s.size);
            }
        }

        // Slight fade-in for entry
        if(transitionFrames > 0){
            int alpha = (int)(255 * (transitionFrames / (float)maxTransitionFrames));
            g2.setColor(new Color(0, 0, 0, Math.max(0, Math.min(200, alpha))));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }

        // Pre-battle conversation screen (no sprite render here)
        if(phase==-1){
            int boxW = gp.screenWidth - 140;
            int boxH = 160;
            int boxX = 70;
            int boxY = gp.screenHeight/2 - boxH/2;
            drawEBPanel(g2, boxX, boxY, boxW, boxH);

            g2.setFont(fontBold18);
            g2.setColor(EB_TEXT);
            drawCentered(g2, currentNPC.name.toUpperCase(), boxX, boxY, boxW, 28);

            g2.setFont(fontPlain16);
            g2.setColor(EB_SUBTEXT);
            g2.drawString(preDialogue[preIndex], boxX+20, boxY+70);
            g2.setFont(fontPlain14);
            g2.drawString("O/Enter: next  W: respond  S: stay silent", boxX+20, boxY+110);
            return;
        }

        // NPC sprite during battle
        int spriteShakeX = (npcShakeFrames > 0) ? (int)(Math.random()*shakeMagnitude - shakeMagnitude/2) : 0;
        int spriteShakeY = (npcShakeFrames > 0) ? (int)(Math.random()*shakeMagnitude - shakeMagnitude/2) : 0;
        BufferedImage npcImg = cachedBattleSprite; // Use cached sprite instead of loading every frame
        if(npcImg != null){
            int spriteW = gp.tileSize * 4;
            int spriteH = gp.tileSize * 4;
            int spriteX = gp.screenWidth/2 - spriteW/2 + spriteShakeX;
            int spriteY = 30 + spriteShakeY;
            g2.drawImage(npcImg, spriteX, spriteY, spriteW, spriteH, null);
        }

        // EB panels for HP
        int uiShakeX = (uiShakeFrames > 0) ? (int)(Math.random()*shakeMagnitude - shakeMagnitude/2) : 0;
        int uiShakeY = (uiShakeFrames > 0) ? (int)(Math.random()*shakeMagnitude - shakeMagnitude/2) : 0;
        int barW = 200, barH = 40;
        drawEBPanel(g2, 40 + uiShakeX, 160 + uiShakeY, barW, barH);
        drawEBPanel(g2, gp.screenWidth - barW - 40 + uiShakeX, 160 + uiShakeY, barW, barH);

        // Player label: YOU
        drawRollingHP(g2, 40 + uiShakeX, 160 + uiShakeY, barW, barH, displayPlayerHP, maxHP, "YOU");
        // NPC label: always their actual name
        String npcLabel = (currentNPC != null && currentNPC.name != null && !currentNPC.name.isBlank())
                ? currentNPC.name.toUpperCase()
                : "NPC";
        drawRollingHP(g2, gp.screenWidth - barW - 40 + uiShakeX, 160 + uiShakeY, barW, barH, displayNpcHP, maxHP, npcLabel);

        g2.setFont(fontBold13);
        g2.setColor(EB_SELECT);
        if(combo > 0){
            g2.drawString("COMBO x" + combo + (combo % 3 == 2 ? "  (next hit crits)" : ""), 40, 218);
        }
        if(guarding){
            g2.setColor(COLOR_CYAN);
            g2.drawString("BRACED: reduced damage", gp.screenWidth - 210, 218);
        }

        // Battle log panel (smaller font + wrapped text)
        int logBoxW = gp.screenWidth - 60;
        int logBoxH = 140;
        int logBoxX = 30 + uiShakeX;
        int logBoxY = 230 + uiShakeY;
        drawEBPanel(g2, logBoxX, logBoxY, logBoxW, logBoxH);
        g2.setFont(fontPlain14);
        g2.setColor(EB_SUBTEXT);
        int textX = logBoxX + 16;
        int textY = logBoxY + 36;
        int textW = logBoxW - 32;
        int lineH = 16;
        drawWrappedText(g2, battleLog, textX, textY, textW, lineH, 6); // up to 6 lines

        // Menu panel (smaller fonts + tighter spacing)
        int menuBoxW = gp.screenWidth - 60;
        int menuBoxH = gp.screenHeight - 360;
        int menuBoxX = 30 + uiShakeX;
        int menuBoxY = 380 + uiShakeY;
        drawEBPanel(g2, menuBoxX, menuBoxY, menuBoxW, menuBoxH);

        if(phase==0){
            g2.setFont(fontBold18);
            g2.setColor(EB_TEXT);
            drawCentered(g2, "! CONVERSATION BATTLE !", menuBoxX, menuBoxY, menuBoxW, 34);
            g2.setFont(fontPlain12);
            g2.setColor(EB_SUBTEXT);
            g2.drawString("O/Enter: start", menuBoxX+16, menuBoxY+menuBoxH-14);
        }else if(phase==1){
            g2.setFont(fontBold14);
            g2.setColor(EB_SUBTEXT);
            g2.drawString("Choose your action:", menuBoxX+16, menuBoxY+26);
            drawEBMenuItem(g2, menuBoxX, menuBoxY, 0, "TALK", selected==0);
            drawEBMenuItem(g2, menuBoxX, menuBoxY, 1, "LISTEN", selected==1);
            g2.setFont(fontPlain11);
            g2.setColor(COLOR_SUBTEXT_OUTLINE);
            g2.drawString("W/S or Up/Down: move    O/Enter: select", menuBoxX+16, menuBoxY+menuBoxH-12);
        }else if(phase==2){
            g2.setFont(fontBold14);
            g2.setColor(EB_SUBTEXT);
            g2.drawString("What will you do?", menuBoxX+16, menuBoxY+26);
            int optionSpacing = 22;
            // guard currentActions for rendering
            Action[] renderActions = (currentActions != null) ? currentActions : new Action[]{};
            if(renderActions.length == 0){
                g2.setColor(COLOR_SUBTEXT_OUTLINE);
                g2.drawString("(No options available)", menuBoxX+30, menuBoxY+56);
            } else {
                actionIndex = Math.max(0, Math.min(actionIndex, renderActions.length - 1));
                for(int i=0;i<renderActions.length;i++){
                    int y = menuBoxY + 56 + i*optionSpacing;
                    String txt = renderActions[i].text.toUpperCase();
                    if(i==actionIndex){
                        g2.setColor(EB_SELECT);
                        g2.drawString("> " + txt, menuBoxX+30, y);
                    }else{
                        g2.setColor(EB_SUBTEXT);
                        g2.drawString(txt, menuBoxX+46, y);
                    }
                }
            }
            g2.setFont(fontPlain11);
            g2.setColor(COLOR_SUBTEXT_OUTLINE);
            g2.drawString("W/S or Up/Down: move    O/Enter: confirm", menuBoxX+16, menuBoxY+menuBoxH-12);
        }else if(phase==3){
            g2.setFont(fontPlain13);
            g2.setColor(EB_SUBTEXT);
            // show the composed reaction + prompt in log panel instead of cramping here
            // ...existing guidance...
            g2.setFont(fontPlain11);
            g2.setColor(COLOR_SUBTEXT_OUTLINE);
            g2.drawString("O/Enter: continue", menuBoxX+16, menuBoxY+menuBoxH-12);
        }else if(phase==4){
            g2.setFont(fontPlain13);
            g2.setColor(EB_SUBTEXT);
            if(playerHP<=0){
                g2.drawString("You ran out of resolve...", menuBoxX+20, menuBoxY+60);
            }else if(npcHP<=0){
                g2.drawString("You broke through. A bond forms.", menuBoxX+20, menuBoxY+60);
            }else{
                g2.drawString("The battle continues!", menuBoxX+20, menuBoxY+60);
            }
            g2.setFont(fontPlain11);
            g2.setColor(COLOR_SUBTEXT_OUTLINE);
            g2.drawString("O/Enter: continue", menuBoxX+16, menuBoxY+menuBoxH-12);
        }else if(phase==5){
            g2.setFont(fontBold16);
            g2.setColor(EB_TEXT);
            drawCentered(g2, "VICTORY!", menuBoxX, menuBoxY, menuBoxW, 28);
            g2.setFont(fontPlain12);
            g2.setColor(EB_SUBTEXT);
            // Friendly follow-up when befriended (avoid named phrasing if unknown)
            String endNote;
            if(currentNPC != null && currentNPC.name != null && !currentNPC.name.isBlank()){
                endNote = currentNPC.name + ": 'Hey, this was good. Want to hang out later?'";
            } else {
                endNote = "They smile warmly. 'This was good. Maybe later?'";
            }
            g2.drawString(endNote, /* menuBoxX */ 50, /* menuBoxY+80 */ 460);
            g2.setColor(COLOR_SUBTEXT_OUTLINE);
            g2.drawString("O/Enter: details", menuBoxX+16, menuBoxY+menuBoxH-12);
        } else if(phase==6){
            int boxW = gp.screenWidth - 160;
            int boxH = 200;
            int boxX = 80;
            int boxY = gp.screenHeight/2 - boxH/2;
            drawEBPanel(g2, boxX, boxY, boxW, boxH);

            g2.setFont(fontBold22);
            g2.setColor(EB_TEXT);
            String winTitle = (npcHP<=0) ? "WIN" : "DEFEAT";
            drawCentered(g2, winTitle, boxX, boxY, boxW, 36);

            g2.setFont(fontPlain16);
            g2.setColor(EB_SUBTEXT);
            if(npcHP<=0){
                // Friendly summary on befriending
                String summary = (currentNPC != null && currentNPC.name != null && !currentNPC.name.isBlank())
                    ? "You befriended " + currentNPC.name + ". They ask to hang out later."
                    : "You befriended them. They ask to hang out later.";
                g2.drawString(summary, boxX + 20, boxY + 80);
            }else{
                String summary = (currentNPC != null && currentNPC.name != null && !currentNPC.name.isBlank())
                    ? currentNPC.name + ": 'Weird. Go away.'"
                    : "They say: 'Weird. Go away.'";
                g2.drawString(summary, boxX + 20, boxY + 80);
            }

            String hpNote = "YOU: " + playerHP + "/" + maxHP + "   " + npcLabel + ": " + npcHP + "/" + maxHP;
            g2.drawString(hpNote, boxX + 20, boxY + 110);

            g2.setFont(fontPlain14);
            g2.drawString("O: close", boxX + 20, boxY + 150);
        }

        // Intermission UI panel
        if(phase==7){
            int boxW = gp.screenWidth - 160;
            int boxH = 140;
            int boxX = 80;
            int boxY = gp.screenHeight/2 - boxH/2;
            drawEBPanel(g2, boxX, boxY, boxW, boxH);

            g2.setFont(fontBold18);
            g2.setColor(EB_TEXT);
            drawCentered(g2, "TAKE A BREATH", boxX, boxY, boxW, 30);

            g2.setFont(fontPlain14);
            g2.setColor(EB_SUBTEXT);
            String beat;
            String name = (currentNPC != null && currentNPC.name != null) ? currentNPC.name : "They";
            // Simple rotating beats by persona
            if("Andrew".equalsIgnoreCase(name)){
                beat = "Andrew: 'Still standing? Good. Keep at it.'";
            } else if("Guero".equalsIgnoreCase(name)){
                beat = "Guero: 'You’re fine. Head up. We got this.'";
            } else if("Delia".equalsIgnoreCase(name) || "Mysterious Girl".equalsIgnoreCase(name)){
                beat = "She breathes. 'Okay... let’s keep going.'";
            } else {
                beat = "Humberto nods quietly. 'One step at a time.'";
            }
            g2.drawString(beat, boxX + 20, boxY + 70);

            g2.setFont(fontPlain12);
            g2.setColor(COLOR_SUBTEXT_OUTLINE);
            g2.drawString("O/Enter: continue", boxX + 20, boxY + boxH - 18);
            return;
        }

        // Extra battle start transition overlay
        if(battleStartTransition > 0){
            int a = (int)(180 * (battleStartTransition / (float)battleStartTransitionMax));
            g2.setColor(new Color(255,255,255, Math.max(0, Math.min(180, a))));
            g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        }
    }

    // Replace old bar with EB style panel + rolling HP
    private void drawRollingHP(Graphics2D g2, int x, int y, int w, int h, int current, int max, String name){
        g2.setColor(EB_TEXT);
        g2.setFont(fontBold14);
        g2.drawString(name, x + 12, y + 18);

        // HP bar inside panel
        int innerX = x + 10;
        int innerY = y + 22;
        int innerW = w - 20;
        int innerH = 12;

        g2.setColor(COLOR_DARK_GRAY);
        g2.fillRect(innerX, innerY, innerW, innerH);
        g2.setColor(COLOR_RED);
        g2.fillRect(innerX, innerY, (int)(innerW * current / (float)max), innerH);
        g2.setColor(EB_BORDER);
        g2.drawRect(innerX, innerY, innerW, innerH);

        // Odometer HP text
        g2.setColor(EB_TEXT);
        g2.setFont(fontBold16);
        String hpText = current + " / " + max;
        g2.drawString(hpText, x + w - 12 - g2.getFontMetrics().stringWidth(hpText), y + h - 10);
    }

    private final java.awt.BasicStroke panelStroke = new java.awt.BasicStroke(3);
    
    private void drawEBPanel(Graphics2D g2, int x, int y, int w, int h){
        g2.setColor(EB_PANEL);
        g2.fillRect(x, y, w, h);
        g2.setColor(EB_BORDER);
        g2.setStroke(panelStroke);
        g2.drawRect(x, y, w, h);
    }

    private void drawCentered(Graphics2D g2, String text, int x, int y, int w, int baseline){
        int tw = g2.getFontMetrics().stringWidth(text);
        int cx = x + (w - tw)/2;
        g2.drawString(text, cx, y + baseline);
    }

    private void drawEBMenuItem(Graphics2D g2, int x, int y, int index, String text, boolean selected){
        int itemY = y + 44 + index*22;
        if(selected){
            g2.setColor(EB_SELECT);
            g2.drawString("> " + text, x + 30, itemY);
        }else{
            g2.setColor(EB_SUBTEXT);
            g2.drawString(text, x + 46, itemY);
        }
    }

    // Wrap helper for battleLog - optimized to reduce allocations
    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight, int maxLines){
        if(text == null || text.isEmpty()) return;
        String[] words = text.split(" ");
        int lines = 0;
        int currentLineWidth = 0;
        int lineStart = 0;
        
        for(int i = 0; i < words.length; i++){
            String word = words[i];
            int wordWidth = g2.getFontMetrics().stringWidth(word);
            int spaceWidth = i > lineStart ? g2.getFontMetrics().stringWidth(" ") : 0;
            
            if(currentLineWidth + spaceWidth + wordWidth <= maxWidth){
                currentLineWidth += spaceWidth + wordWidth;
            } else {
                // Draw current line
                if(lineStart < i){
                    drawWords(g2, words, lineStart, i, x, y + lines * lineHeight);
                }
                lines++;
                if(lines >= maxLines) return;
                currentLineWidth = wordWidth;
                lineStart = i;
            }
        }
        
        // Draw remaining words
        if(lineStart < words.length){
            drawWords(g2, words, lineStart, words.length, x, y + lines * lineHeight);
        }
    }
    
    private void drawWords(Graphics2D g2, String[] words, int start, int end, int x, int y){
        StringBuilder sb = new StringBuilder();
        for(int i = start; i < end; i++){
            if(i > start) sb.append(" ");
            sb.append(words[i]);
        }
        g2.drawString(sb.toString(), x, y);
    }

    // Try to get a valid battle sprite image from the entity; fallback to name-based assets.
    private BufferedImage getBattleSprite(Object ent, NPC npc){
        // 1) Try entity fields: idle, down1, up1, left1, right1
        BufferedImage img = tryGetImageField(ent, "idle");
        if(img == null) img = tryGetImageField(ent, "down1");
        if(img == null) img = tryGetImageField(ent, "up1");
        if(img == null) img = tryGetImageField(ent, "left1");
        if(img == null) img = tryGetImageField(ent, "right1");
        if(img != null) return img;

        // Name-based fallbacks
        String name = (npc != null && npc.name != null) ? npc.name : "";
        if(name.equalsIgnoreCase("Andrew")){
            // Try Andrew assets
            img = loadSprite("res/npc2/andrewIdle.png");
            if(img == null) img = loadSprite("res/npc2/andrewwalk1.png");
            if(img == null) img = loadSprite("res/npc2/andrewright1.png");
        } else if(name.equalsIgnoreCase("Delia") || name.equalsIgnoreCase("Mysterious Girl")){
            // Mysterious Girl/Delia fallbacks
            img = loadSprite("res/npc3/delia.png");
            if(img == null) img = loadSprite("res/npc3/love_idle.png");
            if(img == null) img = loadSprite("res/npc3/love_down1.png");
            return img;
        } else if(name.equalsIgnoreCase("Guero")){
            img = loadSprite("res/npc4/el guero.png");
            if(img == null) img = loadSprite("res/npc4/guero_down1.png");
            if(img == null) img = loadSprite("res/npc4/guero_right1.png");
            return img;
        } else {
            // Humberto defaults
            img = loadSprite("res/npcstudent1/humbertoidle.png");
            if(img == null) img = loadSprite("res/npcstudent1/humbertofoward1.png");
            if(img == null) img = loadSprite("res/npcstudent1/humbertoR.png");
        }
        return img;
    }

    private BufferedImage tryGetImageField(Object obj, String field){
        if(obj == null) return null;
        try{
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(obj);
            if(v instanceof BufferedImage bi) return bi;
        }catch(Exception ignored){}
        return null;
    }

    private BufferedImage loadSprite(String path){
        // classpath (resources packaged without "res/")
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(path.replace("res/",""))){
            if(is != null){
                BufferedImage img = ImageIO.read(is);
                if(img != null) return img;
            }
        }catch(Exception ignored){}
        // absolute file
        try{
            File f = new File(path);
            if(f.exists()) return ImageIO.read(f);
        }catch(Exception ignored){}
        // cwd + path
        try{
            String base = System.getProperty("user.dir");
            File f2 = new File(base, path);
            if(f2.exists()) return ImageIO.read(f2);
        }catch(Exception ignored){}
        return null;
    }

    // Allow external systems to close the battle
    public void endBattle() {
        end();
    }

    private void end(){
        inBattle = false;
        currentNPC = null;
        currentNPCEntity = null;
        cachedBattleSprite = null; // Clear cached sprite
        gp.gameState = GamePanel.GameState.PLAY;
        gp.stopMusic();
        try{
            gp.music.setFilePath("res/sound/song1.wav");
            gp.music.playLoop();
        }catch(Exception ignored){}
    }
}