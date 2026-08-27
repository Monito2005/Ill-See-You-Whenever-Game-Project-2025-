package mainpack1;

import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class sound {
    Clip clip;
    URL soundURL[] = new URL[30];
    // default launch volume (0.0f = mute, 1.0f = full)
    private float defaultVolume = 0.4f;

    public sound(){
        try{
            // Load sound file from the project's res/sound directory on the filesystem.
            soundURL[0] = new java.io.File("res/sound/song1.wav").toURI().toURL();
            soundURL[1] = new java.io.File("res/sound/menu1.wav").toURI().toURL();
            soundURL[3] = new java.io.File("res/sound/introgame.wav").toURI().toURL();
            soundURL[2] = new java.io.File("res/sound/credit1.wav").toURI().toURL();
        } catch(Exception e){
            e.printStackTrace();
            soundURL[0] = null;
        }

    }

    public void setFile(int i){
        try{
            if(i < 0 || i >= soundURL.length || soundURL[i] == null){
                System.err.println("sound: requested index " + i + " not available");
                return;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
            // Apply the default launch volume (40%)
            setVolume(defaultVolume);
        } catch(Exception e){
            e.printStackTrace();}
    }

    public void setFilePath(String path){
        try{
            java.io.File f = new java.io.File(path);
            javax.sound.sampled.AudioInputStream ais =
                    javax.sound.sampled.AudioSystem.getAudioInputStream(f);
            clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(ais);
        }catch(Exception e){
            System.out.println("Cannot load sound path: " + path);
        }
    }

    // Set volume using a linear scale 0.0f (mute) -> 1.0f (full)
    public void setVolume(float volume){
        if(clip == null) return;
        if(volume < 0f) volume = 0f;
        if(volume > 1f) volume = 1f;
        try{
            if(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)){
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if(volume == 0f){
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    float dB = (float)(20.0 * Math.log10(volume));
                    if(dB < gainControl.getMinimum()) dB = gainControl.getMinimum();
                    if(dB > gainControl.getMaximum()) dB = gainControl.getMaximum();
                    gainControl.setValue(dB);
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void play(){
        if(clip != null){
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void playTone(double frequency, int durationMs, float volume){
        try{
            int sampleRate = 44100;
            int sampleCount = Math.max(1, sampleRate * durationMs / 1000);
            byte[] samples = new byte[sampleCount];
            for(int i = 0; i < sampleCount; i++){
                double progress = i / (double)sampleCount;
                double envelope = Math.min(1.0, Math.min(progress * 20.0, (1.0 - progress) * 20.0));
                samples[i] = (byte)(Math.sin(2.0 * Math.PI * frequency * i / sampleRate)
                        * 127.0 * envelope);
            }

            if(clip != null){
                clip.stop();
                clip.close();
            }
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            clip = AudioSystem.getClip();
            clip.open(format, samples, 0, samples.length);
            setVolume(volume);
            clip.start();
        }catch(Exception ignored){}
    }

    public void playLoop(){
        try{
            if(clip != null){
                clip.stop();
                clip.setFramePosition(0);
                clip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
            }
        }catch(Exception ignored){}
    }
    public void loop(){
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void stop(){
        if(clip != null){
            try{
                clip.stop();
                clip.close();
            }catch(Exception ignored){}
            clip = null;
        }
    }
}

