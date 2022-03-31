package NoMathExpectation.NMEBoot;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WordleMirai {
    private final Map<Long, Wordle> wordles = new HashMap<>();
    public int defaultTries;
    public final int maxTries;

    public WordleMirai(File wordlist, int tries, int maxTries) {
        if (tries < 1 || tries > maxTries) {
            throw new IllegalArgumentException("尝试次数不符合要求");
        }
        Wordle.setWordList(wordlist);
        defaultTries = tries;
        this.maxTries = maxTries;
    }

    @NotNull
    public MessageChain getWordleMessage(long id, boolean ignoreEnded) {
        Wordle w = wordles.get(id);
        if (w == null || (!ignoreEnded && w.isEnded())) {
            return new MessageChainBuilder()
                    .append("当前没有正在运行的wordle")
                    .build();
        }
        MessageChainBuilder mcb = new MessageChainBuilder()
                .append(w.getRowsString())
                .append(w.isHardMode() ? "困难模式\n" : "");
        if (w.isPassed()) {
            mcb.append("恭喜！你通过了！");
        } else if (w.getTriesRemain() <= 0) {
            mcb.append("你没有次数了\n正确答案：")
                    .append(w.getAnswer());
        } else {
            mcb.append("你还剩")
                    .append(String.valueOf(w.getTriesRemain()))
                    .append("次机会");
        }
        return mcb.build();
    }

    @NotNull
    public MessageChain newWordle(long id, int tries, boolean hard) {
        Wordle w = wordles.computeIfAbsent(id, x -> new Wordle(tries));
        w.end();
        try {
            if (tries > maxTries) {
                throw new IllegalArgumentException("尝试次数不符合要求");
            }
            w.setTries(tries);
        } catch (IllegalArgumentException e) {
            w.setTries(defaultTries);
        }
        w.generate(hard);
        return getWordleMessage(id, true);
    }

    @NotNull
    public MessageChain newWordle(long id, boolean hard) {
        return newWordle(id, defaultTries, hard);
    }

    @NotNull
    public MessageChain newWordle(long id, int tries) {
        return newWordle(id, tries, false);
    }

    @NotNull
    public MessageChain newWordle(long id) {
        return newWordle(id, false);
    }

    @NotNull
    public MessageChain validateAnswer(long id, String word) {
        Wordle w = wordles.get(id);
        if (w == null || w.isEnded()) {
            return new MessageChainBuilder()
                    .append("当前没有正在运行的wordle")
                    .build();
        }
        try {
            w.validate(word);
        } catch (IllegalArgumentException e) {
            return new MessageChainBuilder()
                    .append(e.getMessage())
                    .build();
        }

        return getWordleMessage(id, true);
    }

    @NotNull
    public MessageReceipt<Contact> sendHelp(@NotNull Contact contact) {
        return contact.sendMessage(new MessageChainBuilder()
                .append(".//wordle|w ...\n")
                .append("help :显示此帮助\n")
                .append("new [尝试次数] [-h|-hard] :开始新一轮wordle，默认尝试次数为")
                .append(String.valueOf(defaultTries))
                .append("，添加-h或-hard启用困难模式\n")
                .append("show :展示当前wordle\n")
                .append("<单词> :提交答案")
                .build());
    }

    public MessageChain parseAndNewWordle(long id, @NotNull String[] args) {
        int tries = defaultTries;
        boolean hard = false;

        for (String arg : args) {
            try {
                tries = Integer.decode(arg);
                continue;
            } catch (NumberFormatException ignored) {}

            switch (arg.toLowerCase(Locale.ROOT)) {
                case "-h":
                case "-hard":
                case "困难":
                case "困难模式":
                    hard = true;
                    break;
                case "-e":
                case "-easy":
                case "简单":
                case "简单模式":
                    hard = false;
                    break;
                default:
            }
        }

        return newWordle(id, tries, hard);
    }
}
