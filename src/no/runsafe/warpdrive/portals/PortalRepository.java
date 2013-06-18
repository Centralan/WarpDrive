package no.runsafe.warpdrive.portals;

import no.runsafe.framework.api.database.IDatabase;
import no.runsafe.framework.api.database.IRow;
import no.runsafe.framework.api.database.ISet;
import no.runsafe.framework.api.database.Repository;
import no.runsafe.framework.minecraft.RunsafeLocation;
import no.runsafe.framework.minecraft.RunsafeServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PortalRepository extends Repository
{
	public PortalRepository(IDatabase database)
	{
		this.database = database;
	}

	public String getTableName()
	{
		return "warpdrive_portals";
	}

	public List<PortalWarp> getPortalWarps()
	{
		List<PortalWarp> warps = new ArrayList<PortalWarp>();

		ISet data = this.database.Query(
			"SELECT `ID`,`permission`,`type`,`world`,`x`,`y`,`z`,`destWorld`,`destX`,`destY`,`destZ`,`destYaw`,`destPitch`,`radius` " +
				"FROM warpdrive_portals"
		);

		for (IRow row : data)
		{
			warps.add(new PortalWarp(
				row.String("ID"),
				new RunsafeLocation(
					RunsafeServer.Instance.getWorld(row.String("world")),
					row.Double("x"),
					row.Double("y"),
					row.Double("z")
				),
				RunsafeServer.Instance.getWorld(row.String("destWorld")),
				(row.Double("destX") == null ? 0 : row.Double("destX")),
				(row.Double("destY") == null ? 0 : row.Double("destY")),
				(row.Double("destZ") == null ? 0 : row.Double("destZ")),
				(row.Float("destYaw") == null ? 0 : row.Float("destYaw")),
				(row.Float("destPitch") == null ? 0 : row.Float("destPitch")),
				PortalType.getPortalType(row.Integer("type")),
				(row.Integer("radius") == null ? 0 : row.Integer("radius")),
				row.String("permission")
			));
		}
		return warps;
	}

	@Override
	public HashMap<Integer, List<String>> getSchemaUpdateQueries()
	{
		HashMap<Integer, List<String>> queries = new HashMap<Integer, List<String>>();
		ArrayList<String> sql = new ArrayList<String>();
		sql.add(
			"CREATE TABLE `warpdrive_portals` (" +
				"`ID` VARCHAR(50) NOT NULL," +
				"`permission` VARCHAR(255) NOT NULL DEFAULT ''," +
				"`type` TINYINT NOT NULL DEFAULT '0'," +
				"`world` VARCHAR(255) NOT NULL," +
				"`x` DOUBLE NOT NULL," +
				"`y` DOUBLE NOT NULL," +
				"`z` DOUBLE NOT NULL," +
				"`destWorld` VARCHAR(255) NOT NULL," +
				"`destX` DOUBLE NULL," +
				"`destY` DOUBLE NULL," +
				"`destZ` DOUBLE NULL," +
				"`destYaw` DOUBLE NULL," +
				"`destPitch` DOUBLE NULL," +
				"PRIMARY KEY (`ID`)" +
				")"
		);
		queries.put(1, sql);

		sql.clear();
		sql.add("ALTER TABLE `warpdrive_portals`" +
				"ADD COLUMN `radius` INT UNSIGNED NULL DEFAULT NULL AFTER `destPitch`;");
		queries.put(2, sql);

		return queries;
	}

	private IDatabase database;
}
