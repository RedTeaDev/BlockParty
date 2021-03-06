package de.leonkoth.blockparty.listener;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.arena.Arena;
import de.leonkoth.blockparty.event.BlockPickEvent;
import de.leonkoth.blockparty.player.PlayerInfo;
import de.leonkoth.blockparty.util.ColorBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static de.leonkoth.blockparty.locale.BlockPartyLocale.NEXT_BLOCK;
import static de.leonkoth.blockparty.locale.BlockPartyLocale.PREFIX;

public class BlockPickListener implements Listener {

    private BlockParty blockParty;

    public BlockPickListener(BlockParty blockParty) {
        this.blockParty = blockParty;

        Bukkit.getPluginManager().registerEvents(this, blockParty.getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPick(BlockPickEvent event) {
        Arena arena = event.getArena();
        String name = ColorBlock.get(event.getAnnouncedBlock()).getName();
        arena.broadcast(PREFIX, NEXT_BLOCK, true, (PlayerInfo[]) null, "%BLOCK%", name);
    }

}
