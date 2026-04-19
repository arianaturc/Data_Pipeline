package service;

import model.EncodingPreset;
import model.PresetFactory;
import orchestrator.*;
import util.FileUtils;
import util.ShellExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


public class VisualsService {

    public static class SceneComplexityAnalyzer implements PipelineStage {

        @Override
        public String getName() {
            return "Scene Complexity Analyzer";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();

            try {
                ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                        "ffprobe",
                        "-v", "error",
                        "-select_streams", "v:0",
                        "-show_entries", "format=bit_rate",
                        "-of", "default=noprint_wrappers=1:nokey=1",
                        inputFile.toString()
                });

                double bitrate = 0;
                if (result.isSuccess() && !result.getStdout().isEmpty()) {
                    try {
                        bitrate = Double.parseDouble(result.getStdout().trim());
                    } catch (NumberFormatException e) {
                        bitrate = 5_000_000;
                    }
                }

                String profile;
                if (bitrate > 10_000_000) {
                    profile = "high";
                } else if (bitrate > 3_000_000) {
                    profile = "medium";
                } else {
                    profile = "low";
                }

                context.setAverageComplexity(bitrate);
                context.setComplexityProfile(profile);

                System.out.printf("     Bitrate: %.0f bps -> Complexity: %s%n", bitrate, profile);

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Complexity analysis failed: " + e.getMessage(), e);
            }
        }
    }

    public static class Transcoder implements PipelineStage {

        @Override
        public String getName() {
            return "Transcoder";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();
            List<EncodingPreset> presets = PresetFactory.createAllPresets();

            int crf = getCrfForProfile(context.getComplexityProfile());

            System.out.printf("    Encoding %d variants (CRF=%d, profile=%s)%n",
                    presets.size(), crf, context.getComplexityProfile());

            for (EncodingPreset preset : presets) {
                try {
                    transcodeVariant(inputFile, preset, crf, context);
                } catch (IOException e) {
                    throw new PipelineException(getName(),
                            "Transcoding failed for " + preset + ": " + e.getMessage(), e);
                }
            }
        }

        private void transcodeVariant(Path inputFile, EncodingPreset preset,
                                      int crf, PipelineContext context)
                throws IOException, PipelineException {

            Path outputDir = context.getConfig().getVideoDir().resolve(preset.getOutputDir());
            FileUtils.ensureDirectory(outputDir);
            Path outputFile = outputDir.resolve(preset.getFilename());

            System.out.printf("    [encoding] %s ... ", preset.getLabel());
            long startTime = System.currentTimeMillis();

            String[] command;

            if (preset.getCodec().equals("libvpx-vp9")) {
                command = new String[]{
                        "ffmpeg", "-y",
                        "-i", inputFile.toString(),
                        "-vf", "scale=" + preset.getWidth() + ":" + preset.getHeight(),
                        "-c:v", preset.getCodec(),
                        "-crf", String.valueOf(crf),
                        "-b:v", "0",
                        "-deadline", "realtime",
                        "-cpu-used", "8",
                        "-c:a", "libopus",
                        "-threads", "4",
                        outputFile.toString()
                };
            } else if (preset.getCodec().equals("libx265")) {
                command = new String[]{
                        "ffmpeg", "-y",
                        "-i", inputFile.toString(),
                        "-vf", "scale=" + preset.getWidth() + ":" + preset.getHeight(),
                        "-c:v", preset.getCodec(),
                        "-crf", String.valueOf(crf),
                        "-preset", "ultrafast",
                        "-tag:v", "hvc1",
                        "-c:a", "aac",
                        "-threads", "4",
                        outputFile.toString()
                };
            } else {
                command = new String[]{
                        "ffmpeg", "-y",
                        "-i", inputFile.toString(),
                        "-vf", "scale=" + preset.getWidth() + ":" + preset.getHeight(),
                        "-c:v", preset.getCodec(),
                        "-crf", String.valueOf(crf),
                        "-preset", "ultrafast",
                        "-c:a", "aac",
                        "-threads", "4",
                        outputFile.toString()
                };
            }

            ShellExecutor.Result result = ShellExecutor.execute(command, 300);
            long elapsed = System.currentTimeMillis() - startTime;

            if (!result.isSuccess()) {
                System.out.printf("FAILED (%.1fs)%n", elapsed / 1000.0);
                throw new PipelineException(getName(),
                        "ffmpeg failed for " + preset.getLabel() + ": " + result.getStderr());
            }

            String assetPath = "video/" + preset.getOutputDir() + "/" + preset.getFilename();
            context.addAsset(assetPath);
            System.out.printf("OK (%.1fs)%n", elapsed / 1000.0);
        }

        private int getCrfForProfile(String profile) {
            if (profile == null) return 23;
            switch (profile) {
                case "high":   return 20;
                case "low":    return 28;
                default:       return 23;
            }
        }
    }


    public static class SpriteGenerator implements PipelineStage {

        @Override
        public String getName() {
            return "Sprite Generator";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();
            Path thumbnailsDir = context.getConfig().getThumbnailsDir();
            Path imagesDir = context.getConfig().getImagesDir();
            int interval = context.getConfig().getThumbnailIntervalSeconds();

            try {
                FileUtils.ensureDirectory(thumbnailsDir);
                FileUtils.ensureDirectory(imagesDir);

                ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                        "ffmpeg", "-y",
                        "-i", inputFile.toString(),
                        "-vf", "fps=1/" + interval + ",scale=320:-1",
                        "-q:v", "5",
                        thumbnailsDir.resolve("thumb_%03d.jpg").toString()
                }, 120);

                if (!result.isSuccess()) {
                    System.out.println("    Warning: thumbnail generation had issues: "
                            + result.getStderr());
                }

                context.addAsset("images/thumbnails/");

                int columns = context.getConfig().getSpriteColumns();
                int totalThumbs = Math.max(1, (int) (context.getVideoDuration() / interval));
                int rows = (int) Math.ceil((double) totalThumbs / columns);

                result = ShellExecutor.execute(new String[]{
                        "ffmpeg", "-y",
                        "-i", inputFile.toString(),
                        "-vf", "fps=1/" + interval + ",scale=160:-1,tile=" + columns + "x" + rows,
                        "-q:v", "5",
                        imagesDir.resolve("sprite_map.jpg").toString()
                }, 120);

                if (result.isSuccess()) {
                    System.out.printf("    Sprite map generated (%dx%d tile, %d thumbs)%n",
                            columns, rows, totalThumbs);
                } else {
                    System.out.println("    Warning: sprite map generation failed: "
                            + result.getStderr());
                }

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Sprite generation failed: " + e.getMessage(), e);
            }
        }
    }
}
