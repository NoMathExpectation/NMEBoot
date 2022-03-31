package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.commandSystem.NormalUserStats;
import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

public class CheckinTimes implements CaptchaGenerator
{
    @NotNull
    @Override
    public MessageChain generate(@NotNull Contact contact) {
        return get(contact);
    }

    @NotNull
    @Override
    public MessageChain get(@NotNull Contact contact) {
        return new MessageChainBuilder()
                .append("这是有记录以来的第几次签到？请将答案附在指令后面。")
                .build();
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        try {
            return NormalUserStats.INSTANCE.getCheckInCountTotal() == Integer.decode(answer) - 1;
        } catch (NumberFormatException ignored) {}
        return false;
    }
}
