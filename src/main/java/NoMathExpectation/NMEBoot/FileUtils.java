package NoMathExpectation.NMEBoot;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.file.AbsoluteFile;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class FileUtils {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final HttpClient HTTP_CLIENT_WITH_PROXY = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 10809))).build();
    public static final Set<String> BLACK_LIST = Set.of("server-sent-events.deno.dev");
    public static final MiraiLogger LOGGER = Main.INSTANCE.getLogger();

    @NotNull
    public static File download(@NotNull String url) throws URISyntaxException, IOException, InterruptedException {
        String[] paths = url.split("[/\\\\]");
        return download(url, "data/NoMathExpectation.NMEBoot/downloads/" + paths[paths.length - 1].replaceAll("\\?.*", ""));
    }

    @NotNull
    public static File download(@NotNull String url, @NotNull String saveFile) throws URISyntaxException, IOException, InterruptedException {
        LOGGER.info("开始下载：" + url);
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

        InputStream input0;
        try {
            input0 = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
        } catch (HttpConnectTimeoutException e) {
            input0 = HTTP_CLIENT_WITH_PROXY.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
        }

        File file;
        try (InputStream input = input0) {
            file = new File(saveFile);
            file.deleteOnExit();
            if (file.isFile() && !file.delete()) {
                throw new RuntimeException("无法删除文件：" + file);
            }
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
                throw new RuntimeException("无法创建到文件：" + file + " 的路径");
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

    @Nullable
    public static File download(AbsoluteFile file) throws IOException, InterruptedException {
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            return download(file.getUrl(), "data/NoMathExpectation.NMEBoot/downloads/" + file.getName());
        } catch (URISyntaxException ignored) {
        }

        return null;
    }

    @Nullable
    public static MessageReceipt<Group> shareFileToAnotherGroup(@NotNull AbsoluteFile file, @NotNull Group to, boolean receiptRequired) throws IOException, URISyntaxException, InterruptedException {
        if (!file.exists()) {
            throw new NoSuchElementException("找不到引用文件");
        }

        File f = FileUtils.download(file.getUrl(), "data/NoMathExpectation.NMEBoot/groupshare/" + file.getName());
        f.deleteOnExit();

        AtomicReference<MessageReceipt<Group>> result = new AtomicReference<>();
        if (receiptRequired) {
            GlobalEventChannel.INSTANCE.subscribe(GroupMessagePostSendEvent.class, e -> {
                if (e.getTarget().getId() != to.getId() || !e.getMessage().serializeToMiraiCode().startsWith("[mirai:file:" + file.getName())) {
                    return ListeningStatus.LISTENING;
                }

                result.set(e.getReceipt());
                Main.INSTANCE.getLogger().info("上传文件引用监听结束");
                return ListeningStatus.STOPPED;
            });
            Main.INSTANCE.getLogger().info("上传文件引用监听开始");
        }
        uploadFile(to, f);
        if (!receiptRequired) {
            return null;
        }
        while (result.get() == null) {
            Thread.onSpinWait();
        }

        return result.get();
    }

    @NotNull
    public static AbsoluteFile uploadFile(@NotNull Group group, @NotNull File file, @NotNull String path) throws IOException {
        LOGGER.info("正在上传文件 " + file.getName() + " 至 " + group.getName() + " (id: " + group.getId() + ")");
        try (ExternalResource er = ExternalResource.create(file)) {
            return group.getFiles().uploadNewFile(path, er);
        }
    }

    @NotNull
    public static AbsoluteFile uploadFile(@NotNull Group group, @NotNull File file) throws IOException {
        return uploadFile(group, file, "/" + file.getName());
    }

    @Nullable
    public static AbsoluteFile getQuotedAbsoluteFile(@NotNull Group group, @NotNull MessageChain message) {
        if (!message.contains(QuoteReply.Key)) {
            return null;
        }
        String replyString = message.get(QuoteReply.Key).getSource().getOriginalMessage().contentToString();
        if (!replyString.startsWith("[文件]")) {
            return null;
        }
        return group.getFiles().getRoot().filesStream().filter(f -> f.getName().startsWith(replyString.substring(4))).findFirst().orElse(null);
    }
}
