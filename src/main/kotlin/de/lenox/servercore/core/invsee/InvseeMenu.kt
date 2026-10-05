package de.lenox.servercore.core.invsee

import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.TooltipDisplay

/**
 * The chest GUI of `/invsee`: a 6 row chest that shows the whole inventory of [target] and edits it live.
 *
 * ```
 * row 0:  ▪  head  chest  legs  feet  ▪  offhand  body  saddle
 * row 1:  main inventory
 * row 2:  main inventory
 * row 3:  main inventory
 * row 4:  ▪  ▪  ▪  ▪  ▪  ▪  ▪  ▪  ▪
 * row 5:  hotbar
 * ```
 *
 * Main inventory above, hotbar below and the armor top to bottom, like in the vanilla inventory screen. `▪` is a
 * filler that can't be touched. The slots read from and write to the target's [Inventory] directly, so changes show
 * up for both sides without any syncing.
 */
class InvseeMenu(containerId: Int, viewer: Inventory, private val view: View) :
	ChestMenu(MenuType.GENERIC_9x6, containerId, viewer, view, ROWS) {

	constructor(containerId: Int, viewer: Inventory, target: ServerPlayer) : this(containerId, viewer, View(target))

	/** Filler slots can't be picked up, swapped, thrown, cloned or collected by a double click. */
	override fun clicked(slotId: Int, buttonNum: Int, input: ContainerInput, player: Player) {
		if (slotId in 0 until SIZE && view.isFiller(slotId)) return
		super.clicked(slotId, buttonNum, input, player)
	}

	override fun canTakeItemForPickAll(stack: ItemStack, slot: Slot): Boolean =
		(slot.container !== view || !view.isFiller(slot.containerSlot)) && super.canTakeItemForPickAll(stack, slot)

	/**
	 * Shift click moves items out of the target's inventory into the viewer's, and from the viewer's into the
	 * target's main inventory or hotbar, never into an armor slot or a filler.
	 */
	override fun quickMoveStack(player: Player, index: Int): ItemStack {
		if (index < SIZE) return super.quickMoveStack(player, index)

		val slot = slots[index]
		if (!slot.hasItem()) return ItemStack.EMPTY
		val stack = slot.item
		val original = stack.copy()
		if (!moveItemStackTo(stack, MAIN_START, MAIN_END, false) && !moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
			return ItemStack.EMPTY
		}
		if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
		return original
	}

	/** Maps the chest slots to the inventory slots of [target], see [InvseeMenu] for the layout. */
	class View(private val target: ServerPlayer) : Container {
		private val inventory = target.inventory

		override fun getContainerSize() = SIZE

		override fun isEmpty() = layout.indices.all { getItem(it).isEmpty }

		override fun getItem(slot: Int): ItemStack {
			val index = layout[slot]
			return if (index == FILLER) filler else inventory.getItem(index)
		}

		override fun removeItem(slot: Int, count: Int): ItemStack {
			val index = layout[slot]
			return if (index == FILLER) ItemStack.EMPTY else inventory.removeItem(index, count)
		}

		override fun removeItemNoUpdate(slot: Int): ItemStack {
			val index = layout[slot]
			return if (index == FILLER) ItemStack.EMPTY else inventory.removeItemNoUpdate(index)
		}

		override fun setItem(slot: Int, stack: ItemStack) {
			val index = layout[slot]
			if (index != FILLER) inventory.setItem(index, stack)
		}

		override fun canPlaceItem(slot: Int, stack: ItemStack) = layout[slot] != FILLER

		override fun setChanged() = inventory.setChanged()

		/** Closes when the target leaves, dies or changes dimension, as then their inventory is no longer this one. */
		override fun stillValid(player: Player) = target.level().server.playerList.getPlayer(target.uuid) === target

		override fun clearContent() {
			layout.forEachIndexed { slot, index -> if (index != FILLER) inventory.setItem(index, ItemStack.EMPTY) }
		}

		fun isFiller(slot: Int) = layout[slot] == FILLER
	}

	private companion object {
		const val ROWS = 6
		const val SIZE = ROWS * 9

		const val MAIN_START = 9
		const val MAIN_END = 36
		const val HOTBAR_START = 45
		const val HOTBAR_END = 54

		/** Marks a chest slot that shows no inventory slot. */
		const val FILLER = -1

		/** The inventory slot (see [Inventory]) shown in each chest slot, or [FILLER]. */
		val layout: IntArray = IntArray(SIZE) { FILLER }.also { layout ->
			// Armor head to feet (inventory slots 39..36), offhand, body armor and saddle.
			layout[1] = 39
			layout[2] = 38
			layout[3] = 37
			layout[4] = 36
			layout[6] = Inventory.SLOT_OFFHAND
			layout[7] = Inventory.SLOT_BODY_ARMOR
			layout[8] = Inventory.SLOT_SADDLE
			// Main inventory (inventory slots 9..35) in rows 1 to 3, hotbar (0..8) in row 5.
			for (i in 0 until 27) layout[MAIN_START + i] = 9 + i
			for (i in 0 until 9) layout[HOTBAR_START + i] = i
		}

		val filler: ItemStack = ItemStack(Items.STAINED_GLASS_PANE.gray()).apply {
			set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, linkedSetOf()))
		}
	}
}
