package com.ImprovedInventoryTag;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ImprovedInventoryTagTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InventoryTagsPlugin.class);
		RuneLite.main(args);
	}
}