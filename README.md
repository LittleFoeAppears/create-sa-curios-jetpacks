# Create SA Curios Jetpacks

A NeoForge compatibility addon for **Create Stuff 'N Additions** and **Curios**.

This mod lets Create Stuff 'N Additions jetpacks and refill tanks work from Curios slots, while keeping normal chest-slot jetpack behavior available.

## Supported versions

- Minecraft `1.21.1`
- NeoForge `21.1.x`, tested with `21.1.228`
- Curios NeoForge `9.5.1+1.21.1`
- Create Stuff 'N Additions `2.1.0e`
- Create

## Features

### Jetpacks

- Adds Curios `body` slot support for Create Stuff 'N Additions jetpacks.
- Jetpacks equipped in the Curios body slot can fly normally.
- Jetpacks equipped in the Curios body slot can refill from compatible tanks.
- Jetpack visuals render on the player when the Curios slot visibility is enabled.
- Optional armor stat support for Curios-equipped jetpacks.
- In-game config screen for Curios jetpack armor behavior.

### Refill tanks

- Adds Curios `belt` slot support for Create Stuff 'N Additions refill tanks.
- Belt-equipped tanks can refill jetpacks equipped in the Curios body slot.
- Belt-equipped tanks can also refill jetpacks equipped in the normal chest armor slot.
- Belt tank visuals render on the player when the Curios slot visibility is enabled.

### Create basin support

- Tanks can be refilled by right-clicking Create basins filled with the correct fluid.
- Water-filled basins refill Filling Tanks.
- Lava-filled basins refill Fueling Tanks.
- Refilling from a basin consumes `1000 mB` of fluid from the basin.
- The tank gains `+100` stock, matching Create Stuff 'N Additions source-block refill behavior.

Basins can still be refilled normally with buckets, pipes, or other Create systems.

## Armor stat config

Curios-equipped jetpacks can optionally provide their base chestplate armor stats.

The config screen can be opened from:

```text
Mods -> Create SA Curios Jetpacks -> Config
```

Available options:

- **Curios Jetpack Armor Stats**
  - Enables or disables armor stats from Curios-equipped jetpacks.

- **Chest Armor Interaction**
  - **Chest Slot Empty Only**: Curios jetpack armor stats only apply when the normal chest armor slot is empty.
  - **Stack With Chest Armor**: Curios jetpack armor stats stack with armor worn in the normal chest slot.

### Armor stat limitation

This feature applies base armor attributes only:

- armor
- armor toughness
- knockback resistance

Curios-equipped jetpacks are **not** treated as full vanilla chest armor. Enchantments and modded armor hooks may not behave exactly like they do when the jetpack is equipped in the normal chest armor slot.

## Supported jetpacks

- `create_sa:brass_jetpack_chestplate`
- `create_sa:andesite_jetpack_chestplate`
- `create_sa:copper_jetpack_chestplate`
- `create_sa:netherite_jetpack_chestplate`

## Supported refill tanks

- `create_sa:small_filling_tank`
- `create_sa:medium_filling_tank`
- `create_sa:large_filling_tank`
- `create_sa:small_fueling_tank`
- `create_sa:medium_fueling_tank`
- `create_sa:large_fueling_tank`
- `create_sa:creative_filling_tank`

## Tank and jetpack behavior

Filling Tanks provide water.

Fueling Tanks provide fuel/lava.

The Creative Filling Tank can refill both water and fuel, matching Create Stuff 'N Additions behavior.

Jetpacks use different resources depending on their type:

- Andesite Jetpack: fuel only
- Copper Jetpack: water only
- Brass Jetpack: fuel and water
- Netherite Jetpack: fuel and water

## Multiplayer / LAN

This mod is required on both sides.

For LAN or multiplayer, install it on:

- the server or LAN host
- every joining client

## Building from source

Install JDK 21, then build with Gradle:

```bash
gradle build
```

If you use the Gradle wrapper, run one of these instead:

```bash
./gradlew build
```

```bat
gradlew.bat build
```

The built mod jar will appear in:

```text
build/libs/create_sa_curios_jetpacks-1.2.18.jar
```

## License

This project is open source. See `LICENSE` for details.
