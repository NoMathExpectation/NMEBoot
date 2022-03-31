package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Random;

public class TwoOneFunction implements CaptchaGenerator {
    private int x;
    private int y;
    private final int min;
    private final int max;
    private MessageChain captcha;

    private final Random random = new Random();

    public TwoOneFunction(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值必须不大于最大值");
        }
        this.min = min;
        this.max = max;
    }

    @NotNull
    private String parse(@NotNull String prefix, int a, @NotNull String s) {
        switch (a) {
            case -1:
                return prefix + "-" + s;
            case 0:
                return prefix;
            case 1:
                return prefix + ("".equals(prefix) ? s : "+" + s);
            default:
                return prefix + (("".equals(prefix) || (a < 0)) ? (a + s) : ("+" + a + s));
        }
    }

    @NotNull
    private String parse(int a, @NotNull String s) {
        return parse("", a, s);
    }

    @NotNull
    @Override
    public MessageChain generate(@NotNull Contact contact) {
        x = random.nextInt(1 + max - min) + min;
        y = random.nextInt(1 + max - min) + min;

        int a, b, c, d;
        do {
            a = random.nextInt(1 + max - min) + min;
            b = random.nextInt(1 + max - min) + min;
            c = random.nextInt(1 + max - min) + min;
            d = random.nextInt(1 + max - min) + min;
        }
        while (a * d == b * c);
        captcha = new MessageChainBuilder()
                .append("请解决以下方程组，并以 <x> <y> 格式附到指令后提交：\n")
                .append(parse(parse(a, "x"), b, "y"))
                .append("=")
                .append(String.valueOf(a * x + b * y))
                .append("\n")
                .append(parse(parse(c, "x"), d, "y"))
                .append("=")
                .append(String.valueOf(c * x + d * y))
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
        try {
            return Arrays.equals(new int[]{x, y}, Arrays.stream(answer.split("\\s+|\n+")).mapToInt(Integer::decode).toArray());
        } catch (NumberFormatException ignored) {
        }
        return false;
    }
}
