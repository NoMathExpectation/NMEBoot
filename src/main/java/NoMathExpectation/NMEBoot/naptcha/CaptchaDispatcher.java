package NoMathExpectation.NMEBoot.naptcha;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.*;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CaptchaDispatcher {
    private final List<CaptchaGenerator> generators = new ArrayList<>();
    private final Random random = new Random();
    private final Map<Long, CaptchaGenerator> currentCaptcha = new HashMap<>();
    private final Map<Long, LocalDateTime> lastUsed = new HashMap<>();
    public static final Duration ExpireTime = Duration.ofMinutes(5);

    @NotNull
    public CaptchaDispatcher register(CaptchaGenerator generator) {
        generators.add(generator);
        return this;
    }

    @NotNull
    public static String obfuscate(@NotNull String s, int p) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < s.length(); i++) {
            if (random.nextInt(p) == p / 2) {
                sb.append(((char) random.nextInt(0x10000)));
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    @NotNull
    public static Message obfuscate(@NotNull Message msg, int p) {
        if (msg instanceof SingleMessage) {
            if (msg instanceof PlainText) {
                return new PlainText(obfuscate(msg.contentToString(), p));
            }
            return msg;
        }

        MessageChainBuilder mcb = new MessageChainBuilder();
        for (SingleMessage sm : ((MessageChain) msg)) {
            if (sm instanceof PlainText) {
                mcb.append(obfuscate(sm.contentToString(), p));
            } else {
                mcb.append(sm);
            }
        }
        return mcb.build();
    }

    @NotNull
    public MessageChain generate(@NotNull Contact contact) {
        List<CaptchaGenerator> filteredGenerators = generators.stream().filter(c -> c.useOnlyIn.isEmpty() || c.useOnlyIn.contains(contact.getId())).collect(Collectors.toList());
        CaptchaGenerator captcha = filteredGenerators.get(random.nextInt(filteredGenerators.size()));
        currentCaptcha.put(contact.getId(), captcha);
        lastUsed.put(contact.getId(), LocalDateTime.now());
        return captcha.generate(contact);
        //return obfuscate(captcha.generate(contact), 10);
    }

    @NotNull
    public MessageChain getCaptcha(@NotNull Contact contact) {
        if (expired(contact.getId())) {
            return generate(contact);
        }
        lastUsed.put(contact.getId(), LocalDateTime.now());
        return currentCaptcha.get(contact.getId()).get(contact);
        //return obfuscate(currentCaptcha.get(contact.getId()).get(contact), 10);
    }

    public boolean checkAnswer(String answer, @NotNull Contact contact) {
        if (!expired(contact.getId()) && currentCaptcha.get(contact.getId()).check(answer, contact)) {
            expire(contact.getId());
            return true;
        }
        return false;
    }

    public void expire(long id) {
        lastUsed.put(id, LocalDateTime.MIN);
    }

    public boolean expired(long id) {
        LocalDateTime dt = lastUsed.get(id);
        if (dt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(dt.plus(ExpireTime));
    }

    public CaptchaGenerator getCurrentCaptcha(long id)
    {return currentCaptcha.get(id);}
}
