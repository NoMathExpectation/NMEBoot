package NoMathExpectation.NMEBoot.commandSystem;

import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

public interface Executable {
    @NotNull
    MessageChainBuilder appendHelp(@NotNull MessageChainBuilder mcb, @NotNull MessageEvent e);

    boolean onMessage(@NotNull MessageEvent e, @NotNull NormalUser user, @NotNull String command, @NotNull String miraiCommand) throws Exception;
}
