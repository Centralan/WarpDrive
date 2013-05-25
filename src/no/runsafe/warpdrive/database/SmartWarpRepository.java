package no.runsafe.warpdrive.database;

import no.runsafe.framework.database.IDatabase;
import no.runsafe.framework.database.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartWarpRepository extends Repository
{
	public SmartWarpRepository(IDatabase database)
	{
		this.database = database;
	}

	@Override
	public String getTableName()
	{
		return "smartwarp_settings";
	}

	@Override
	public HashMap<Integer, List<String>> getSchemaUpdateQueries()
	{
		HashMap<Integer, List<String>> queries = new HashMap<Integer, List<String>>();
		ArrayList<String> sql = new ArrayList<String>();
		sql.add(
			"CREATE TABLE smartwarp_settings (" +
				"`world` varchar(255) NOT NULL," +
				"`range` integer NOT NULL," +
				"PRIMARY KEY(`world`)" +
			")"
		);
		queries.put(1, sql);
		return queries;
	}

	public int getRange(String world)
	{
		Map<String, Object> settings = database.QueryRow(
			"SELECT range FROM smartwarp_settings WHERE world=?",
			world
		);
		if(settings == null)
			return -1;
		return (Integer)settings.get("range");
	}

	public void setRange(String world, int range)
	{
		database.Update(
			"INSERT INTO smartwarp_settings (world, range) VALUES (?, ?)" +
				" ON DUPLICATE KEY UPDATE range=VALUES(range)",
			range, world
		);
	}

	private IDatabase database;
}
