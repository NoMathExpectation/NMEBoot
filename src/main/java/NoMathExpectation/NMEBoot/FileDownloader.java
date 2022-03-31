package NoMathExpectation.NMEBoot;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

public class FileDownloader {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    public static final Set<String> BLACK_LIST = Set.of("server-sent-events.deno.dev");

    @NotNull
    public static File download(@NotNull String url) throws URISyntaxException, IOException, InterruptedException {
        String[] paths = url.split("[/\\\\]");
        return download(url, "data/NoMathExpectation.NMEBoot/downloads/" + paths[paths.length - 1].replaceAll("\\?.*", ""));
    }

    @NotNull
    public static File download(@NotNull String url, @NotNull String saveFile) throws URISyntaxException, IOException, InterruptedException {
        Main.INSTANCE.getLogger().info("开始下载：" + url);
        for (String s : BLACK_LIST) {
            if (url.contains(s)) {
                throw new IllegalArgumentException("418 I'm a teapot");
            }
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            Main.INSTANCE.getLogger().error("未知的url：" + url);
            throw e;
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "Java HttpClient")
                .header("Accept", "*/*")
                .timeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_2)
                .build();

        File file;
        try (InputStream input = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
            file = new File(saveFile);
            if (file.isFile() && !file.delete()) {
                throw new RuntimeException("无法删除文件：" + file);
            }
            if (!file.createNewFile()) {
                throw new RuntimeException("无法创建文件：" + file);
            }
            try (OutputStream out = new FileOutputStream(file)) {
                byte[] bytes = new byte[1024];
                while (true) {
                    int count = input.read(bytes);
                    if (count == -1) {
                        break;
                    }
                    out.write(bytes, 0, count);
                }
            }
        } catch (Exception e) {
            Main.INSTANCE.getLogger().error("保存文件：" + saveFile + "失败");
            throw e;
        }
        Main.INSTANCE.getLogger().info("下载结束");
        return file;
    }
}
