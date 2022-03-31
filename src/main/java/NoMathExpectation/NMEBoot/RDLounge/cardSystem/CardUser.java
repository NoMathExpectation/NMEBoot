package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.Serializer;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CardUser implements Serializable {
    private static final long serialVersionUID = 2021120501L;

    private static final Map<Long, CardUser> USERS = new HashMap<>();
    private static final MiraiLogger logger = Main.INSTANCE.getLogger();
    public static final Serializer<CardUser> SERIALIZER = new Serializer<>();
    private static final Random RANDOM = Main.executeCenter.RANDOM;

    @NotNull
    @Contract(pure = true)
    public static Map<Long, CardUser> getUsers() {
        return Collections.unmodifiableMap(USERS);
    }

    public static CardUser addUser(@NotNull CardUser user) {
        return USERS.put(user.id, user);
    }

    public final long id;
    public String name;
    private final List<Item> inventory = new ArrayList<>();
    private int token = 0;

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return id == ((Number) o).longValue();
        }
        if (!(o instanceof CardUser)) {
            return false;
        }
        return id == (((CardUser) o).id);
    }

    public static boolean isCoin(@NotNull String s) {
        return s.equals("硬币") || s.toLowerCase(Locale.ROOT).equals("coin") || s.toLowerCase(Locale.ROOT).equals("token");
    }

    @Override
    public String toString() {
        return name;
    }

    public List<Card> getCards() {
        return inventory.stream().filter(item -> item instanceof Card).map(card -> ((Card) card)).collect(Collectors.toList());
    }

    public String showSimple(boolean showId) {
        return name + (showId ? " (id:" + id + ")" : "");
    }

    public CardUser(long id, @NotNull String name) {
        this.id = id;
        this.name = name;
        addUser(this);
    }

    public int getToken() {
        return token;
    }

    public boolean addItem(@NotNull Item i, boolean immuneUseOnGet) {
        logger.info(showSimple(true) + "获得了物品:" + i.showSimple(true));
        if (i.equals(ItemLibrary.COIN) || !immuneUseOnGet && i.useOnGet) {
            i.use(this);
            logger.info(showSimple(true) + "使用了物品:" + i.showSimple(true));
            return true;
        }
        return inventory.add(i);
    }

    public boolean addItem(@NotNull Item i) {
        return addItem(i, false);
    }

    public boolean hasItem(Item i) {
        return inventory.contains(i);
    }

    public boolean hasItemStrict(Item i) {
        if (i.equals(ItemLibrary.COIN)) {
            return token > 0;
        }

        for (Item item : inventory) {
            if (item.hashCode() == i.hashCode()) {
                return true;
            }
        }
        return false;
    }

    protected enum FilterType {GENERAL, CARD, CARD_TYPE}

    @NotNull
    @Contract(pure = true)
    protected static Predicate<Item> getFilter(FilterType t, @NotNull String s) {
        if (s.startsWith("!")) {
            return i -> !getFilter(t, s.substring(1)).test(i);
        }

        if (s.contains("^")) {
            String[] xors = s.split("\\^");
            Predicate<Item> p = i -> false;
            for (String xor: xors) {
                Predicate<Item> finalP = p;
                p = i -> finalP.test(i) ^ getFilter(t, xor).test(i);
            }
            return p;
        }

        if (s.contains("/")) {
            String[] ors = s.split("/");
            Predicate<Item> p = i -> false;
            for (String or: ors) {
                Predicate<Item> finalP = p;
                p = i -> finalP.test(i) || getFilter(t, or).test(i);
            }
            return p;
        }

        if (s.contains("&")) {
            String[] ands = s.split("&");
            Predicate<Item> p = i -> true;
            for (String and: ands) {
                Predicate<Item> finalP = p;
                p = i -> finalP.test(i) && getFilter(t, and).test(i);
            }
            return p;
        }

        switch (t) {
            case GENERAL:
                if (s.toLowerCase(Locale.ROOT).startsWith("card")) {
                    return i -> getFilter(FilterType.CARD, s.substring(4)).test(i);
                }
                if (s.toLowerCase(Locale.ROOT).startsWith("precise:")) {
                    return i -> i.id.equals(s.substring(8)) || i.name.equals(s.substring(8));
                }
                return i -> i.id.equals(s) || i.name.toLowerCase(Locale.ROOT).contains(s.toLowerCase(Locale.ROOT));
            case CARD:
                if (s.startsWith(":")) {
                    return i -> i instanceof Card && getFilter(FilterType.CARD_TYPE, s.substring(1)).test(i);
                }
                return i -> i instanceof Card;
            case CARD_TYPE:
                return i -> i instanceof Card && ((Card) i).cardClass.toUpperCase(Locale.ROOT).equals(s.toUpperCase(Locale.ROOT));
        }
        return i -> true;
    }

    @NotNull
    @Contract(pure = true)
    protected static Predicate<Item> getFilter(@NotNull String s) {
        return getFilter(FilterType.GENERAL, s);
    }

    public List<Item> searchItem(@NotNull String s) {
        List<Item> searchItems = new ArrayList<>(inventory);
        if (token > 0) {
            searchItems.addAll(Collections.nCopies(token, ItemLibrary.COIN));
        }

        if (s.equals("*")) {
            return searchItems;
        }

        Stream<Item> itemStream = searchItems.stream();
        for (String f : s.split("\\s+|\n+")) {
            itemStream = itemStream.filter(getFilter(f));
        }

        return itemStream.collect(Collectors.toList());
    }

    @NotNull
    public static List<Item> uniqueItem(@NotNull List<Item> items) {
        List<Item> uniqueItems = new ArrayList<>();
        for(Item i : items)
        {
            if(!uniqueItems.contains(i)) {
                uniqueItems.add(i);
            }
        }
        return uniqueItems;
    }

    public boolean useItem(@NotNull Item i) {
        return useItem(i.id);
    }

    public boolean useItem(String id) {
        List<Item> searchItems = searchItem(id);
        if (searchItems.isEmpty()) {
            return false;
        }
        Item realItem = searchItems.remove(RANDOM.nextInt(searchItems.size()));
        discardItemStrict(realItem);
        if (!realItem.use(this)) {
            inventory.add(realItem);
            return false;
        }
        logger.info(showSimple(true) + "使用了物品:" + realItem.showSimple(true));
        return true;
    }

    public boolean discardItemStrict(Item i) {
        if (i.equals(ItemLibrary.COIN)) {
            return pay(1);
        }

        for (int n = 0; n < inventory.size(); n++) {
            if (inventory.get(n).hashCode() == i.hashCode()) {
                inventory.remove(n);
                logger.info("移除了" + showSimple(true) + "的物品:" + i.showSimple(true));
                return true;
            }
        }
        return false;
    }

    @NotNull
    public Item discardItem(@NotNull Item i) {
        if (i.equals(ItemLibrary.COIN)) {
            if (pay(1)) {
                return ItemLibrary.COIN;
            } else {
                return Item.EMPTY;
            }
        }

        Item realItem;
        try {
            realItem = inventory.remove(inventory.indexOf(i));
        } catch (IndexOutOfBoundsException e) {
            return Item.EMPTY;
        }
        logger.info(showSimple(true) + "丢弃了物品:" + realItem.showSimple(true));
        return realItem;
    }

    @NotNull
    public Item discardItem(String id) {
        return discardItem(new Item(id, "", ""));
    }

    @NotNull
    public Item searchAndDiscardItem(@NotNull String s) {
        List<Item> searchItems = searchItem(s);
        if (searchItems.isEmpty()) {
            return Item.EMPTY;
        }
        Item item = searchItems.remove(RANDOM.nextInt(searchItems.size()));
        discardItemStrict(item);
        logger.info(showSimple(true) + "丢弃了物品:" + item.showSimple(true));
        return item;
    }

    public List<Item> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    @NotNull
    public MessageChain getInventoryMessageChain() {
        MessageChainBuilder mcb = new MessageChainBuilder();
        Map<String, Integer> countMap = new HashMap<>();

        for (Item i : inventory) {
            String name = i.showSimple(false);
            if (countMap.containsKey(name)) {
                int count = countMap.get(name);
                countMap.put(name, ++count);
            } else {
                countMap.put(name, 1);
            }
        }

        mcb.append(new At(id))
                .append("的物品有：\n\n")
                .append(String.valueOf(token))
                .append("x 硬币\n");

        for (String name : countMap.keySet()) {
            mcb.append("")
                    .append(String.valueOf(countMap.get(name)))
                    .append("x ")
                    .append(name)
                    .append("\n");
        }

        mcb.append("你已经收集了")
                .append(String.valueOf(getCardsCollected()))
                .append("/")
                .append(String.valueOf(CardPool.getLoadedCards().size()))
                .append("张卡片");

        return mcb.build();
    }

    @NotNull
    public static MessageChain getInventoryMessageChain(@NotNull List<Item> inventory) {
        MessageChainBuilder mcb = new MessageChainBuilder();
        Map<String, Integer> countMap = new HashMap<>();

        for (Item i : inventory) {
            String name = i.showSimple(false);
            if (countMap.containsKey(name)) {
                int count = countMap.get(name);
                countMap.put(name, ++count);
            } else {
                countMap.put(name, 1);
            }
        }

        for (String name : countMap.keySet()) {
            mcb.append("")
                    .append(String.valueOf(countMap.get(name)))
                    .append("x ")
                    .append(name)
                    .append("\n");
        }

        return mcb.build();
    }

    public int getCardsCollected() {
        List<Card> uniqueCards = new ArrayList<>();
        for (Card card : getCards()) {
            if (!uniqueCards.contains(card)) {
                uniqueCards.add(card);
            }
        }
        return uniqueCards.size();
    }

    private final Map<String, LocalDateTime> lastPulled = new HashMap<>();

    public Card pull(@NotNull CardPool cardPool) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastPulled.get(cardPool.id);
        LocalDateTime then;
        if (lastTime != null) {
            then = lastTime.plusSeconds(cardPool.pullCooldown);
            if (now.isBefore(then)) {
                Card time = Card.EMPTY.clone();
                Duration d = Duration.between(now, then);
                time.description = "请再等待" + d.toDays() + "天" + d.toHoursPart() + "时" + d.toMinutesPart() + "分" + d.toSecondsPart() + "秒";
                return time;
            }
        }
        Card card = cardPool.pull();
        if (card.equals(Card.EMPTY)) {
            card.description = "这个卡池是空的，稍后再来看看吧";
        } else {
            inventory.add(card);
            logger.info(showSimple(true) + "从" + cardPool.showSimple(true) + "抽到了卡片:" + card.showSimple(true));
            lastPulled.put(cardPool.id, now);
        }
        return card;
    }

    public Item pull(@NotNull ItemPool itemPool) throws CloneNotSupportedException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastPulled.get(itemPool.id);
        LocalDateTime then;
        if (lastTime != null) {
            then = lastTime.plusSeconds(itemPool.pullCooldown);
            if (now.isBefore(then)) {
                Item time = Item.EMPTY.clone();
                Duration d = Duration.between(now, then);
                time.description = "请再等待" + d.toDays() + "天" + d.toHoursPart() + "时" + d.toMinutesPart() + "分" + d.toSecondsPart() + "秒";
                return time;
            }
        }
        Item item = itemPool.pull();
        if (item.equals(Item.EMPTY)) {
            item.description = "这个物品池是空的，稍后再来看看吧";
        } else {
            inventory.add(item);
            logger.info(showSimple(true) + "从" + itemPool.showSimple(true) + "获得了物品:" + item.showSimple(true));
            lastPulled.put(itemPool.id, now);
        }
        return item;
    }

    private LocalDateTime lastPrayed = LocalDateTime.MIN;

    public Duration pray() {
        long prayCooldown = 82800L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime then = lastPrayed.plusSeconds(prayCooldown);
        if (now.isAfter(then)) {
            giveToken(1);
            lastPrayed = now;
            StatsData.INSTANCE.setPrayCount(StatsData.INSTANCE.getPrayCount() + 1);
            return Duration.ZERO;
        }
        return Duration.between(now, then);
    }

    public void giveToken(int count) {
        token += count;
        logger.info(showSimple(true) + "获得了" + count + "枚硬币，现在有" + token + "枚硬币");
    }

    public boolean pay(int count) {
        if (token >= count) {
            token -= count;
            logger.info(showSimple(true) + "支付了" + count + "枚硬币，现在有" + token + "枚硬币");
            return true;
        } else {
            return false;
        }
    }

    public MessageChain boxIn() {
        List<Item> cards = inventory.stream().filter(item -> item instanceof Card).collect(Collectors.toList());
        if (cards.isEmpty()) {
            return new MessageChainBuilder().append("你没有任何卡片").build();
        }
        Card card = (Card) cards.remove(RANDOM.nextInt(cards.size()));
        inventory.remove(card);
        CardPool.getPools().get("box").add(card);
        logger.info(showSimple(true) + "将卡片:" + card.showSimple(true) + "放进了交换箱里");
        return new MessageChainBuilder()
                .append(new At(id))
                .append("将一张")
                .append(card.showSimple(false))
                .append("放进了盒子里")
                .build();
    }

    public MessageChain boxOut(Contact contact) {
        Card card = CardPool.getPools().get("box").pull();
        if (card.equals(Card.EMPTY)) {
            return new MessageChainBuilder().append("交换箱里没有任何卡片").build();
        }
        inventory.add(card);
        logger.info(showSimple(true) + "从交换箱里获得了卡片:" + card.showSimple(true));
        return new MessageChainBuilder()
                .append(new At(id))
                .append("从交换箱里获得了一张")
                .append(card.showSimple(false))
                .append("！\n")
                .append(card.getImage(contact))
                .build();
    }

    public MessageChain useBox(Contact contact) {
        List<Card> cards = getCards();
        if (cards.isEmpty()) {
            return new MessageChainBuilder().append("你没有任何卡片").build();
        }
        if (CardPool.getPools().get("box").isEmpty()) {
            return new MessageChainBuilder().append("交换箱没有任何卡片").build();
        }
        Card original = cards.remove(RANDOM.nextInt(cards.size()));
        inventory.remove(original);
        Card after = CardPool.getPools().get("box").exchange(original);
        inventory.add(after);
        logger.info(showSimple(true) + "用卡片:" + after.showSimple(true) + "替换了:" + original.showSimple(true));
        return new MessageChainBuilder()
                .append(new At(id))
                .append("将一张")
                .append(original.showSimple(false))
                .append("放进了盒子里，拿出了一张")
                .append(after.showSimple(false))
                .append("！\n")
                .append(after.getImage(contact))
                .build();
    }
}
