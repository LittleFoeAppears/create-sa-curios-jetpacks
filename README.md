Yes — your current README was outdated. It still described the mod like it only handled Curios jetpacks, and it had the old “if it builds but does not work” section you wanted removed.

I made an updated README for version **1.1.1**:

[Download updated README.md](sandbox:/mnt/data/README_updated.md)

I also made a full updated source ZIP with the new README already inside:

[Download updated source ZIP](sandbox:/mnt/data/create-sa-curios-jetpacks-source-1.1.1-readme-updated.zip)

Here is the updated README text:

# Create SA Curios Jetpacks

A NeoForge compatibility addon for **Create Stuff 'N Additions** and **Curios**.

This mod lets Create Stuff 'N Additions jetpacks and refill tanks work from Curios slots, while keeping normal chest-slot jetpack behavior unchanged.

## Supported versions

* Minecraft `1.21.1`
* NeoForge `21.1.x`, tested with `21.1.228`
* Curios NeoForge `9.5.1+1.21.1`
* Create Stuff 'N Additions `2.1.0e`
* Create

## Features

* Adds Curios `body` slot support for Create Stuff 'N Additions jetpacks.
* Jetpacks equipped in the Curios body slot can fly normally.
* Jetpacks equipped in the Curios body slot can refill from compatible tanks.
* Jetpack visuals render on the player when the Curios slot visibility is enabled.
* Adds Curios `belt` slot support for Create Stuff 'N Additions refill tanks.
* Belt-equipped tanks can refill jetpacks equipped in the Curios body slot.
* Belt-equipped tanks can also refill jetpacks equipped in the normal chest armor slot.
* Belt tank visuals render on the player when the Curios slot visibility is enabled.
* Adds support for refilling tanks from Create basins.
* Works in singleplayer and LAN/multiplayer when installed on both the server/host and clients.

## Supported jetpacks

* `create_sa:brass_jetpack_chestplate`
* `create_sa:andesite_jetpack_chestplate`
* `create_sa:copper_jetpack_chestplate`
* `create_sa:netherite_jetpack_chestplate`

## Supported refill tanks

* `create_sa:small_filling_tank`
* `create_sa:medium_filling_tank`
* `create_sa:large_filling_tank`
* `create_sa:small_fueling_tank`
* `create_sa:medium_fueling_tank`
* `create_sa:large_fueling_tank`
* `create_sa:creative_filling_tank`

## Tank and jetpack behavior

Filling Tanks provide water.

Fueling Tanks provide fuel/lava.

The Creative Filling Tank can refill both water and fuel, matching Create Stuff 'N Additions behavior.

Jetpacks use different resources depending on their type:

* Andesite Jetpack: fuel only
* Copper Jetpack: water only
* Brass Jetpack: fuel and water
* Netherite Jetpack: fuel and water

## Create basin support

Tanks can be refilled by right-clicking Create basins that contain the correct fluid.

* Right-click a water-filled basin with a Filling Tank to refill the tank.
* Right-click a lava-filled basin with a Fueling Tank to refill the tank.
* Refilling from a basin consumes `1000 mB` of fluid from the basin.
* The tank gains `+100` stock, matching Create Stuff 'N Additions source-block refill behavior.

Basins can still be refilled normally with buckets, pipes, or other Create systems.

## Multiplayer / LAN

This mod is required on both sides.

For LAN or multiplayer, install it on:

* the server or LAN host
* every joining client

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
build/libs/create_sa_curios_jetpacks-1.1.1.jar
```

## License

This project is open source. See `LICENSE` for details.
