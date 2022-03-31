package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

public class RealCaptcha implements CaptchaGenerator {
    //private Code code = Jcaptcha.createcode();

    @NotNull
    @Override
    public MessageChain generate(@NotNull Contact contact) {
        return null;
    }

    @NotNull
    @Override
    public MessageChain get(@NotNull Contact contact) {
        return null;
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        return false;
    }
}
