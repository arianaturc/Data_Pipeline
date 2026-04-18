package model;

public class EncodingPreset {
    private final String label;
    private final String codec;
    private final String container;
    private final int width;
    private final int height;
    private final String outputDir;

    public EncodingPreset(String label, String codec, String container,
                          int width, int height, String outputDir) {
        this.label = label;
        this.codec = codec;
        this.container = container;
        this.width = width;
        this.height = height;
        this.outputDir = outputDir;
    }

    public String getFilename() {
        return label + "." + container;
    }

    public String getLabel() {
        return label;
    }

    public String getCodec() {
        return codec;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getOutputDir() {
        return outputDir;
    }

    @Override
    public String toString() {
        return label + " (" + codec + ", " + width + "x" + height + ")";
    }
}
