package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class Stats {
    @NotNull
    public static MessageChain getGeneral() {
        return new MessageChainBuilder()
                .append("硬币总和：")
                .append(String.valueOf(CardUser.getUsers().values().stream().mapToInt(CardUser::getToken).sum()))
                .append("\n祈祷次数：")
                .append(String.valueOf(StatsData.INSTANCE.getPrayCount()))
                .append("\n卡片总和：")
                .append(String.valueOf(CardUser.getUsers().values().stream().mapToInt(user -> user.getCards().size()).sum()))
                .append("\n卡片抽取数：")
                .append(String.valueOf(StatsData.INSTANCE.getCardPullCount()))
                .append("\n物品抽取数：")
                .append(String.valueOf(StatsData.INSTANCE.getItemPullCount()))
                .append("\n总抽取数：")
                .append(String.valueOf(StatsData.INSTANCE.getCardPullCount() + StatsData.INSTANCE.getItemPullCount()))
                .append("\n拍卖成功数：")
                .append(String.valueOf(StatsData.INSTANCE.getAuctionSuccessCount()))
                .append("\n拍卖流拍数：")
                .append(String.valueOf(StatsData.INSTANCE.getAuctionFailCount()))
                .append("\n拍卖错误数：")
                .append(String.valueOf(StatsData.INSTANCE.getAuctionErrorCount()))
                .append("\n总拍卖数：")
                .append(String.valueOf(StatsData.INSTANCE.getAuctionSuccessCount() + StatsData.INSTANCE.getAuctionFailCount() + StatsData.INSTANCE.getAuctionErrorCount()))
                .build();
    }

    @NotNull
    public static MessageChain getCoins() {
        MessageChainBuilder mcb = new MessageChainBuilder();
        mcb.append("硬币排行榜：\n");
        List<CardUser> sortedUsers = CardUser.getUsers().values().stream().sorted((a, b) -> b.getToken() - a.getToken()).collect(Collectors.toList());
        for (int i = 0; i < sortedUsers.size(); i++) {
            CardUser user = sortedUsers.get(i);
            mcb.append(String.valueOf(i + 1))
                    .append(": ")
                    .append(user.name)
                    .append("  ")
                    .append(String.valueOf(user.getToken()))
                    .append("枚硬币\n");
        }
        return mcb.build();
    }

    @NotNull
    public static MessageChain getCardsCollection() {
        MessageChainBuilder mcb = new MessageChainBuilder();
        mcb.append("全收集排行榜：\n");
        List<CardUser> sortedUsers = CardUser.getUsers().values().stream().sorted((a, b) -> b.getCardsCollected() - a.getCardsCollected()).collect(Collectors.toList());
        for (int i = 0; i < sortedUsers.size(); i++) {
            CardUser user = sortedUsers.get(i);
            mcb.append(String.valueOf(i + 1))
                    .append(": ")
                    .append(user.name)
                    .append("  ")
                    .append(String.valueOf(user.getCardsCollected()))
                    .append("张卡片\n");
        }
        return mcb.build();
    }
}
