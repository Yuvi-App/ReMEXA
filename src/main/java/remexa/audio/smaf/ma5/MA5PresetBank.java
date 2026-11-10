package remexa.audio.smaf.ma5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class MA5PresetBank {

    private static final int ZERO_ADDRESS_WAVE_ID = 128;
    private static final int[] ZERO_ADDRESS_WAVE = {-236, 334};
    private static final List<List<Entry>> PROGRAMS = load();

    private MA5PresetBank() { }

    public static MA5VoiceProgram fm(int program, int midiNote) {
        Entry entry = find(program, midiNote);
        return entry == null ? null : entry.fm;
    }

    public static MA5PcmVoiceProgram pcm(int program, int midiNote) {
        Entry entry = find(program, midiNote);
        return entry == null ? null : entry.pcm;
    }

    public static int[] specialWave(MA5PcmVoiceProgram program) {
        return program.romWave() && program.waveId() == ZERO_ADDRESS_WAVE_ID
                ? ZERO_ADDRESS_WAVE : null;
    }

    private static Entry find(int program, int note) {
        if (program < 0 || program > 127 || note < 0 || note > 127) return null;
        for (Entry entry : PROGRAMS.get(program))
            if (note >= entry.low && note <= entry.high) return entry;
        return null;
    }

    private static List<List<Entry>> load() {
        var programs = new ArrayList<List<Entry>>(128);
        for (int i = 0; i < 128; i++) programs.add(new ArrayList<>());
        var input = MA5PresetBank.class.getResourceAsStream("melodic-presets.tsv");
        if (input == null) throw new IllegalStateException("Missing native melodic presets");
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String row; (row = reader.readLine()) != null;) {
                if (row.startsWith("#") || row.startsWith("program\t") || row.isBlank()) continue;
                String[] fields = row.split("\t");
                int program = Integer.parseInt(fields[0]);
                int low = Integer.parseInt(fields[1]), high = Integer.parseInt(fields[2]);
                byte[] packet = HexFormat.of().parseHex(fields[7]);
                MA5VoiceProgram fm = fields[3].equals("FM") ? MA5VoiceProgram.decode(packet) : null;
                MA5PcmVoiceProgram pcm = fields[3].startsWith("PCM") ? MA5PcmVoiceProgram.decode(packet) : null;
                if (pcm != null && fields[3].equals("PCM_ZERO")) pcm = pcm.withRomWave(ZERO_ADDRESS_WAVE_ID);
                if (program < 0 || program > 127 || low < 0 || high > 127 || low > high
                        || (fm == null && pcm == null))
                    throw new IllegalStateException("Invalid native melodic preset: " + row);
                programs.get(program).add(new Entry(low, high, fm, pcm));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read native melodic presets", e);
        }
        for (int program = 0; program < programs.size(); program++) {
            int next = 0;
            for (Entry entry : programs.get(program)) {
                if (entry.low != next) throw new IllegalStateException("Preset key coverage: " + program);
                next = entry.high + 1;
            }
            if (next != 128) throw new IllegalStateException("Missing native preset: " + program);
            programs.set(program, List.copyOf(programs.get(program)));
        }
        return List.copyOf(programs);
    }

    private record Entry(int low, int high, MA5VoiceProgram fm, MA5PcmVoiceProgram pcm) { }
}
