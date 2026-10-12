# Echoes of the Canopy (Forge 1.20.1, Mojang mappings)

Drop `src/` into a Forge 1.20.1 MDK (set `mod_id=echoesofthecanopy`, group `com.example.echoesofthecanopy`).
NOT compiled or run-tested in the authoring environment -- build it and report any mapping drift.

## Datagen (required once)
In `build.gradle` (MDK default, make sure it is present):

    sourceSets.main.resources { srcDir 'src/generated/resources' }
    runs { data {
        workingDirectory project.file('run')
        args '--mod', mod_id, '--all', '--output', file('src/generated/resources/'),
             '--existing', file('src/main/resources/')
        mods { "${mod_id}" { source sourceSets.main } }
    } }

Run `./gradlew runData`: generates blockstate, block/item models, lang, loot table, recipe.
Hand-made assets: `mods.toml`, `pack.mcmeta`, and two PLACEHOLDER 16x16 textures (replace them).

## Controls
Right-click: fire hook. Right-click again: retract. While pulled: Jump or Sneak releases.
