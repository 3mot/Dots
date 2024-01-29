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
package com.ImprovedInventoryTag;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

class DotsOverlay extends WidgetItemOverlay
{
	private final ItemManager itemManager;
	private final DotsPlugin plugin;
	private final DotsConfig config;
	private final Cache<Long, Image> fillCache;
	private final Cache<Integer, DotsTag> tagCache;
	private final DotsTag NONE = new DotsTag();

	@Inject
	private DotsOverlay(ItemManager itemManager, DotsPlugin plugin, DotsConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnEquipment();
		showOnInventory();
		showOnInterfaces(
				InterfaceID.CHAMBERS_OF_XERIC_INVENTORY,
				InterfaceID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE,
				InterfaceID.CHAMBERS_OF_XERIC_STORAGE_UNIT_SHARED,
				InterfaceID.GRAVESTONE
		);
		fillCache = CacheBuilder.newBuilder()
				.concurrencyLevel(1)
				.maximumSize(32)
				.build();
		tagCache = CacheBuilder.newBuilder()
				.concurrencyLevel(1)
				.maximumSize(39)
				.build();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		final DotsTag dotsTag = getTag(itemId);
		if (dotsTag == null || dotsTag.color == null)
		{
			return;
		}

		final Color color = dotsTag.color;

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (config.showTagDot())
		{
			int centerX = (int) bounds.getX() + (int) bounds.getWidth() / 2;
			int centerY = (int) bounds.getY() + (int) bounds.getHeight() / 2;
			int dotSize = config.dotSize();
			graphics.setColor(color);
			graphics.fillRect(centerX - dotSize / 2 - 5, centerY - dotSize / 2, dotSize, dotSize);
		}
	}

	private DotsTag getTag(int itemId)
	{
		DotsTag dotsTag = tagCache.getIfPresent(itemId);
		if (dotsTag == null)
		{
			dotsTag = plugin.getTag(itemId);
			if (dotsTag == null)
			{
				tagCache.put(itemId, NONE);
				return null;
			}

			if (dotsTag == NONE)
			{
				return null;
			}

			tagCache.put(itemId, dotsTag);
		}
		return dotsTag;
	}

	void invalidateCache()
	{
		fillCache.invalidateAll();
		tagCache.invalidateAll();
	}
}
