package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.SerializableFunction;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Item implements Serializable, Cloneable {
    private static final long serialVersionUID = 2021120502L;

    public static final Item EMPTY = new Item("empty", "empty", "");

    public final String id;
    public String name;
    public String description;
    public boolean useOnGet;
    protected final SerializableFunction<CardUser, Boolean> useFunction;

    public Item(@NotNull String id, @NotNull String name, @NotNull String description, SerializableFunction<CardUser, Boolean> fun, boolean useOnGet) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.useOnGet = useOnGet;
        useFunction = fun;
    }

    public Item(@NotNull String id, @NotNull String name, @NotNull String description) {
        this(id, name, description, null, false);
    }

    public String show() {
        return "物品展示\n\n" + "物品名：" + name + "\nid：" + id + "\n描述：\n" + description;
    }

    public String showSimple(boolean showId) {
        return name + (showId ? " (id:" + id + ")" : "");
    }

    public boolean use(CardUser user) {
        if (useFunction == null) {
            return false;
        }
        return useFunction.apply(user);
    }

    @Override
    public boolean equals(Object o) {
        return ((o instanceof String) && id.equals(o)) || ((o instanceof Item) && id.equals(((Item) o).id));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Item clone() throws CloneNotSupportedException {
        return (Item) super.clone();
    }
}
