package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.Serializer;
import NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

public class ItemPool implements Serializable {
    private static final long serialVersionUID = 2021122401L;

    private static final Map<String, ItemPool> POOLS = new HashMap<>();
    private static final MiraiLogger logger = Main.INSTANCE.getLogger();
    public static final Serializer<ItemPool> SERIALIZER = new Serializer<>();

    @NotNull
    @Contract(pure = true)
    public static Map<String, ItemPool> getPools() {
        return Collections.unmodifiableMap(POOLS);
    }

    public static ItemPool addPool(ItemPool pool) {
        return POOLS.put(pool.id, pool);
    }

    private final List<Item> items = new ArrayList<>();
    public final String id;
    public final String name;
    public final long pullCooldown;
    public final boolean exhaustible;
    private final Random random = ExecuteCenter.INSTANCE.RANDOM;

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return id.equals(o);
        }
        if (!(o instanceof ItemPool)) {
            return false;
        }
        return id.equals(((ItemPool) o).id);
    }

    @Override
    public String toString() {
        return name;
    }

    public String showSimple(boolean showId) {
        return name + (showId ? " (id:" + id + ")" : "");
    }

    public ItemPool(@NotNull String id, @NotNull String name, long cooldown, boolean exhaustible) {
        this.id = id;
        this.name = name;
        pullCooldown = cooldown;
        this.exhaustible = exhaustible;
        addPool(this);
    }

    public ItemPool(@NotNull String id, long cooldown, boolean exhaustible) {
        this(id, id, cooldown, exhaustible);
    }

    public boolean add(Item item) {
        if (!exhaustible && items.contains(item)) {
            return false;
        }
        return items.add(item);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean remove(Item item) {
        return items.remove(item);
    }

    public boolean removeAll(Item item) {
        return items.removeIf(item1 -> item1.equals(item));
    }

    public void removeAll() {
        items.clear();
    }

    @NotNull
    public Item pull() throws CloneNotSupportedException {
        if (items.isEmpty()) {
            return Card.EMPTY.clone();
        }
        StatsData.INSTANCE.setItemPullCount(StatsData.INSTANCE.getItemPullCount() + 1);
        if (exhaustible) {
            return items.remove(random.nextInt(items.size()));
        } else {
            return items.get(random.nextInt(items.size())).clone();
        }
    }

    @NotNull
    public Item pull(@NotNull Function<ItemPool, Item> pullFun) throws CloneNotSupportedException {
        if (items.isEmpty()) {
            return Item.EMPTY.clone();
        }
        StatsData.INSTANCE.setItemPullCount(StatsData.INSTANCE.getItemPullCount() + 1);
        return pullFun.apply(this);
    }
}
