package com.ImprovedInventoryTag;

import net.runelite.client.config.*;

@ConfigGroup(DotsConfig.GROUP)
public interface DotsConfig extends Config
{
	String GROUP = "inventorytags";

	@ConfigSection(
			name = "Dots Configuration",
			description = "Tag stuff with dots!",
			position = 0
	)
	String tagStyleSection = "tagStyleSection";

	@ConfigItem(
			position = 0, // Adjust position as needed
			keyName = "showTagDot",
			name = "On/Off",
			description = "Configures whether or not a small dot should be shown at the center of item tags",
			section = tagStyleSection
	)
	default boolean showTagDot()
	{
		return false;
	}

	@Range(
			min = 1,
			max = 10 // Set a maximum value as per your requirements
	)
	@ConfigItem(
			position = 1, // Adjust position as needed
			keyName = "dotSize",
			name = "Dot Size",
			description = "Configures the size of the dot shown at the center of item tags",
			section = tagStyleSection
	)
	default int dotSize()
	{
		return 3; // Default value
	}
}
