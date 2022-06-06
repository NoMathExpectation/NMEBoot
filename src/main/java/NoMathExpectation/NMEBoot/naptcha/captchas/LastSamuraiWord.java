package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter;
import NoMathExpectation.NMEBoot.commandSystem.services.RDLoungeIntegrated;
import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

public class LastSamuraiWord implements CaptchaGenerator {
    public LastSamuraiWord() {
        useOnlyIn.addAll(RDLoungeIntegrated.USING_GROUP);
    }

    @Override
    @NotNull
    public MessageChain generate(@NotNull Contact contact) {
        return new MessageChainBuilder().append("武士上次说了什么？请将此单词附到指令后面提交").build();
    }

    @Override
    public @NotNull
    MessageChain get(@NotNull Contact contact) {
        return generate(contact);
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        return ExecuteCenter.INSTANCE.getLastSamuraiWord().equals(answer);
    }
}
