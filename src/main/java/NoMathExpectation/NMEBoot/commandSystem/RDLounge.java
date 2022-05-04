package NoMathExpectation.NMEBoot.commandSystem;

import NoMathExpectation.NMEBoot.FileUtils;
import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.Utils;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.file.AbsoluteFile;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.NoSuchElementException;

public final class RDLounge implements Executable {
    public static final long GROUP_ID = 951070053L;
    static final long NYAN_MILK_SUPPLIER = 916813153L;
    static final long UD2 = 2546915249L;
    static final long OFFSET_NYAN = 1909482450L;

    @Override
    @NotNull
    public MessageChainBuilder appendHelp(@NotNull MessageChainBuilder mcb, @NotNull MessageEvent e) {
        if (e.getSubject().getId() == GROUP_ID) {
            mcb = mcb
                    .append("RDL:\n")
                    .append("//rdhelp :编辑器帮助\n")
                    .append("//chart <n> <song> :快速下载匹配歌名的第n个谱子，如未找到则下载第一个谱子（暂时停用）\n")
                    .append("//convert <type> :将音视频文件转换成指定类型\n")
                    .append("//nurse [-h|args] :检查谱面是否有错误(credits: ud2)\n");
        }
        return mcb;
    }

    private boolean atOtto(@NotNull MessageEvent e) {
        if (e.getMessage().contains(new At(2858194513L))) {
            if (ExecuteCenter.INSTANCE.saySamurai(false)) {
                return true;
            }
            e.getSubject().sendMessage(new MessageChainBuilder()
                    .append(new QuoteReply(e.getSource()))
                    .append("找凹兔干嘛？\n如果你想知道如何使用编辑器，这里是编辑器教程：https://rd.rdlevel.cn\n输入//help可查看所有指令帮助")
                    .build());
            return true;
        }
        return false;
    }

    private boolean atOffsetNyan(@NotNull MessageEvent e, @NotNull String msg, @NotNull Contact contact) {
        if (e.getSender().getId() == UD2 && msg.matches("BPM:\\s\\d+\\.\\d+\\s\\(\\d+\\)\\s*\n偏移:\\s\\d+ms\\s*")) {
            contact.sendMessage(new QuoteReply(e.getSource()).plus(new At(OFFSET_NYAN)));
            return true;
        }
        return false;
    }

    private String repeat;
    private int repeatCount = 0;

    private void repeat(@NotNull MessageEvent e, @NotNull String msg) {
        if (e.getSubject().getId() == GROUP_ID) {
            if (msg.equals(repeat)) {
                if (++repeatCount == 5) {
                    if (ExecuteCenter.INSTANCE.saySamurai(false)) {
                        return;
                    }
                    e.getSubject().sendMessage(MiraiCode.deserializeMiraiCode(repeat));
                }
            } else {
                repeat = msg;
                repeatCount = 1;
            }
        }
        Main.INSTANCE.getLogger().info("repeat:" + repeatCount);
    }

    private MessageReceipt<Contact> levelSearchMessage;
    private String chart;
    private int chartChosen;

    @NotNull
    private MessageReceipt<Contact> sendLevelSearchQuery(@NotNull Contact contact) {
        return contact.sendMessage(new MessageChainBuilder()
                .append("!~rdlevel search song \"")
                .append(MiraiCode.deserializeMiraiCode(chart.replace("\"", "\\\\\"")))
                .append("\" page ")
                .append(String.valueOf((chartChosen - 1) / 5 + 1))
                .build());
    }

    @NotNull
    private ListeningStatus levelSearchHandler(@NotNull MessageEvent e) {
        if (e.getSubject().getId() == GROUP_ID && e.getSender().getId() == UD2) {
            if (e.getMessage().size() < 3) {
                return ListeningStatus.LISTENING;
            }
            String result = e.getMessage().get(2).contentToString();
            Main.INSTANCE.getLogger().info("\n" + result);
            if (result.startsWith("网络")) {
                try {
                    levelSearchMessage.recall();
                } catch (Exception ignored) {}
                e.getSubject().sendMessage("请不要使用铁丝上网(");
                Main.INSTANCE.getLogger().info("网络请求错误，监听结束");
                return ListeningStatus.STOPPED;
            }
            if (result.startsWith("无结果")) {
                try {
                    levelSearchMessage.recall();
                } catch (Exception ignored) {}
                if (chartChosen > 5) {
                    Main.INSTANCE.getLogger().info("未在此页找到谱面，搜索第一页中...");
                    chartChosen = 1;
                    levelSearchMessage = sendLevelSearchQuery(e.getSubject());
                    return ListeningStatus.LISTENING;
                }
                e.getSubject().sendMessage("无结果");
                Main.INSTANCE.getLogger().info("未找到谱面，监听结束");
                return ListeningStatus.STOPPED;
            }
            if (result.startsWith("参数后应有空格分隔") || result.startsWith("引号内的字符串")) {
                try {
                    levelSearchMessage.recall();
                } catch (Exception ignored) {
                }
                e.getSubject().sendMessage("非法的歌曲名");
                Main.INSTANCE.getLogger().info("非法的歌曲名，监听结束");
                return ListeningStatus.STOPPED;
            }
            String uuid;
            try {
                uuid = result.split("\n")[(chartChosen - 1) % 5 * 2];
            } catch (Exception exception) {
                Main.INSTANCE.getLogger().info("此页无指定序号谱面，将使用此页的第一个谱面");
                uuid = result.split("\n")[0];
            }
            if (uuid.matches("[0-9a-zA-Z]{22}")) {
                try {
                    levelSearchMessage.recall();
                } catch (Exception ignored) {
                }
                e.getSubject().sendMessage("!~rdlevel download " + uuid);
                Main.INSTANCE.getLogger().info("已找到谱面，监听结束");
                return ListeningStatus.STOPPED;
            }
        }
        return ListeningStatus.LISTENING;
    }

    private AbsoluteFile fileToUd2;
    private MessageReceipt<Group> requestToUd2;

    @NotNull
    private ListeningStatus ud2Listener(@NotNull GroupMessageEvent e) {
        if (e.getSubject().getId() == NYAN_MILK_SUPPLIER && e.getSender().getId() == UD2) {
            Group group = e.getBot().getGroup(GROUP_ID);
            if (group == null) {
                Main.INSTANCE.getLogger().warning("传达ud2消息时未找到饭制部");
            } else {
                MessageChain msg = e.getMessage();
                if (!msg.contains(FileMessage.Key)) {
                    group.sendMessage(Alias.INSTANCE.alias(msg, GROUP_ID));
                } else {
                    AbsoluteFile file = msg.get(FileMessage.Key).toAbsoluteFile(e.getSubject());
                    try {
                        FileUtils.shareFileToAnotherGroup(file, group, false);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        group.sendMessage(ex.getMessage());
                    }
                }
            }

            try {
                requestToUd2.recall();
            } catch (Exception ignored) {}
            requestToUd2 = null;

            if (fileToUd2 != null && fileToUd2.exists()) {
                fileToUd2.delete();
            }
            fileToUd2 = null;

            Main.INSTANCE.getLogger().info("ud2监听结束");
            return ListeningStatus.STOPPED;
        }
        return ListeningStatus.LISTENING;
    }

    private void newUd2Request(@NotNull Message message, @NotNull Group from) throws Exception {
        Group targetGroup = Bot.getInstances().get(0).getGroup(NYAN_MILK_SUPPLIER);
        if (targetGroup == null) {
            throw new NoSuchElementException("找不到指定群");
        }
        if (!targetGroup.contains(UD2)) {
            throw new NoSuchElementException("ud2已退群");
        }
        if (message instanceof MessageChain && ((MessageChain) message).contains(QuoteReply.Key)) {
            String replyString = ((MessageChain) message).get(QuoteReply.Key).getSource().getOriginalMessage().contentToString();
            if (!replyString.startsWith("[文件]")) {
                return;
            }
            from.sendMessage("警告：目前不支持文件转发引用");
            /*Thread fileUploadThread = new Thread(() -> {
                fileToUd2 = from.getFiles().getRoot().filesStream().filter(f -> f.getName().equals(replyString.substring(4))).findFirst().get();
                try {
                    MessageReceipt<Group> fileUploadResult = FileDownloader.shareFileToAnotherGroup(fileToUd2, targetGroup, true);
                    QuoteReply ud2FileQuote = fileUploadResult.quote();
                    fileToUd2 = ud2FileQuote.getSource().getOriginalMessage().get(FileMessage.Key).toAbsoluteFile(targetGroup);
                    GlobalEventChannel.INSTANCE.subscribe(GroupMessageEvent.class, this::ud2Listener);
                    requestToUd2 = fileUploadResult.quoteReply(message);
                    Main.INSTANCE.getLogger().info("ud2监听开始");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    from.sendMessage(ex.getMessage());
                }
            });
            fileUploadThread.setDaemon(true);
            fileUploadThread.start();
            return;*/
        }

        requestToUd2 = targetGroup.sendMessage(message);
        GlobalEventChannel.INSTANCE.subscribe(GroupMessageEvent.class, this::ud2Listener);
        Main.INSTANCE.getLogger().info("ud2监听开始");
    }

    @Override
    public boolean onMessage(@NotNull MessageEvent e, @NotNull NormalUser user, @NotNull String command, @NotNull String miraiCommand) throws Exception {
        if (e.getSubject().getId() != GROUP_ID) {
            return false;
        }

        repeat(e, miraiCommand);

        /*if(autoRDNurse(e))
        {return true;}*/

        if (atOtto(e)) {
            return true;
        }

        String msg, miraiMsg;
        boolean hasQuote = e.getMessage().contains(QuoteReply.Key);
        if (hasQuote) {
            msg = command.replaceAll("\\[mirai:quote:\\[\\d*],\\[\\d*]]", "");
            miraiMsg = miraiCommand.replaceAll("\\[mirai:quote:\\[\\d*],\\[\\d*]]", "");
        } else {
            msg = command;
            miraiMsg = miraiCommand;
        }

        Contact from0 = e.getSubject();

        Contact from = Alias.INSTANCE.alias(from0);

        /*if (msg.startsWith("!~") || msg.equals("c!p")) {
            if (((Group) e.getSubject()).contains(UD2)) {
                return false;
            }
            newUd2Request(Alias.INSTANCE.alias(e.getMessage(), GROUP_ID), (Group) e.getSubject());
            return false;
        }*/

        if (!msg.startsWith("//")) {
            return false;
        }

        if(atOffsetNyan(e, msg, from)) {
            return true;
        }

        String[] cmd = msg.substring(2).split("\\s+");
        String[] miraiCmd = miraiMsg.substring(2).split("\\s+");
        if (cmd.length < 1) {
            return false;
        }
        switch (cmd[0]) {
            case "checkin":
            case "brainpower":
            case "heyall":
            case "majsoul":
            case "c":
            case "card":
                e.getSender().sendMessage("应群规要求，请移步群916813153使用指令");
                break;
            case "rdhelp":
                from.sendMessage("编辑器帮助网址：https://rd.rdlevel.cn");
                break;
            case "chart":
                from.sendMessage("此指令已暂停使用");
                break;
                /*if (cmd.length < 3) {
                    from.sendMessage("参数过少。");
                    break;
                }
                try {
                    chartChosen = Integer.decode(cmd[1]);
                    if (chartChosen <= 0) {
                        throw new IllegalArgumentException("必须为正数");
                    }
                } catch (Exception exception) {
                    from.sendMessage("非法谱面序号");
                    break;
                }
                chart = miraiMsg.replaceFirst("//chart(\\s+|(\\\\n)+)\\d*(\\s+|(\\\\n)+)", "");
                Main.INSTANCE.getLogger().info("开始搜索谱面:" + chart);
                Main.INSTANCE.getLogger().info("请求序号:" + cmd[1]);
                e.getBot().getEventChannel().subscribe(MessageEvent.class, this::levelSearchHandler);
                levelSearchMessage = sendLevelSearchQuery(e.getSubject());
                Main.INSTANCE.getLogger().info("回复监听开始");
                break;*/
            case "convert":
                if (!hasQuote) {
                    from.sendMessage("未找到引用消息");
                    break;
                }

                Thread t = new Thread(() -> {
                    try {
                        Main.INSTANCE.getLogger().info("文件转换开始");
                        from.sendMessage("开始转换");
                        String type;
                        try {
                            type = cmd[1];
                        } catch (IndexOutOfBoundsException ex) {
                            type = "ogg";
                        }
                        Group group = (Group) e.getSubject();

                        File before = FileUtils.download(FileUtils.getQuotedAbsoluteFile(group, e.getMessage()));

                        if (before == null) {
                            throw new NoSuchElementException("未找到引用文件");
                        }

                        Main.INSTANCE.getLogger().info("文件名：" + before.getName() + "，转换类型：" + type);

                        File after = Utils.audioAndVideoConvert(before, type);
                        FileUtils.uploadFile(group, after);

                        Main.INSTANCE.getLogger().info("文件转换结束");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        from.sendMessage(ex.getMessage());
                    }
                });
                t.setDaemon(true);
                t.start();
                break;
            case "nurse":
                if (!hasQuote) {
                    from.sendMessage("未找到引用消息");
                    break;
                }

                t = new Thread(() -> {
                    try {
                        Main.INSTANCE.getLogger().info("rdnurse开始");
                        from.sendMessage("开始检查");
                        String args;
                        try {
                            args = cmd[1];
                        } catch (IndexOutOfBoundsException ex) {
                            args = "";
                        }
                        Group group = (Group) e.getSubject();

                        File file = FileUtils.download(FileUtils.getQuotedAbsoluteFile(group, e.getMessage()));

                        if (file == null) {
                            throw new NoSuchElementException("未找到引用文件");
                        }

                        Main.INSTANCE.getLogger().info("文件名：" + file.getName());

                        from.sendMessage(Utils.rdNurse(file, args));

                        Main.INSTANCE.getLogger().info("rdnurse结束");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        from.sendMessage(ex.getMessage());
                    }
                });
                t.setDaemon(true);
                t.start();
                break;
            default:
                return false;
        }
        return true;
    }
}
