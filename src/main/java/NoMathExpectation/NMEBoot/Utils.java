package NoMathExpectation.NMEBoot;

import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Utils {
    public static final MiraiLogger LOGGER = Main.INSTANCE.getLogger();

    @NotNull
    public static String writeStreamToString(InputStream input, int bufferSize) throws IOException {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }

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
    public static String writeStreamToString(@NotNull InputStream input) throws IOException {
        return writeStreamToString(input, 1024);
    }

    public static void writeStreamToStream(@NotNull InputStream input, @NotNull OutputStream output, int bufferSize) throws IOException {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }

        try (input; output) {
            byte[] bytes = new byte[bufferSize];
            while (true) {
                int count = input.read(bytes);
                if (count == -1) {
                    break;
                }
                output.write(bytes, 0, count);
            }
        }
    }

    public static void writeStreamToStream(@NotNull InputStream input, @NotNull OutputStream output) throws IOException {
        writeStreamToStream(input, output, 1024);
    }

    //see https://github.com/sjx233/rd-nurse
    @NotNull
    public static Process newRdNurseProcess(String args, @NotNull File f) throws IOException {
        LOGGER.info("rd-nurse " + args + ", with " + f.getName());
        ProcessBuilder pb;
        if (args == null || args.isBlank()) {
            pb = new ProcessBuilder("deno", "run", "https://cdn.jsdelivr.net/gh/sjx233/rd-nurse@main/main.ts");
        } else {
            pb = new ProcessBuilder("deno", "run", "https://cdn.jsdelivr.net/gh/sjx233/rd-nurse@main/main.ts", args);
        }
        pb.redirectInput(ProcessBuilder.Redirect.from(f));

        return pb.start();
    }

    @NotNull
    public static String rdNurse(@NotNull File file, @NotNull String args) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            new File("data/NoMathExpectation.NMEBoot/rdlevel").mkdirs();
            StringBuilder sb = new StringBuilder();

            Set<? extends ZipEntry> levels = zip.stream().filter(x -> !x.isDirectory() && x.getName().endsWith(".rdlevel")).collect(Collectors.toSet());
            for (ZipEntry level: levels) {
                writeStreamToStream(zip.getInputStream(level), new FileOutputStream("data/NoMathExpectation.NMEBoot/rdlevel/" + level.getName()));
                sb.append(level.getName());
                sb.append(" :\n");
                try {
                    sb.append(rdNurse(new File("data/NoMathExpectation.NMEBoot/rdlevel/" + level.getName()), args));
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    sb.append(e.getMessage());
                }
            }

            return sb.toString();
        } catch (ZipException ignored) {}

        Process p = newRdNurseProcess(args, file);

        try {
            p.waitFor();
        } catch (InterruptedException ignored) {}

        String error = writeStreamToString(p.getErrorStream());
        if (!error.isBlank()) {
            throw new RuntimeException(error);
        }

        String result = writeStreamToString(p.getInputStream());
        if (result.isBlank()) {
            result = "未检测出错误\n";
        }

        return result;
    }

    public static final String FFMPEG = "plugin-libraries/ffmpeg/bin/ffmpeg.exe";

    @NotNull
    public static Process newFfmpegProcess(@NotNull String input, @NotNull String output) throws IOException {
        return Runtime.getRuntime().exec(new String[] {FFMPEG, "-xerror", "-nostdin", "-y", "-i", input, output});
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
        after.deleteOnExit();

        Process p = newFfmpegProcess(f.getPath(), path);
        try {
            p.waitFor();
        } catch (InterruptedException ignored) {
        }
        
        String[] message = writeStreamToString(p.getErrorStream()).split("\n");
        String errorMessage = message[message.length - 1];
        if (after.length() == 0) {
            throw new RuntimeException(errorMessage);
        }

        return new File(path);
    }

    //see https://github.com/ytdl-org/youtube-dl
    @NotNull
    public static File[] downloadYoutubeVideo(@NotNull String link) throws IOException {
        Process p = new ProcessBuilder("yt-dlp", "-q", "-o", "data/NoMathExpectation.NMEBoot/downloads/youtube/%(title)s [%(id)s].%(ext)s", link).start();
        try {
            p.waitFor();
        } catch (InterruptedException ignored) {}

        String error = writeStreamToString(p.getErrorStream());

        if (!error.isBlank()) {
            throw new RuntimeException(error);
        }

        return Objects.requireNonNull(new File("data/NoMathExpectation.NMEBoot/downloads/youtube/").listFiles());
    }
}
