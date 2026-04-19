package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShellExecutor {


    public static class Result {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public Result(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }


        public String getStdout() {
            return stdout.trim();
        }

        public String getStderr() {
            return stderr.trim();
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }


    public static Result execute(String[] command, int timeoutSeconds) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Thread stdoutThread = new Thread(() -> {
            try { stdoutBuilder.append(readStream(process.getInputStream())); }
            catch (IOException ignored) {}
        });

        Thread stderrThread = new Thread(() -> {
            try { stderrBuilder.append(readStream(process.getErrorStream())); }
            catch (IOException ignored) {}
        });

        stdoutThread.start();
        stderrThread.start();

        try {
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new Result(-1, stdoutBuilder.toString(),
                        "Process timed out after " + timeoutSeconds + "s");
            }
            stdoutThread.join(5000);
            stderrThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new Result(-1, stdoutBuilder.toString(), "Process interrupted");
        }

        return new Result(process.exitValue(), stdoutBuilder.toString(), stderrBuilder.toString());
    }


    public static Result execute(String[] command) throws IOException {
        return execute(command, 300);
    }

    private static String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
