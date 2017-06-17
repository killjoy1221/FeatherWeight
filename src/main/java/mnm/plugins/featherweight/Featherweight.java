package mnm.plugins.featherweight;

import static mnm.plugins.featherweight.Permissions.*;
import static org.spongepowered.api.command.args.GenericArguments.*;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

@Plugin(
        id = "featherweight",
        name = "Featherweight"
)
public class Featherweight {

    @Inject
    public Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    public ConfigurationLoader<CommentedConfigurationNode> configLoader;

    private Map<User, FlightManager> userMap = Maps.newHashMap();

    private Config config;

    @Listener
    public void preinit(GamePreInitializationEvent event) throws IOException, ObjectMappingException {

        CommentedConfigurationNode node = this.configLoader.load();

        TypeToken<Config> type = TypeToken.of(Config.class);

        this.config = node.getValue(type, new Config());
        node.setValue(type, this.config);

        this.configLoader.save(node);
    }

    @Listener
    public void init(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .permission(FLY)
                .arguments(requiringPermission(userOrSource(Text.of("user")), FLY_OTHER))
                .child(CommandSpec.builder()
                        .permission(SPEED_SET)
                        .description(Text.of("Sets speed multipliers for flying or walking"))
                        .arguments(
                                requiringPermission(userOrSource(Text.of("user")), SPEED_SET_OTHER),
                                optional(doubleNum(Text.of("speed"))))
                        .executor(this::onSpeedCommand)
                        .build(), "speed")
                .executor(this::onFlyCommand)
                .build(), "fly");
    }

    @Nonnull
    private CommandResult onFlyCommand(CommandSource commandSource, CommandContext commandContext) {
        if (!config.fly.enabled) {
            commandSource.sendMessage(Text.of(TextColors.RED, "Flight is not enabled"));
            return CommandResult.empty();
        }
        User player = commandContext.<User>getOne(Text.of("user")).orElseThrow(RuntimeException::new);
        boolean fly = getFlight(player).toggleFlight();
        Text enabled = fly ? Text.of(TextColors.GREEN, "ENABLED") : Text.of(TextColors.RED, "DISABLED");
        String forUser = player.equals(commandSource) ? "" : player.getName();
        commandSource.sendMessage(Text.of(TextColors.YELLOW, "Flight ", enabled, forUser));
        return CommandResult.success();
    }

    @Nonnull
    private CommandResult onSpeedCommand(CommandSource commandSource, CommandContext commandContext) {
        if (!config.speed.enabled) {
            commandSource.sendMessage(Text.of(TextColors.RED, "Speed is not enabled"));
            return CommandResult.empty();
        }
        User player = commandContext.<User>getOne(Text.of("user")).orElseThrow(RuntimeException::new);
        double speed = commandContext.<Double>getOne("speed").orElse(1D);

        if (speed < 0) {
            commandSource.sendMessage(Text.of("Speed cannot be negative."));
            return CommandResult.empty();
        }
        if (speed > config.speed.max) {
            if (!commandSource.hasPermission(SPEED_SET_OVER)) {
                commandSource.sendMessage(Text.of("Fly speed exceeds max of ", TextColors.DARK_RED, config.speed.max));
                return CommandResult.empty();
            } else {
                commandSource.sendMessage(Text.of(TextColors.DARK_RED, "WARNING: ", TextColors.RESET, "Speeds above max may result in rubber banding"));
            }
        }
        getFlight(player).setSpeed(speed);

        String forUser = player.equals(commandSource) ? "" : "for " + player.getName() + " ";

        commandSource.sendMessage(Text.of("Flight speed " + forUser + "set to " + speed));

        // apply the speeds if online
        player.getPlayer().ifPresent(this::activateSpeed);

        return CommandResult.success();
    }

    @Listener
    public void onChangeHeldItem(ChangeInventoryEvent.Held change, @First Player player) {
        activateSpeed(player);
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        // The text message could be configurable, check the docs on how to do so!
        activateSpeed(player);

        if (config.fly.enabled && config.fly.autofly && player.hasPermission(FLY_AUTO)) {
            getFlight(player).setFlight(true);
        }

    }

    @Listener
    public void onRespawn(RespawnPlayerEvent event) {
        getFlight(event.getTargetEntity()).flight();
        activateSpeed(event.getTargetEntity());
    }

    private void activateSpeed(Player player) {
        if (player.gameMode().get() != GameModes.SPECTATOR && player.hasPermission(SPEED)) {
            Predicate<ItemStack> matches = config.speed.item::matches;
            Optional<ItemStack> mainhand = player.getItemInHand(HandTypes.MAIN_HAND).filter(matches);
            Optional<ItemStack> offhand = player.getItemInHand(HandTypes.OFF_HAND).filter(matches);

            boolean hasFeather = mainhand.isPresent() || offhand.isPresent();

            FlightManager fly = getFlight(player);
            fly.speed(hasFeather);
        }
    }

    private FlightManager getFlight(User player) {
        return userMap.computeIfAbsent(player, FlightManager::new);
    }


}
