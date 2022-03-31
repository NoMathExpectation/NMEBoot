package NoMathExpectation.NMEBoot;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mahjong {

    protected static Process newProcess() throws IOException {
        return Runtime.getRuntime().exec("py plugins/majsoul.py");
    }

    @NotNull
    public static String shanten(@NotNull String man, @NotNull String pin, @NotNull String sou) throws IOException {
        Process p = newProcess();
        try (OutputStream input = p.getOutputStream()) {
            input.write(("m,p,s=('" + man + "','" + pin + "','" + sou + "')\n").getBytes(StandardCharsets.UTF_8));
            input.flush();
        }
        try (InputStreamReader output = new InputStreamReader(p.getInputStream())) {
            List<Character> charList = new ArrayList<>();
            while (true) {
                int i = output.read();
                if (i == -1) {
                    break;
                }
                charList.add((char) i);
            }
            char[] chars = new char[charList.size()];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = charList.get(i);
            }
            return String.valueOf(chars);
        }
    }

    @NotNull
    public static String parse(@NotNull String argLine) throws IOException, IllegalArgumentException {
        String man = "";
        String pin = "";
        String sou = "";

        String[] args = argLine.split("\\s+|\n+");
        Main.INSTANCE.getLogger().info(Arrays.toString(args));
        for (String arg : args) {
            if (arg.startsWith("man") || arg.startsWith("m")) {
                man = arg.replaceAll("(man)|m", "");
            }
            else if (arg.startsWith("pin") || arg.startsWith("p")) {
                man = arg.replaceAll("(pin)|p", "");
            }
            else if (arg.startsWith("sou") || arg.startsWith("s")) {
                man = arg.replaceAll("(sou)|s", "");
            }
            else {
                throw new IllegalArgumentException("无效的参数");
            }
        }

        return shanten(man, pin, sou);
    }

}
