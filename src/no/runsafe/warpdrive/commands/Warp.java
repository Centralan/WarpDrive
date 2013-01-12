package no.runsafe.warpdrive.commands;

import no.runsafe.framework.command.RunsafePlayerCommand;
import no.runsafe.framework.event.block.ISignChange;
import no.runsafe.framework.event.player.IPlayerRightClickSign;
import no.runsafe.framework.output.ChatColour;
import no.runsafe.framework.output.IOutput;
import no.runsafe.framework.server.RunsafeLocation;
import no.runsafe.framework.server.block.RunsafeBlock;
import no.runsafe.framework.server.block.RunsafeSign;
import no.runsafe.framework.server.item.RunsafeItemStack;
import no.runsafe.framework.server.player.RunsafePlayer;
import no.runsafe.warpdrive.StaticWarp;
import no.runsafe.warpdrive.database.WarpRepository;
import org.apache.commons.lang.StringUtils;

public class Warp extends RunsafePlayerCommand implements IPlayerRightClickSign, ISignChange
{
	public Warp(WarpRepository repository, IOutput output)
	{
		super("warp", "destination");
		warpRepository = repository;
		console = output;
	}

	@Override
	public String requiredPermission()
	{
		return "runsafe.warp.use";
	}

	@Override
	public String OnExecute(RunsafePlayer player, String[] strings)
	{
		String target = getArg("destination");
		if (!player.hasPermission("runsafe.warp.use.*")
			&& !player.hasPermission(String.format("runsafe.warp.use.%s", target)))
			return "You do not have permission to use this warp.";

		RunsafeLocation destination = warpRepository.GetPublic(target);
		if (destination == null)
			return String.format("The warp %s does not exist.", target);
		StaticWarp.safePlayerTeleport(destination, player, false);
		return null;
	}

	@Override
	public String getCommandUsage(RunsafePlayer executor)
	{
		return String.format(
			"/%1$s\nExisting warps: %2$s",
			getCommandParams(),
			StringUtils.join(warpRepository.GetPublicList(), ", ")
		);
	}

	@Override
	public boolean OnPlayerRightClickSign(RunsafePlayer player, RunsafeItemStack itemStack, RunsafeSign sign)
	{
		if (!sign.getLine(0).contains(warpHeader))
			return true;

		String name = sign.getLine(1).toLowerCase();
		RunsafeLocation destination = warpRepository.GetPublic(name);
		if (destination == null)
		{
			console.write(String.format("%s used a invalid warp sign %s.", player.getName(), name));
			return false;
		}
		if (!player.hasPermission("runsafe.warpsign.use.*")
			&& !player.hasPermission(String.format("runsafe.warpsign.use.%s", name)))
			return false;

		StaticWarp.safePlayerTeleport(destination, player, false);
		return false;
	}

	@Override
	public boolean OnSignChange(RunsafePlayer player, RunsafeBlock runsafeBlock, String[] strings)
	{
		if (!strings[0].toLowerCase().contains("[warp]") && !strings[0].toLowerCase().contains(warpHeader))
			return true;
		if (player.hasPermission("runsafe.warpsign.create"))
		{
			((RunsafeSign) runsafeBlock.getBlockState()).setLine(0, warpHeader);
			return true;
		}
		return false;
	}

	final WarpRepository warpRepository;
	final IOutput console;
	private static final String warpHeader = "[" + ChatColour.BLUE.toBukkit() + "warp" + ChatColour.RESET.toBukkit() + "]";
}
