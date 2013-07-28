package no.runsafe.warpdrive.commands;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.IOutput;
import no.runsafe.framework.api.IScheduler;
import no.runsafe.framework.api.command.argument.RequiredArgument;
import no.runsafe.framework.api.command.player.PlayerAsyncCommand;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.minecraft.player.RunsafePlayer;
import no.runsafe.warpdrive.database.WarpRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SetHome extends PlayerAsyncCommand implements IConfigurationChanged
{
	public SetHome(IScheduler scheduler, WarpRepository repository, IOutput output)
	{
		super(
			"sethome", "Saves your current location as a home", "runsafe.home.set", scheduler,
			new RequiredArgument("name")
		);
		warpRepository = repository;
		console = output;
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		Map<String, String> section = configuration.getConfigValuesAsMap("private.max");
		console.write("Loading configuration..");
		console.write("private.max:");
		for (String key : section.keySet())
			console.write(String.format("  %s: %s", key, section.get(key)));
		privateWarpLimit.clear();
		for (String key : section.keySet())
			privateWarpLimit.put(key, Integer.valueOf(section.get(key)));
	}

	@Override
	public String OnAsyncExecute(RunsafePlayer player, Map<String, String> parameters)
	{
		List<String> homes = warpRepository.GetPrivateList(player.getName());
		String name = parameters.get("name").toLowerCase();
		if (!homes.contains(name))
		{
			int limit = 0;
			for (String group : player.getGroups())
			{
				if (privateWarpLimit.containsKey(group))
					limit = Math.max(limit, privateWarpLimit.get(group));
			}
			if (limit == 0)
				limit = privateWarpLimit.get("default");
			if (homes.size() >= limit)
				return String.format("You are only allowed %d homes on this server.", limit);
		}

		warpRepository.Persist(player.getName(), name, false, player.getLocation());
		return String.format("Current location saved as the home %s.", name);
	}

	private final WarpRepository warpRepository;
	private final ConcurrentHashMap<String, Integer> privateWarpLimit = new ConcurrentHashMap<String, Integer>();
	private final IOutput console;
}
