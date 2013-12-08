package no.runsafe.warpdrive;

import no.runsafe.framework.api.*;
import no.runsafe.framework.api.event.IAsyncEvent;
import no.runsafe.framework.api.event.player.IPlayerRightClickSign;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.RunsafeLocation;
import no.runsafe.framework.minecraft.RunsafeServer;
import no.runsafe.framework.minecraft.block.RunsafeSign;
import no.runsafe.framework.minecraft.item.meta.RunsafeMeta;
import no.runsafe.framework.text.ChatColour;
import no.runsafe.framework.timer.ForegroundWorker;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SnazzyWarp extends ForegroundWorker<String, SnazzyWarp.WarpParameters> implements
	IPlayerRightClickSign, IAsyncEvent, IConfigurationChanged
{
	public SnazzyWarp(IScheduler scheduler, Engine engine, IDebug output)
	{
		super(scheduler, 2);
		this.engine = engine;
		debugger = output;
	}

	@Override
	public boolean OnPlayerRightClickSign(IPlayer thePlayer, RunsafeMeta runsafeItemStack, RunsafeSign theSign)
	{
		if (theSign.getLine(0).equalsIgnoreCase(signHeader))
		{
			if (!snazzyWarps.containsKey(theSign.getLine(1))
				|| snazzyWarps.get(theSign.getLine(1)).refresh(theSign))
				snazzyWarps.put(theSign.getLine(1), new WarpParameters(theSign));
			Push(thePlayer.getName(), snazzyWarps.get(theSign.getLine(1)));
			debugger.debugFine("Pushed player click..");
			return false;
		}
		return true;
	}

	@Override
	public void process(String player, WarpParameters parameters)
	{
		if (parameters == null)
			return;
		debugger.outputDebugToConsole(String.format("Player %s teleporting", player), Level.FINE);
		IPlayer target = RunsafeServer.Instance.getPlayer(player);
		ILocation destination = parameters.getTarget();
		if (target.isOnline() && destination != null)
			target.teleport(destination);
		else
			debugger.outputDebugToConsole("Unable to find destination..", Level.FINE);
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		change_after = Duration.standardSeconds(configuration.getConfigValueAsInt("snazzy.timeout"));
	}

	private final Engine engine;

	class WarpParameters
	{
		WarpParameters(RunsafeSign sign)
		{
			maxDistance = Integer.parseInt(sign.getLine(2));
			minDistance = Integer.parseInt(sign.getLine(3));
			world = RunsafeServer.Instance.getWorld(sign.getLine(1));
			if (world == null)
				debugger.debugFine("New warp sign created outside a world!");
			else
				debugger.debugFine(
					"Configured new warp sign %s in world %s [%d-%d].",
					sign.getLine(1),
					world.getName(),
					minDistance,
					maxDistance
				);
		}

		public ILocation getTarget()
		{
			if (target == null || expires.isBefore(DateTime.now()) || !engine.targetFloorIsSafe(target, true))
			{
				target = getNewTarget();
				expires = DateTime.now().plus(change_after);
			}
			return target;
		}

		private ILocation getNewTarget()
		{
			boolean negX = rng.nextInt(100) > 50;
			boolean negZ = rng.nextInt(100) > 50;
			double randomX = (negX ? -1 : 1) * rng.nextInt(maxDistance);
			double randomZ = (negZ ? -1 : 1) * rng.nextInt(maxDistance);
			if (randomX < minDistance && randomZ < minDistance)
			{
				if (rng.nextBoolean())
					randomX += (negX ? -1 : 1) * minDistance;
				else
					randomZ += (negZ ? -1 : 1) * minDistance;
			}
			ILocation target = null;
			int retries = 10;
			while (target == null && retries-- > 0)
				target = engine.findRandomSafeSpot(
					new RunsafeLocation(world, randomX + 0.5, 64.0D, randomZ + 0.5)
				);
			if (target != null)
			{
				target.setX(target.getBlockX() + 0.5);
				target.setY(target.getY() - 1);
				target.setZ(target.getBlockZ() + 0.5);
			}
			return target;
		}

		private final IWorld world;
		private final int maxDistance;
		private final int minDistance;
		private final Random rng = new Random();
		private DateTime expires = DateTime.now();
		private ILocation target;

		public boolean refresh(RunsafeSign sign)
		{
			return maxDistance != Integer.parseInt(sign.getLine(2)) || minDistance != Integer.parseInt(sign.getLine(3));
		}
	}

	private final ConcurrentHashMap<String, WarpParameters> snazzyWarps = new ConcurrentHashMap<String, WarpParameters>();
	private final IDebug debugger;
	private Duration change_after;
	public static final String signHeader = ChatColour.DARK_BLUE.toBukkit() + "[Snazzy Warp]";
	public static final String signTag = "[snazzy warp]";
}