package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.Main;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AuctionCenter {
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    public AuctionCenter() {/*Main.INSTANCE.getLogger().info("拍卖系统已启动");*/}

    protected boolean runAuction(Auction auction) {
        if (pool.isShutdown()) {
            auction.discardAuction();
            return false;
        }
        pool.execute(auction);
        Main.INSTANCE.getLogger().info("一个新的拍卖已提交，提交人：" + auction.auctioner.showSimple(true) + "，提交物品：" + auction.item.showSimple(true) + "，初始价格：" + auction.getCurrentPrice());
        return true;
    }

    public synchronized MessageChain newAuction(@NotNull CardUser auctioner, @NotNull String id, @NotNull Contact group, int initialCost) {
        Item item = auctioner.searchAndDiscardItem(id);
        if (item.equals(Item.EMPTY)) {
            return new MessageChainBuilder()
                    .append("你没有此物品。")
                    .build();
        }
        if (runAuction(new Auction(auctioner, item, group, initialCost))) {
            return new MessageChainBuilder()
                    .append("提交成功，你的拍卖排在第")
                    .append(String.valueOf(Auction.getRunningAuctions().size()))
                    .append("位。")
                    .build();
        }
        return new MessageChainBuilder()
                .append("提交失败！")
                .build();
    }

    public synchronized MessageChain bid(@NotNull CardUser bider, int amount) {
        Auction currentAuction = Auction.getCurrentAuction();
        if (currentAuction == null) {
            return new MessageChainBuilder()
                    .append("当前没有物品在拍卖。")
                    .build();
        }
        if (currentAuction.bid(bider, amount)) {
            return new MessageChainBuilder()
                    .append("竞标成功。")
                    .build();
        }
        if (currentAuction.auctioner.equals(bider)) {
            return new MessageChainBuilder()
                    .append("你不能竞标自己的物品。")
                    .build();
        }
        if (bider.getToken() < amount) {
            return new MessageChainBuilder()
                    .append("你的硬币不足。")
                    .build();
        }
        return new MessageChainBuilder()
                .append("你的竞标金额不足。")
                .build();
    }

    public void stop() {
        for (Auction auction : Auction.getRunningAuctions()) {
            auction.stopAuction();
        }
        pool.shutdown();
        try {
            if (pool.awaitTermination(60, TimeUnit.SECONDS)) {
                Main.INSTANCE.getLogger().info("拍卖系统已停止");
            } else {
                Main.INSTANCE.getLogger().warning("拍卖系统等待停止超时");
            }
        } catch (InterruptedException e) {
            Main.INSTANCE.getLogger().warning("拍卖系统等待停止被中断");
        }
    }

    public MessageChain stopCurrentAuction() {
        if (Auction.getCurrentAuction() == null) {
            return new MessageChainBuilder()
                    .append("当前没有拍卖。")
                    .build();
        }
        Auction.getCurrentAuction().stopAuction();
        return new MessageChainBuilder()
                .append("当前拍卖已停止。")
                .build();
    }

    public MessageChain stopCurrentAuction(CardUser user) {
        if (Auction.getCurrentAuction() == null) {
            return new MessageChainBuilder()
                    .append("当前没有拍卖。")
                    .build();
        }
        if (Auction.getCurrentAuction().auctioner.equals(user)) {
            return stopCurrentAuction();
        }
        return new MessageChainBuilder()
                .append("当前拍卖不是你的")
                .build();
    }
}

class Auction implements Runnable {
    public static final float TAX = 0.5f;

    private static final Set<Auction> RUNNING_AUCTIONS = new HashSet<>();
    private static Auction currentAuction;

    public final Contact group;
    public final Item item;
    public final CardUser auctioner;
    private CardUser bider = null;
    private int cost;
    private final int timeSecs;
    private LocalDateTime endTime;
    private final int restoreTimeSecs;
    private List<Integer> reminders;
    private final List<Integer> restoredReminders;

    public Auction(@NotNull CardUser auctioner, @NotNull Item item, @NotNull Contact group, int initialCost, int timeSecs, int restoreTimeSecs, List<Integer> remindTimeBeforeEndSecs) {
        if (initialCost < 0) {
            throw new IllegalArgumentException("初始价格不能小于0！");
        }
        RUNNING_AUCTIONS.add(this);
        this.auctioner = auctioner;
        this.group = group;
        this.item = item;
        this.cost = initialCost;
        this.timeSecs = timeSecs;
        this.restoreTimeSecs = restoreTimeSecs;
        reminders = remindTimeBeforeEndSecs.stream().sorted().collect(Collectors.toList());
        restoredReminders = remindTimeBeforeEndSecs.stream().filter(s -> s < restoreTimeSecs).sorted().collect(Collectors.toList());
    }

    public Auction(@NotNull CardUser auctioner, @NotNull Item item, @NotNull Contact group, int initialCost) {
        this(auctioner, item, group, initialCost, 120, 30, List.of(10, 30, 60));
    }

    @Override
    public void run() {
        if (endTime != null && !endTime.isAfter(LocalDateTime.now())) {
            RUNNING_AUCTIONS.remove(this);
            auctioner.addItem(item, true);
            Main.INSTANCE.getLogger().info("拍卖已过期，提交人：" + auctioner.showSimple(true) + "，提交物品：" + item.showSimple(true));
            return;
        }

        currentAuction = this;
        Main.INSTANCE.getLogger().info("一个新的拍卖已开始，提交人：" + auctioner.showSimple(true) + "，提交物品：" + item.showSimple(true) + "，初始价格：" + cost);
        try {
            endTime = LocalDateTime.now().plusSeconds(timeSecs);
            try {
                group.sendMessage(new MessageChainBuilder()
                        .append(new At(auctioner.id))
                        .append("以")
                        .append(String.valueOf(cost))
                        .append("枚硬币的初始价格发起了对")
                        .append(item.showSimple(false))
                        .append("的拍卖！输入//c auc bid <价格> 来竞标")
                        .append(item instanceof Card ? ((Card) item).getImage(group) : new PlainText(""))
                        .build());
            } catch (CancellationException ignored) {
            }

            while (endTime.isAfter(LocalDateTime.now())) {
                if (!reminders.isEmpty() && Duration.between(LocalDateTime.now(), endTime).getSeconds() < reminders.get(reminders.size() - 1)) {
                    Main.INSTANCE.getLogger().info("拍卖余剩" + reminders.get(reminders.size() - 1) + "秒");
                    try {
                        group.sendMessage("拍卖余剩" + reminders.get(reminders.size() - 1) + "秒。使用//c auc bid <价格> 来竞标");
                    } catch (CancellationException ignored) {
                    }
                    reminders.remove(reminders.size() - 1);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    stopAuction();
                }
            }

            Main.INSTANCE.getLogger().info("拍卖已结束，最终价格：" + cost);
            if (bider == null) {
                auctioner.addItem(item, true);
                StatsData.INSTANCE.setAuctionFailCount(StatsData.INSTANCE.getAuctionFailCount() + 1);
                try {
                    group.sendMessage(new MessageChainBuilder()
                            .append(new At(auctioner.id))
                            .append("，你的物品流拍了！物品已返回背包")
                            .build());
                } catch (CancellationException ignored) {
                }
            } else {
                auctioner.giveToken(Math.round(cost * (1 - TAX)));
                bider.addItem(item, true);
                StatsData.INSTANCE.setAuctionSuccessCount(StatsData.INSTANCE.getAuctionSuccessCount() + 1);
                try {
                    group.sendMessage(new MessageChainBuilder()
                            .append(new At(bider.id))
                            .append("以")
                            .append(String.valueOf(cost))
                            .append("枚硬币的价格获得了")
                            .append(item.showSimple(false))
                            .append("！")
                            .append(item instanceof Card ? ((Card) item).getImage(group) : new PlainText(""))
                            .build());
                } catch (CancellationException ignored) {
                }
            }
        } catch (Exception e) {
            auctioner.addItem(item, true);
            if (bider != null) {
                bider.giveToken(cost);
            }
            Main.INSTANCE.getLogger().warning("拍卖发生错误，提交人：" + auctioner.showSimple(true) + "，当前竞标者：" + (bider == null ? "无" : bider.showSimple(true)) + "，交易物品：" + item.showSimple(true) + "，当前价格：" + cost);
            StatsData.INSTANCE.setAuctionErrorCount(StatsData.INSTANCE.getAuctionErrorCount() + 1);
            e.printStackTrace();
            try {
                group.sendMessage("拍卖发生错误，物品与硬币已返回背包。");
            } catch (CancellationException ignored) {
            }
        }
        RUNNING_AUCTIONS.remove(this);
        currentAuction = null;
    }

    public synchronized boolean bid(@NotNull CardUser newBider, int amount) {
        if (auctioner.equals(newBider)) {
            return false;
        }
        if (bider == null) {
            if (amount < cost || !newBider.pay(amount)) {
                return false;
            }
        } else {
            if (amount > cost && newBider.pay(amount)) {
                bider.giveToken(cost);
            } else {
                return false;
            }
        }
        bider = newBider;
        cost = amount;
        refreshEndTime(false);
        Main.INSTANCE.getLogger().info("竞标者易位，竞标者：" + (bider == null ? "无" : bider.showSimple(true)) + "，新价格：" + getCurrentPrice());
        group.sendMessage(new MessageChainBuilder()
                .append(new At(newBider.id))
                .append("以")
                .append(String.valueOf(amount))
                .append("枚硬币的价格竞标此物品！\n使用//c auc bid <价格> 来竞标")
                .build());
        return true;
    }

    public CardUser getCurrentBider() {
        return bider;
    }

    public int getCurrentPrice() {
        return cost;
    }

    public void refreshEndTime(boolean forced) {
        if (!forced && Duration.between(LocalDateTime.now(), endTime).getSeconds() >= restoreTimeSecs) {
            return;
        }
        endTime = LocalDateTime.now().plusSeconds(restoreTimeSecs);
        reminders = restoredReminders;
        group.sendMessage("余剩时间已重置至" + restoreTimeSecs + "秒");
        Main.INSTANCE.getLogger().info("余剩时间已重置");
    }

    public void stopAuction() {
        if (bider != null) {
            bider.giveToken(cost);
            bider = null;
        }
        endTime = LocalDateTime.MIN;
    }

    public void endAuction() {
        endTime = LocalDateTime.MIN;
    }

    public boolean discardAuction() {
        if (currentAuction == this) {
            return false;
        }
        Main.INSTANCE.getLogger().info("拍卖已废弃，提交人：" + auctioner.showSimple(true) + "，提交物品：" + item.showSimple(true) + "，初始价格：" + cost);
        auctioner.addItem(item, true);
        RUNNING_AUCTIONS.remove(this);
        return true;
    }

    @NotNull
    @Contract(pure = true)
    public static Set<Auction> getRunningAuctions() {
        return Collections.unmodifiableSet(RUNNING_AUCTIONS);
    }

    @Nullable
    public static Auction getCurrentAuction() {
        return currentAuction;
    }
}
