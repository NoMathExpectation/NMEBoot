package NoMathExpectation.NMEBoot.commandSystem;

import NoMathExpectation.NMEBoot.FileDownloader;
import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardUser;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.Item;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.ItemLibrary;
import kotlin.Pair;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageSyncEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter.*;

public final class General implements Executable {
    @Override
    @NotNull
    public MessageChainBuilder appendHelp(@NotNull MessageChainBuilder mcb, @NotNull MessageEvent e) {
        return mcb
                .append("通用:\n")
                .append("//hello :发送 \"Hello, world!\"\n")
                .append("//help :显示所有指令\n")
                .append("//repeat <text> :复读机\n")
                .append("//luck :测测你今天的运气\n")
                .append("//alias :别称\n")
                .append("//stat|stats :统计数据\n")
                .append("//checkin :签到\n")
                .append("//download <url> :下载文件\n")
                .append("//wordle|w :wordle\n")
                //.append("//moral <text>:AI预测是否道德（仅供参考，不作为任何依据）\n")
                .append("//feedback <text> :给作者反馈\n")
                .append("//114514 [count]\n")
                .append("//1919810 [count]\n\n");
    }

    @Override
    public boolean onMessage(@NotNull MessageEvent e, @NotNull NormalUser user, @NotNull String command, @NotNull String miraiCommand) throws Exception {
        String msg, miraiMsg;
        boolean hasQuote = e.getMessage().contains(QuoteReply.Key);
        if (hasQuote) {
            msg = command.replaceAll("\\[mirai:quote:\\[\\d*],\\[\\d*]]", "");
            miraiMsg = miraiCommand.replaceAll("\\[mirai:quote:\\[\\d*],\\[\\d*]]", "");
        } else {
            msg = command;
            miraiMsg = miraiCommand;
        }
        if (!msg.startsWith("//")) {
            return false;
        }

        Contact from0 = e.getSubject();
        LocalDate date = LocalDate.now();
        if(date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1) {
            from0 = AprilFool.INSTANCE.getModifiedContact(from0);
        }
        Contact from = Alias.INSTANCE.alias(from0);

        if (from.getId() == RDLounge.GROUP_ID && ExecuteCenter.INSTANCE.getSamurai()) {
            return false;
        }
        String[] cmd = msg.substring(2).split("\\s+");
        String[] miraiCmd = miraiMsg.substring(2).split("\\s+");
        if (cmd.length < 1) {
            return false;
        }
        switch (cmd[0]) {
            case "hello":
                from.sendMessage("Hello, world!");
                break;
            case "what?":
                from.sendMessage("\u0014");
                break;
            case "obfuscate":
                AprilFool.INSTANCE.getModifiedContact(from).sendMessage(new PlainText(".").plus(MiraiCode.deserializeMiraiCode(miraiMsg)));
                break;
            case "alias":
                if (!(e instanceof GroupMessageEvent || e instanceof GroupMessageSyncEvent)) {
                    from.sendMessage("此指令不支持私聊使用");
                    break;
                }
                if (cmd.length < 2) {
                    from.sendMessage(Alias.INSTANCE.sendHelp()).recallIn(30000);
                    break;
                }
                switch (cmd[1]) {
                    case "help":
                        from.sendMessage(Alias.INSTANCE.sendHelp()).recallIn(30000);
                        break;
                    case "show":
                        String s = Alias.INSTANCE.toString(from.getId());
                        if (s.isBlank()) {
                            from.sendMessage("没有正在使用的别称").recallIn(60000);
                            break;
                        }
                        e.getSubject().sendMessage(s).recallIn(60000);
                        break;
                    case "add":
                        Pattern p = Pattern.compile("(?<!(?<!\\\\)\\\\)'");
                        Matcher m = p.matcher(msg);
                        List<Integer> pos = new ArrayList<>();
                        while(m.find()) {
                            pos.add(m.start());
                        }
                        if (pos.size() != 4) {
                            from.sendMessage("错误的输入格式");
                            break;
                        }

                        String patternFrom = msg.substring(pos.get(0) + 1, pos.get(1)).replace("\\\\", "\\").replace("\\'", "'");
                        String patternTo = msg.substring(pos.get(2) + 1, pos.get(3)).replace("\\\\", "\\").replace("\\'", "'");
                        try {
                            Pattern.compile(patternFrom);
                        } catch (PatternSyntaxException ex) {
                            from.sendMessage("错误的regex格式");
                            break;
                        }

                        Matcher posMatcher = (Pattern.compile("\\d+").matcher(msg));
                        if (posMatcher.find(pos.get(3))) {
                            Alias.INSTANCE.add(from.getId(), new Pair<>(patternFrom, patternTo), Integer.parseInt(msg.substring(posMatcher.start(), posMatcher.end())));
                        } else {
                        Alias.INSTANCE.add(from.getId(), new Pair<>(patternFrom, patternTo));
                        }
                        e.getSubject().sendMessage("已保存: " + patternFrom + " -> " + patternTo);
                        break;
                    case "remove":
                        try {
                            Pair<String, String> pair = Alias.INSTANCE.remove(from.getId(), Integer.decode(cmd[2]), isAdminOrBot(e));
                            e.getSubject().sendMessage("已移除: " + pair.getFirst() + " -> " + pair.getSecond());
                        } catch (NumberFormatException ex) {
                            from.sendMessage("请输入一个非负整数!");
                        } catch (IndexOutOfBoundsException ex) {
                            from.sendMessage("未找到对应别称");
                        } catch (IllegalStateException ex) {
                            from.sendMessage("你没有权限移除此别称");
                        }
                        break;
                    case "move":
                        try {
                            Alias.INSTANCE.move(from.getId(), Integer.decode(cmd[2]), Integer.decode(cmd[3]));
                            from.sendMessage("操作成功");
                        } catch (NumberFormatException ex) {
                            from.sendMessage("请输入非负整数!");
                        } catch (IndexOutOfBoundsException ex) {
                            from.sendMessage("未找到对应别称");
                        }
                        break;
                    case "protect":
                        if (!isAdminOrBot(e)) {
                            from.sendMessage("你没有权限执行此指令");
                        }
                        try {
                            if (Alias.INSTANCE.protect(from.getId(), Integer.decode(cmd[2]))) {
                                from.sendMessage("已保护此别称");
                            } else {
                                from.sendMessage("已取消此别称的保护");
                            }
                        } catch (NumberFormatException ex) {
                          from.sendMessage("请输入非负整数!");
                        } catch (IndexOutOfBoundsException ex) {
                            from.sendMessage("未找到别称");
                        }
                        break;
                    default:
                        from.sendMessage("未知的参数，输入//alias help以查看帮助");
                }
                break;
            case "execute":
                if (!isAdminOrBot(e)) {
                    from.sendMessage("你没有权限使用此指令");
                    break;
                }

                if (cmd.length < 3) {
                    from.sendMessage("参数不足");
                    break;
                }

                Set<Long> users = atParse(cmd[1]);
                for (long l: users) {
                    NormalUser u = NormalUser.getUsers().get(l);
                    if (u == null) {
                        continue;
                    }
                    INSTANCE.execute(e, u, msg.replaceFirst("//execute\\s+\\S+\\s+", ""), miraiMsg.replaceFirst("//execute\\s+\\S+\\s+", ""));
                }

                break;
            case "repeat":
            case "repeat!":
                if (cmd[0].equals("repeat") && hasQuote) {
                    from.sendMessage(new MessageChainBuilder()
                            .append(new QuoteReply(e.getSource()))
                            .append(Objects.requireNonNull(e.getMessage().get(QuoteReply.Key)).getSource().getOriginalMessage())
                            .build());
                    break;
                }
                if (cmd.length < 2) {
                    from.sendMessage("参数过少。");
                    break;
                }
                from.sendMessage(new MessageChainBuilder()
                        .append(new QuoteReply(e.getSource()))
                        .append(MiraiCode.deserializeMiraiCode(miraiMsg.replaceFirst("//repeat[!]?(\\s+|(\\\\n)+)", "")))
                        .build());
                break;
            case "luck":
            case "辣氪":
                from.sendMessage(new MessageChainBuilder()
                        .append(new QuoteReply(e.getSource()))
                        .append("你今天的运气是：")
                        .append(String.valueOf(user.getLuck()))
                        .build());
                break;
            case "stats":
            case "stat":
                if (cmd.length < 2) {
                    from.sendMessage(NormalUserStats.INSTANCE.getStatList()).recallIn(30000);
                    break;
                }
                from.sendMessage(NormalUserStats.INSTANCE.getStat(cmd[1], from.getId())).recallIn(60000);
                break;
            /*case "faq":
                if (!(from instanceof Group)) {
                    from.sendMessage("只有群内才能使用此指令");
                    break;
                }
                if (cmd.length < 2) {
                    FAQ.INSTANCE.getHelp(from, isAdminOrBot(e)).recallIn(30000);
                }
                switch (cmd[1]) {
                    case "help":
                        FAQ.INSTANCE.getHelp(from, isAdminOrBot(e)).recallIn(30000);
                        break;
                    case "new":
                        if (!isAdminOrBot(e)) {
                            from.sendMessage("只有管理员才可以录制faq");
                            break;
                        }
                        try {
                            FAQ.INSTANCE.start(from.getId(), user.id);
                        } catch (Exception ex) {
                            from.sendMessage(ex.getMessage());
                            break;
                        }
                        from.sendMessage("开始录制");
                        break;
                    case "discard":
                        if (!isAdminOrBot(e)) {
                            from.sendMessage("只有管理员才可以丢弃faq");
                            break;
                        }
                        try {
                            FAQ.INSTANCE.discard(from.getId());
                        } catch (Exception ex) {
                            from.sendMessage(ex.getMessage());
                            break;
                        }
                        from.sendMessage("已丢弃");
                        break;
                    case "save":
                        if (cmd.length < 4) {
                            from.sendMessage("请输入保存名字和描述！");
                            break;
                        }
                        try {
                            FAQ.INSTANCE.save(from.getId(), cmd[2], msg.replaceFirst("//faq\\s+save\\s+\\S+\\s+", ""));
                        } catch (Exception ex) {
                            from.sendMessage(ex.getMessage());
                            break;
                        }
                        from.sendMessage("已保存");
                        break;
                    case "remove":
                        if (cmd.length < 3) {
                            from.sendMessage("请输入要移除的名字！");
                            break;
                        }
                        try {
                            FAQ.INSTANCE.remove(from.getId(), cmd[2]);
                        } catch (Exception ex) {
                            from.sendMessage(ex.getMessage());
                            break;
                        }
                        from.sendMessage("已移除");
                        break;
                    default:
                        FAQ.INSTANCE.send((Group) from, cmd[1]);
                }
                break;*/
            case "checkin":
                if (user.isCheckedIn()) {
                    from.sendMessage("你今天已经签到过了");
                    break;
                }
                if (cmd.length < 2) {
                    from.sendMessage(Main.naptcha.getCaptcha(from));
                    break;
                }
                if (Main.naptcha.checkAnswer(msg.replaceFirst("//checkin\\s+", ""), from)) {
                    user.checkIn();
                    from.sendMessage("签到成功！你是第" + user.getCheckInRank() + "个签到的。\n你已经连续签到了" + user.getCheckInStreak() + "天");
                    if (user.getCheckInRank() <= 10000 && from.getId() == RDLounge.GROUP_ID) {
                        CardUser cardUser = CardUser.getUsers().get(user.id);
                        if (cardUser == null) {
                            cardUser = new CardUser(user.id, user.name);
                        } else {
                            cardUser.name = user.name;
                        }
                        Item item = ItemLibrary.LIBRARY.pull();
                        cardUser.addItem(item, true);
                        from.sendMessage(new MessageChainBuilder()
                                .append(new At(cardUser.id))
                                .append("获得了一个")
                                .append(item.showSimple(false))
                                .append("！\n")
                                .build());
                    }
                    break;
                }
                from.sendMessage("验证码错误");
                break;
            case "wordle":
            case "w":
                if (cmd.length < 2) {
                   Main.wordle.sendHelp(from).recallIn(30000);
                   break;
                }
                switch (cmd[1]) {
                    case "help":
                        Main.wordle.sendHelp(from).recallIn(30000);
                        break;
                    case "new":
                        from.sendMessage(Main.wordle.parseAndNewWordle(from.getId(), cmd)).recallIn(60000);
                        break;
                    case "show":
                        from.sendMessage(Main.wordle.getWordleMessage(from.getId(), false)).recallIn(60000);
                        break;
                    default:
                        from.sendMessage(Main.wordle.validateAnswer(from.getId(), cmd[1])).recallIn(60000);
                }
                break;
            case "moral":
                if (cmd.length < 2) {
                    from.sendMessage("参数不足");
                    break;
                }
                new Thread(() ->
                {
                    try {
                        HttpRequest request = HttpRequest.newBuilder(new URI("https://delphi.allenai.org/?a1=" + msg.replaceFirst("//moral\\s+", "")))
                                .header("User-Agent", "Java HttpClient").header("Accept", "*/*")
                                .timeout(Duration.ofSeconds(10))
                                .version(HttpClient.Version.HTTP_2).build();
                        HttpResponse<String> response = ExecuteCenter.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                        Main.INSTANCE.getLogger().info(response.body());
                        Matcher answerMatcher = Pattern.compile("<span class=\"sc-carFqZ devysi\">.*?</span>").matcher(response.body());
                        answerMatcher.find();
                        String answer = response.body().substring(answerMatcher.start(), answerMatcher.end()).replace("<span class=\"sc-carFqZ devysi\">", "").replace("</span>", "");
                        from.sendMessage(new MessageChainBuilder()
                                .append(new QuoteReply(e.getSource()))
                                .append("Delphi speculates:\n\n")
                                .append(answer)
                                .append("\n\n以上结果仅供参考，不作为任何依据。")
                                .build());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        from.sendMessage("网络错误");
                    }
                }).start();
                break;
            case "warn":
                if (!isAdminOrBot(e)) {
                    from.sendMessage("你没有权限这么做");
                    break;
                }
                if (cmd.length < 2) {
                    from.sendMessage("参数过少。");
                    break;
                }
                users = ExecuteCenter.atParse(miraiCmd[1]);
                if (users.isEmpty()) {
                    from.sendMessage("未找到成员。");
                    break;
                }
                MessageChainBuilder mcb = new MessageChainBuilder();
                Set<Long> bannedUsers = new HashSet<>();
                for (long userid : users) {
                    NormalUser selectedUser = NormalUser.getUsers().get(userid);
                    selectedUser.warn();
                    if (selectedUser.isBanned()) {
                        bannedUsers.add(userid);
                    }
                    mcb.append(new At(userid))
                            .append("被警告了！这是TA的第")
                            .append(String.valueOf(selectedUser.getWarns()))
                            .append("次警告。\n");
                }
                from.sendMessage(mcb.build());
                if (bannedUsers.isEmpty()) {
                    break;
                }
                mcb = new MessageChainBuilder();
                for (long userid : bannedUsers) {
                    mcb.append(new At(userid))
                            .append(" ");
                }
                mcb.append("警告次数达到了上限，被自动封禁了！");
                from.sendMessage(mcb.build());
                break;
            case "ban":
                if (!isAdminOrBot(e)) {
                    from.sendMessage("你没有权限这么做");
                    break;
                }
                if (cmd.length < 2) {
                    from.sendMessage("参数过少。");
                    break;
                }
                users = atParse(cmd[1]);
                if (users.isEmpty()) {
                    from.sendMessage("未找到成员。");
                    break;
                }
                Duration banDuration;
                try {
                    banDuration = Duration.parse(cmd[2]);
                } catch (Exception ex) {
                    banDuration = NormalUser.autoBanTime;
                }
                String banReason = cmd.length < 4 ? "none" : cmd[3];
                mcb = new MessageChainBuilder();
                for (long userid : users) {
                    NormalUser selectedUser = NormalUser.getUsers().get(userid);
                    selectedUser.ban(banDuration, banReason);
                    mcb.append(new At(userid))
                            .append(" ");
                }
                mcb.append(" was samuraied in ")
                        .append(String.valueOf(banDuration.toDays()))
                        .append("d ")
                        .append(String.valueOf(banDuration.toHoursPart()))
                        .append("h ")
                        .append(String.valueOf(banDuration.toMinutesPart()))
                        .append("m ")
                        .append(String.valueOf(banDuration.toSecondsPart()))
                        .append("s for ")
                        .append(banReason)
                        .append(". ")
                        .append(INSTANCE.getSamuraiWord());
                from.sendMessage(mcb.build());
                break;
            case "download":
                if (cmd.length < 2) {
                    from.sendMessage("缺少地址。");
                    break;
                }
                from.sendMessage("开始下载...");
                new Thread(() ->
                {
                    try {
                        File download = FileDownloader.download(msg.replaceFirst("//download\\s+", ""));
                        try(ExternalResource er = ExternalResource.create(download)) {
                            if (e.getSubject() instanceof Group) {
                                ((Group) e.getSubject()).getFiles().uploadNewFile("/" + download.getName(), er);
                                return;
                            }
                            from.sendMessage("不支持私聊文件下载");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        from.sendMessage(ex.getMessage());
                    }
                }).start();
                break;
            case "feedback":
                if (cmd.length < 2) {
                    from.sendMessage("参数过少。");
                    break;
                }
                e.getBot().getAsFriend().sendMessage(new MessageChainBuilder()
                        .append("用户反馈：\n来自：\n")
                        .append(e.getSender().getNick())
                        .append("\n(id:")
                        .append(String.valueOf(user.id))
                        .append(")\n反馈内容：\n")
                        .append(MiraiCode.deserializeMiraiCode(miraiMsg.replaceFirst("//feedback(\\s+|(\\\\n)+)", "")))
                        .build());
                e.getSubject().sendMessage("反馈已发送。");
                break;
            case "114514":
            case "1919810":
                int count;
                try {
                    count = Integer.decode(cmd[1]);
                } catch (Exception ignored) {
                    count = ExecuteCenter.INSTANCE.RANDOM.nextInt(51);
                }
                if (count == 0) {
                    from.sendMessage("什么也没有发生");
                    break;
                }
                if (count < 0) {
                    from.sendMessage("earthOL.physics.ThermalException:沼气自发地回到了化粪池");
                    break;
                }
                if (count > 1000) {
                    from.sendMessage("程序被化粪池过浓的沼气熏死了");
                    break;
                }
                StringBuilder stinkBuilder = new StringBuilder();
                if (cmd[0].equals("1919810") && (e instanceof GroupMessageEvent && ((GroupMessageEvent) e).getGroup().contains(RDLounge.UD2)) || (e instanceof GroupMessageSyncEvent && ((GroupMessageSyncEvent) e).getGroup().contains(RDLounge.UD2))) {
                    stinkBuilder.append("!~say 哼，哼，哼，");
                } else {
                    stinkBuilder.append("哼，哼，哼，");
                }
                from.sendMessage(stinkBuilder.append("啊".repeat(count)).append("！").toString());
                break;
            case "serialize":
                if (hasQuote) {
                    from.sendMessage("转换为Mirai码后：" + Objects.requireNonNull(e.getMessage().get(QuoteReply.Key)).getSource().getOriginalMessage().serializeToMiraiCode());
                } else {
                    from.sendMessage("转换为Mirai码后：" + miraiMsg);
                }
                break;
            case "deserialize":
                from.sendMessage(new QuoteReply(e.getSource()).plus("反序列化后内容为："));
                if (hasQuote) {
                    from.sendMessage(MiraiCode.deserializeMiraiCode(Objects.requireNonNull(e.getMessage().get(QuoteReply.Key)).getSource().getOriginalMessage().contentToString()));
                } else {
                    from.sendMessage(MiraiCode.deserializeMiraiCode(msg.replaceFirst("//deserialize\\s+", "")));
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
