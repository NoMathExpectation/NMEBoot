package NoMathExpectation.NMEBoot.naptcha;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public interface CaptchaGenerator {
    Set<Long> useOnlyIn = new HashSet<>();

    @NotNull
    MessageChain generate(@NotNull Contact contact);

    @NotNull
    MessageChain get(@NotNull Contact contact);

    boolean check(@NotNull String answer, @NotNull Contact contact);
}
