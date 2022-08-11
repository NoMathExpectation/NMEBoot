package NoMathExpectation.NMEBoot.naptcha;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public interface CaptchaGenerator {
    default Set<Long> getUseOnlyIn() {
        return Collections.emptySet();
    }

    @NotNull
    MessageChain generate(@NotNull Contact contact);

    @NotNull
    MessageChain get(@NotNull Contact contact);

    boolean check(@NotNull String answer, @NotNull Contact contact);
}
