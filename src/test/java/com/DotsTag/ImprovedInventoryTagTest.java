package com.DotsTag;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ImprovedInventoryTagTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DotsPlugin.class);
		RuneLite.main(args);
	}
}