package NoMathExpectation.NMEBoot.RDLounge.cardSystem;

import NoMathExpectation.NMEBoot.SerializableFunction;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;
import net.mamoe.mirai.utils.OverFileSizeMaxException;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Card extends Item {
    private static final long serialVersionUID = 2021120503L;

    public static final File ERROR_IMAGE = new File("config/NoMathExpectation.NMEBoot/cards/error.png");
    public static final Card EMPTY = new Card("empty", "empty", "none", ERROR_IMAGE);

    public final String cardClass;
    protected File image;

    Card(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull String cardClass, @NotNull File image, SerializableFunction<CardUser, Boolean> fun, boolean useOnGet) {
        super(id, name, description, fun, useOnGet);
        this.cardClass = cardClass;
        this.image = image;
    }

    Card(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull String cardClass, @NotNull File image) {
        this(id, name, description, cardClass, image, null, false);
    }

    Card(@NotNull String id, @NotNull String name, @NotNull String cardClass, @NotNull File image) {
        this(id, name, "", cardClass, image);
    }

    public File getImageFile() {
        return image;
    }

    public Image getImage(Contact contact) {
        if (image.equals(ERROR_IMAGE) || !image.isFile()) {
            image = ERROR_IMAGE;
            for (Card card : CardPool.getLoadedCards()) {
                if (equals(card)) {
                    image = card.getImageFile();
                    break;
                }
            }
        }
        try {
            return ExternalResource.uploadAsImage(image, contact);
        } catch (OverFileSizeMaxException ignored) {
        }
        return ExternalResource.uploadAsImage(ERROR_IMAGE, contact);
    }

    @Override
    public String show() {
        return "物品展示\n\n" + "物品名：[" + cardClass + "]" + name + "\nid：" + id + "\n描述：\n" + description;
    }

    @Override
    public String showSimple(boolean showId) {
        return "[" + cardClass + "]" + name + (showId ? " (id:" + id + ")" : "");
    }

    @Override
    public Card clone() {
        try {
            return (Card) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
