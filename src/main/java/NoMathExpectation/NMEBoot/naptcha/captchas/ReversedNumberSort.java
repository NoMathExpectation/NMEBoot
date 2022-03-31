package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Random;

public class ReversedNumberSort implements CaptchaGenerator {
    private MessageChain captcha;

    private final Random random = new Random();

    public final int max;
    public final int min;
    private int[] numbers;

    public ReversedNumberSort(int count, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值必须不大于最大值");
        }
        numbers = new int[count];
        this.min = min;
        this.max = max;
    }

    @Override
    @NotNull
    public MessageChain generate(@NotNull Contact contact) {
        numbers = Arrays.stream(numbers).map(x -> random.nextInt(1 + max - min) + min).toArray();
        captcha = new MessageChainBuilder()
                .append("请按从大到小的顺序排列以下数字，以 <num> <num>... 形式附到指令后提交：\n")
                .append(Arrays.toString(numbers))
                .build();
        return captcha;
    }

    @Override
    public @NotNull
    MessageChain get(@NotNull Contact contact) {
        return captcha;
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        int[] sorted = Arrays.stream(numbers).boxed().sorted((a, b) -> b - a).mapToInt(x -> x).toArray();
        try {
            if (Arrays.equals(sorted, Arrays.stream(answer.split("\\s+|\n+")).mapToInt(Integer::decode).toArray())) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }
}
