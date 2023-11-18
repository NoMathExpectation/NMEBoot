package NoMathExpectation.NMEBoot.commandSystem.services;

import NoMathExpectation.NMEBoot.FileUtils;
import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.RDLounge.Mahjong;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.*;
import NoMathExpectation.NMEBoot.RDLounge.rhythmCafe.RhythmCafeSearchEngine;
import NoMathExpectation.NMEBoot.Utils;
import NoMathExpectation.NMEBoot.commandSystem.*;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;
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

public final class RDLoungeIntegrated implements Executable {
    public static final long RDLOUNGE = 951070053L;
    public static final long NYAN_MILK = 916813153L;
    static final long UD2 = 2546915249L;
    static final long OFFSET_NYAN = 1909482450L;

    public static final Set<Long> USING_GROUP = Set.of(/*951070053L, */916813153L, 884249803L);
    public static final Set<Long> FULL_FUNCTION_GROUP = Set.of(916813153L, 884249803L);

    public static boolean isUsingGroup(long id) {
        return USING_GROUP.contains(id);
    }

    public static boolean isFullFunctionGroup(long id) {
        return FULL_FUNCTION_GROUP.contains(id);
    }

    private static boolean testNotFullFunctionGroup(@NotNull Contact contact) {
        if (!isFullFunctionGroup(contact.getId())) {
            replyNotFullFunctionGroup(contact);
            return true;
        }
        return false;
    }

    private static void replyNotFullFunctionGroup(@NotNull Contact contact) {
        contact.sendMessage("请在水群使用此指令");
    }

    private static final Random RANDOM = new Random();
    public static final AuctionCenter AUCTION_CENTER = new AuctionCenter();

    @Override
    @NotNull
    public MessageChainBuilder appendHelp(@NotNull MessageChainBuilder mcb, @NotNull MessageEvent e) {
        long id = e.getSubject().getId();
        if (isUsingGroup(id)) {
            if(isFullFunctionGroup(id)) {
                mcb.append("真·");
            }
            mcb.append("RDL特供:\n")
                    .append("//chart :搜索谱面 （已转移）\n");
            if(isFullFunctionGroup(id)) {
                mcb.append("//nurse [-h|args] :检查谱面是否有错误(credits: ud2)\n")
                        .append("//rdhelp :编辑器帮助\n")
                        .append("//convert <type> :将音视频文件转换成指定类型\n")
                        .append("//c 或 //card ://card help （已部分转移）\n")
                        .append("//samurai :Samurai.\n")
                        .append("//majsoul :向听数计算，例子：//majsoul m123 p456 s789 （将移除）\n")
                        .append("//brainpower （将移除）\n")
                        .append("//heyall （将移除）\n\n");
            }
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

    @Override
    public boolean onMessage(@NotNull MessageEvent e, @NotNull NormalUser user, @NotNull String command, @NotNull String miraiCommand) throws Exception {
        if (!isUsingGroup(e.getSubject().getId())) {
            return false;
        }

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

        if (isFullFunctionGroup(from.getId()) && !(msg.equals("//samurai") || msg.startsWith("//samurai ")) && ExecuteCenter.INSTANCE.saySamurai(false)) {
            return true;
        }

        /*if(atOffsetNyan(e, msg, from)) {
            return true;
        }*/

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
                if (testNotFullFunctionGroup(from)) {
                    break;
                }
                from.sendMessage("编辑器帮助网址：https://rd.rdlevel.cn");
                break;

            case "brainpower":
                if (testNotFullFunctionGroup(from)) {
                    break;
                }
                from.sendMessage("\uD835\uDCDE-\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8 \uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD4-\uD835\uDCD0-\uD835\uDCD0-\uD835\uDCD8-\uD835\uDCD0-\uD835\uDCE4- \uD835\uDCD9\uD835\uDCDE-\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8 \uD835\uDCD0\uD835\uDCD0\uD835\uDCD4-\uD835\uDCDE-\uD835\uDCD0-\uD835\uDCD0-\uD835\uDCE4-\uD835\uDCE4-\uD835\uDCD0- \uD835\uDCD4-\uD835\uDC86\uD835\uDC86\uD835\uDC86-\uD835\uDC86\uD835\uDC86-\uD835\uDC86\uD835\uDC86\uD835\uDC86 \uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD4-\uD835\uDCD0-\uD835\uDCD4-\uD835\uDCD8-\uD835\uDCD4-\uD835\uDCD0-\uD835\uDCD9\uD835\uDCDE-\uD835\uDCF8\uD835\uDCF8\uD835\uDCF8-\uD835\uDCF8\uD835\uDCF8-\uD835\uDCF8\uD835\uDCF8-\uD835\uDCF8\uD835\uDCF8 \uD835\uDCD4\uD835\uDCD4\uD835\uDCD4\uD835\uDCD4\uD835\uDCDE-\uD835\uDCD0-\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0-\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0\uD835\uDCD0");
                break;

            case "heyall":
                if (testNotFullFunctionGroup(from)) {
                    break;
                }
                from.sendMessage("Hey all, Scott here.");
                break;

            case "samurai":
                if (testNotFullFunctionGroup(from)) {
                    break;
                }
                ExecuteCenter.INSTANCE.samurai();
                if (!ExecuteCenter.INSTANCE.saySamurai(false)) {
                    from.sendMessage("武士离开了咖啡店。");
                }
                break;

            case "majsoul":
                if (testNotFullFunctionGroup(from)) {
                    break;
                }
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
                if (cmd.length < 2) {
                    from.sendMessage(RhythmCafeSearchEngine.INSTANCE.sendHelp()).recallIn(30000);
                    break;
                }

                switch (cmd[1]) {
                    case "help":
                        from.sendMessage(RhythmCafeSearchEngine.INSTANCE.sendHelp()).recallIn(30000);
                        break;

                    case "search":
                        from.sendMessage(RhythmCafeSearchEngine.INSTANCE.search(msg.replaceFirst("//chart[\\s\n]search[\\s\n]", ""), 5)).recallIn(60000);
                        break;

                    case "page":
                        if (!RhythmCafeSearchEngine.INSTANCE.isSearched()) {
                            from.sendMessage("请先进行一次搜索");
                            break;
                        }
                        try {
                            int page = Integer.decode(cmd[2]);
                            from.sendMessage(RhythmCafeSearchEngine.INSTANCE.pageTo(page)).recallIn(60000);
                        } catch (IllegalArgumentException ex) {
                            from.sendMessage("请输入一个非负整数！");
                        }
                        break;

                    case "info":
                        if (!RhythmCafeSearchEngine.INSTANCE.isSearched()) {
                            from.sendMessage("请先进行一次搜索");
                            break;
                        }
                        try {
                            int index = Integer.decode(cmd[2]);
                            from.sendMessage(RhythmCafeSearchEngine.INSTANCE.getDescriptionJavaWithContact(index, from)).recallIn(60000);
                        } catch (IllegalArgumentException ex) {
                            from.sendMessage("请输入一个非负整数！");
                        } catch (IndexOutOfBoundsException ex) {
                            from.sendMessage("未找到对应编号的谱面");
                        }
                        break;

                    case "link":
                    case "link2":
                        if (!RhythmCafeSearchEngine.INSTANCE.isSearched()) {
                            from.sendMessage("请先进行一次搜索");
                            break;
                        }
                        try {
                            int index = Integer.decode(cmd[2]);
                            String link;
                            if (cmd[1].equals("link")) {
                                link = RhythmCafeSearchEngine.INSTANCE.getLink(index);
                            } else {
                                link = RhythmCafeSearchEngine.INSTANCE.getLink2(index);
                            }
                            from.sendMessage(link).recallIn(60000);
                        } catch (IllegalArgumentException ex) {
                            from.sendMessage("请输入一个非负整数！");
                        } catch (IndexOutOfBoundsException ex) {
                            from.sendMessage("未找到对应编号的谱面");
                        }
                        break;

                    case "download":
                        if (!RhythmCafeSearchEngine.INSTANCE.isSearched()) {
                            from.sendMessage("请先进行一次搜索");
                            break;
                        }
                        try {
                            int index = Integer.decode(cmd[2]);
                            from.sendMessage("开始下载");
                            RhythmCafeSearchEngine.INSTANCE.downloadAndUpload((Group) e.getSubject(), index);
                        } catch (IllegalArgumentException ex) {
                            from.sendMessage("请输入一个非负整数！");
                        } catch (IndexOutOfBoundsException ex) {
                            from.sendMessage("未找到对应编号的谱面");
                        }
                        break;

                    default:
                        from.sendMessage("未知的指令，输入//chart help以获得帮助");
                }
                break;

            case "convert":
                if (testNotFullFunctionGroup(from)) {
                    break;
                }

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
                if (testNotFullFunctionGroup(from)) {
                    break;
                }

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

                        from.sendMessage(Utils.rdNurse(file, args)).recallIn(60000);

                        Main.INSTANCE.getLogger().info("rdnurse结束");
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
                from.sendMessage("搬迁中，请使用/card代替本指令");
                if (true) {
                    break;
                }

                if (testNotFullFunctionGroup(from)) {
                    break;
                }

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
