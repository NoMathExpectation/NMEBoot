package NoMathExpectation.NMEBoot.commandSystem;

import NoMathExpectation.NMEBoot.FileUtils;
import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.RDLounge.Mahjong;
import NoMathExpectation.NMEBoot.RDLounge.Utils;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.*;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import static NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter.atParse;
import static NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter.isAdminOrBot;

public final class RDLounge implements Executable {
    public static final long GROUP_ID = 951070053L;
    static final long NYAN_MILK_SUPPLIER = 916813153L;
    static final long UD2 = 2546915249L;
    static final long OFFSET_NYAN = 1909482450L;

    private static final Random RANDOM = ExecuteCenter.INSTANCE.RANDOM;
    public static final AuctionCenter AUCTION_CENTER = new AuctionCenter();

    @Override
    @NotNull
    public MessageChainBuilder appendHelp(@NotNull MessageChainBuilder mcb, @NotNull MessageEvent e) {
        if (e.getSubject().getId() == GROUP_ID) {
            mcb = mcb
                    .append("RDL特供:\n")
                    .append("//rdhelp :编辑器帮助\n")
                    .append("//chart <n> <song> :快速下载匹配歌名的第n个谱子，如未找到则下载第一个谱子（暂时停用）\n")
                    .append("//convert <type> :将音视频文件转换成指定类型\n")
                    .append("//c 或 //card ://card help\n")
                    .append("//samurai :Samurai.\n")
                    .append("//majsoul :向听数计算，例子：//majsoul m123 p456 s789\n")
                    .append("//brainpower\n")
                    .append("//heyall\n\n");
        }
        return mcb;
    }

    private boolean autoRDNurse(@NotNull MessageEvent e) {
        if (e.getSender().getId() != UD2 && e.getMessage().contains(FileMessage.Key) && (e.getMessage().get(FileMessage.Key).getName().endsWith(".rdzip") || e.getMessage().get(FileMessage.Key).getName().endsWith(".rdlevel"))) {
            new Thread(() ->
            {
                try {
                    Thread.sleep((RANDOM.nextInt(10) + 1) * 1000);
                } catch
                (InterruptedException ex) {
                    ex.printStackTrace();
                }
                e.getSubject().sendMessage(new MessageChainBuilder()
                        .append(new QuoteReply(e.getSource()))
                        .append("!~rdnurse")
                        .build());
            }).start();
            return true;
        }
        return false;
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

    private boolean tiredPaige(Contact from) {
        int work = RANDOM.nextInt(1024);
        Main.INSTANCE.getLogger().info("现在是" + LocalDateTime.now().getHour() + "时" + LocalDateTime.now().getMinute() + "分，工作值：" + work);
        if ((LocalDateTime.now().getHour() <= 5 || LocalDateTime.now().getHour() >= 18) && work == 511) {
            from.sendMessage("我六点就该下班了。");
            Main.INSTANCE.getLogger().info("我六点就该下班了。");
            return true;
        }
        return false;
    }

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

        /*if (atOtto(e)) {
            return true;
        }*/

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
        LocalDate date = LocalDate.now();
        if(date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1) {
            from0 = AprilFool.INSTANCE.getModifiedContact(from0);
        }
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

        if (!(msg.equals("//samurai") || msg.startsWith("//samurai ")) && ExecuteCenter.INSTANCE.saySamurai(false)) {
            return true;
        }

        if(atOffsetNyan(e, msg, from)) {
            return true;
        }

        if (tiredPaige(from)) {
            return true;
        }

        String[] cmd = msg.substring(2).split("\\s+");
        String[] miraiCmd = miraiMsg.substring(2).split("\\s+");
        if (cmd.length < 1) {
            return false;
        }
        switch (cmd[0]) {
            case "rdhelp":
                from.sendMessage("编辑器帮助网址：https://rd.rdlevel.cn");
                break;
            case "brainpower":
                from.sendMessage("\uD835\uDCDE-\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8 \uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD4-\uD835\uDCD0-\uD835\uDCD0-\uD835\uDCD8-\uD835\uDCD0-\uD835\uDCE4- \uD835\uDCD9\uD835\uDCDE-\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8 \uD835\uDCD0\uD835\uDCD0\uD835\uDCD4-\uD835\uDCDE-\uD835\uDCD0-\uD835\uDCD0-\uD835\uDCE4-\uD835\uDCE4-\uD835\uDCD0- \uD835\uDCD4-\uD835\uDC86\uD835\uDC86\uD835\uDC86-\uD835\uDC86\uD835\uDC86-\uD835\uDC86\uD835\uDC86\uD835\uDC86 \uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD4-\uD835\uDCD0-\uD835\uDCD4-\uD835\uDCD8-\uD835\uDCD4-\uD835\uDCD0-\uD835\uDCD9\uD835\uDCDE-\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8-\uD835\uDCF8\uD835\uDCF8-\uD835\uDCF8\uD835\uDCF8-\uD835\uDCF8\uD835\uDCF8 \uD835\uDCD4\uD835\uDCD4\uD835\uDCD4\uD835\uDCD4\uD835\uDCDE-\uD835\uDCD0-\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0-\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0");
                break;
            case "heyall":
                from.sendMessage("Hey all, Scott here.");
                break;
            case "samurai":
                ExecuteCenter.INSTANCE.samurai();
                if (!ExecuteCenter.INSTANCE.saySamurai(false)) {
                    from.sendMessage("武士离开了咖啡店。");
                }
                break;
            case "majsoul":
                if(cmd.length < 2) {
                    from.sendMessage("请附加参数");
                    break;
                }
                try {
                    from.sendMessage(Mahjong.parse(msg.replaceFirst("//majsoul\\s+", "")));
                } catch (Exception ex) {
                    from.sendMessage(ex.getMessage());
                    ex.printStackTrace();
                }
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
            case "c":
            case "card":
                if (cmd.length < 2) {
                    from.sendMessage("使用//card help来显示抽卡帮助");
                    break;
                }
                CardUser cardUser = CardUser.getUsers().get(user.id);
                if (cardUser == null) {
                    cardUser = new CardUser(user.id, user.name);
                } else {
                    cardUser.name = user.name;
                }
                switch (cmd[1]) {
                    case "help":
                        from.sendMessage(new MessageChainBuilder()
                                        .append("抽卡帮助：\n")
                                        .append("//c|card...\n")
                                        .append("help : 显示此帮助\n")
                                        .append("stat/stats : 统计数据\n")
                                        .append("p/pull : 常驻抽卡\n")
                                        .append("level : 关卡抽卡池（-2硬币）\n")
                                        .append("pray/claim : 领取工资\n")
                                        .append("inv/inventory : 查看物品栏\n")
                                        .append("show <name/id> : 搜索或展示物品\n")
                                        .append("use <name/id> : 使用物品\n")
                                        .append("auc/auction : 拍卖物品\n")
                                        .append("throw <name/id> : 扔出物品，若匹配多个则随机扔出一个，使用*代表所有物品\n")
                                        .append("catch : 接住物品\n")
                                        .append("b/box [in/out] : 往盒子里随机放进（+1硬币）/抽出（-2硬币）一张卡，同时执行免费\n")
                                        .build())
                                .recallIn(60000);
                        break;
                    case "stat":
                    case "stats":
                        if (cmd.length < 3) {
                            from.sendMessage(new MessageChainBuilder()
                                            .append("//c stat/stats ...\n")
                                            .append("general : 主要的统计数据\n")
                                            .append("coin/coins : 硬币排行榜\n")
                                            .append("card/cards : 全收集排行榜")
                                            .build())
                                    .recallIn(60000);
                            break;
                        }
                        switch (cmd[2]) {
                            case "general":
                                from.sendMessage(Stats.getGeneral()).recallIn(30000);
                                break;
                            case "coin":
                            case "coins":
                                from.sendMessage(Stats.getCoins()).recallIn(30000);
                                break;
                            case "card":
                            case "cards":
                                from.sendMessage(Stats.getCardsCollection()).recallIn(30000);
                                break;
                            default:
                                from.sendMessage("无效的项目");
                        }
                        break;
                    case "p":
                    case "pull":
                        Card card = cardUser.pull(CardPool.getPools().get("general"));
                        if (card.equals(Card.EMPTY)) {
                            from.sendMessage(card.description);
                        } else {
                            from.sendMessage(new MessageChainBuilder()
                                    .append(new At(cardUser.id))
                                    .append("抽到了一张")
                                    .append(card.showSimple(false))
                                    .append("!\n")
                                    .append(card.getImage(from))
                                    .build());
                        }
                        break;
                    case "level":
                        int levelToken = 2;
                        if (!cardUser.pay(levelToken)) {
                            from.sendMessage("你的硬币不足。");
                            break;
                        }
                        card = cardUser.pull(CardPool.getPools().get("levels"));
                        if (card.equals(Card.EMPTY)) {
                            from.sendMessage(card.description);
                            cardUser.giveToken(levelToken);
                        } else {
                            from.sendMessage(new MessageChainBuilder()
                                    .append(new At(cardUser.id))
                                    .append("抽到了一张")
                                    .append(card.showSimple(false))
                                    .append("！\n")
                                    .append(card.getImage(from))
                                    .build());
                        }
                        break;
                    case "pray":
                    case "claim":
                        Duration d = cardUser.pray();
                        if (d.equals(Duration.ZERO)) {
                            from.sendMessage("你领取了在中海医院的工资：1枚硬币");
                        } else {
                            from.sendMessage("请再等待" + d.toDays() + "天" + d.toHoursPart() + "时" + d.toMinutesPart() + "分" + d.toSecondsPart() + "秒");
                        }
                        break;
                    case "inv":
                    case "inventory":
                        from.sendMessage(cardUser.getInventoryMessageChain()).recallIn(60000);
                        break;
                    case "b":
                    case "box":
                        if (cmd.length < 3) {
                            int tokenInAndOut = 0;
                            if (!cardUser.pay(tokenInAndOut)) {
                                from.sendMessage("你的硬币不足。");
                                break;
                            }
                            MessageChain chain = cardUser.useBox(from);
                            if (chain.contentToString().equals("你没有任何卡片") || chain.contentToString().equals("交换箱没有任何卡片")) {
                                cardUser.giveToken(tokenInAndOut);
                            }
                            from.sendMessage(chain);
                            break;
                        }
                        switch (cmd[2]) {
                            case "in":
                                int tokenIn = 1;
                                String suffix = "";
                                MessageChain chain = cardUser.boxIn();
                                if (!chain.contentToString().equals("你没有任何卡片")) {
                                    cardUser.giveToken(tokenIn);
                                    suffix = "，获得了" + tokenIn + "枚硬币";
                                }
                                from.sendMessage(chain.plus(suffix));
                                break;
                            case "out":
                                int tokenOut = 2;
                                if (!cardUser.pay(tokenOut)) {
                                    from.sendMessage("你的硬币不足。");
                                    break;
                                }
                                chain = cardUser.boxOut(from);
                                if (chain.contentToString().equals("交换箱里没有任何卡片")) {
                                    cardUser.giveToken(tokenOut);
                                }
                                from.sendMessage(chain);
                                break;
                            case "get":
                                if (isAdminOrBot(e)) {
                                    from.sendMessage(CardPool.getPools().get("box").getCards().toString());
                                    break;
                                }
                            default:
                                from.sendMessage("未知参数。可用参数：in/out/不填");
                        }
                        break;
                    case "throw":
                        if (cmd.length < 3) {
                            from.sendMessage("请输入你要扔出的物品的名字或id");
                            break;
                        }

                        if (cmd[2].toLowerCase(Locale.ROOT).equals("exception")) {
                            throw new RuntimeException("由//c throw人为触发");
                        }

                        Set<Long> ids = atParse(miraiCmd[2]);
                        if (!ids.isEmpty()) {
                            for (long id : ids) {
                                from.sendMessage(new MessageChainBuilder()
                                        .append(new At(id))
                                        .append("被扔出了咖啡店！")
                                        .build());
                            }
                            break;
                        }

                        boolean userThrown = false;
                        for (CardUser u : CardUser.getUsers().values()) {
                            if (u.name.equals(cmd[2])) {
                                from.sendMessage(new MessageChainBuilder()
                                        .append(new At(u.id))
                                        .append("被扔出了咖啡店！")
                                        .build());
                                userThrown = true;
                                break;
                            }
                        }
                        if (userThrown) {
                            break;
                        }

                        Item item = cardUser.searchAndDiscardItem(msg.replaceFirst("//(c|card)\\s+throw\\s+", ""));
                        if (item.equals(Item.EMPTY)) {
                            from.sendMessage("找不到物品");
                            break;
                        }
                        from.sendMessage(new MessageChainBuilder()
                                .append(new At(cardUser.id))
                                .append("扔出了一个")
                                .append("物品")
                                //.append(item.showSimple(false))
                                .append("！")
                                .build());
                        if (RANDOM.nextInt(10) == 3) {
                            from.sendMessage("武士恰好路过，扔出去的物品被他接到了...");
                            ExecuteCenter.INSTANCE.saySamurai(true);
                        } else {
                            ItemPool.getPools().get("ground").add(item);
                        }
                        while (!cardUser.getInventory().isEmpty() && RANDOM.nextInt(10) == 7) {
                            item = cardUser.searchAndDiscardItem("*");
                            from.sendMessage(new MessageChainBuilder()
                                    .append(new At(cardUser.id))
                                    .append("不小心又连带扔出了一个物品！")
                                    .build());
                            if (RANDOM.nextInt(10) == 3) {
                                from.sendMessage("武士恰好路过，扔出去的物品被他接到了...");
                                ExecuteCenter.INSTANCE.saySamurai(true);
                            } else {
                                ItemPool.getPools().get("ground").add(item);
                            }
                        }
                        break;
                    case "catch":
                        item = ItemPool.getPools().get("ground").pull();
                        if (item.equals(Card.EMPTY)) {
                            from.sendMessage("地板上空空如也...");
                            break;
                        }
                        if (item instanceof Card) {
                            from.sendMessage(new MessageChainBuilder()
                                    .append(new At(cardUser.id))
                                    .append("捡到了一张")
                                    .append(item.showSimple(false))
                                    .append("！\n")
                                    .append(((Card) item).getImage(from))
                                    .build());
                        } else {
                            from.sendMessage(new MessageChainBuilder()
                                    .append(new At(cardUser.id))
                                    .append("捡到了一个")
                                    .append(item.showSimple(false))
                                    .append("！")
                                    .build());
                        }
                        cardUser.addItem(item);
                        break;
                    case "show":
                        if (cmd.length < 3) {
                            from.sendMessage("请输入你要展示的物品的名字或id");
                            break;
                        }
                        List<Item> searchItems = cardUser.searchItem(msg.replaceFirst("//(c|card)\\s+show\\s+", ""));
                        if (searchItems.isEmpty()) {
                            from.sendMessage("未找到物品");
                            break;
                        }
                        if (CardUser.uniqueItem(searchItems).size() == 1) {
                            MessageChainBuilder mcb = new MessageChainBuilder().append(searchItems.get(0).show());
                            if (searchItems.get(0) instanceof Card) {
                                mcb.append(((Card) searchItems.get(0)).getImage(from));
                            }
                            from.sendMessage(mcb.build()).recallIn(30000);
                            break;
                        }
                        from.sendMessage(new MessageChainBuilder()
                                .append("找到以下物品：\n")
                                .append(CardUser.getInventoryMessageChain(searchItems))
                                .build()).recallIn(30000);
                        break;
                    case "use":
                        if (cmd.length < 3) {
                            from.sendMessage("请输入你要使用的物品的名字或id");
                            break;
                        }
                        if (!cardUser.useItem(msg.replaceFirst("//(c|card)\\s+use\\s+", ""))) {
                            from.sendMessage("没有找到对应物品，或此物品目前不可使用");
                        }
                        break;
                    case "auc":
                    case "auction":
                        if (cmd.length < 3) {
                            from.sendMessage(new MessageChainBuilder()
                                    .append("//c auction/auc ...\n")
                                    .append("new <初始价格> <物品名字或id> : 发起拍卖（收取最终价格50%的手续费）\n")
                                    .append("bid <价格> : 竞标物品\n")
                                    .append("stop : 停止自己当前的拍卖")
                                    .build()).recallIn(30000);
                            break;
                        }
                        switch (cmd[2]) {
                            case "new":
                                int cost;
                                try {
                                    cost = Integer.decode(cmd[3]);
                                    if (cost < 0) {
                                        throw new IllegalArgumentException("必须为非负整数");
                                    }
                                } catch (IndexOutOfBoundsException | IllegalArgumentException exception) {
                                    from.sendMessage("请输入一个非负整数！");
                                    break;
                                }
                                String id = msg.replaceFirst("//(c|card)\\s+(auc|auction)\\s+new\\s+\\d+\\s+", "");
                                if (id.isBlank()) {
                                    from.sendMessage("请输入物品名或id！");
                                    break;
                                }
                                from.sendMessage(AUCTION_CENTER.newAuction(cardUser, id, from, cost));
                                break;
                            case "bid":
                                try {
                                    cost = Integer.decode(cmd[3]);
                                    if (cost < 0) {
                                        throw new IllegalArgumentException("必须为非负整数");
                                    }
                                } catch (IndexOutOfBoundsException | IllegalArgumentException exception) {
                                    from.sendMessage("请输入一个非负整数！");
                                    break;
                                }
                                from.sendMessage(AUCTION_CENTER.bid(cardUser, cost));
                                break;
                            case "stop":
                                if (isAdminOrBot(e)) {
                                    from.sendMessage(AUCTION_CENTER.stopCurrentAuction());
                                } else {
                                    from.sendMessage(AUCTION_CENTER.stopCurrentAuction(cardUser));
                                }
                                break;
                            default:
                                from.sendMessage("未知的参数，可用参数有：new/bid/stop");
                        }
                        break;
                    case "reload":
                        if (isAdminOrBot(e)) {
                            CardPool.removeLoaded();
                            CardPool.LoadCards();
                            from.sendMessage("重载完成");
                            break;
                        }
                    default:
                        from.sendMessage("未知的参数，输入//c help以查看帮助");
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
