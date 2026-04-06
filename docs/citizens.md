# Citizens Integration

If you use [Citizens](https://wiki.citizensnpcs.co/), you can link your existing Citizens NPCs to SovereignTraders catalogs. Players right-click the Citizens NPC and the shop opens.

> Citizens is **not required**. SovereignTraders has its own vendor NPC system. This is only for servers that already use Citizens and want to keep their existing NPCs.

## Linking a Citizens NPC

1. Look at your Citizens NPC (within 5 blocks)
2. Run:

```
/sovereign citizen link <catalog>
```

Example:

```
/sovereign citizen link weapons
```

Now right-clicking that Citizens NPC opens the `weapons` catalog.

## Unlinking

Look at the NPC and run:

```
/sovereign citizen unlink
```

## Listing all links

```
/sovereign citizen list
```

Shows all Citizens NPC IDs and which catalogs they're linked to.

## How it works

SovereignTraders listens for Citizens `NPCRightClickEvent`. When a linked NPC is right-clicked, it cancels the Citizens interaction and opens the catalog instead.

Links are saved in `plugins/SovereignTraders/citizens-bridge.yml` and persist across restarts.
