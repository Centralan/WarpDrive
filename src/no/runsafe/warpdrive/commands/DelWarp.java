package no.runsafe.warpdrive.commands;

import no.runsafe.framework.api.IScheduler;
import no.runsafe.framework.api.command.player.PlayerAsyncCommand;
import no.runsafe.framework.minecraft.player.RunsafePlayer;
import no.runsafe.warpdrive.database.WarpRepository;

import java.util.HashMap;

public class DelWarp extends PlayerAsyncCommand
{
	public DelWarp(IScheduler scheduler, WarpRepository repository)
	{
		super("delwarp", "Deletes a warp location", "runsafe.warp.delete", scheduler, "name");
		warpRepository = repository;
	}

	@Override
	public String OnAsyncExecute(RunsafePlayer player, HashMap<String, String> parameters)
	{
		warpRepository.DelPublic(parameters.get("name"));
		return String.format("Deleted public warp %s.", parameters.get("name"));
	}

	private final WarpRepository warpRepository;
}
