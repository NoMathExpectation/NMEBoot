package NoMathExpectation.NMEBoot;

import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardUser;
import NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class FunctionLibrary {
    public static class TestForLuck implements SerializableFunction<CardUser, Boolean> {
        @NotNull
        @Override
        public Boolean apply(@NotNull CardUser user) {
            if (ExecuteCenter.INSTANCE.RANDOM.nextBoolean()) {
                user.giveToken(1);
                ExecuteCenter.INSTANCE.getLastContact().sendMessage("你获得了一枚硬币");
            } else {
                user.giveToken(-1);
                ExecuteCenter.INSTANCE.getLastContact().sendMessage("你失去了一枚硬币");
            }
            return true;
        }
    }

    public static class Bomb implements SerializableFunction<CardUser, Boolean> {
        @NotNull
        @Override
        public Boolean apply(@NotNull CardUser user) {
            user.giveToken(-1);
            ExecuteCenter.INSTANCE.getLastContact().sendMessage(new MessageChainBuilder()
                    .append(new At(user.id))
                    .append("原地爆炸了！失去了1枚硬币")
                    .build());
            return true;
        }
    }

    public static class Worm implements SerializableFunction<CardUser, Boolean> {
        @Override
        public Boolean apply(CardUser user) {
            List<NoMathExpectation.NMEBoot.RDLounge.cardSystem.Card> cards = user.getCards();
            if (cards.isEmpty()) {
                ExecuteCenter.INSTANCE.getLastContact().sendMessage(new MessageChainBuilder()
                        .append(new At(user.id))
                        .append("什么卡片也没有，蛀虫不情愿地溜走了")
                        .build());
            } else {
                user.discardItemStrict(cards.get(new Random().nextInt(cards.size())));
                ExecuteCenter.INSTANCE.getLastContact().sendMessage(new MessageChainBuilder()
                        .append("蛀虫啃掉了")
                        .append(new At(user.id))
                        .append("的一张卡片，味道好极了")
                        .build());
            }
            return true;
        }
    }

    public static class Coin implements SerializableFunction<CardUser, Boolean> {
        @NotNull
        @Override
        public Boolean apply(@NotNull CardUser user) {
            user.giveToken(1);
            return true;
        }
    }

    public static class Card implements SerializableFunction<CardUser, Boolean> {
        public Boolean apply(@NotNull CardUser user) {
            ExecuteCenter.INSTANCE.getLastContact().sendMessage("你舔了一下这张卡牌，口味比任天堂的游戏卡带还要糟糕");
            return false;
        }
    }
}
