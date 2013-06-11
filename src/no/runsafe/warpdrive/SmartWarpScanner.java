package no.runsafe.warpdrive;

import no.runsafe.framework.api.IOutput;
import no.runsafe.framework.api.IScheduler;
import no.runsafe.framework.api.event.plugin.IPluginEnabled;
import no.runsafe.framework.minecraft.RunsafeLocation;
import no.runsafe.framework.minecraft.RunsafeServer;
import no.runsafe.framework.minecraft.RunsafeWorld;
import no.runsafe.framework.timer.ForegroundWorker;
import no.runsafe.warpdrive.database.SmartWarpChunkRepository;
import no.runsafe.warpdrive.database.SmartWarpRepository;
import org.bukkit.World;

import java.util.HashMap;

public class SmartWarpScanner extends ForegroundWorker<String, RunsafeLocation> implements IPluginEnabled
{
	public SmartWarpScanner(IScheduler scheduler, IOutput console, SmartWarpRepository warpRepository, SmartWarpChunkRepository chunkRepository, Engine engine)
	{
		super(scheduler);
		this.console = console;
		this.warpRepository = warpRepository;
		this.chunkRepository = chunkRepository;
		this.engine = engine;

		this.setInterval(10);
	}

	public void Setup(RunsafeWorld world, String radius)
	{
		int r = Integer.valueOf(radius);
		warpRepository.setRange(world.getName(), r);
		range.put(world.getName(), r);
		if (!progress.containsKey(world.getName()))
			progress.put(world.getName(), 0D);
		if (!isQueued(world.getName()))
			ScheduleNext(world.getName());
	}

	@Override
	public void OnPluginEnabled()
	{
		for (String world : warpRepository.getWorlds())
		{
			progress.put(world, warpRepository.getProgress(world));
			range.put(world, warpRepository.getRange(world));
			ScheduleNext(world);
		}
	}

	@Override
	public void process(String world, RunsafeLocation location)
	{
		location = engine.findTop(location);
		if (engine.targetFloorIsSafe(location, true))
			chunkRepository.saveTarget(location, true, false);
		if (location.getWorld().getRaw().getEnvironment() != World.Environment.NETHER)
		{
			location.setY(50);
			int air = 0;
			while (location.getBlockY() > 10)
			{
				location.decrementY(1);
				if (location.getBlock().isAir())
					air++;
				else if (air > 1)
				{
					location.incrementY(1);
					if (engine.targetFloorIsSafe(location, true))
						chunkRepository.saveTarget(location, true, true);
					location.decrementY(2);
				}
				else
					air = 0;
			}
		}
		Double p = progress.get(world) + 1;
		progress.put(world, p);
		warpRepository.setProgress(world, p);
		if (progress.get(world) % 1000 == 0)
		{
			double d = range.get(world) / 16;
			console.writeColoured(
				"Scanning location %.0f/%.0f in %s (%.2f%%)",
				p, d * d, world, 100D * p / (d * d)
			);
		}
		ScheduleNext(world);
	}

	private void ScheduleNext(String world)
	{
		int d = range.get(world) / 16;
		double target = d * d;
		if (progress.get(world) >= target)
		{
			console.writeColoured("Nothing left to scan in %s, stopping!", world);
			return;
		}
		Push(world, CalculateNextLocation(world));
	}

	private RunsafeLocation CalculateNextLocation(String world)
	{
		if (!worlds.containsKey(world))
			worlds.put(world, RunsafeServer.Instance.getWorld(world));
		int r = range.get(world) / 16;
		double offset = (range.get(world) / 2) - 0.5;
		double x = 16 * (progress.get(world) % r) - offset;
		double z = 16 * (progress.get(world) / r) - offset;
		return new RunsafeLocation(worlds.get(world), x, 255, z);
	}

	private HashMap<String, Double> progress = new HashMap<String, Double>();
	private HashMap<String, Integer> range = new HashMap<String, Integer>();
	private HashMap<String, RunsafeWorld> worlds = new HashMap<String, RunsafeWorld>();
	private IOutput console;
	private SmartWarpRepository warpRepository;
	private SmartWarpChunkRepository chunkRepository;
	private Engine engine;
}