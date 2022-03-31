package NoMathExpectation.NMEBoot.commandSystem;

import NoMathExpectation.NMEBoot.Main;
import NoMathExpectation.NMEBoot.Serializer;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent;
import net.mamoe.mirai.event.events.GroupMessageSyncEvent;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NormalUser implements Serializable {
    private static final long serialVersionUID = 2021122501L;

    private static final Map<Long, NormalUser> USERS = new HashMap<>();
    private static final MiraiLogger logger = Main.INSTANCE.getLogger();
    public static final Serializer<NormalUser> SERIALIZER = new Serializer<>();
    private static final Random RANDOM = Main.executeCenter.RANDOM;

    @NotNull
    @Contract(pure = true)
    public static Map<Long, NormalUser> getUsers() {
        return Collections.unmodifiableMap(USERS);
    }

    public static NormalUser addUser(NormalUser user) {
        return USERS.put(user.id, user);
    }

    public final long id;
    public String name;

    NormalUser(long id, @NotNull String name) {
        this.id = id;
        this.name = name;
        addUser(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return id == ((Number) o).longValue();
        }
        if (!(o instanceof NormalUser)) {
            return false;
        }
        return id == (((NormalUser) o).id);
    }

    public String showSimple(boolean showId) {
        return name + (showId ? " (id:" + id + ")" : "");
    }

    private int luck;
    private LocalDate luckDate = LocalDate.MIN;

    public int getLuck() {
        LocalDate now = LocalDate.now();
        if (now.isAfter(luckDate)) {
            luck = RANDOM.nextInt(101);
            luckDate = now;
        }
        return luck;
    }

    static {
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, e -> increaseMessageCount(e.getSender().getId(), e.getSubject().getId()));
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessagePostSendEvent.class, e -> increaseMessageCount(e.getBot().getId(), e.getTarget().getId()));
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageSyncEvent.class, e -> increaseMessageCount(e.getSender().getId(), e.getSubject().getId()));
    }

    private int checkInStreak = 0;
    private int checkInRank = 0;
    private static LocalDate checkInDate = LocalDate.parse(NormalUserStats.INSTANCE.getCheckInDate());
    private static int checkInCount = NormalUserStats.INSTANCE.getCheckInCount();
    private LocalDate lastCheckedIn = LocalDate.MIN;
    private LocalDateTime lastCheckedInExact = LocalDateTime.MIN;

    public static LocalDate getCheckInDate() {
        return checkInDate;
    }

    public static int getCheckInCount() {
        return checkInCount;
    }

    public boolean checkIn() {
        if (LocalDate.now().equals(lastCheckedIn)) {
            return false;
        }
        if (!LocalDate.now().equals(checkInDate)) {
            checkInCount = 1;
            checkInDate = LocalDate.now();
        } else {
            checkInCount++;
        }
        checkInRank = checkInCount;
        if (LocalDate.now().equals(lastCheckedIn.plusDays(1))) {
            checkInStreak++;
        } else {
            checkInStreak = 1;
        }
        lastCheckedIn = LocalDate.now();
        lastCheckedInExact = LocalDateTime.now();
        NormalUserStats.INSTANCE.setCheckInCountTotal(NormalUserStats.INSTANCE.getCheckInCountTotal() + 1);
        logger.info(showSimple(true) + "已签到");
        return true;
    }

    public boolean isCheckedIn() {
        return LocalDate.now().equals(lastCheckedIn);
    }

    public int getCheckInStreak() {
        return checkInStreak;
    }

    public int getCheckInRank() {
        return checkInRank;
    }

    public LocalDateTime getLastCheckInTime() {
        return lastCheckedInExact;
    }

    private LocalDateTime bannedUntil = LocalDateTime.MIN;
    private String bannedReason = "none";

    public LocalDateTime ban(@NotNull Duration d, @NotNull String reason) {
        if (bannedUntil.isBefore(LocalDateTime.now())) {
            bannedUntil = LocalDateTime.now().plus(d);
        } else {
            bannedUntil = bannedUntil.plus(d);
        }
        bannedReason = reason;
        return bannedUntil;
    }

    public LocalDateTime ban(Duration d) {
        return ban(d, "none");
    }

    public void unban() {
        bannedUntil = LocalDateTime.now();
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public Duration getBannedRemain() {
        return Duration.between(LocalDateTime.now(), bannedUntil);
    }

    public boolean isBanned() {
        return !getBannedRemain().isNegative();
    }

    public String getBannedReason() {
        return bannedReason;
    }

    private int warns = 0;
    public static final int banThreshold = 3;
    public static final Duration autoBanTime = Duration.ofDays(1);

    public boolean warn() {
        if (++warns >= 3) {
            ban(autoBanTime, "警告次数达到上限，自动封禁");
            return true;
        }
        return false;
    }

    public int getWarns() {
        return warns;
    }

    public void unwarn() {
        warns--;
    }

    private final Map<Long, Integer> messageCount = new HashMap<>();
    private final Map<Long, Integer> messageCountDaily = new HashMap<>();
    private static LocalDate messageCountDate = LocalDate.parse(NormalUserStats.INSTANCE.getMessageCountDate());

    public static LocalDate getMessageCountDate() {
        return messageCountDate;
    }

    public int getMessageCount(long id) {
        return messageCount.computeIfAbsent(id, x -> 0);
    }

    public int getMessageCountDaily(long id) {
        return messageCountDaily.computeIfAbsent(id, x -> 0);
    }

    public int getTotalMessageCountDaily() {
        return messageCountDaily.values().stream().mapToInt(x -> x).sum();
    }

    public void resetMessageCountDaily() {
        messageCountDaily.clear();
    }

    public int getTotalMessageCount() {
        return messageCount.values().stream().mapToInt(x -> x).sum();
    }

    public static void increaseMessageCount(long id, long group) {
        if (!LocalDate.now().equals(messageCountDate)) {
            NormalUser.getUsers().values().forEach(NormalUser::resetMessageCountDaily);
            messageCountDate = LocalDate.now();
        }

        NormalUser user = NormalUser.getUsers().get(id);
        if (user == null) {
            return;
        }

        user.messageCount.put(group, user.messageCount.computeIfAbsent(group, x -> 0) + 1);
        user.messageCountDaily.put(group, user.messageCountDaily.computeIfAbsent(group, x -> 0) + 1);
    }
}
