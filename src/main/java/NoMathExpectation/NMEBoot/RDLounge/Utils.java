package NoMathExpectation.NMEBoot.RDLounge;

import NoMathExpectation.NMEBoot.Main;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

public class Utils {
    public static final String FFMPEG = "plugin-libraries/ffmpeg/bin/ffmpeg.exe";

    public static Process newFfmpegProcess(String args) throws IOException {
        Main.INSTANCE.getLogger().info("ffmpeg " + args);
        return Runtime.getRuntime().exec(FFMPEG + " " + args);
    }

    @NotNull
    public static String writeStreamToString(InputStream input, int bufferSize) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (input; Reader errorStreamReader = new InputStreamReader(input)) {
            char[] chars = new char[bufferSize];
            while (true) {
                int count = errorStreamReader.read(chars);
                if (count == -1) {
                    break;
                }
                sb.append(chars, 0, count);
            }
        }

        return sb.toString();
    }

    @NotNull
    public static String writeStreamToString(InputStream input) throws IOException {
        return writeStreamToString(input, 1024);
    }

    @NotNull
    @Contract("_, _ -> new")
    public static File audioAndVideoConvert(@NotNull File f, @NotNull String type) throws IOException {
        String path = f.getPath();
        if (path.endsWith(".")) {
            path += type;
        } else {
            String[] split = path.split("\\.");
            split[split.length - 1] = type;
            StringJoiner sj = new StringJoiner(".");
            for (String s : split) {
                sj.add(s);
            }
            path = sj.toString();
        }

        File after = new File(path);
        after.delete();
        after.deleteOnExit();

        Process p = newFfmpegProcess("-i \"" + f.getPath() + "\" \"" + path + "\"");
        p.getOutputStream().write("y\n".getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().flush();
        try {
            p.waitFor();
        } catch (InterruptedException ignored) {
        }
        
        String[] message = writeStreamToString(p.getErrorStream()).split("\n");
        String errorMessage = message[message.length - 1];
        if (!after.isFile()) {
            throw new RuntimeException(errorMessage);
        }

        return new File(path);
    }
}
