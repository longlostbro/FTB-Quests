package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftbquests.quest.reward.ItemReward;
import com.feed_the_beast.ftbquests.quest.task.FTBQuestsTasks;
import com.feed_the_beast.ftbquests.quest.task.ItemTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskType;
import com.google.gson.*;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

public class BetterQuestingImporter {

    private MinecraftServer _server;
    public boolean ImportBetterQuestingQuests(MinecraftServer server)
    {
        _server = server;
        // Load Better Questing databases and import into FTBQ
        String defaultDir = "config/betterquesting/";
        File curWorldDir = server.getFile(server.getFolderName());

        File fileDatabase = new File(defaultDir, "QuestDatabase.json");
        File fileProgress = new File(defaultDir, "QuestProgress.json");
        File fileParties = new File(defaultDir, "QuestingParties.json");
        File fileLives = new File(defaultDir, "LifeDatabase.json");
        File fileNames = new File(defaultDir, "NameCache.json");
        if(new File(curWorldDir, "QuestDatabase.json").exists() && !fileDatabase.exists())
        {
            // currently not supporting legacy
            return false;
        }
        boolean useDefault = true;//!fileDatabase.exists();
        if(useDefault)
        {
            fileDatabase = new File(defaultDir, "DefaultQuests.json");
        }
        else
        {
			/*JsonObject defTmp = JsonHelper.ReadFromFile(new File(BQ_Settings.defaultDir, "DefaultQuests.json"));
			QuestSettings tmpSettings = new QuestSettings();
			tmpSettings.readFromNBT(NBTConverter.JSONtoNBT_Object(defTmp, new NBTTagCompound(), true).getCompoundTag("questSettings"));
			packVer = tmpSettings.getProperty(NativeProps.PACK_VER);
			packName = tmpSettings.getProperty(NativeProps.PACK_NAME);*/
        }
        JsonObject j1 = ReadFromFile(fileDatabase);

        NBTTagCompound nbt1 = JSONtoNBT_Object(j1, new NBTTagCompound(), true);

        ReadQuestDatabase(nbt1.getTagList("questDatabase", 10));


        String fVer = nbt1.hasKey("format", 8) ? nbt1.getString("format") : "0.0.0";
        String bVer = nbt1.getString("build");
        if(fVer != "2.0.0" || bVer != "3.5.2.77")
        {
            server.logWarning(String.format("Better Questing expected format version 2.0.0 and build version 3.5.2.77 and found fVer:%s bVer:%s", fVer, bVer));
        }
        server.logDebug("fver:"+fVer+" bver:"+bVer);
        server.logDebug("Starting import");
        return false;
    }
    private void ReadQuestDatabase(NBTTagList nbt)
    {
        QuestChapter chapter = new QuestChapter(ServerQuestFile.INSTANCE);
        chapter.title = "BQ Import";
        for(int i = 0; i < nbt.tagCount(); i++)
        {
            NBTBase entry = nbt.get(i);

            if(entry.getId() != 10)
            {
                continue;
            }

            NBTTagCompound qTag = (NBTTagCompound)entry;

            int qID = qTag.hasKey("questID", 99) ? qTag.getInteger("questID") : -1;

            if(qID < 0)
            {
                continue;
            }
            Quest quest = new Quest(chapter);


           // this.qInfo.readFromNBT();
            GetPreReqs(qTag);
            GetTasks(qTag, quest);
            GetRewards(qTag, quest);
            chapter.quests.add(quest);
            /*chapter.quests.add(qID,quest);
            IQuest quest = getValue(qID);
            quest = quest != null? quest : this.createNew(qID);
            quest.readFromNBT(qTag);*/
        }
        ServerQuestFile.INSTANCE.chapters.add(chapter);
        ServerQuestFile.INSTANCE.refreshIDMap();
        ServerQuestFile.INSTANCE.saveNow();
    }
    private void GetRewards(NBTTagCompound qTag, Quest quest)
    {
        NBTTagList jsonRewards = qTag.getTagList("rewards", 10);
        for(int i = 0; i < jsonRewards.tagCount(); i++)
        {
            NBTBase entry = jsonRewards.get(i);

            if(entry.getId() != 10)
            {
                continue;
            }

            NBTTagCompound jsonReward = (NBTTagCompound)entry;
            String rewardId = jsonReward.getString("rewardID");
            int index = jsonReward.hasKey("index", 99) ? jsonReward.getInteger("index") : -1;
            switch(rewardId)
            {
                case "bq_standard:item":
                    NBTTagList rList = jsonReward.getTagList("rewards", 10);
                    for(int j = 0; j < rList.tagCount(); j++)
                    {
                        NBTBase entry2 = rList.get(j);

                        if(entry2 == null || entry2.getId() != 10)
                        {
                            continue;
                        }

                        try
                        {
                            ItemStack item = JsonToItemStack((NBTTagCompound)entry2);

                            if(item != null)
                            {
                                ItemReward itemReward = new ItemReward(quest);
                                itemReward.stack = item;
                                itemReward.id = index;
                                quest.rewards.add(itemReward);
                            } else
                            {
                                continue;
                            }
                        } catch(Exception e)
                        {
                            _server.logSevere("Unable to load reward item data");
                        }
                    }
                    break;
                default:
                    _server.logWarning(String.format("Reward '%s' is not currently supported.", rewardId));
                    break;
            }
        }
    }
    private void GetTasks(NBTTagCompound qTag, Quest quest)
    {
        NBTTagList nbtTasks = qTag.getTagList("tasks", 10);
        for(int i = 0; i < nbtTasks.tagCount(); i++)
        {
            NBTBase entry = nbtTasks.get(i);

            if(entry.getId() != 10)
            {
                continue;
            }

            NBTTagCompound jsonTask = (NBTTagCompound)entry;
            String taskId = jsonTask.getString("taskID");
            switch(taskId)
            {
                case "bq_standard:retrieval":
                    ItemTask itemTask = new ItemTask(quest);

                    int index = jsonTask.hasKey("index", 99) ? jsonTask.getInteger("index") : -1;
                    itemTask.id = index;
                    itemTask.nbtMode = jsonTask.getBoolean("ignoreNBT") ? ItemTask.NBTMatchingMode.IGNORE : ItemTask.NBTMatchingMode.MATCH;
                    itemTask.consumeItems = jsonTask.getBoolean("consume");
                    NBTTagList iList = jsonTask.getTagList("requiredItems", 10);
                    for(int j = 0; j < iList.tagCount(); j++)
                    {
                        itemTask.items.add(JsonToItemStack(iList.getCompoundTagAt(j)));
                    }
                    break;
                default:
                    _server.logWarning(String.format("Task '%s' is not currently supported.", taskId));
                    continue;
            }
        }
    }

    public static ItemStack JsonToItemStack(NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKey("id")) {
            int count = nbt.getInteger("Count");
            String oreDict = nbt.getString("OreDict");
            int damage = nbt.hasKey("Damage", 99) ? nbt.getInteger("Damage") : -1;
            damage = damage >= 0 ? damage : 32767;
            String jID;
            Item item;
            if (nbt.hasKey("id", 99)) {
                int id = nbt.getInteger("id");
                item = (Item)Item.REGISTRY.getObjectById(id);
                jID = "" + id;
            } else {
                jID = nbt.getString("id");
                item = (Item)Item.REGISTRY.getObject(new ResourceLocation(jID));
            }

            return new ItemStack(item, count, damage);
        } else {
            return new ItemStack(Blocks.STONE);
        }
    }
    private void GetProperties(NBTTagCompound qTag)
    {
        NBTTagCompound pTag = qTag.getCompoundTag("properties");
    }

    private void GetPreReqs(NBTTagCompound qTag) {
        /*if(qTag.getTagId("preRequisites") == 11) // Native NBT
        {
            for(int prID : qTag.getIntArray("preRequisites"))
            {
                if(prID < 0)
                {
                    continue;
                }

                IQuest tmp = parentDB.getValue(prID);

                if(tmp == null)
                {
                    // TODO: Make this unnecessary and only use IDs. Seriously, it adds out-of-order loading and that's a problem.
                    // Track parent-child mapping in a separate database which also holds the conditions and their data
                    tmp = parentDB.createNew(prID);
                }

                preRequisites.add(tmp);
            }
        } else // Probably an NBTTagList
        {
            NBTTagList rList = qTag.getTagList("preRequisites", 4);
            for(int i = 0; i < rList.tagCount(); i++)
            {
                NBTBase pTag = rList.get(i);
                int prID = pTag instanceof NBTPrimitive ? ((NBTPrimitive)pTag).getInt() : -1;

                if(prID < 0)
                {
                    continue;
                }

                IQuest tmp = parentDB.getValue(prID);

                if(tmp == null)
                {
                    tmp = parentDB.createNew(prID);
                }

                preRequisites.add(tmp);
            }
        }*/
    }

    private static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject ReadFromFile(File file)
    {

        if(file == null || !file.exists())
        {
            return new JsonObject();
        }

        try(InputStreamReader fr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
        {
            return GSON.fromJson(fr, JsonObject.class);
        } catch(Exception e)
        {
            return new JsonObject(); // Just a safety measure against NPEs
        }
    }
    public static NBTTagCompound JSONtoNBT_Object(JsonObject jObj, NBTTagCompound tags, boolean format)
    {
        if(jObj == null)
        {
            return tags;
        }

        for(Entry<String, JsonElement> entry : jObj.entrySet())
        {
            String key = entry.getKey();

            if(!format)
            {
                tags.setTag(key, JSONtoNBT_Element(entry.getValue(), (byte)0, false));
            } else
            {
                String[] s = key.split(":");
                byte id = 0;

                try
                {
                    id = Byte.parseByte(s[s.length - 1]);
                    key = key.substring(0, key.lastIndexOf(":" + id));
                } catch(Exception e)
                {
                    if(tags.hasKey(key))
                    {
                        //QuestingAPI.getLogger().log(Level.WARN, "JSON/NBT formatting conflict on key '" + key + "'. Skipping...");
                        continue;
                    }
                }

                tags.setTag(key, JSONtoNBT_Element(entry.getValue(), id, true));
            }
        }

        return tags;
    }
    private static NBTBase JSONtoNBT_Element(JsonElement jObj, byte id, boolean format)
    {
        if(jObj == null)
        {
            return new NBTTagString();
        }

        byte tagID = id <= 0? fallbackTagID(jObj) : id;

        try
        {
            if(tagID == 1 && (id <= 0 || jObj.getAsJsonPrimitive().isBoolean())) // Edge case for BQ2 legacy files
            {
                return new NBTTagByte(jObj.getAsBoolean() ? (byte)1 : (byte)0);
            } else if(tagID >= 1 && tagID <= 6)
            {
                return instanceNumber(jObj.getAsNumber(), tagID);
            } else if(tagID == 8)
            {
                return new NBTTagString(jObj.getAsString());
            } else if(tagID == 10)
            {
                return JSONtoNBT_Object(jObj.getAsJsonObject(), new NBTTagCompound(), format);
            } else if(tagID == 7) // Byte array
            {
                JsonArray jAry = jObj.getAsJsonArray();

                byte[] bAry = new byte[jAry.size()];

                for(int i = 0; i < jAry.size(); i++)
                {
                    bAry[i] = jAry.get(i).getAsByte();
                }

                return new NBTTagByteArray(bAry);
            } else if(tagID == 11)
            {
                JsonArray jAry = jObj.getAsJsonArray();

                int[] iAry = new int[jAry.size()];

                for(int i = 0; i < jAry.size(); i++)
                {
                    iAry[i] = jAry.get(i).getAsInt();
                }

                return new NBTTagIntArray(iAry);
            } else if(tagID == 9)
            {
                NBTTagList tList = new NBTTagList();

                if(jObj.isJsonArray())
                {
                    JsonArray jAry = jObj.getAsJsonArray();

                    for(int i = 0; i < jAry.size(); i++)
                    {
                        JsonElement jElm = jAry.get(i);
                        tList.appendTag(JSONtoNBT_Element(jElm, (byte)0, format));
                    }
                } else if(jObj.isJsonObject())
                {
                    JsonObject jAry = jObj.getAsJsonObject();

                    for(Entry<String,JsonElement> entry : jAry.entrySet())
                    {
                        try
                        {
                            String[] s = entry.getKey().split(":");
                            byte id2 = Byte.parseByte(s[s.length - 1]);
                            //String key = entry.getKey().substring(0, entry.getKey().lastIndexOf(":" + id));
                            tList.appendTag(JSONtoNBT_Element(entry.getValue(), id2, format));
                        } catch(Exception e)
                        {
                            tList.appendTag(JSONtoNBT_Element(entry.getValue(), (byte)0, format));
                        }
                    }
                }

                return tList;
            }
        } catch(Exception e)
        {
            //QuestingAPI.getLogger().log(Level.ERROR, "An error occured while parsing JsonElement to NBTBase (" + tagID + "):", e);
        }

        //QuestingAPI.getLogger().log(Level.WARN, "Unknown NBT representation for " + jObj.toString() + " (ID: " + tagID + ")");
        return new NBTTagString();
    }
    public static NBTBase instanceNumber(Number num, byte type)
    {
        switch (type)
        {
            case 1:
                return new NBTTagByte(num.byteValue());
            case 2:
                return new NBTTagShort(num.shortValue());
            case 3:
                return new NBTTagInt(num.intValue());
            case 4:
                return new NBTTagLong(num.longValue());
            case 5:
                return new NBTTagFloat(num.floatValue());
            default:
                return new NBTTagDouble(num.doubleValue());
        }
    }

    private static byte fallbackTagID(JsonElement jObj)
    {
        byte tagID = 0;

        if(jObj.isJsonPrimitive())
        {
            JsonPrimitive prim = jObj.getAsJsonPrimitive();

            if(prim.isNumber())
            {
                if(prim.getAsString().contains(".")) // Just in case we'll choose the largest possible container supporting this number type (Long or Double)
                {
                    tagID = 6;
                } else
                {
                    tagID = 4;
                }
            } else if(prim.isBoolean())
            {
                tagID = 1;
            } else
            {
                tagID = 8; // Non-number primitive. Assume string
            }
        } else if(jObj.isJsonArray())
        {
            JsonArray array = jObj.getAsJsonArray();

            for(JsonElement entry : array)
            {
                if(entry.isJsonPrimitive() && tagID == 0) // Note: TagLists can only support Integers, Bytes and Compounds (Strings can be stored but require special handling)
                {
                    try
                    {
                        for(JsonElement element : array)
                        {
                            // Make sure all entries can be bytes
                            if(element.getAsLong() != element.getAsByte()) // In case casting works but overflows
                            {
                                throw new ClassCastException();
                            }
                        }
                        tagID = 7; // Can be used as byte
                    } catch(Exception e1)
                    {
                        try
                        {
                            for(JsonElement element : array)
                            {
                                // Make sure all entries can be integers
                                if(element.getAsLong() != element.getAsInt()) // In case casting works but overflows
                                {
                                    throw new ClassCastException();
                                }
                            }
                            tagID = 11;
                        } catch(Exception e2)
                        {
                            tagID = 9; // Is primitive however requires TagList interpretation
                        }
                    }
                } else if(!entry.isJsonPrimitive())
                {
                    break;
                }
            }

            tagID = 9; // No data to judge format. Assuming tag list
        } else
        {
            tagID = 10;
        }

        return tagID;
    }
}
