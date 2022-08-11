package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.Serializer;
import NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CardPool implements Serializable {
    private static final long serialVersionUID = 2021120504L;

    private static final Map<String, CardPool> POOLS = new HashMap<>();
    private static final List<Card> LOADED_CARDS = new ArrayList<>();
    private static final MiraiLogger logger = Main.INSTANCE.getLogger();
    public static final Serializer<CardPool> SERIALIZER = new Serializer<>();

    @NotNull
    @Contract(pure = true)
    public static Map<String, CardPool> getPools() {
        return Collections.unmodifiableMap(POOLS);
    }

    public static CardPool addPool(CardPool pool) {
        return POOLS.put(pool.id, pool);
    }

    @NotNull
    @Contract(pure = true)
    public static List<Card> getLoadedCards() {
        return Collections.unmodifiableList(LOADED_CARDS);
    }

    public static void removeLoaded() {
        LOADED_CARDS.clear();
    }

    private final List<Card> cards = new ArrayList<>();
    public final String id;
    public final String name;
    public final long pullCooldown;
    public final boolean exhaustible;
    private final Map<String, Double> classProbability;
    private final Random random = ExecuteCenter.INSTANCE.RANDOM;

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return id.equals(o);
        }
        if (!(o instanceof CardPool)) {
            return false;
        }
        return id.equals(((CardPool) o).id);
    }

    @Override
    public String toString() {
        return name;
    }

    public String showSimple(boolean showId) {
        return name + (showId ? " (id:" + id + ")" : "");
    }

    public CardPool(@NotNull String id, @NotNull String name, long cooldown, boolean exhaustible, Map<String, Double> classProbability) {
        this.id = id;
        this.name = name;
        pullCooldown = cooldown;
        this.exhaustible = exhaustible;
        this.classProbability = classProbability;
        addPool(this);
    }

    public CardPool(@NotNull String id, @NotNull String name, long cooldown, boolean exhaustible) {
        this(id, name, cooldown, exhaustible, null);
    }

    public CardPool(@NotNull String id, long cooldown, boolean exhaustible) {
        this(id, id, cooldown, exhaustible);
    }

    public static void LoadCards() {
        getPools().get("general").removeAll();
        getPools().get("general").loadFromFile("config/NoMathExpectation.NMEBoot/cards/general");
        getPools().get("levels").removeAll();
        getPools().get("levels").loadFromFile("config/NoMathExpectation.NMEBoot/cards/rdlevels");
    }

    public CardPool loadFromFile(@NotNull File file) {
        if (file.isFile()) {
            String[] splitName = file.getName().replaceAll("\\.(png|jpg|gif)", "").split("---");
            if (splitName.length < 3) {
                logger.warning("无效的卡片名称：" + file.getName());
                return this;
            }
            Card card = new Card(splitName[0], splitName[1], splitName[2], file);
            LOADED_CARDS.add(card);
            cards.add(card);
            logger.debug("已加载卡片：" + file.getName());
            return this;
        }
        if (!file.isDirectory() && !file.mkdirs()) {
            logger.error("创建目录：'" + file.getPath() + "' 失败！");
            return this;
        }
        for (File cardFile : Objects.requireNonNull(file.listFiles())) {
            loadFromFile(cardFile);
        }
        return this;
    }

    public CardPool loadFromFile(@NotNull String source) {
        return loadFromFile(new File(source));
    }

    public boolean add(Card card) {
        if (!exhaustible && cards.contains(card)) {
            return false;
        }
        return cards.add(card);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public boolean remove(Card card) {
        return cards.remove(card);
    }

    public boolean removeAll(Card card) {
        return cards.removeIf(card1 -> card1.equals(card));
    }

    public void removeAll() {
        cards.clear();
    }

    @NotNull
    public Card pull() {
        if (cards.isEmpty()) {
            return Card.EMPTY.clone();
        }
        StatsData.INSTANCE.setCardPullCount(StatsData.INSTANCE.getCardPullCount() + 1);
        if (classProbability == null) {
            if (exhaustible) {
                return cards.remove(random.nextInt(cards.size()));
            } else {
                return cards.get(random.nextInt(cards.size())).clone();
            }
        } else {
            Set<String> probabilities = classProbability.keySet();
            Set<String> notEmptyProbabilities = new HashSet<>();
            double sum = 0.0d;
            for (String classString : probabilities) {
                if (!cards.stream().filter(x -> x.cardClass.equals(classString)).findAny().orElse(Card.EMPTY).equals(Card.EMPTY)) {
                    notEmptyProbabilities.add(classString);
                    sum += classProbability.get(classString);
                }
            }
            double rand = random.nextDouble() * sum;
            String cardClass = "";
            for (String classString : notEmptyProbabilities) {
                cardClass = classString;
                rand -= classProbability.get(classString);
                if (rand <= 0) {
                    break;
                }
            }
            String finalCardClass = cardClass;
            List<Card> classifiedCards = cards.stream().filter(x -> x.cardClass.equals(finalCardClass)).collect(Collectors.toList());
            return classifiedCards.get(random.nextInt(classifiedCards.size()));
        }
    }

    @NotNull
    public Card pull(@NotNull Function<CardPool, Card> pullFun) {
        if (cards.isEmpty()) {
            return Card.EMPTY;
        }
        StatsData.INSTANCE.setCardPullCount(StatsData.INSTANCE.getCardPullCount() + 1);
        return pullFun.apply(this);
    }


    public Card exchange(Card original) {
        cards.add(original);
        StatsData.INSTANCE.setCardPullCount(StatsData.INSTANCE.getCardPullCount() + 1);
        return pull();
    }
}
