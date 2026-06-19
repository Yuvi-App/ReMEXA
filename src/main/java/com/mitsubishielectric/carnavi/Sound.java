package com.mitsubishielectric.carnavi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import remexa.audio.pcm.RenderedPcmAudio;
import remexa.audio.pcm.RenderedPcmPlayer;
import remexa.host.runtime.MidletRuntime;
import remexa.probes.DebugLog;
import remexa.probes.LogCategory;
import remexa.probes.SdkStubSupport;

public final class Sound {
    private static final String LOG_SOURCE = Sound.class.getName();
    private static final int SAMPLE_RATE = 44_100;
    private static final int CHANNEL_COUNT = 2;
    private static final int MAX_ACTIVE_PLAYERS = 16;
    private static final ConcurrentMap<Integer, RenderedPcmAudio> EFFECT_CACHE = new ConcurrentHashMap<>();
    private static final Object ACTIVE_LOCK = new Object();
    private static final List<ActiveEffect> ACTIVE_PLAYERS = new ArrayList<>();

    private static volatile int volume = 100;
    private static volatile boolean muted;

    public Sound() {
        SdkStubSupport.log(LOG_SOURCE, "Sound");
    }

    public static void play(int soundId) {
        SdkStubSupport.log(LOG_SOURCE, "play", soundId);
        ClassLoader ownerClassLoader = MidletRuntime.currentAppClassLoader();
        MidletRuntime.ensureThreadActive();
        if (muted || volume <= 0) {
            return;
        }

        try {
            RenderedPcmAudio audio = EFFECT_CACHE.computeIfAbsent(Integer.valueOf(soundId), Sound::createEffectAudio);
            RenderedPcmPlayer.prewarm(SAMPLE_RATE, CHANNEL_COUNT);
            RenderedPcmPlayer player = new RenderedPcmPlayer(audio);
            applyVolume(player);
            player.setCompletionListener(() -> {
                forget(player);
                closeQuietly(player);
            });
            track(soundId, ownerClassLoader, player);
            player.play(1);
        } catch (RuntimeException exception) {
            DebugLog.log(
                    LogCategory.MEDIA,
                    LOG_SOURCE,
                    "Unable to play Mitsubishi car navigation sound " + soundId + ": " + describeException(exception)
            );
        }
    }

    public static void stop() {
        SdkStubSupport.log(LOG_SOURCE, "stop");
        List<RenderedPcmPlayer> snapshot = drainActivePlayers(-1, false);
        snapshot.forEach(Sound::closeQuietly);
    }

    public static void stop(int soundId) {
        SdkStubSupport.log(LOG_SOURCE, "stop", soundId);
        List<RenderedPcmPlayer> snapshot = drainActivePlayers(soundId, true);
        snapshot.forEach(Sound::closeQuietly);
    }

    public static void shutdownOwnedPlayers(ClassLoader ownerClassLoader) {
        List<RenderedPcmPlayer> players = new ArrayList<>();
        synchronized (ACTIVE_LOCK) {
            for (int index = 0; index < ACTIVE_PLAYERS.size(); index++) {
                ActiveEffect effect = ACTIVE_PLAYERS.get(index);
                if (!effect.isOwnedBy(ownerClassLoader)) {
                    continue;
                }
                players.add(effect.player());
                ACTIVE_PLAYERS.remove(index--);
            }
        }
        players.forEach(Sound::closeQuietly);
    }

    public static void setVolume(int level) {
        SdkStubSupport.log(LOG_SOURCE, "setVolume", level);
        volume = Math.max(0, Math.min(100, level));
        for (RenderedPcmPlayer player : activePlayerSnapshot()) {
            applyVolumeQuietly(player);
        }
    }

    public static int getVolume() {
        SdkStubSupport.log(LOG_SOURCE, "getVolume");
        return volume;
    }

    public static void setMute(boolean mute) {
        SdkStubSupport.log(LOG_SOURCE, "setMute", mute);
        muted = mute;
        if (mute) {
            stop();
        }
    }

    public static boolean isMuted() {
        SdkStubSupport.log(LOG_SOURCE, "isMuted");
        return muted;
    }

    private static void track(int soundId, ClassLoader ownerClassLoader, RenderedPcmPlayer player) {
        List<RenderedPcmPlayer> evicted = new ArrayList<>();
        synchronized (ACTIVE_LOCK) {
            ACTIVE_PLAYERS.add(new ActiveEffect(soundId, ownerClassLoader, player));
            while (ACTIVE_PLAYERS.size() > MAX_ACTIVE_PLAYERS) {
                evicted.add(ACTIVE_PLAYERS.remove(0).player());
            }
        }
        evicted.forEach(Sound::closeQuietly);
    }

    private static void forget(RenderedPcmPlayer player) {
        synchronized (ACTIVE_LOCK) {
            ACTIVE_PLAYERS.removeIf(effect -> effect.player() == player);
        }
    }

    private static List<RenderedPcmPlayer> drainActivePlayers(int soundId, boolean matchSoundId) {
        List<RenderedPcmPlayer> players = new ArrayList<>();
        synchronized (ACTIVE_LOCK) {
            for (int index = 0; index < ACTIVE_PLAYERS.size(); index++) {
                ActiveEffect effect = ACTIVE_PLAYERS.get(index);
                if (matchSoundId && effect.soundId() != soundId) {
                    continue;
                }
                players.add(effect.player());
                ACTIVE_PLAYERS.remove(index--);
            }
        }
        return players;
    }

    private static List<RenderedPcmPlayer> activePlayerSnapshot() {
        synchronized (ACTIVE_LOCK) {
            List<RenderedPcmPlayer> players = new ArrayList<>(ACTIVE_PLAYERS.size());
            for (ActiveEffect effect : ACTIVE_PLAYERS) {
                players.add(effect.player());
            }
            return players;
        }
    }

    private static void closeQuietly(RenderedPcmPlayer player) {
        try {
            player.close();
        } catch (RuntimeException exception) {
            DebugLog.log(LogCategory.MEDIA, LOG_SOURCE, "Unable to close sound player: " + describeException(exception));
        }
    }

    private static void applyVolumeQuietly(RenderedPcmPlayer player) {
        try {
            applyVolume(player);
        } catch (RuntimeException exception) {
            DebugLog.log(LogCategory.MEDIA, LOG_SOURCE, "Unable to apply sound volume: " + describeException(exception));
        }
    }

    private static void applyVolume(RenderedPcmPlayer player) {
        player.setVolume(muted ? 0 : Math.round(volume * 127.0f / 100.0f));
    }

    private static RenderedPcmAudio createEffectAudio(Integer soundId) {
        byte[] pcm = encodePcm16LeStereo(renderEffect(soundId == null ? 0 : soundId.intValue()));
        return new RenderedPcmAudio(SAMPLE_RATE, CHANNEL_COUNT, pcm.length / (CHANNEL_COUNT * 2), pcm);
    }

    private static float[] renderEffect(int soundId) {
        return switch (soundId) {
            case 0, 18 -> renderSweep(soundId, 160, 95, 90, 0.90f, Wave.SINE, 0.45f, 0.00f, 0.0f);
            case 1, 19 -> concat(
                    renderSweep(soundId, 950, 620, 45, 0.78f, Wave.TRIANGLE, 0.12f, 0.00f, 0.0f),
                    renderSweep(soundId + 1, 260, 180, 55, 0.62f, Wave.SQUARE, 0.28f, 0.00f, 0.0f)
            );
            case 2, 20 -> renderSweep(soundId, 190, 95, 420, 0.72f, Wave.SQUARE, 0.25f, 0.18f, 12.0f);
            case 10 -> concat(
                    renderSweep(soundId, 523, 659, 70, 0.62f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f),
                    renderSweep(soundId, 659, 784, 70, 0.62f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f),
                    renderSweep(soundId, 784, 1046, 120, 0.58f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f)
            );
            case 11 -> renderSweep(soundId, 360, 460, 240, 0.62f, Wave.SQUARE, 0.10f, 0.08f, 8.0f);
            case 12 -> renderSweep(soundId, 460, 280, 280, 0.68f, Wave.SQUARE, 0.20f, 0.12f, 7.0f);
            case 13 -> renderSweep(soundId, 760, 920, 42, 0.48f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f);
            case 14 -> renderSweep(soundId, 920, 760, 42, 0.48f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f);
            case 15 -> concat(
                    renderSweep(soundId, 660, 880, 60, 0.58f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f),
                    renderSweep(soundId, 880, 1320, 120, 0.54f, Wave.TRIANGLE, 0.00f, 0.00f, 0.0f)
            );
            case 16 -> renderSweep(soundId, 880, 220, 180, 0.68f, Wave.SINE, 0.08f, 0.04f, 9.0f);
            case 17 -> concat(
                    renderSweep(soundId, 330, 110, 260, 0.74f, Wave.SQUARE, 0.15f, 0.08f, 5.0f),
                    renderSweep(soundId, 110, 70, 160, 0.50f, Wave.SINE, 0.20f, 0.00f, 0.0f)
            );
            case 23 -> renderSweep(soundId, 120, 220, 95, 0.76f, Wave.SQUARE, 0.40f, 0.00f, 0.0f);
            case 26 -> renderSweep(soundId, 540, 360, 90, 0.58f, Wave.TRIANGLE, 0.08f, 0.00f, 0.0f);
            case 31 -> renderSweep(soundId, 300, 740, 180, 0.64f, Wave.TRIANGLE, 0.12f, 0.05f, 9.0f);
            default -> renderDefault(soundId);
        };
    }

    private static float[] renderDefault(int soundId) {
        int base = 220 + Math.floorMod(soundId * 83, 660);
        int end = base + 80 - Math.floorMod(soundId * 47, 160);
        int duration = 60 + Math.floorMod(soundId * 17, 130);
        Wave wave = switch (Math.floorMod(soundId, 3)) {
            case 0 -> Wave.SINE;
            case 1 -> Wave.TRIANGLE;
            default -> Wave.SQUARE;
        };
        float noise = Math.floorMod(soundId, 5) == 0 ? 0.16f : 0.04f;
        return renderSweep(soundId, base, Math.max(80, end), duration, 0.55f, wave, noise, 0.04f, 6.0f);
    }

    private static float[] renderSweep(int seed,
                                       int startFrequency,
                                       int endFrequency,
                                       int durationMillis,
                                       float gain,
                                       Wave wave,
                                       float noiseAmount,
                                       float vibratoDepth,
                                       float vibratoRate) {
        int frames = Math.max(1, Math.round(SAMPLE_RATE * durationMillis / 1000.0f));
        float[] samples = new float[frames];
        double phase = 0.0;
        int noiseState = seed * 1_103_515_245 + 12_345;
        for (int index = 0; index < frames; index++) {
            float progress = frames == 1 ? 1.0f : index / (float) (frames - 1);
            double frequency = startFrequency + (endFrequency - startFrequency) * progress;
            if (vibratoDepth > 0.0f && vibratoRate > 0.0f) {
                frequency *= 1.0 + vibratoDepth * Math.sin(2.0 * Math.PI * vibratoRate * index / SAMPLE_RATE);
            }
            phase += 2.0 * Math.PI * Math.max(1.0, frequency) / SAMPLE_RATE;
            double tone = wave.sample(phase);
            noiseState = noiseState * 1_103_515_245 + 12_345;
            float noise = (((noiseState >>> 16) & 0x7FFF) / 16_384.0f) - 1.0f;
            float mixed = (float) (tone * (1.0f - noiseAmount) + noise * noiseAmount);
            samples[index] = mixed * gain * envelope(index, frames);
        }
        return samples;
    }

    private static float envelope(int frame, int frames) {
        float progress = frames <= 1 ? 1.0f : frame / (float) (frames - 1);
        float attackFrames = SAMPLE_RATE * 0.005f;
        float attack = attackFrames <= 1.0f ? 1.0f : Math.min(1.0f, frame / attackFrames);
        float decay = (float) Math.pow(1.0f - progress, 1.28f);
        return attack * decay;
    }

    private static float[] concat(float[]... parts) {
        int totalLength = 0;
        for (float[] part : parts) {
            totalLength += part.length;
        }
        float[] result = new float[totalLength];
        int offset = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static byte[] encodePcm16LeStereo(float[] samples) {
        byte[] pcm = new byte[samples.length * CHANNEL_COUNT * 2];
        int output = 0;
        for (float sample : samples) {
            int value = Math.round(Math.max(-1.0f, Math.min(1.0f, sample)) * 32767.0f);
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                pcm[output++] = (byte) (value & 0xFF);
                pcm[output++] = (byte) ((value >>> 8) & 0xFF);
            }
        }
        return pcm;
    }

    private static String describeException(Throwable exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record ActiveEffect(int soundId, ClassLoader ownerClassLoader, RenderedPcmPlayer player) {
        private boolean isOwnedBy(ClassLoader candidate) {
            return candidate == null || ownerClassLoader == null || ownerClassLoader == candidate;
        }
    }

    private enum Wave {
        SINE {
            @Override
            double sample(double phase) {
                return Math.sin(phase);
            }
        },
        TRIANGLE {
            @Override
            double sample(double phase) {
                return Math.asin(Math.sin(phase)) * (2.0 / Math.PI);
            }
        },
        SQUARE {
            @Override
            double sample(double phase) {
                return Math.sin(phase) >= 0.0 ? 1.0 : -1.0;
            }
        };

        abstract double sample(double phase);
    }
}
