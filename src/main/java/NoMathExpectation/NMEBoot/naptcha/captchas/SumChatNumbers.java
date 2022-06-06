package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.commandSystem.UsingGroup;
import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.EventPriority;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent;
import net.mamoe.mirai.event.events.GroupMessageSyncEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SumChatNumbers extends SimpleListenerHost implements CaptchaGenerator {

    protected final Map<Long, List<Long>> chatNumberGroups = new HashMap<>();
    protected final int sumCount;

    public SumChatNumbers(int count) {
        sumCount = count;
        GlobalEventChannel.INSTANCE.parentScope(Main.INSTANCE).registerListenerHost(this);
    }

    protected synchronized void addNumbers(long id, @NotNull MessageChain m) {
        if (!UsingGroup.INSTANCE.getGroup().contains(id) || (Main.naptcha.getCurrentCaptcha(id) == this && !Main.naptcha.expired(id))) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (SingleMessage singleMessage : m) {
            if(singleMessage instanceof PlainText) {
                sb.append(((PlainText) singleMessage).getContent());
            }
        }
        String s = sb.toString();
        List<Long> numbers = chatNumberGroups.computeIfAbsent(id, k -> Collections.synchronizedList(new ArrayList<>()));
        Matcher answerMatcher = Pattern.compile("-?\\d+").matcher(s);
        while (answerMatcher.find()) {
            try {
                numbers.add(Long.valueOf(s.substring(answerMatcher.start(), answerMatcher.end())));
            } catch (NumberFormatException ignored) {}
        }
        while(numbers.size() > sumCount)
        {numbers.remove(0);}
        Main.INSTANCE.getLogger().info("" + id + ": " + numbers);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void numberGather(@NotNull GroupMessageEvent e) {
        addNumbers(e.getSubject().getId(), e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void numberGather(@NotNull GroupMessagePostSendEvent e) {
        addNumbers(e.getTarget().getId(), e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void numberGather(@NotNull GroupMessageSyncEvent e) {
        addNumbers(e.getSubject().getId(), e.getMessage());
    }


    @NotNull
    @Override
    public MessageChain generate(@NotNull Contact contact) {
        return get(contact);
    }

    @NotNull
    @Override
    public MessageChain get(@NotNull Contact contact) {
        List<Long> numbers = chatNumberGroups.computeIfAbsent(contact.getId(), k -> Collections.synchronizedList(new ArrayList<>()));
        return new MessageChainBuilder()
                .append("请将聊天中出现过的最后")
                .append(String.valueOf(Math.min(numbers.size(), sumCount)))
                .append("个纯文本（不包括@提及和此消息）阿拉伯整数（不包括上下标）加起来，并附到指令后面提交。\n（数字记录已暂停，直到验证通过或过期）")
                .build();
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        List<Long> numbers = chatNumberGroups.computeIfAbsent(contact.getId(), k -> Collections.synchronizedList(new ArrayList<>()));
        try {
            return numbers.stream().mapToLong(x -> x).sum() == Long.decode(answer);
        } catch (NumberFormatException ignored) {}
        return false;
    }
}
