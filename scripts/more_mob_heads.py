from glob import iglob
from json import load, dumps
from os.path import abspath
from sys import argv, exit
from uuid import UUID

from nbtlib import parse_nbt


def array_to_uuid(arr):
    arr = [i.to_bytes(4, byteorder='big', signed=True) for i in arr]
    raw_uuid = b''.join(arr)
    return UUID(bytes=raw_uuid)


def run():
    if len(argv) < 2:
        print("Need to specify a directory containing entity loot tables")
        exit(1)
        return
    print(f"Root directory for loot tables: {abspath(argv[1])}")

    missing = []
    heads = []
    for filename in iglob(abspath(argv[1]) + '/**/*.json', recursive=True):
        done = False
        with open(filename, "r") as file:
            json = load(file)
            loot_table_name = filename.split("/")[-1]
            if "sheep" in filename:
                loot_table_name = "/".join(filename.split("/")[-2:])
            if "pools" in json:
                for pool in json["pools"]:
                    for entry in pool["entries"]:
                        if "item" == entry["type"] and "minecraft:player_head" == entry["name"]:
                            parse_head(entry, pool["conditions"], loot_table_name, heads)
                            done = True
                        elif "minecraft:alternatives" == entry["type"] and "children" in entry:
                            for entry1 in entry["children"]:
                                if "item" == entry1["type"] and "minecraft:player_head" == entry1["name"]:
                                    conditions = entry["conditions"]
                                    if "conditions" in entry1:
                                        conditions += entry1["conditions"]
                                    parse_head(entry1, conditions, loot_table_name, heads)
                                    done = True

        if not done and not (filename.endswith("shulker.json") or filename.endswith("ender_dragon.json")):
            missing.append(filename)

    if len(missing):
        print("==== Missing ====")
        print("\n".join(sorted(missing)))
        return

    with open("../vanilla-tweaks-bukkit/src/main/resources/data/more_mob_heads.json", "w") as out:
        out.write(dumps(heads, ensure_ascii=False, indent=2))


    # print("\n".join([str(head["needsPlayer"]) for head in heads]))


def parse_head(entry, conditions, table_name, heads):
    nbt = parse_nbt(entry["functions"][0]["tag"])

    needs_player = False
    requires_customization = table_name == "wither.json"
    chance = 1.0
    looting_multiplier = 0.0
    for condition in conditions:
        if "killed_by_player" == condition["condition"]:
            needs_player = True
        elif "entity_properties" == condition["condition"]:
            requires_customization = True
        elif "random_chance_with_looting" == condition["condition"]:
            chance = float(condition["chance"])
            looting_multiplier = float(condition["looting_multiplier"])
        elif "alternative" == condition["condition"]:
            requires_customization = True
            for alt in condition["terms"]:
                if alt["condition"] != "entity_properties":
                    print(f"Unhandled alternatives condition: {alt['condition']} on {table_name}")
        elif "inverted" == condition["condition"]:
            requires_customization = True
            if condition["term"]["condition"] != "entity_properties":
                print(f"Unhandled inverted condition: {condition['term']['condition']} on {table_name}")
        else:
            print(f"Unhandled condition: {condition['condition']} on {table_name}")

    heads.append({
        "tableName": table_name,
        "uuid": str(array_to_uuid(nbt["SkullOwner"]["Id"])),
        "name": str(nbt["SkullOwner"]["Name"]),
        "texture": str(nbt["SkullOwner"]["Properties"]["textures"][0]["Value"]),
        "needsPlayer": needs_player,
        "requiresCustomization": requires_customization,
        "chance": chance,
        "lootingMultiplier": looting_multiplier
    })


if __name__ == "__main__":
    run()