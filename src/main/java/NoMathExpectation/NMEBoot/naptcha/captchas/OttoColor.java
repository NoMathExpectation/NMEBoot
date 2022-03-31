package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.commandSystem.RDLounge;
import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Random;

public class OttoColor implements CaptchaGenerator {
    public OttoColor() {
        colors = new File("config/NoMathExpectation.NMEBoot/ottocolor").listFiles();
        if (colors == null) {
            throw new RuntimeException("凹兔颜色文件夹缺失！");
        }
        useOnlyIn.add(RDLounge.GROUP_ID);
    }

    private final File[] colors;
    private File selectedColor;

    private MessageChain captcha;

    @NotNull
    @Override
    public MessageChain generate(@NotNull Contact contact) {
        selectedColor = colors[new Random().nextInt(colors.length)];
        captcha = new MessageChainBuilder()
                .append("以下凹兔的颜色是什么？请附到指令后面提交\n")
                .append(ExternalResource.uploadAsImage(selectedColor, contact))
                .build();
        return captcha;
    }

    @NotNull
    @Override
    public MessageChain get(@NotNull Contact contact) {
        return captcha;
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        return (answer + ".gif").equals(selectedColor.getName());
    }
}
