# I'll See You Whenever

*I'll See You Whenever* is a small story-driven conversation RPG about finding connection while trying to feel at home in a new place.

The project is being built around a simple idea: conversations can feel like battles, but winning is not about overpowering someone. It is about listening, being honest, taking risks, and choosing how you show up. The goal is to make a game that feels personal, replayable, and welcoming while still having satisfying game feel. The game is more of a love letter towards myself, back in my freshmen year of college I had a hard time fitting in and talking to people. I wanted this game to represent that feeling. This is a small demo I worked on in a span of a week, artwork and production done by me Gustavo Castillo. This project is a passion project of mines and I want to do much more with this in the future.

## What I Am Trying To Achieve

- Tell a character-focused story about confidence, belonging, and friendship.
- Make dialogue choices matter through affinity, branching responses, and different outcomes.
- Turn difficult conversations into short, energetic choice-based battles.
- Give every interaction a distinct personality, tone, and relationship arc.
- Build a game that feels good to play through readable UI, music, sound effects, animation, and feedback.

## Current Gameplay

1. Explore the school and meet the characters.
2. Walk up to an NPC and press `O` to interact.
3. Read their dialogue and choose how to respond.
4. Enter a conversation battle where actions affect damage and affinity.
5. Use `TALK` for stronger attacks or `LISTEN` to heal and reduce incoming damage.
6. Build combos to trigger critical hits every third successful hit.
7. Befriend characters, or face the consequences of pushing them away.

The current characters include Humberto, Andrew, Delia, and Guero. Each has their own dialogue, responses, battle style, and relationship outcome.

## Controls

| Key | Action |
| --- | --- |
| `W` / `Up` | Move up or select the previous option |
| `S` / `Down` | Move down or select the next option |
| `A` / `Left` | Move left |
| `D` / `Right` | Move right |
| `O` / `Enter` | Confirm, interact, or advance dialogue |
| `P` / `Escape` | Cancel or skip some dialogue |

## Running From Source

The project requires JDK 25.

Compile from the repository root.

### macOS or Linux

```bash
mkdir -p bin
javac --enable-preview --release 25 -d bin $(find src -name "*.java")
java --enable-preview -cp bin mainpack1.mainclass
```

### Windows PowerShell

```powershell
$sources = Get-ChildItem -Path .\src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --enable-preview --release 25 -d .\bin $sources
java --enable-preview -cp .\bin mainpack1.mainclass
```

Run the commands from the repository root so the game can find the `res/` assets and music.

## Project Layout

- `src/` - Java source code
- `res/` - maps, sprites, tile art, and music
- `bin/` - local compiled classes; generated class files are not committed
- `MonitoRPG.jar` - packaged Java build
- `MonitoRPG.exe` - Windows launcher build

## Project Status

This is an active personal game project. The core exploration, dialogue, relationship, battle, audio, and ending systems are in place, while the story, balance, presentation, and character content are still being refined.

## Credits

This project was developed worked on by Gustavo Castillo
Music Produced By Gustavo Castillo & Marco Telles (Ziday)
Zidays Links: https://ziday.newgrounds.com , https://soundcloud.com/ziday
