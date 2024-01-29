/*
 * Copyright (c) 2018 kulers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.DotsTag;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.applet.Applet;
import java.awt.Color;
import java.util.*;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import static net.runelite.api.InventoryID.EQUIPMENT;
import static net.runelite.api.InventoryID.INVENTORY;

import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
		name = "Dots",
		description = "Add the ability to tag items, objects and NPCs with a dot!",
		tags = {"highlight", "items", "overlay", "tagging", "tags", "dots"},
)
@Slf4j
public class DotsPlugin extends Plugin
{
	private static final String ITEM_KEY_PREFIX = "item_";
	private static final String TAG_KEY_PREFIX = "tag_";

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private DotsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Provides
	DotsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DotsConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		convertConfig();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	DotsTag getTag(int itemId)
	{
		String tag = configManager.getConfiguration(DotsConfig.GROUP, TAG_KEY_PREFIX + itemId);
		if (tag == null || tag.isEmpty())
		{
			return null;
		}

		return gson.fromJson(tag, DotsTag.class);
	}

	void setTag(int itemId, DotsTag dotsTag)
	{
		String json = gson.toJson(dotsTag);
		configManager.setConfiguration(DotsConfig.GROUP, TAG_KEY_PREFIX + itemId, json);
	}

	void unsetTag(int itemId)
	{
		configManager.unsetConfiguration(DotsConfig.GROUP, TAG_KEY_PREFIX + itemId);
	}

	private void convertConfig()
	{
		String migrated = configManager.getConfiguration(DotsConfig.GROUP, "migrated");
		if (!"1".equals(migrated))
		{
			return;
		}

		int removed = 0;
		List<String> keys = configManager.getConfigurationKeys(DotsConfig.GROUP + "." + ITEM_KEY_PREFIX);
		for (String key : keys)
		{
			String[] str = key.split("\\.", 2);
			if (str.length == 2)
			{
				configManager.unsetConfiguration(str[0], str[1]);
				++removed;
			}
		}

		log.debug("Removed {} old tags", removed);
		configManager.setConfiguration(DotsConfig.GROUP, "migrated", "2");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals(DotsConfig.GROUP))
		{
			overlay.invalidateCache();
		}
	}
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (!client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		if (event.getType() == MenuAction.EXAMINE_OBJECT.getId()) {
			// Check for your specific object conditions here

			// Create a new menu entry
			int idx = client.getMenuEntries().length;
			MenuEntry customEntry = client.createMenuEntry(idx)
					.setOption("Dots") // Set your custom option text here
					.setTarget(event.getTarget())
					.setParam0(event.getActionParam0())
					.setParam1(event.getActionParam1())
					.setIdentifier(event.getIdentifier())
					.setType(MenuAction.RUNELITE); // Set the custom action type here

			// Define what happens when your custom menu option is clicked
			customEntry.onClick(e -> {
				// Logic for handling the custom menu option click
				performCustomAction(e.getIdentifier(), e.getParam0(), e.getParam1());
			});
		}
	}

	private void performCustomAction(int identifier, int param0, int param1) {
		// Implement the logic for your custom action here
		// You can use identifier, param0, and param1 to identify and interact with the specific game object
	}


	@Subscribe
	public void onMenuOpened(final MenuOpened event)
	{
		if (!client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		final MenuEntry[] entries2 = event.getMenuEntries();
		processInventoryTags(entries2, client);
	}



	private void processInventoryTags(MenuEntry[] entries, Client client) {
		for (int idx = entries.length - 1; idx >= 0; --idx)
		{
			final MenuEntry entry = entries[idx];
			final Widget w = entry.getWidget();

			if (w != null && WidgetUtil.componentToInterface(w.getId()) == InterfaceID.INVENTORY
					&& "Examine".equals(entry.getOption()) && entry.getIdentifier() == 10)
			{
				final int itemId = w.getItemId();
				final DotsTag dotsTag = getTag(itemId);

				final MenuEntry parent = client.createMenuEntry(idx)
						.setOption(ColorUtil.prependColorTag("Dots", new Color(255,0,255)))
						.setTarget(entry.getTarget())
						.setType(MenuAction.RUNELITE_SUBMENU);

				Set<Color> invEquipmentColors = new HashSet<>();
				invEquipmentColors.addAll(getColorsFromItemContainer(INVENTORY));
				invEquipmentColors.addAll(getColorsFromItemContainer(EQUIPMENT));
				for (Color color : invEquipmentColors)
				{
					if (dotsTag == null || !dotsTag.color.equals(color))
					{
						client.createMenuEntry(idx)
								.setOption(ColorUtil.prependColorTag("Color", color))
								.setType(MenuAction.RUNELITE)
								.setParent(parent)
								.onClick(e ->
								{
									DotsTag t = new DotsTag();
									t.color = color;
									setTag(itemId, t);
								});
					}
				}

				client.createMenuEntry(idx)
						.setOption("Set Color")
						.setType(MenuAction.RUNELITE)
						.setParent(parent)
						.onClick(e ->
						{
							Color color = dotsTag == null ? Color.WHITE : dotsTag.color;
							SwingUtilities.invokeLater(() ->
							{
								RuneliteColorPicker colorPicker = colorPickerManager.create(SwingUtilities.windowForComponent((Applet) client),
										color, "Select a Dot Color", true);
								colorPicker.setOnClose(c ->
								{
									DotsTag t = new DotsTag();
									t.color = c;
									setTag(itemId, t);
								});
								colorPicker.setVisible(true);
							});
						});

				if (dotsTag != null)
				{
					client.createMenuEntry(idx)
							.setOption("Reset")
							.setType(MenuAction.RUNELITE)
							.setParent(parent)
							.onClick(e -> unsetTag(itemId));
				}
			}
		}
		}

	private List<Color> getColorsFromItemContainer(InventoryID inventoryID)
	{
		List<Color> colors = new ArrayList<>();
		ItemContainer container = client.getItemContainer(inventoryID);
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				DotsTag dotsTag = getTag(item.getId());
				if (dotsTag != null && dotsTag.color != null)
				{
					if (!colors.contains(dotsTag.color))
					{
						colors.add(dotsTag.color);
					}
				}
			}
		}
		return colors;
	}
}
