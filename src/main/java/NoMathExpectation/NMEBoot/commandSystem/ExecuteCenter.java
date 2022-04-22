package NoMathExpectation.NMEBoot.commandSystem;

import NoMathExpectation.NMEBoot.Main;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExecuteCenter extends SimpleListenerHost {
    public static ExecuteCenter INSTANCE;
    private Contact lastContact;
    private final HashSet<Executable> cmdSet;

    public final Random RANDOM = new Random();

    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private boolean samurai = false;
    private String lastSamuraiWord = "";

    public void loadSamurai() {
        samurai = Samurai.INSTANCE.getSamurai();
        lastSamuraiWord = Samurai.INSTANCE.getLastSamuraiWord();
        if (samurai) {
            Main.INSTANCE.getLogger().info("武士进入了咖啡店");
        }
    }

    public boolean samurai() {
        samurai = !samurai;
        Samurai.INSTANCE.setSamurai(samurai);
        if (samurai) {
            Main.INSTANCE.getLogger().info("武士进入了咖啡店");
        } else {
            Main.INSTANCE.getLogger().info("武士离开了咖啡店");
        }
        return samurai;
    }

    public boolean getSamurai() {
        return samurai;
    }

    public boolean saySamurai(boolean forced) {
        if (lastContact.getId() == RDLounge.GROUP_ID && samurai || forced) {
            lastContact.sendMessage(getSamuraiWord());
        }
        return samurai;
    }

    @NotNull
    public String getSamuraiWord() {
        String[] says = "Samurai. Donut. Great. Nice.".split(" ");
        lastSamuraiWord = says[RANDOM.nextInt(says.length)];
        Samurai.INSTANCE.setLastSamuraiWord(lastSamuraiWord);
        return lastSamuraiWord;
    }

    @NotNull
    public String getLastSamuraiWord() {
        return lastSamuraiWord;
    }

    @NotNull
    public static Set<Long> atParse(String atString) {
        Set<Long> ats = new HashSet<>();
        Matcher answerMatcher = Pattern.compile("(\\[mirai:at:\\d+])|(@\\d+)").matcher(atString);
        while (answerMatcher.find()) {
            String at = atString.substring(answerMatcher.start(), answerMatcher.end());
            Matcher digitMatcher = Pattern.compile("\\d+").matcher(at);
            digitMatcher.find();
            ats.add(Long.decode(at.substring(digitMatcher.start(), digitMatcher.end())));
        }
        return ats;
    }

    public static boolean isGroup(@NotNull MessageEvent e) {
        return e instanceof GroupMessageEvent || e instanceof GroupMessageSyncEvent;
    }

    public static boolean isAdminOrBot(@NotNull MessageEvent e) {
        if (e instanceof GroupMessageEvent) {
            return ((GroupMessageEvent) e).getPermission() != MemberPermission.MEMBER;
        }
        return e.getSender().getId() == e.getBot().getId();
    }

    public ExecuteCenter() {
        cmdSet = new HashSet<>();
        INSTANCE = this;
    }

    public Contact getLastContact() {
        return lastContact;
    }

    public ExecuteCenter register(Executable cmdSystem) {
        cmdSet.add(cmdSystem);
        return this;
    }

    public void sendHelp(MessageEvent e) {
        MessageChainBuilder mcb = new MessageChainBuilder();
        for (Executable s : cmdSet) {
            mcb = s.appendHelp(mcb, e);
        }
        lastContact.sendMessage(mcb.build()).recallIn(60000);
    }

    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable e) {
        e.printStackTrace();
        if (lastContact.getId() == RDLounge.GROUP_ID) {
            lastContact.sendMessage("武士走进了妮可的咖啡店，要了一份甜甜圈。");
            if (!samurai) {
                samurai();
                saySamurai(false);
            }
        } else if (UsingGroup.INSTANCE.getGroup().contains(lastContact.getId())) {
            lastContact.sendMessage("发生了一个错误，请查看控制台了解详情。");
        }
    }

    @EventHandler
    public synchronized void message(@NotNull MessageEvent e) throws Exception {
        LocalDateTime start = LocalDateTime.now();
        if (!UsingGroup.INSTANCE.getGroup().contains(e.getSubject().getId()) && e.getSubject().getId() != e.getBot().getId()) {
            return;
        }

        lastContact = e.getSubject();
        LocalDate date = LocalDate.now();
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1) {
            lastContact = AprilFool.INSTANCE.getModifiedContact(lastContact);
        }
        lastContact = Alias.INSTANCE.alias(e.getSubject());

        String msg = e.getMessage().contentToString();
        String miraiMsg = e.getMessage().serializeToMiraiCode();

        NormalUser user = NormalUser.getUsers().get(e.getSender().getId());
        if (user == null) {
            user = new NormalUser(e.getSender().getId(), e.getSenderName());
        } else {
            user.name = e.getSenderName();
        }

        if (msg.startsWith("//") && user.isBanned()) {
            Duration d = user.getBannedRemain();
            if (lastContact.getId() == RDLounge.GROUP_ID) {
                lastContact.sendMessage(user.name + " was samuraied in " + d.toDays() + "d " + d.toHoursPart() + "h " + d.toMinutesPart() + "m " + d.toSecondsPart() + "s for " + user.getBannedReason() + ". " + getSamuraiWord());
            }
            return;
        }

        if (!(msg.startsWith("//alias") || (e.getSubject().getId() == RDLounge.GROUP_ID && getSamurai() && msg.startsWith("//samurai")))) {
            msg = Alias.INSTANCE.alias(msg, lastContact.getId());
        }

        if (!(msg.startsWith("//alias") || (e.getSubject().getId() == RDLounge.GROUP_ID && getSamurai() && msg.startsWith("//samurai")))) {
            miraiMsg = Alias.INSTANCE.alias(miraiMsg, lastContact.getId());
        }

        if (!isAdminOrBot(e) && msg.startsWith("//") && (Block.INSTANCE.checkBlocked(e.getSubject().getId(), e.getSender().getId(), msg) || Block.INSTANCE.checkBlocked(e.getSubject().getId(), e.getSender().getId(), miraiMsg))) {
            lastContact.sendMessage("你没有权限使用此指令");
            return;
        }

        try {
            if (msg.split("\\s+|\n+")[0].equals("//help")) {
                if (samurai && lastContact.getId() == RDLounge.GROUP_ID) {
                    saySamurai(false);
                    return;
                }
                sendHelp(e);
                return;
            }
        } catch (Exception ignored) {}

        String[] cmds = msg.split("(?<!\\\\);[\\s\n]+");
        String[] miraiCmds = miraiMsg.split("(?<!\\\\\\\\);[\\s\\\\n]+");

        for (int i = 0; i < Math.min(cmds.length, miraiCmds.length); i++) {
            String cmd = cmds[i].replaceAll("\\\\;", ";");
            String miraiCmd = miraiCmds[i].replaceAll("\\\\\\\\;", ";");
            execute(e, user, cmd, miraiCmd);
        }

        LocalDateTime end = LocalDateTime.now();
        Main.INSTANCE.getLogger().info("执行时间：" + Duration.between(start, end).toMillis() + "ms");
    }

    public boolean execute(@NotNull MessageEvent e, @NotNull NormalUser user, @NotNull String cmd, @NotNull String miraiCmd) throws Exception {
        //Main.INSTANCE.getLogger().info("cmd: " + cmd);
        //Main.INSTANCE.getLogger().info("miraicmd: " + miraiCmd);

        for (Executable s : cmdSet) {
            if (s.onMessage(e, user, cmd, miraiCmd)) {
                return true;
            }
        }

        if (cmd.startsWith("//") && (UsingGroup.INSTANCE.getGroup().contains(e.getSubject().getId()) || e.getSender().getId() == e.getBot().getId())) {
            lastContact.sendMessage("未知的指令，输入//help以获得帮助。");
        }
        return false;
    }

    @EventHandler
    public void nudge(@NotNull NudgeEvent e) throws Exception {
        long to = e.getTarget().getId();
        if (to == e.getBot().getId() && to != e.getFrom().getId()) {
            e.getFrom().nudge().sendTo(e.getSubject());
        }
    }

    @EventHandler
    public void recall(@NotNull MessageRecallEvent e) throws Exception {
    }
}
