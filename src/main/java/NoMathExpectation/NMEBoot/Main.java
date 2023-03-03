package NoMathExpectation.NMEBoot;

import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardPool;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardSystemData;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardUser;
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.ItemPool;
import NoMathExpectation.NMEBoot.commandSystem.ExecuteCenter;
import NoMathExpectation.NMEBoot.commandSystem.NormalUser;
import NoMathExpectation.NMEBoot.commandSystem.NormalUserStats;
import NoMathExpectation.NMEBoot.commandSystem.services.ARG2023;
import NoMathExpectation.NMEBoot.commandSystem.services.General;
import NoMathExpectation.NMEBoot.commandSystem.services.RDLoungeIntegrated;
import NoMathExpectation.NMEBoot.commands.InterceptingCommandCall;
import NoMathExpectation.NMEBoot.commands.SimpleCommandsKt;
import NoMathExpectation.NMEBoot.inventory.ItemLibraryKt;
import NoMathExpectation.NMEBoot.inventory.card.CardRepository;
import NoMathExpectation.NMEBoot.inventory.temporal.DataTransfer;
import NoMathExpectation.NMEBoot.naptcha.CaptchaDispatcher;
import NoMathExpectation.NMEBoot.naptcha.captchas.*;
import NoMathExpectation.NMEBoot.sending.InspectingSendEventsKt;
import NoMathExpectation.NMEBoot.utils.*;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Main extends JavaPlugin {
    public static final Main INSTANCE = new Main();
    public static ExecuteCenter executeCenter;
    public static CaptchaDispatcher naptcha;
    public static WordleMirai wordle = new WordleMirai(new File("config/NoMathExpectation.NMEBoot/wordle.txt"), 6, 25);

    private Main() {
        super(new JvmPluginDescriptionBuilder("NoMathExpectation.NMEBoot", "1.3.0-beta25-2023030402")
                .name("NMEBoot")
                .author("NoMathExpectation")
                .build());
        executeCenter = new ExecuteCenter()
                .register(new General())
                .register(new RDLoungeIntegrated())
                .register(ARG2023.INSTANCE);
        naptcha = new CaptchaDispatcher()
                .register(new NumberSort(5, -100, 100))
                //.register(new SumChatNumbers(5))
                .register(new ReversedNumberSort(5, -100, 100))
                .register(new LastSamuraiWord())
                .register(new OttoColor())
                .register(new CheckinTimes())
                .register(new CardClass())
                .register(new TwoOneFunction(-10, 10));
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage pcs) {
        pcs.contributeCommandCallInterceptorProvider(InterceptingCommandCall.INSTANCE);
    }

    @Override
    public void onEnable() {
        executeCenter.loadSamurai();

        //加载General
        Map<String, Double> generalProbability = new HashMap<>();
        generalProbability.put("R", 90d);
        generalProbability.put("SR", 9d);
        generalProbability.put("UR", 1d);
        generalProbability.put("DC", 0.5d);
        generalProbability.put("ALT", 0.5d);
        generalProbability.put("C", 1d);
        new CardPool("general", "标准池", 64800L, false, generalProbability).loadFromFile("config/NoMathExpectation.NMEBoot/cards/general");

        //加载levelCards
        new CardPool("levels", "关卡池", 86400L, false, null).loadFromFile("config/NoMathExpectation.NMEBoot/cards/rdlevels");

        //加载box
        CardPool box;
        if (CardSystemData.INSTANCE.getBox().size() == 0) {
            box = new CardPool("box", "交换盒", 0L, true);
        } else {
            int boxSize = CardSystemData.INSTANCE.getBox().size();
            byte[] boxByte = new byte[boxSize];
            for (int i = 0; i < boxSize; i++) {
                boxByte[i] = CardSystemData.INSTANCE.getBox().get(i);
            }
            try {
                box = CardPool.SERIALIZER.deserialize(boxByte);
                CardPool.addPool(box);
            } catch (Exception e) {
                getLogger().error("加载交换盒失败！");
                throw new RuntimeException(e);
            }
        }

        //加载ground
        new ItemPool("ground", "地面", 0, true);

        //加载用户
        for (byte[] userBytes : NormalUserStats.INSTANCE.getUser()) {
            try {
                NormalUser.addUser(NormalUser.SERIALIZER.deserialize(userBytes));
            } catch (Exception e) {
                getLogger().error("加载某用户失败!");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        for (byte[] userBytes : CardSystemData.INSTANCE.getUser()) {
            try {
                CardUser.addUser(CardUser.SERIALIZER.deserialize(userBytes));
            } catch (Exception e) {
                getLogger().error("加载某用户失败!");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        //新物品系统
        ItemLibraryKt.registerAllItems();

        CardRepository.reloadRepositories();

        PermissionsKt.registerPermissions();

        SimpleCommandsKt.registerCommands();
        CommandManager.INSTANCE.registerCommand(DataTransfer.INSTANCE, false); //temporal

        RecentActiveContact.INSTANCE.startListening();

        //注册监听
        InspectingSendEventsKt.inspectSendEvents();

        DatabaseConfig.INSTANCE.load();

        //GlobalEventChannel.INSTANCE.parentScope(this).registerListenerHost(executeCenter);

        MessageHistory.INSTANCE.recordStart();
        MessageHistoryKt.nudgeForRandomMessage();
        Repeat.INSTANCE.startMonitor();

        //自动保存
        Thread t = new Thread(this::autoSave);
        t.setDaemon(true);
        t.start();

        //end
        getLogger().info("NMEBoot已加载。");
    }

    @Override
    public void onDisable() {
        RDLoungeIntegrated.AUCTION_CENTER.stop();
        KtorClientKt.getKtorClient().close();
        save();
        getLogger().info("NMEBoot已停用。");
    }

    public void autoSave() {
        try {
            Thread.sleep(120000);
        } catch (InterruptedException ignored) {}

        getLogger().verbose("自动保存开始");
        save();
        getLogger().verbose("自动保存结束");

        Thread t = new Thread(this::autoSave);
        t.setDaemon(true);
        t.start();
    }

    public void save() {
        //保存用户
        Set<byte[]> userData = new HashSet<>();
        for (CardUser user : CardUser.getUsers().values()) {
            try {
                userData.add(CardUser.SERIALIZER.serialize(user));
            } catch (IOException e) {
                getLogger().error("保存用户:" + user.showSimple(true) + ")失败！");
                e.printStackTrace();
            }
        }
        CardSystemData.INSTANCE.setUser(userData);

        userData = new HashSet<>();
        for (NormalUser user : NormalUser.getUsers().values()) {
            try {
                userData.add(NormalUser.SERIALIZER.serialize(user));
            } catch (IOException e) {
                getLogger().error("保存用户:" + user.showSimple(true) + "失败！");
                e.printStackTrace();
            }
        }
        NormalUserStats.INSTANCE.setCheckInCount(NormalUser.getCheckInCount());
        NormalUserStats.INSTANCE.setCheckInDate(NormalUser.getCheckInDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        NormalUserStats.INSTANCE.setMessageCountDate(NormalUser.getMessageCountDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        NormalUserStats.INSTANCE.setUser(userData);

        //保存box
        try {
            byte[] boxByte = CardPool.SERIALIZER.serialize(CardPool.getPools().get("box"));
            List<Byte> byteList = new ArrayList<>();
            for (byte b : boxByte) {
                byteList.add(b);
            }
            CardSystemData.INSTANCE.setBox(byteList);
        } catch (IOException e) {
            getLogger().error("保存交换盒失败！");
            e.printStackTrace();
        }
    }
}