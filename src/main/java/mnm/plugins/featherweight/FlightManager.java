package mnm.plugins.featherweight;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;

public class FlightManager {

    private static final double FLY_SPEED = 0.05D;

    private final User user;

    private boolean flightEnabled;
    private double speed = 2;

    private boolean goingFast;

    FlightManager(User user) {
        this.user = user;
    }

    void speed(boolean speeding) {
        user.getPlayer().ifPresent(player -> {
            if (speeding) {
                // gotta go fast
                goingFast = player.offer(Keys.FLYING_SPEED, FLY_SPEED * speed).isSuccessful();

            } else if (goingFast) {
                // You're too slow!
                goingFast = !player.offer(Keys.FLYING_SPEED, FLY_SPEED).isSuccessful();
            }
        });
    }

    void flight() {
        user.getPlayer().ifPresent(player -> {
            player.offer(Keys.CAN_FLY, flightEnabled);
            // stop flying
            if (player.get(Keys.IS_FLYING).orElse(false)) {
                player.offer(Keys.IS_FLYING, false);
            }
        });
    }

    void setSpeed(double speed) {
        this.speed = speed;
    }

    void setFlight(boolean b) {
        this.flightEnabled = true;
        flight();
    }

    boolean toggleFlight() {
        this.flightEnabled ^= true;
        flight();
        return this.flightEnabled;
    }

    public boolean isGoingFast() {
        return goingFast;
    }
}
