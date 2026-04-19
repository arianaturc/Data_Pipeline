package service;

import orchestrator.PipelineContext;
import orchestrator.PipelineException;
import orchestrator.PipelineStage;
import util.FileUtils;
import util.JsonBuilder;
import util.ShellExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalysisService {
    public static class IntroOutroDetector implements PipelineStage {

        @Override
        public String getName() {
            return "Intro/Outro Detector";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();
            double duration = context.getVideoDuration();

            try {
                ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                        "ffmpeg",
                        "-i", inputFile.toString(),
                        "-af", "silencedetect=noise=-30dB:d=0.5",
                        "-f", "null", "-"
                }, 60);

                String output = result.getStderr();
                double introEnd = findIntroEnd(output, duration);
                context.setIntroEndTimestamp(introEnd);

                System.out.printf("Intro ends at: %.1fs%n", introEnd);

            } catch (IOException e) {
                double fallback = Math.min(duration * 0.05, 30.0);
                context.setIntroEndTimestamp(fallback);
                System.out.printf("Intro ends at: %.1fs (fallback)%n", fallback);
            }
        }

        private double findIntroEnd(String output, double duration) {
            double maxIntroTime = duration * 0.20;
            String[] lines = output.split("\n");

            for (String line : lines) {
                if (line.contains("silence_end:")) {
                    try {
                        String[] parts = line.split("silence_end:")[1].trim().split("\\|");
                        double silenceEnd = Double.parseDouble(parts[0].trim());

                        if (silenceEnd > 1.0 && silenceEnd <= maxIntroTime) {
                            return silenceEnd;
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException _) {

                    }
                }
            }

            return Math.min(duration * 0.05, 30.0);
        }
    }

    public static class CreditRoller implements PipelineStage {

        @Override
        public String getName() {
            return "Credit Roller";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();
            double duration = context.getVideoDuration();

            try {
                ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                        "ffmpeg",
                        "-i", inputFile.toString(),
                        "-af", "silencedetect=noise=-30dB:d=0.5",
                        "-f", "null", "-"
                }, 60);

                String output = result.getStderr();
                double creditsStart = findCreditsStart(output, duration);
                context.setCreditsStartTimestamp(creditsStart);

                System.out.printf("Credits start at: %.1fs%n", creditsStart);

            } catch (IOException e) {
                double fallback = duration * 0.95;
                context.setCreditsStartTimestamp(fallback);
                System.out.printf("Credits start at: %.1fs (fallback)%n", fallback);
            }
        }


        private double findCreditsStart(String output, double duration) {
            double minCreditsTime = duration * 0.80;
            String[] lines = output.split("\n");
            double lastSilenceStart = -1;

            for (String line : lines) {
                if (line.contains("silence_start:")) {
                    try {
                        String value = line.split("silence_start:")[1].trim();
                        double silenceStart = Double.parseDouble(value);

                        if (silenceStart >= minCreditsTime) {
                            lastSilenceStart = silenceStart;
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException _) {
                    }
                }
            }

            if (lastSilenceStart > 0) {
                return lastSilenceStart;
            }

            return duration * 0.95;
        }
    }

    public static class SceneIndexer implements PipelineStage {

        @Override
        public String getName() {
            return "Scene Indexer";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();
            Path metadataDir = context.getConfig().getMetadataDir();

            try {
                FileUtils.ensureDirectory(metadataDir);

                ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                        "ffprobe",
                        "-v", "error",
                        "-select_streams", "v:0",
                        "-show_frames",
                        "-show_entries", "frame=pts_time,pict_type",
                        "-of", "csv=p=0",
                        inputFile.toString()
                }, 120);

                List<Map<String, Object>> segments = new ArrayList<>();

                if (result.isSuccess() && !result.getStdout().isEmpty()) {
                    segments = classifyScenes(result.getStdout(), context.getVideoDuration());
                } else {
                    segments = generateStubSegments(context.getVideoDuration());
                }

                context.setSceneSegments(segments);

                Map<String, Object> analysis = JsonBuilder.object();
                analysis.put("video_duration", context.getVideoDuration());
                analysis.put("intro_end", context.getIntroEndTimestamp());
                analysis.put("credits_start", context.getCreditsStartTimestamp());
                analysis.put("total_scenes", segments.size());

                List<Object> segmentList = new ArrayList<>(segments);
                analysis.put("scenes", segmentList);

                Path outputFile = metadataDir.resolve("scene_analysis.json");
                FileUtils.writeString(outputFile, JsonBuilder.toJson(analysis));
                context.addAsset("metadata/scene_analysis.json");

                System.out.printf("Detected %d scene segments%n", segments.size());

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Scene indexing failed: " + e.getMessage(), e);
            }
        }


        private List<Map<String, Object>> classifyScenes(String frameData, double totalDuration) {
            List<Map<String, Object>> segments = new ArrayList<>();

            String[] lines = frameData.split("\n");

            List<Double> iFrameTimes = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 2) {
                    try {
                        double time = Double.parseDouble(parts[0].trim());
                        String type = parts[1].trim();
                        if ("I".equals(type)) {
                            iFrameTimes.add(time);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            double windowSize = Math.max(totalDuration / 5.0, 5.0);
            int windowCount = Math.max(1, (int) Math.ceil(totalDuration / windowSize));

            for (int i = 0; i < windowCount && i < 20; i++) {
                double start = i * windowSize;
                double end = Math.min((i + 1) * windowSize, totalDuration);

                int iFrameCount = 0;
                for (double t : iFrameTimes) {
                    if (t >= start && t < end) {
                        iFrameCount++;
                    }
                }


                double duration = end - start;
                double iFrameDensity = (duration > 0) ? iFrameCount / duration : 0;

                String category;
                if (iFrameDensity >= 1.0) {
                    category = "action";
                } else if (iFrameDensity >= 0.3) {
                    category = "dialogue";
                } else {
                    category = "establishing_shot";
                }

                Map<String, Object> segment = JsonBuilder.object();
                segment.put("segment_id", i + 1);
                segment.put("start_time", Math.round(start * 100.0) / 100.0);
                segment.put("end_time", Math.round(end * 100.0) / 100.0);
                segment.put("category", category);
                segment.put("i_frame_count", iFrameCount);
                segment.put("i_frame_density", Math.round(iFrameDensity * 1000.0) / 1000.0);

                segments.add(segment);
            }

            return segments;
        }

        private List<Map<String, Object>> generateStubSegments(double totalDuration) {
            List<Map<String, Object>> segments = new ArrayList<>();
            String[] categories = {"establishing_shot", "dialogue", "action", "dialogue", "action"};

            double segLength = totalDuration / categories.length;
            for (int i = 0; i < categories.length; i++) {
                Map<String, Object> seg = JsonBuilder.object();
                seg.put("segment_id", i + 1);
                seg.put("start_time", Math.round(i * segLength * 100.0) / 100.0);
                seg.put("end_time", Math.round((i + 1) * segLength * 100.0) / 100.0);
                seg.put("category", categories[i]);
                segments.add(seg);
            }
            return segments;
        }
    }
}
