package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.FunctionLibrary;
import NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ItemLibrary {
    public static final ItemPool LIBRARY = new ItemPool("itemLibrary", "物品库", 0, false);

    public static final Item TEST_FOR_LUCK = new Item("testforluck", "运气检测器", "使用此物品，你将有50%的几率获得1枚硬币，也将有50%的几率失去1枚硬币", new FunctionLibrary.TestForLuck(), false);
    public static final Item BOMB = new Item("bomb", "雷管", "将此物品扔出去，捡到的人会原地爆炸，扣除1枚硬币！", new FunctionLibrary.Bomb(), true);
    public static final Item COIN = new Item("coin", "硬币", "咖啡店的通用代币，中海医院常用其奖励表现良好的实习生", new FunctionLibrary.Coin(), true);
    public static final Item WORM = new Item("worm", "蛀虫", "将它扔出去，它会啃掉捡到的人的一张卡片！", new FunctionLibrary.Worm(), true);

    public static class RedPacket extends Item {
        private final int amount, min, max;
        private final Set<Long> CollectedUsers;

        private static final long serialVersionUID = 2731198109350585085L;

        public RedPacket(int amount, int min, int max, @NotNull Set<Long> collectedUsers) {
            super("red packet", "红包", "签到获得的礼物，内含" + amount + "枚硬币，丢出去的话，每个捡到的人都会得到" + min + "~" + max + "枚硬币", null, true);
            if (amount <= 0) {
                throw new IllegalArgumentException("硬币必须为正整数");
            }
            if (min > max) {
                throw new IllegalArgumentException("最小值不能大于最大值");
            }
            if (min > amount) {
                throw new IllegalArgumentException("最小值不能大于总硬币数");
            }
            this.amount = amount;
            this.min = min;
            this.max = max;
            this.CollectedUsers = collectedUsers;
        }

        public RedPacket(int amount, int min, int max) {
            this(amount, min, max, new HashSet<>());
        }

        @Override
        public boolean use(CardUser user) {
            Contact lastContact = ExecuteCenter.INSTANCE.getLastContact();
            if (CollectedUsers.contains(user.id)) {
                lastContact.sendMessage("你已经领取过这个红包了");
                ItemPool.getPools().get("ground").add(this);
                return true;
            }
            int getAmount = new Random().nextInt(max + 1) + min;
            user.giveToken(getAmount);
            CollectedUsers.add(user.id);
            lastContact.sendMessage(new MessageChainBuilder()
                    .append(new At(user.id))
                    .append("获得了")
                    .append(String.valueOf(getAmount))
                    .append("枚硬币！")
                    .build());
            int remain = amount - getAmount;
            if (remain > 0) {
                int subMin = min;
                int subMax = max;
                if (remain < min) {
                    subMin = remain;
                }
                if (remain < max) {
                    subMax = remain;
                }
                ItemPool.getPools().get("ground").add(new RedPacket(remain, subMin, subMax, CollectedUsers));
                lastContact.sendMessage("红包余剩" + remain + "枚硬币，已返回地面。");
            } else {
                lastContact.sendMessage("红包已经领完了");
            }
            return true;
        }

        @Override
        public RedPacket clone() {
            return new RedPacket(amount, min, max);
        }
    }

    static {
        LIBRARY.add(TEST_FOR_LUCK);
        LIBRARY.add(BOMB);
        LIBRARY.add(COIN);
        LIBRARY.add(WORM);
        //LIBRARY.add(new RedPacket(10, 1, 3));
    }

}
