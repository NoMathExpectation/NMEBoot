package NoMathExpectation.NMEBoot;

import NoMathExpectation.NMEBoot.utils.MessageHistoryKt;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.file.AbsoluteFile;
import net.mamoe.mirai.contact.file.AbsoluteFileFolder;
import net.mamoe.mirai.contact.file.AbsoluteFolder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.FileMessage;
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
import java.util.List;
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
        return download(url, "data/NoMathExpectation.NMEBoot/downloads/" + getFileNameByUrl(url));
    }

    @NotNull
    public static String getFileNameByUrl(@NotNull String url) {
        String[] paths = url.split("[/\\\\]");
        return paths[paths.length - 1].replaceAll("\\?.*", "");
    }

    @NotNull
    public static InputStream getDownloadStream(@NotNull String url) throws IOException, InterruptedException, URISyntaxException {
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

        try {
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
        } catch (HttpConnectTimeoutException e) {
            return HTTP_CLIENT_WITH_PROXY.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
        }
    }

    @NotNull
    public static File download(@NotNull String url, @NotNull String saveFile) throws URISyntaxException, IOException, InterruptedException {
        LOGGER.info("开始下载：" + url);

        File file;
        try (InputStream input = getDownloadStream(url)) {
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

        AtomicReference<MessageReceipt<Group>> result = new AtomicReference<>();
        if (receiptRequired) {
            GlobalEventChannel.INSTANCE.parentScope(Main.INSTANCE).subscribe(GroupMessagePostSendEvent.class, e -> {
                if (e.getTarget().getId() != to.getId() || !e.getMessage().serializeToMiraiCode().startsWith("[mirai:file:" + file.getName())) {
                    return ListeningStatus.LISTENING;
                }

                result.set(e.getReceipt());
                Main.INSTANCE.getLogger().info("上传文件引用监听结束");
                return ListeningStatus.STOPPED;
            });
            Main.INSTANCE.getLogger().info("上传文件引用监听开始");
        }
        uploadFile(to, file.getUrl());
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

    @NotNull
    public static AbsoluteFile uploadFile(@NotNull Group group, @NotNull InputStream inputStream, @NotNull String path) throws IOException {
        LOGGER.info("正在通过流上传文件至 " + group.getName() + " (id: " + group.getId() + ")");
        try (ExternalResource er = ExternalResource.create(inputStream)) {
            return group.getFiles().uploadNewFile(path, er);
        }
    }

    @NotNull
    public static AbsoluteFile uploadFile(@NotNull Group group, @NotNull String url, @NotNull String path) throws IOException, URISyntaxException, InterruptedException {
        return uploadFile(group, getDownloadStream(url), path);
    }

    @NotNull
    public static AbsoluteFile uploadFile(@NotNull Group group, @NotNull String url) throws IOException, URISyntaxException, InterruptedException {
        return uploadFile(group, url, "/" + getFileNameByUrl(url));
    }

    @Nullable
    public static AbsoluteFile getQuotedAbsoluteFile(@NotNull Group group, @NotNull MessageChain message) {
        if (!message.contains(QuoteReply.Key)) {
            return null;
        }
//        String replyString = message.get(QuoteReply.Key).getSource().getOriginalMessage().contentToString();
//        if (!replyString.startsWith("[文件]")) {
//            return null;
//        }

        QuoteReply quoted = message.get(QuoteReply.Key);
        if (quoted == null) {
            return null;
        }
        MessageChain roaming = MessageHistoryKt.roaming(quoted.getSource(), group);
        if (roaming == null) {
            return null;
        }
        FileMessage fileMessage = roaming.get(FileMessage.Key);
        if (fileMessage == null) {
            return null;
        }
        return fileMessage.toAbsoluteFile(group);

//        ArrayList<AbsoluteFile> files = new ArrayList<>();
//        addFileOrFolderToFileList(files, group.getFiles().getRoot());
//
//
//        return files.stream().filter(f -> f.getName().startsWith(replyString.substring(4))).findFirst().orElse(null);
    }

    private static void addFileOrFolderToFileList(@NotNull List<AbsoluteFile> files, @NotNull AbsoluteFileFolder ff) {
        if (ff instanceof AbsoluteFolder) {
            ((AbsoluteFolder) ff).childrenStream().forEach(x -> addFileOrFolderToFileList(files, x));
            return;
        }
        files.add((AbsoluteFile) ff);
    }
}
