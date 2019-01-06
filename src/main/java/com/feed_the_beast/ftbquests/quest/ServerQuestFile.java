package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import com.feed_the_beast.ftbquests.net.edit.MessageDeleteObjectResponse;
import com.feed_the_beast.ftbquests.util.FTBQuestsTeamData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author LatvianModder
 */
public class ServerQuestFile extends QuestFile
{
	public static ServerQuestFile INSTANCE;

	public final Universe universe;
	public final File file;
	public boolean shouldSave = false;
	private boolean isLoading = false;

	public ServerQuestFile(Universe u, File f)
	{
		universe = u;
		file = f;
	}

	public boolean load()
	{
		if (!file.exists())
		{
			NBTUtils.writeNBT(file, new NBTTagCompound());
		}

		NBTTagCompound nbt = NBTUtils.readNBT(file);

		if (nbt == null)
		{
			return false;
		}

		isLoading = true;
		readDataFull(nbt);
		isLoading = false;
		return true;
	}

	public boolean importBetterQuestingQuests(MinecraftServer server)
	{
		// Load Better Questing databases and import into FTBQ
		return true;
	}

	@Override
	public boolean isClient()
	{
		return false;
	}

	@Override
	public boolean isLoading()
	{
		return isLoading;
	}

	@Nullable
	@Override
	public ITeamData getData(short team)
	{
		if (team == 0)
		{
			return null;
		}

		ForgeTeam t = universe.getTeam(team);
		return t.isValid() ? FTBQuestsTeamData.get(t) : null;
	}

	@Nullable
	@Override
	public ITeamData getData(String team)
	{
		if (team.isEmpty())
		{
			return null;
		}

		ForgeTeam t = universe.getTeam(team);
		return t.isValid() ? FTBQuestsTeamData.get(t) : null;
	}

	@Override
	public Collection<FTBQuestsTeamData> getAllData()
	{
		Collection<ForgeTeam> teams = universe.getTeams();
		List<FTBQuestsTeamData> list = new ArrayList<>(teams.size());

		for (ForgeTeam team : teams)
		{
			if (team.isValid())
			{
				list.add(FTBQuestsTeamData.get(team));
			}
		}

		return list;
	}

	@Override
	public void deleteObject(int id)
	{
		QuestObjectBase object = getBase(id);

		if (object != null)
		{
			object.deleteChildren();
			object.deleteSelf();
			clearCachedData();
			save();
		}

		new MessageDeleteObjectResponse(id).sendToAll();
	}

	public void save()
	{
		shouldSave = true;
		universe.markDirty();
	}

	public void saveNow()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writeDataFull(nbt);
		NBTUtils.writeNBTSafe(file, nbt);
	}

	public void unload()
	{
		if (shouldSave)
		{
			saveNow();
			shouldSave = false;
		}

		deleteChildren();
		deleteSelf();
	}
}