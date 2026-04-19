package service;

import orchestrator.PipelineContext;
import orchestrator.PipelineException;
import orchestrator.PipelineStage;
import util.FileUtils;
import util.ShellExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AudioTextService {
    public static class SpeechToText implements PipelineStage {

        @Override
        public String getName() {
            return "Speech-to-Text";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path textDir = context.getConfig().getTextDir();

            try {
                FileUtils.ensureDirectory(textDir);

                double duration = context.getVideoDuration();

                StringBuilder transcript = new StringBuilder();
                transcript.append("SOURCE TRANSCRIPT (English Master)\n");
                transcript.append("===================================\n");
                transcript.append(String.format("Video Duration: %.1fs%n%n", duration));

                var scenes = context.getSceneSegments();
                if (scenes != null && !scenes.isEmpty()) {
                    for (var scene : scenes) {
                        double start = ((Number) scene.get("start_time")).doubleValue();
                        double end = ((Number) scene.get("end_time")).doubleValue();
                        String category = (String) scene.get("category");

                        transcript.append(String.format("[%s - %s] (%s)%n",
                                formatTimestamp(start), formatTimestamp(end), category));
                        transcript.append(getStubDialogue(category));
                        transcript.append("\n\n");
                    }
                } else {
                    transcript.append("[00:00 - " + formatTimestamp(duration) + "]\n");
                    transcript.append("[Transcription placeholder - ASR engine not available]\n");
                }

                Path outputFile = textDir.resolve("source_transcript.txt");
                FileUtils.writeString(outputFile, transcript.toString());
                context.addAsset("text/source_transcript.txt");

                System.out.println("    Transcript generated (stub)");

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Transcription failed: " + e.getMessage(), e);
            }
        }

        private String getStubDialogue(String category) {
            switch (category) {
                case "dialogue":
                    return "[Speaker 1]: This is a sample dialogue line.\n"
                            + "[Speaker 2]: And this is the response.";
                case "action":
                    return "[Sound effects: intense music, impacts]\n"
                            + "[No dialogue detected]";
                case "establishing_shot":
                    return "[Ambient audio: atmospheric background]\n"
                            + "[Narrator]: Setting the scene...";
                default:
                    return "[Audio content detected]";
            }
        }

        private String formatTimestamp(double seconds) {
            int mins = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format("%02d:%02d", mins, secs);
        }
    }


    public static class Translator implements PipelineStage {

        @Override
        public String getName() {
            return "Translator";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path textDir = context.getConfig().getTextDir();
            List<String> languages = context.getConfig().getTargetLanguages();

            try {
                for (String lang : languages) {
                    StringBuilder translation = new StringBuilder();
                    translation.append(String.format(
                            "TRANSLATION: %s%n", lang.toUpperCase()));
                    translation.append("===================================\n");
                    translation.append("Status: STUB - Pending professional translation\n");
                    translation.append(String.format("Source: English Master%n"));
                    translation.append(String.format("Target: %s%n%n", lang));

                    var scenes = context.getSceneSegments();
                    if (scenes != null) {
                        for (var scene : scenes) {
                            double start = ((Number) scene.get("start_time")).doubleValue();
                            double end = ((Number) scene.get("end_time")).doubleValue();
                            translation.append(String.format("[%02d:%02d - %02d:%02d] ",
                                    (int) start / 60, (int) start % 60,
                                    (int) end / 60, (int) end % 60));
                            translation.append(String.format(
                                    "[%s translation placeholder]%n", lang));
                        }
                    }

                    String filename = lang + "_translation.txt";
                    FileUtils.writeString(textDir.resolve(filename), translation.toString());
                    context.addAsset("text/" + filename);

                    System.out.printf("    Translation stub: %s%n", filename);
                }
            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Translation failed: " + e.getMessage(), e);
            }
        }
    }


    public static class AIDubber implements PipelineStage {

        @Override
        public String getName() {
            return "AI Dubber";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();
            Path audioDir = context.getConfig().getAudioDir();
            List<String> languages = context.getConfig().getTargetLanguages();

            try {
                FileUtils.ensureDirectory(audioDir);

                for (String lang : languages) {
                    String filename = lang + "_dub_synthetic.aac";
                    Path outputFile = audioDir.resolve(filename);

                    ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                            "ffmpeg", "-y",
                            "-i", inputFile.toString(),
                            "-vn",
                            "-af", "asetrate=44100*1.02,aresample=44100",
                            "-c:a", "aac",
                            "-b:a", "128k",
                            outputFile.toString()
                    }, 120);

                    if (result.isSuccess()) {
                        context.addAsset("audio/" + filename);
                        System.out.printf("    Synthetic dub: %s%n", filename);
                    } else {
                        ShellExecutor.Result fallback = ShellExecutor.execute(new String[]{
                                "ffmpeg", "-y",
                                "-i", inputFile.toString(),
                                "-vn",
                                "-c:a", "aac",
                                "-b:a", "128k",
                                outputFile.toString()
                        }, 120);

                        if (fallback.isSuccess()) {
                            context.addAsset("audio/" + filename);
                            System.out.printf("    Audio extracted (fallback): %s%n", filename);
                        } else {
                            System.out.printf("    Warning: audio extraction failed for %s%n", lang);
                        }
                    }
                }
            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "AI dubbing failed: " + e.getMessage(), e);
            }
        }
    }
}
