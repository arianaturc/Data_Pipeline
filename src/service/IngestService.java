package service;

import orchestrator.PipelineContext;
import orchestrator.PipelineException;
import orchestrator.PipelineStage;
import util.FileUtils;
import util.ShellExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IngestService {
    public static class IntegrityCheck implements PipelineStage {

        @Override
        public String getName() {
            return "Integrity Check";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();

            if (!Files.exists(inputFile)) {
                throw new PipelineException(getName(),
                        "Input file not found: " + inputFile);
            }

            try {

                String checksum = FileUtils.computeSHA256(inputFile);
                context.setFileChecksum(checksum);

                long size = Files.size(inputFile);
                System.out.printf("File: %s (%s)%n", inputFile.getFileName(),
                        FileUtils.readableSize(size));
                System.out.printf("SHA-256: %s%n", checksum);

                String expected = context.getConfig().getExpectedChecksum();
                if (expected != null && !expected.equalsIgnoreCase(checksum)) {
                    throw new PipelineException(getName(),
                            "Checksum mismatch! Expected: " + expected + ", Got: " + checksum);
                }
            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Failed to read input file: " + e.getMessage(), e);
            }
        }
    }


    public static class FormatValidator implements PipelineStage {

        @Override
        public String getName() {
            return "Format Validator";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path inputFile = context.getConfig().getInputFile();

            try {

                ShellExecutor.Result result = ShellExecutor.execute(new String[]{
                        "ffprobe",
                        "-v", "error",
                        "-select_streams", "v:0",
                        "-show_entries", "stream=width,height,codec_name,duration",
                        "-show_entries", "format=duration",
                        "-of", "default=noprint_wrappers=1",
                        inputFile.toString()
                });

                if (!result.isSuccess()) {
                    throw new PipelineException(getName(),
                            "ffprobe failed: " + result.getStderr());
                }


                String output = result.getStdout();
                context.setVideoWidth(parseIntField(output, "width", 1920));
                context.setVideoHeight(parseIntField(output, "height", 1080));
                context.setVideoCodec(parseStringField(output, "codec_name", "unknown"));
                context.setVideoDuration(parseDoubleField(output, "duration", 0.0));

                System.out.printf("Resolution: %dx%d%n",
                        context.getVideoWidth(), context.getVideoHeight());
                System.out.printf("Codec: %s%n", context.getVideoCodec());
                System.out.printf("Duration: %.1fs%n", context.getVideoDuration());

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Failed to run ffprobe: " + e.getMessage(), e);
            }
        }

        private int parseIntField(String output, String field, int defaultVal) {
            for (String line : output.split("\n")) {
                if (line.startsWith(field + "=")) {
                    try {
                        return Integer.parseInt(line.split("=", 2)[1].trim());
                    } catch (NumberFormatException e) {
                        return defaultVal;
                    }
                }
            }
            return defaultVal;
        }

        private double parseDoubleField(String output, String field, double defaultVal) {
            for (String line : output.split("\n")) {
                if (line.startsWith(field + "=")) {
                    try {
                        return Double.parseDouble(line.split("=", 2)[1].trim());
                    } catch (NumberFormatException e) {
                        return defaultVal;
                    }
                }
            }
            return defaultVal;
        }

        private String parseStringField(String output, String field, String defaultVal) {
            for (String line : output.split("\n")) {
                if (line.startsWith(field + "=")) {
                    String val = line.split("=", 2)[1].trim();
                    return val.isEmpty() ? defaultVal : val;
                }
            }
            return defaultVal;
        }
    }
}
