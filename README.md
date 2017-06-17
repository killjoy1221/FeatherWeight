# FeatherWeight
A sponge plugin for controlling flight and speed

## Usage
Use **/fly** to toggle flight. While flying, hold a **feather** (configurable) to increase your speed. 

## Commands
* **/fly [user]** - Toggles flight for a user (or sender if  user is not present)
* **/fly speed [user] [speed]** - Sets the flight speed modifier for a user. If *user* is not present, the sender is used. If *speed* is not present, 1 is used.

## Permissions
* **fetherweight.fly** Allows access the /fly command
* **fetherweight.fly.auto** Allows auto-enabling flight on join
* **fetherweight.fly.other** Allows setting someone else's flight setting
* **fetherweight.speed** Allows use of increased flight speed
* **featherweight.speed.set** Allows the setting of the flight speed modifier
* **fetherwegith.speed.set.other** Allows setting of someone else's flight speed modifier
* **featherweight.speed.set.over** Allows setting the speed modifier above the max. ***WARNING:** Setting speed too high may cause undesired movement behavior.*

## Config
**Location:** `config/featherweight.conf`

### Default config
```
fly {
    # Enables auto-enabling of flight on join
    autofly=false
    # Enables the toggling of flight. If disabled, flight will only be available in the proper gamemodes.
    enabled=true
}
speed {
    # Enables fast flying with an item
    enabled=true
    # When this item is held, fast flying will be on
    item="minecraft:feather"
    # The max speed normal players are limited to. Admins can go above this.
    max=15.0
}
```