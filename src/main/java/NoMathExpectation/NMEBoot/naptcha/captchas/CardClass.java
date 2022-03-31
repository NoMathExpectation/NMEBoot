package NoMathExpectation.NMEBoot.naptcha.captchas;

import NoMathExpectation.NMEBoot.RDLounge.cardSystem.Card;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardPool;
import NoMathExpectation.NMEBoot.naptcha.CaptchaGenerator;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Random;

public class CardClass implements CaptchaGenerator {
    private Card card;
    private final Random random = new Random();

    @NotNull
    @Override
    public MessageChain generate(@NotNull Contact contact) {
        try {
            card = CardPool.getLoadedCards().get(random.nextInt(CardPool.getLoadedCards().size()));
        } catch (IndexOutOfBoundsException e) {
            card = Card.EMPTY;
        }
        return get(contact);
    }

    @NotNull
    @Override
    public MessageChain get(@NotNull Contact contact) {
        if (card.equals(Card.EMPTY)) {
            return new MessageChainBuilder()
                    .append("输入任意字符并附到指令后提交。")
                    .build();
        }
        return new MessageChainBuilder()
                .append("以下卡片的等级是什么？请将答案附到指令后提交。")
                .append(card.getImage(contact))
                .build();
    }

    @Override
    public boolean check(@NotNull String answer, @NotNull Contact contact) {
        return card.equals(Card.EMPTY) || card.cardClass.toUpperCase(Locale.ROOT).equals(answer.trim().toUpperCase(Locale.ROOT));
    }
}
