package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PresetFactory {
    private static final Map<String, int[]> RESOLUTIONS = new LinkedHashMap<>();
    static {
        RESOLUTIONS.put("4k",    new int[]{3840, 2160});
        RESOLUTIONS.put("1080p", new int[]{1920, 1080});
        RESOLUTIONS.put("720p",  new int[]{1280, 720});
    }

    private static final String[][] CODECS = {
            {"h264", "libx264",    "mp4"},
            {"vp9",  "libvpx-vp9", "webm"},
            {"hevc", "libx265",    "mkv"},
    };

    public static List<EncodingPreset> createAllPresets() {
        List<EncodingPreset> presets = new ArrayList<>();

        for (String[] codecInfo : CODECS) {
            String dirName = codecInfo[0];
            String ffmpegCodec = codecInfo[1];
            String container = codecInfo[2];

            for (Map.Entry<String, int[]> res : RESOLUTIONS.entrySet()) {
                String resLabel = res.getKey();
                int w = res.getValue()[0];
                int h = res.getValue()[1];

                String label = resLabel + "_" + dirName;
                presets.add(new EncodingPreset(label, ffmpegCodec, container, w, h, dirName));
            }
        }

        return presets;
    }
}
