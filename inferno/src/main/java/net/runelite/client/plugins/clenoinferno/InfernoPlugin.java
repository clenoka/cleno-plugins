/*
 * Copyright (c) 2019, Jacky <liangj97@gmail.com>
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
package net.runelite.client.plugins.clenoinferno;

import com.google.inject.Provides;
import com.openosrs.client.game.NPCManager;
import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HeadIcon;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.clenoinferno.displaymodes.InfernoPrayerDisplayMode;
import net.runelite.client.plugins.clenoinferno.displaymodes.InfernoSafespotDisplayMode;
import net.runelite.client.plugins.clenoinferno.displaymodes.InfernoWaveDisplayMode;
import net.runelite.client.plugins.clenoinferno.displaymodes.InfernoZukShieldDisplayMode;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.widgets.Prayers;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Cleno Inferno",
	enabledByDefault = false,
	description = "Inferno helper",
	tags = {"combat", "overlay", "pve", "pvm"}
)
@Slf4j
public class InfernoPlugin extends Plugin
{
	private static final int INFERNO_REGION = 9043;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private NPCManager npcManager;

	@Inject
	private InfernoOverlay infernoOverlay;

	@Inject
	private InfernoWaveOverlay waveOverlay;

	@Inject
	private InfernoInfoBoxOverlay jadOverlay;

	@Inject
	private InfernoOverlay prayerOverlay;

	@Inject
	private InfernoConfig config;

	@Getter(AccessLevel.PACKAGE)
	private InfernoConfig.FontStyle fontStyle = InfernoConfig.FontStyle.BOLD;
	@Getter(AccessLevel.PACKAGE)
	private int textSize = 32;

	private WorldPoint lastLocation = new WorldPoint(0, 0, 0);

	@Getter(AccessLevel.PACKAGE)
	private int currentWaveNumber;

	@Getter(AccessLevel.PACKAGE)
	private final List<InfernoNPC> infernoNpcs = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, Map<InfernoNPC.Attack, Integer>> upcomingAttacks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private InfernoNPC.Attack closestAttack = null;

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> obstacles = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private boolean finalPhase = false;
	private boolean finalPhaseTick = false;
	private int ticksSinceFinalPhase = 0;
	@Getter(AccessLevel.PACKAGE)
	private NPC zukShield = null;
	private NPC zuk = null;
	private WorldPoint zukShieldLastPosition = null;
	private WorldPoint zukShieldBase = null;
	private int zukShieldCornerTicks = -2;

	private int zukShieldNegativeXCoord = -1;
	private int zukShieldPositiveXCoord = -1;
	private int zukShieldLastNonZeroDelta = 0;
	private int zukShieldLastDelta = 0;
	private int zukShieldTicksLeftInCorner = -1;

	@Getter(AccessLevel.PACKAGE)
	private InfernoNPC centralNibbler = null;

	// 0 = total safespot
	// 1 = pray melee
	// 2 = pray range
	// 3 = pray magic
	// 4 = pray melee, range
	// 5 = pray melee, magic
	// 6 = pray range, magic
	// 7 = pray all
	@Getter(AccessLevel.PACKAGE)
	private final Map<WorldPoint, Integer> safeSpotMap = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, List<WorldPoint>> safeSpotAreas = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	List<InfernoBlobDeathSpot> blobDeathSpots = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private long lastTick;

	private InfernoSpawnTimerInfobox spawnTimerInfoBox;

	public static final int JAL_NIB = 7574;
	public static final int JAL_MEJRAH = 7578;
	public static final int JAL_MEJRAH_STAND = 7577;
	public static final int JAL_AK_RANGE_ATTACK = 7581;
	public static final int JAL_AK_MELEE_ATTACK = 7582;
	public static final int JAL_AK_MAGIC_ATTACK = 7583;
	public static final int JAL_IMKOT = 7597;
	public static final int JAL_XIL_MELEE_ATTACK = 7604;
	public static final int JAL_XIL_RANGE_ATTACK = 7605;
	public static final int JAL_ZEK_MAGE_ATTACK = 7610;
	public static final int JAL_ZEK_MELEE_ATTACK = 7612;
	public static final int JALTOK_JAD_MAGE_ATTACK = 7592;
	public static final int JALTOK_JAD_RANGE_ATTACK = 7593;
	public static final int TZKAL_ZUK = 7566;
	private InfernoNPC.Attack lastActivatedPrayer = null;
	public static int drinkTickTimeout = 0;



	@Provides
	InfernoConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InfernoConfig.class);
	}

	@Override
	protected void startUp()
	{
		waveOverlay.setDisplayMode(config.waveDisplay());
		waveOverlay.setWaveHeaderColor(config.getWaveOverlayHeaderColor());
		waveOverlay.setWaveTextColor(config.getWaveTextColor());

		if (isInInferno())
		{
			overlayManager.add(infernoOverlay);

			if (config.waveDisplay() != InfernoWaveDisplayMode.NONE)
			{
				overlayManager.add(waveOverlay);
			}

			overlayManager.add(jadOverlay);
			overlayManager.add(prayerOverlay);
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(infernoOverlay);
		overlayManager.remove(waveOverlay);
		overlayManager.remove(jadOverlay);
		overlayManager.remove(prayerOverlay);

		infoBoxManager.removeInfoBox(spawnTimerInfoBox);

		currentWaveNumber = -1;
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!"inferno".equals(event.getGroup()))
		{
			return;
		}

		if (event.getKey().endsWith("color"))
		{
			waveOverlay.setWaveHeaderColor(config.getWaveOverlayHeaderColor());
			waveOverlay.setWaveTextColor(config.getWaveTextColor());
		}
		else if ("waveDisplay".equals(event.getKey()))
		{
			overlayManager.remove(waveOverlay);

			waveOverlay.setDisplayMode(config.waveDisplay());

			if (isInInferno() && config.waveDisplay() != InfernoWaveDisplayMode.NONE)
			{
				overlayManager.add(waveOverlay);
			}
		}
	}
	public static int ticks = 0;
	@Subscribe
	private void onGameTick(GameTick event)
	{
		ticks++;
		if (!isInInferno())
		{
			return;
		}

		lastTick = System.currentTimeMillis();

		upcomingAttacks.clear();
		calculateUpcomingAttacks();

		closestAttack = null;
		calculateClosestAttack();

		safeSpotMap.clear();
		calculateSafespots();

		safeSpotAreas.clear();
		calculateSafespotAreas();

		obstacles.clear();
		calculateObstacles();

		centralNibbler = null;
		calculateCentralNibbler();

		calculateSpawnTimerInfobox();

		manageBlobDeathLocations();

		InfernoNPC.Attack closestAttack = getClosestAttack();

		if (closestAttack != null)
		{
		enablePrayer(getPrayerForAttackType(closestAttack), ticks);
		}
		else
		{
		// If no attack is imminent, you might want to flick to conserve prayer
		// or deactivate prayers depending on your strategy
		flick(ticks);
		}

//		if (closestAttack != null && config.autoPrayer())
//		{
//			activatePrayer(closestAttack);
//		}
//		if (closestAttack == null)
//			{
//			if (config.autoPrayer())
//				deactivatePrayers();
//			}

		//if finalPhaseTick, we will skip incrementing because we already did it in onNpcSpawned
		if (finalPhaseTick)
		{
			finalPhaseTick = false;
		}
		else if (finalPhase)
		{
			ticksSinceFinalPhase++;
		}
		if (config.drinkSara() && (Skills.getBoostedLevel(Skill.HITPOINTS) <= config.healSetpoint() || drinkingSara) && drinkTickTimeout < 0)
			{
			int doses = getAllSaraDoses();
			log.info("sara doses: " + doses);
			if (!drinkingSara)
				{
				saraDosesToStopAt = doses - 3;
				if (saraDosesToStopAt < 0) saraDosesToStopAt = 0;
				log.info("setup sara drink process, stop at sara doses: " + saraDosesToStopAt);
				drinkingSara = true;
				}
			Item saraBrew = Inventory.getFirst(ItemID.SARADOMIN_BREW1, ItemID.SARADOMIN_BREW2, ItemID.SARADOMIN_BREW3, ItemID.SARADOMIN_BREW4);
			if (saraBrew != null && calculateSaraBoost() != Skills.getBoostedLevel(Skill.HITPOINTS) && getAllSaraDoses() > saraDosesToStopAt)
				{
				shortSleep();
				saraBrew.interact("Drink");
				drinkTickTimeout = 3;
				}
			else
				{
				log.info("stop sara drink");
				drinkingSara = false;
				}
			}
		if (((config.drinkRestore() && Skills.getBoostedLevel(Skill.PRAYER) < 33) || (config.drinkSara() && Skills.getBoostedLevel(Skill.RANGED) < Skills.getLevel(Skill.RANGED))) && drinkTickTimeout < 0)
			{
			Item superRestore = Inventory.getFirst(ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4);
			if (superRestore != null)
				{
				shortSleep();
				superRestore.interact("Drink");
				drinkTickTimeout = 3;
				}
			}

		if (drinkTickTimeout >= -5) drinkTickTimeout--;
	}
	public static boolean drinkingSara = false;
	public static int saraDosesToStopAt = 0;
	public static int calculateSaraBoost()
	{
		return Skills.getLevel(Skill.HITPOINTS) + (int)(Math.floor(Skills.getLevel(Skill.HITPOINTS) * 0.15)) + 2;
	}
	public static int getAllSaraDoses()
	{
		return Inventory.getCount(ItemID.SARADOMIN_BREW1) +
					   Inventory.getCount(ItemID.SARADOMIN_BREW2) * 2 +
					   Inventory.getCount(ItemID.SARADOMIN_BREW3) * 3 +
					   Inventory.getCount(ItemID.SARADOMIN_BREW4) * 4;
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (!isInInferno())
		{
			return;
		}

		final int npcId = event.getNpc().getId();

		if (npcId == NpcID.ANCESTRAL_GLYPH)
		{
			zukShield = event.getNpc();
			return;
		}

		final InfernoNPC.Type infernoNPCType = InfernoNPC.Type.typeFromId(npcId);

		if (infernoNPCType == null)
		{
			return;
		}

		switch (infernoNPCType)
		{
			case BLOB:
				// Blobs need to be added to the end of the list because the prayer for their detection tick
				// will be based on the upcoming attacks of other NPC's
				infernoNpcs.add(new InfernoNPC(event.getNpc()));
				return;
			case MAGE:
				if (zuk != null && spawnTimerInfoBox != null)
				{
					spawnTimerInfoBox.reset();
					spawnTimerInfoBox.run();
				}
				break;
			case ZUK:
				finalPhase = false;
				zukShieldCornerTicks = -2;
				zukShieldLastPosition = null;
				zukShieldBase = null;
				log.debug("[INFERNO] Zuk spawn detected, not in final phase");

				if (config.spawnTimerInfobox())
				{
					zuk = event.getNpc();

					if (spawnTimerInfoBox != null)
					{
						infoBoxManager.removeInfoBox(spawnTimerInfoBox);
					}

					spawnTimerInfoBox = new InfernoSpawnTimerInfobox(itemManager.getImage(ItemID.TZREKZUK), this);
					infoBoxManager.addInfoBox(spawnTimerInfoBox);
				}
				break;
			case HEALER_ZUK:
				finalPhase = true;
				ticksSinceFinalPhase = 1;
				finalPhaseTick = true;
				for (InfernoNPC infernoNPC : infernoNpcs)
				{
					if (infernoNPC.getType() == InfernoNPC.Type.ZUK)
					{
						infernoNPC.setTicksTillNextAttack(-1);
					}
				}
				log.debug("[INFERNO] Final phase detected!");
				break;
		}


		infernoNpcs.add(0, new InfernoNPC(event.getNpc()));
	}
	private Prayer getPrayerForAttackType(InfernoNPC.Attack attackType)
	{
		switch (attackType)
			{
			case MAGIC:
				return Prayer.PROTECT_FROM_MAGIC;
			case RANGED:
				return Prayer.PROTECT_FROM_MISSILES;
			case MELEE:
				return Prayer.PROTECT_FROM_MELEE;
			default:
				return null;
			}
	}
//	private void activatePrayer(InfernoNPC.Attack attackType)
//	{
//		if (attackType == null)
//		{
//			deactivatePrayers();
//			return;
//		}
//
//			//MessageUtils.addMessage("Closest Attack: " + attackType);
//
//			switch (attackType)
//				{
//				case MAGIC:
//					if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MAGIC))
//						{
//					//	MessageUtils.addMessage("Prayer Magic");
//						Static.getClient().invokeMenuAction("Activate", "Protect from Magic", 1, 57, -1, 35454997);
//						activateOffensivePrayer();
//						}
//					break;
//				case RANGED:
//					if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MISSILES))
//						{
//					//	MessageUtils.addMessage("Prayer Range");
//						Static.getClient().invokeMenuAction("Activate", "Protect from Missiles", 1, 57, -1, 35454998);
//						activateOffensivePrayer();
//						}
//					break;
//				case MELEE:
//					if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MELEE))
//						{
//					//	MessageUtils.addMessage("Prayer Melee");
//						Static.getClient().invokeMenuAction("Activate", "Protect from Melee", 1, 57, -1, 35454999);
//						activateOffensivePrayer();
//
//						break;
//						}
//					default:
//					MessageUtils.addMessage("No valid attack type detected");
//					// Handle unexpected attack types or no attack
//					break;
//				}
//
//			}
//	private void activateOffensivePrayer()
//	{
//		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
//
//		if (equipment == null)
//		{
//			return;
//		}
//
//		Item[] items = equipment.getItems();
//		// Assuming slot ID for weapon is correct. Adjust as necessary.
//		Item weapon = items[EquipmentInventorySlot.WEAPON.getSlotIdx()];
//
//		if (weapon == null)
//		{
//			return;
//		}
//
//		// You need to determine the IDs for magic and ranged weapons
//		if (isMagicWeapon(weapon.getId()) && config.autoOffensivePrayer())
//		{
//				if (!Prayers.isEnabled(Prayer.AUGURY))
//				{
//					Static.getClient().invokeMenuAction("Activate", "Augury", 1, 57, -1, 35455012);
//				}
//			}
//			else if (isRangedWeapon(weapon.getId()) && config.autoOffensivePrayer())
//			{
//				if (!Prayers.isEnabled(Prayer.RIGOUR))
//					{
//						Static.getClient().invokeMenuAction("Activate", "Rigour", 1, 57, -1, 35455009);
//					}
//		}
//	}
//
//	private boolean isMagicWeapon(int itemId)
//	{
//		Set<Integer> magicWeaponIDs = new HashSet<>(Arrays.asList(
//				6914, 21006
//		));
//
//		return magicWeaponIDs.contains(itemId);
//	}
//
//	private boolean isRangedWeapon(int itemId)
//	{
//		// List of item IDs for ranged weapons
//		Set<Integer> rangedWeaponIDs = new HashSet<>(Arrays.asList(
//				20997, 12926
//		));
//
//		return rangedWeaponIDs.contains(itemId);
//	}
//	private void deactivatePrayers()
//	{
//
//		if (Prayers.isEnabled(Prayer.PROTECT_FROM_MAGIC))
//			{
//			Static.getClient().invokeMenuAction("Deactivate", "Protect from Magic", 1, 57, -1, 35454997);
//			}
//		if (Prayers.isEnabled(Prayer.PROTECT_FROM_MISSILES))
//			{
//			Static.getClient().invokeMenuAction("Deactivate", "Protect from Missiles", 1, 57, -1, 35454998);
//			}
//		if (Prayers.isEnabled(Prayer.PROTECT_FROM_MELEE))
//			{
//			Static.getClient().invokeMenuAction("Deactivate", "Protect from Melee", 1, 57, -1, 35454999);
//			}
//		if (Prayers.isEnabled(Prayer.RIGOUR))
//			{
//			Static.getClient().invokeMenuAction("Deactivate", "Rigour", 1, 57, -1, 35455009);
//			}
//		if (Prayers.isEnabled(Prayer.AUGURY))
//			{
//			Static.getClient().invokeMenuAction("Deactivate", "Augury", 1, 57, -1, 35455012);
//			}
//	}
	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (!isInInferno())
		{
			return;
		}

		int npcId = event.getNpc().getId();

		switch (npcId)
		{
			case NpcID.ANCESTRAL_GLYPH:
				zukShield = null;
				return;
			case NpcID.TZKALZUK:
				zuk = null;

				if (spawnTimerInfoBox != null)
				{
					infoBoxManager.removeInfoBox(spawnTimerInfoBox);
				}

				spawnTimerInfoBox = null;
				break;
			default:
				break;
		}

		infernoNpcs.removeIf(infernoNPC -> infernoNPC.getNpc() == event.getNpc());
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		if (!isInInferno())
		{
			return;
		}

		if (event.getActor() instanceof NPC)
		{
			final NPC npc = (NPC) event.getActor();

			if (ArrayUtils.contains(InfernoNPC.Type.NIBBLER.getNpcIds(), npc.getId())
				&& npc.getAnimation() == 7576)
			{
				infernoNpcs.removeIf(infernoNPC -> infernoNPC.getNpc() == npc);
			}


			if (config.indicateBlobDeathLocation() && InfernoNPC.Type.typeFromId(npc.getId()) == InfernoNPC.Type.BLOB && npc.getAnimation() == InfernoBlobDeathSpot.BLOB_DEATH_ANIMATION)
			{
				// Remove from list so the ticks overlay doesn't compete
				// with the tile overlay.
				infernoNpcs.removeIf(infernoNPC -> infernoNPC.getNpc() == npc);
				blobDeathSpots.add(new InfernoBlobDeathSpot(npc.getLocalLocation()));
			}

		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (!isInInferno())
		{
			infernoNpcs.clear();

			currentWaveNumber = -1;

			overlayManager.remove(infernoOverlay);
			overlayManager.remove(waveOverlay);
			overlayManager.remove(jadOverlay);
			overlayManager.remove(prayerOverlay);

			zukShield = null;
			zuk = null;

			if (spawnTimerInfoBox != null)
			{
				infoBoxManager.removeInfoBox(spawnTimerInfoBox);
			}

			spawnTimerInfoBox = null;
		}
		else if (currentWaveNumber == -1)
		{
			infernoNpcs.clear();

			currentWaveNumber = 1;

			overlayManager.add(infernoOverlay);
			overlayManager.add(jadOverlay);
			overlayManager.add(prayerOverlay);

			if (config.waveDisplay() != InfernoWaveDisplayMode.NONE)
			{
				overlayManager.add(waveOverlay);
			}
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!isInInferno() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();

		if (event.getMessage().contains("Wave:"))
		{
			message = message.substring(message.indexOf(": ") + 2);
			currentWaveNumber = Integer.parseInt(message.substring(0, message.indexOf('<')));
		}
	}

	private boolean isInInferno()
	{
		return ArrayUtils.contains(client.getMapRegions(), INFERNO_REGION);
	}

	int getNextWaveNumber()
	{
		return currentWaveNumber == -1 || currentWaveNumber == 69 ? -1 : currentWaveNumber + 1;
	}

	private void calculateUpcomingAttacks()
	{
		for (InfernoNPC infernoNPC : infernoNpcs)
		{
			infernoNPC.gameTick(client, lastLocation, finalPhase, ticksSinceFinalPhase);

			if (infernoNPC.getType() == InfernoNPC.Type.ZUK && zukShieldCornerTicks == -1)
			{
				infernoNPC.updateNextAttack(InfernoNPC.Attack.UNKNOWN, 12); // TODO: Could be 10 or 11. Test!
				zukShieldCornerTicks = 0;
			}

			// Map all upcoming attacks and their priority + determine which NPC is about to attack next
			if (infernoNPC.getTicksTillNextAttack() > 0 && isPrayerHelper(infernoNPC)
				&& (infernoNPC.getNextAttack() != InfernoNPC.Attack.UNKNOWN
				|| (config.indicateBlobDetectionTick() && infernoNPC.getType() == InfernoNPC.Type.BLOB
				&& infernoNPC.getTicksTillNextAttack() >= 4)))
			{
				upcomingAttacks.computeIfAbsent(infernoNPC.getTicksTillNextAttack(), k -> new HashMap<>());

				if (config.indicateBlobDetectionTick() && infernoNPC.getType() == InfernoNPC.Type.BLOB
					&& infernoNPC.getTicksTillNextAttack() >= 4)
				{
					upcomingAttacks.computeIfAbsent(infernoNPC.getTicksTillNextAttack() - 3, k -> new HashMap<>());
					upcomingAttacks.computeIfAbsent(infernoNPC.getTicksTillNextAttack() - 4, k -> new HashMap<>());

					// If there's already a magic attack on the detection tick, group them
					if (upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).containsKey(InfernoNPC.Attack.MAGIC))
					{
						if (upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).get(InfernoNPC.Attack.MAGIC) > InfernoNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).put(InfernoNPC.Attack.MAGIC, InfernoNPC.Type.BLOB.getPriority());
						}
					}
					// If there's already a ranged attack on the detection tick, group them
					else if (upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).containsKey(InfernoNPC.Attack.RANGED))
					{
						if (upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).get(InfernoNPC.Attack.RANGED) > InfernoNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).put(InfernoNPC.Attack.RANGED, InfernoNPC.Type.BLOB.getPriority());
						}
					}
					// If there's going to be a magic attack on the blob attack tick, pray range on the detect tick so magic is prayed on the attack tick
					else if (upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).containsKey(InfernoNPC.Attack.MAGIC)
						|| upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 4).containsKey(InfernoNPC.Attack.MAGIC))
					{
						if (!upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).containsKey(InfernoNPC.Attack.RANGED)
							|| upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).get(InfernoNPC.Attack.RANGED) > InfernoNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).put(InfernoNPC.Attack.RANGED, InfernoNPC.Type.BLOB.getPriority());
						}
					}
					// If there's going to be a ranged attack on the blob attack tick, pray magic on the detect tick so range is prayed on the attack tick
					else if (upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).containsKey(InfernoNPC.Attack.RANGED)
						|| upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 4).containsKey(InfernoNPC.Attack.RANGED))
					{
						if (!upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).containsKey(InfernoNPC.Attack.MAGIC)
							|| upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).get(InfernoNPC.Attack.MAGIC) > InfernoNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).put(InfernoNPC.Attack.MAGIC, InfernoNPC.Type.BLOB.getPriority());
						}
					}
					// If there's no magic or ranged attack on the detection tick, create a magic pray blob
					else
					{
						upcomingAttacks.get(infernoNPC.getTicksTillNextAttack() - 3).put(InfernoNPC.Attack.MAGIC, InfernoNPC.Type.BLOB.getPriority());
					}
				}
				else
				{
					final InfernoNPC.Attack attack = infernoNPC.getNextAttack();
					final int priority = infernoNPC.getType().getPriority();

					if (!upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).containsKey(attack)
						|| upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).get(attack) > priority)
					{
						upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).put(attack, priority);
					}
				}
			}
		}
	}

	private void calculateClosestAttack()
	{
		if (config.prayerDisplayMode() == InfernoPrayerDisplayMode.PRAYER_TAB
			|| config.prayerDisplayMode() == InfernoPrayerDisplayMode.BOTH)
		{
			int closestTick = 999;
			int closestPriority = 999;

			for (Integer tick : upcomingAttacks.keySet())
			{
				final Map<InfernoNPC.Attack, Integer> attackPriority = upcomingAttacks.get(tick);

				for (InfernoNPC.Attack currentAttack : attackPriority.keySet())
				{
					final int currentPriority = attackPriority.get(currentAttack);
					if (tick < closestTick || (tick == closestTick && currentPriority < closestPriority))
					{
						closestAttack = currentAttack;
						closestPriority = currentPriority;
						closestTick = tick;
					}
				}
			}
		}
	}

	private void calculateSafespots()
	{
		if (currentWaveNumber < 69)
		{
			if (config.safespotDisplayMode() != InfernoSafespotDisplayMode.OFF)
			{
				int checkSize = (int) Math.floor(config.safespotsCheckSize() / 2.0);

				for (int x = -checkSize; x <= checkSize; x++)
				{
					for (int y = -checkSize; y <= checkSize; y++)
					{
						final WorldPoint checkLoc = client.getLocalPlayer().getWorldLocation().dx(x).dy(y);

						if (obstacles.contains(checkLoc))
						{
							continue;
						}

						for (InfernoNPC infernoNPC : infernoNpcs)
						{
							if (!isNormalSafespots(infernoNPC))
							{
								continue;
							}

							if (!safeSpotMap.containsKey(checkLoc))
							{
								safeSpotMap.put(checkLoc, 0);
							}

							if (infernoNPC.canAttack(client, checkLoc)
								|| infernoNPC.canMoveToAttack(client, checkLoc, obstacles))
							{
								if (infernoNPC.getType().getDefaultAttack() == InfernoNPC.Attack.MELEE)
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 1);
									}
									else if (safeSpotMap.get(checkLoc) == 2)
									{
										safeSpotMap.put(checkLoc, 4);
									}
									else if (safeSpotMap.get(checkLoc) == 3)
									{
										safeSpotMap.put(checkLoc, 5);
									}
									else if (safeSpotMap.get(checkLoc) == 6)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}

								if (infernoNPC.getType().getDefaultAttack() == InfernoNPC.Attack.MAGIC
									|| (infernoNPC.getType() == InfernoNPC.Type.BLOB
									&& safeSpotMap.get(checkLoc) != 2 && safeSpotMap.get(checkLoc) != 4))
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 3);
									}
									else if (safeSpotMap.get(checkLoc) == 1)
									{
										safeSpotMap.put(checkLoc, 5);
									}
									else if (safeSpotMap.get(checkLoc) == 2)
									{
										safeSpotMap.put(checkLoc, 6);
									}
									else if (safeSpotMap.get(checkLoc) == 5)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}

								if (infernoNPC.getType().getDefaultAttack() == InfernoNPC.Attack.RANGED
									|| (infernoNPC.getType() == InfernoNPC.Type.BLOB
									&& safeSpotMap.get(checkLoc) != 3 && safeSpotMap.get(checkLoc) != 5))
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 2);
									}
									else if (safeSpotMap.get(checkLoc) == 1)
									{
										safeSpotMap.put(checkLoc, 4);
									}
									else if (safeSpotMap.get(checkLoc) == 3)
									{
										safeSpotMap.put(checkLoc, 6);
									}
									else if (safeSpotMap.get(checkLoc) == 4)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}

								if (infernoNPC.getType() == InfernoNPC.Type.JAD
									&& infernoNPC.getNpc().getWorldArea().isInMeleeDistance(checkLoc))
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 1);
									}
									else if (safeSpotMap.get(checkLoc) == 2)
									{
										safeSpotMap.put(checkLoc, 4);
									}
									else if (safeSpotMap.get(checkLoc) == 3)
									{
										safeSpotMap.put(checkLoc, 5);
									}
									else if (safeSpotMap.get(checkLoc) == 6)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}
							}
						}
					}
				}
			}
		}
		else if (currentWaveNumber == 69 && zukShield != null)
		{
			final WorldPoint zukShieldCurrentPosition = zukShield.getWorldLocation();

			if (zukShieldLastPosition != null && zukShieldLastPosition.getX() != zukShieldCurrentPosition.getX() && zukShieldCornerTicks == -2)
			{
				zukShieldBase = zukShieldLastPosition;
				zukShieldCornerTicks = -1;
			}

			if (zukShieldLastPosition != null)
			{
				int zukShieldDelta = zukShieldCurrentPosition.getX() - zukShieldLastPosition.getX();

				//if zuk shield moved, update zukShieldLastNonZeroDelta to show the direction
				if (zukShieldDelta != 0)
				{
					zukShieldLastNonZeroDelta = zukShieldDelta;
				}

				//reset corner ticks when the shield started to move out of the corner
				if (zukShieldLastDelta == 0 && zukShieldDelta != 0)
				{
					zukShieldTicksLeftInCorner = 4;
				}

				//if zuk shield did not move, also set the negative/positive XCoords for the shield
				if (zukShieldDelta == 0)
				{
					if (zukShieldLastNonZeroDelta > 0)
					{
						zukShieldPositiveXCoord = zukShieldCurrentPosition.getX();
					}
					else if (zukShieldLastNonZeroDelta < 0)
					{
						zukShieldNegativeXCoord = zukShieldCurrentPosition.getX();
					}

					//if zukShieldCorner Ticks > 0, decrement it
					if (zukShieldTicksLeftInCorner > 0)
					{
						zukShieldTicksLeftInCorner--;
					}
				}

				zukShieldLastDelta = zukShieldDelta;
			}

			zukShieldLastPosition = zukShieldCurrentPosition;

			if (config.safespotDisplayMode() != InfernoSafespotDisplayMode.OFF)
			{
				if ((finalPhase && config.safespotsZukShieldAfterHealers() == InfernoZukShieldDisplayMode.LIVE)
					|| (!finalPhase && config.safespotsZukShieldBeforeHealers() == InfernoZukShieldDisplayMode.LIVE))
				{
					drawZukSafespot(zukShield.getWorldLocation().getX(), zukShield.getWorldLocation().getY(), 0);
				}

				if ((finalPhase && config.safespotsZukShieldAfterHealers() == InfernoZukShieldDisplayMode.LIVEPLUSPREDICT)
					|| (!finalPhase && config.safespotsZukShieldBeforeHealers() == InfernoZukShieldDisplayMode.LIVEPLUSPREDICT))
				{
					//draw the normal live safespot
					drawZukSafespot(zukShield.getWorldLocation().getX(), zukShield.getWorldLocation().getY(), 0);

					drawZukPredictedSafespot();
				}
				else if ((finalPhase && config.safespotsZukShieldAfterHealers() == InfernoZukShieldDisplayMode.PREDICT)
					|| (!finalPhase && config.safespotsZukShieldBeforeHealers() == InfernoZukShieldDisplayMode.PREDICT))
				{
					drawZukPredictedSafespot();
				}
			}
		}
	}

	private void drawZukPredictedSafespot()
	{
		final WorldPoint zukShieldCurrentPosition = zukShield.getWorldLocation();
		//only do this if both xcoords defined.
		if (zukShieldPositiveXCoord != -1 && zukShieldNegativeXCoord != -1)
		{
			int nextShieldXCoord = zukShieldCurrentPosition.getX();

			//calculate the next zuk shield position
			for (InfernoNPC infernoNPC : infernoNpcs)
			{
				if (infernoNPC.getType() == InfernoNPC.Type.ZUK)
				{
					int ticksTilZukAttack = finalPhase ? infernoNPC.getTicksTillNextAttack() : infernoNPC.getTicksTillNextAttack() - 1;

					if (ticksTilZukAttack < 1)
					{
						if (finalPhase)
						{
							//if ticksTilZukAttack < 1 and finalPhase, must be due to finalPhase. don't render predicted safepot until next attack.
							return;
						}
						else
						{
							//safe to start to render the next safespot
							ticksTilZukAttack = 10;
						}
					}

					//if zuk shield moving in positive direction
					if (zukShieldLastNonZeroDelta > 0)
					{
						nextShieldXCoord += ticksTilZukAttack;

						//nextShieldPosition appears to be past the rightmost spot, must adjust
						if (nextShieldXCoord > zukShieldPositiveXCoord)
						{
							//reduce by number of ticks spent in corner
							nextShieldXCoord -= zukShieldTicksLeftInCorner;

							//nextShieldPosition is LT or equal to the rightmost spot
							if (nextShieldXCoord <= zukShieldPositiveXCoord)
							{
								//shield should be at that spot
								nextShieldXCoord = zukShieldPositiveXCoord;
							}
							else
							{
								//nextShieldPosition is right of the rightmost spot still
								nextShieldXCoord = zukShieldPositiveXCoord - nextShieldXCoord + zukShieldPositiveXCoord;
							}
						}
					}
					else
					{
						//moving in negative direction
						nextShieldXCoord -= ticksTilZukAttack;

						//nextShieldPosition appears to be past the leftmost spot, must adjust
						if (nextShieldXCoord < zukShieldNegativeXCoord)
						{
							//add by number of ticks spent in corner
							nextShieldXCoord += zukShieldTicksLeftInCorner;

							//nextShieldPosition is GT or equal to the leftmost spot
							if (nextShieldXCoord >= zukShieldNegativeXCoord)
							{
								//shield should be at that spot
								nextShieldXCoord = zukShieldNegativeXCoord;
							}
							else
							{
								//nextShieldPosition is left of the leftmost spot still
								nextShieldXCoord = zukShieldNegativeXCoord - nextShieldXCoord + zukShieldNegativeXCoord;
							}
						}
					}
				}
			}

			//draw the predicted safespot
			drawZukSafespot(nextShieldXCoord, zukShield.getWorldLocation().getY(), 2);
		}
	}

	private void drawZukSafespot(int xCoord, int yCoord, int colorSafeSpotId)
	{
		for (int x = xCoord - 1; x <= xCoord + 3; x++)
		{
			for (int y = yCoord - 4; y <= yCoord - 2; y++)
			{
				safeSpotMap.put(new WorldPoint(x, y, client.getPlane()), colorSafeSpotId);
			}
		}
	}

	private void calculateSafespotAreas()
	{
		if (config.safespotDisplayMode() == InfernoSafespotDisplayMode.AREA)
		{
			for (WorldPoint worldPoint : safeSpotMap.keySet())
			{
				if (!safeSpotAreas.containsKey(safeSpotMap.get(worldPoint)))
				{
					safeSpotAreas.put(safeSpotMap.get(worldPoint), new ArrayList<>());
				}

				safeSpotAreas.get(safeSpotMap.get(worldPoint)).add(worldPoint);
			}
		}

		lastLocation = client.getLocalPlayer().getWorldLocation();
	}

	private void calculateObstacles()
	{
		for (NPC npc : client.getNpcs())
		{
			obstacles.addAll(npc.getWorldArea().toWorldPointList());
		}
	}

	private void manageBlobDeathLocations()
	{
		if (config.indicateBlobDeathLocation())
		{
			blobDeathSpots.forEach(InfernoBlobDeathSpot::decrementTick);
			blobDeathSpots.removeIf(InfernoBlobDeathSpot::isDone);
		}
	}

	private void calculateCentralNibbler()
	{
		InfernoNPC bestNibbler = null;
		int bestAmountInArea = 0;
		int bestDistanceToPlayer = 999;

		for (InfernoNPC infernoNPC : infernoNpcs)
		{
			if (infernoNPC.getType() != InfernoNPC.Type.NIBBLER)
			{
				continue;
			}

			int amountInArea = 0;
			final int distanceToPlayer = infernoNPC.getNpc().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation());

			for (InfernoNPC checkNpc : infernoNpcs)
			{
				if (checkNpc.getType() != InfernoNPC.Type.NIBBLER
					|| checkNpc.getNpc().getWorldArea().distanceTo(infernoNPC.getNpc().getWorldArea()) > 1)
				{
					continue;
				}

				amountInArea++;
			}

			if (amountInArea > bestAmountInArea
				|| (amountInArea == bestAmountInArea && distanceToPlayer < bestDistanceToPlayer))
			{
				bestNibbler = infernoNPC;

				// update tracked values
				bestAmountInArea = amountInArea;
				bestDistanceToPlayer = distanceToPlayer;
			}
		}

		if (bestNibbler != null)
		{
			centralNibbler = bestNibbler;
		}
	}

	private void calculateSpawnTimerInfobox()
	{
		if (zuk == null || finalPhase || spawnTimerInfoBox == null)
		{
			return;
		}

		final int pauseHp = 600;
		final int resumeHp = 480;

		int hp = calculateNpcHp(zuk.getHealthRatio(), zuk.getHealthScale(), npcManager.getHealth(zuk.getId()));

		if (hp <= 0)
		{
			return;
		}

		if (spawnTimerInfoBox.isRunning())
		{
			if (hp >= resumeHp && hp < pauseHp)
			{
				spawnTimerInfoBox.pause();
			}
		}
		else
		{
			if (hp < resumeHp)
			{
				spawnTimerInfoBox.run();
			}
		}
	}

	private static int calculateNpcHp(int ratio, int health, int maxHp)
	{
		// See OpponentInfo Plugin
		// Copyright (c) 2016-2018, Adam <Adam@sigterm.info>
		// Copyright (c) 2018, Jordan Atwood <jordan.atwood423@gmail.com>

		if (ratio < 0 || health <= 0 || maxHp == -1)
		{
			return -1;
		}

		int exactHealth = 0;

		if (ratio > 0)
		{
			int minHealth = 1;
			int maxHealth;

			if (health > 1)
			{
				if (ratio > 1)
				{
					minHealth = (maxHp * (ratio - 1) + health - 2) / (health - 1);
				}

				maxHealth = (maxHp * ratio - 1) / (health - 1);

				if (maxHealth > maxHp)
				{
					maxHealth = maxHp;
				}
			}
			else
			{
				maxHealth = maxHp;
			}

			exactHealth = (minHealth + maxHealth + 1) / 2;
		}

		return exactHealth;
	}

	private boolean isPrayerHelper(InfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.prayerBat();
			case BLOB:
			case MAGEBLOB:
			case RANGEBLOB:
			case MELEEBLOB:
				return config.prayerBlob();
			case MELEE:
				return config.prayerMeleer();
			case RANGER:
				return config.prayerRanger();
			case MAGE:
				return config.prayerMage();
			case HEALER_JAD:
				return config.prayerHealerJad();
			case JAD:
				return config.prayerJad();
			default:
				return false;
		}
	}

	boolean isTicksOnNpc(InfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.ticksOnNpcBat();
			case BLOB:
				return config.ticksOnNpcBlob();
			case MELEE:
				return config.ticksOnNpcMeleer();
			case RANGER:
				return config.ticksOnNpcRanger();
			case MAGE:
				return config.ticksOnNpcMage();
			case HEALER_JAD:
				return config.ticksOnNpcHealerJad();
			case JAD:
				return config.ticksOnNpcJad();
			case ZUK:
				return config.ticksOnNpcZuk();
			default:
				return false;
		}
	}

	boolean isNormalSafespots(InfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.safespotsBat();
			case BLOB:
				return config.safespotsBlob();
			case MELEE:
				return config.safespotsMeleer();
			case RANGER:
				return config.safespotsRanger();
			case MAGE:
				return config.safespotsMage();
			case HEALER_JAD:
				return config.safespotsHealerJad();
			case JAD:
				return config.safespotsJad();
			default:
				return false;
		}
	}

	boolean isIndicateNpcPosition(InfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.indicateNpcPositionBat();
			case BLOB:
				return config.indicateNpcPositionBlob();
			case MELEE:
				return config.indicateNpcPositionMeleer();
			case RANGER:
				return config.indicateNpcPositionRanger();
			case MAGE:
				return config.indicateNpcPositionMage();
			default:
				return false;
		}
	}
	private void enablePrayer(Prayer prayer, int originTick)
	{
		Prayer overhead = getOverhead();
		if (overhead == null || overhead != prayer)
		{
		shortSleep();
		Prayers.toggle(prayer);
		}
		else
		flick(originTick);
		}

	public Prayer getOverhead()
	{
		HeadIcon ourIcon = Players.getLocal().getOverheadIcon();
		if (ourIcon != null)
		{
		switch (ourIcon)
			{
		case MELEE:
			return Prayer.PROTECT_FROM_MELEE;
		case MAGIC:
			return Prayer.PROTECT_FROM_MAGIC;
		case RANGED:
			return Prayer.PROTECT_FROM_MISSILES;
		}
		}
		return null;
	}
	public void flick(int originTick)
	{
		Prayer overhead = getOverhead();
		if (config.flick())
		{
		if (overhead != null)
		{
		log.info("flicking ENABLED");
		shortSleep();
		if (ticks > originTick) return;
		Prayers.toggle(overhead);
		Prayers.toggle(Prayer.RIGOUR);
		shortSleep();
		if (ticks > originTick) return;
		Prayers.toggle(overhead);
		Prayers.toggle(Prayer.RIGOUR);
		}
		}
		else
		{
		log.info("flick DISABLED");
		}
	}
	public static void shortSleep()
	{
		Time.sleep(50, 200);
	}
}