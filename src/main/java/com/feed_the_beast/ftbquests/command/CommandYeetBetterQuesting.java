package com.feed_the_beast.ftbquests.command;

import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import com.feed_the_beast.ftbquests.net.MessageSyncEditingMode;
import com.feed_the_beast.ftbquests.quest.BetterQuestingImporter;
import com.feed_the_beast.ftbquests.quest.ServerQuestFile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CommandYeetBetterQuesting extends CommandBase
{
	@Override
	public String getName()
	{
		return "yeet_better_questing";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "commands.ftbquests.yeet_better_questing.usage";
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 2;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return server.isSinglePlayer() || super.checkPermission(server, sender);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
	{
		return Collections.emptyList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		boolean success = new BetterQuestingImporter().ImportBetterQuestingQuests(server);

		if (success)
		{
			sender.sendMessage(new TextComponentTranslation("commands.ftbquests.editing_mode.imported"));
		}
		else
		{
			sender.sendMessage(new TextComponentTranslation("commands.ftbquests.editing_mode.failed"));
		}
	}
}