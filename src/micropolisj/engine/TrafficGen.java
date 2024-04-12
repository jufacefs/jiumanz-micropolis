// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.*;
import static micropolisj.engine.TileConstants.*;

/**
 * Contains the code for generating city traffic.
 */
class TrafficGen
{
	final Micropolis city;
	int mapX;
	int mapY;
	ZoneType sourceZone;


	static final int MAX_TRAFFIC_DISTANCE = 30;

	public TrafficGen(Micropolis city)
	{
		this.city = city;
	}

	int makeTraffic(int mapX, int mapY)
	{
		List<CityLocation> perimeterRoad = findPerimeterRoad(mapX, mapY);
		if (!perimeterRoad.isEmpty()) //look for road on this zone's perimeter
		{

			if (tryDrive(perimeterRoad))  //attempt to drive somewhere
			{
				// success; incr trafdensity

				return 1;
			}

			return 0;
		}
		else
		{
			// no road found
			return -1;
		}
	}

	void setTrafficMem(Stack<CityLocation> positions)
	{
		while (!positions.isEmpty())
		{
			CityLocation pos = positions.pop();
			mapX = pos.x;
			mapY = pos.y;
			assert city.testBounds(mapX, mapY);

			// check for road/rail
			int tile = city.getTile(mapX, mapY);
			if (tile >= ROADBASE && tile < POWERBASE)
			{
				city.addTraffic(mapX, mapY, 50);
			}
		}
	}

	static final int [] PerimX = { -1, 0, 1,  2, 2, 2,  1, 0,-1, -2,-2,-2 };
	static final int [] PerimY = { -2,-2,-2, -1, 0, 1,  2, 2, 2,  1, 0,-1 };
	List<CityLocation> findPerimeterRoad(int mapX, int mapY)
	{

		List<CityLocation> cityLocations = new ArrayList<>();
		for (int z = 0; z < 12; z++)
		{
			int tx = mapX + PerimX[z];
			int ty = mapY + PerimY[z];

			if (roadTest(tx, ty))
			{
				cityLocations.add(new CityLocation(tx,ty));
			}
		}
		return cityLocations;
	}

	boolean roadTest(int tx, int ty)
	{
		if (!city.testBounds(tx, ty)) {
			return false;
		}

		char c = city.getTile(tx, ty);

		if (c < ROADBASE)
			return false;
		else if (c > LASTRAIL)
			return false;
		else if (c >= POWERBASE && c < LASTPOWER)
			return false;
		else
			return true;
	}

	boolean tryDrive(List<CityLocation> list)
	{
		Stack<CityLocation> positions = new Stack<CityLocation>();
		Set<CityLocation> visited = new HashSet<>();

		for (CityLocation cityLocation : list) {
			if(visited.contains(cityLocation)) {
				continue;
			}
			visited.add(cityLocation);
			CityLocation location = new CityLocation(cityLocation.x,cityLocation.y);
			for (int z = 0; z < MAX_TRAFFIC_DISTANCE; z++) //maximum distance to try
			{
				if (tryGo(visited,location,positions))
				{
					// got a road
					if (driveDone(location.x, location.y))
					{
						setTrafficMem(positions);
						// destination reached
						return true;
					}
				}
				else
				{
					// deadend, try backing up
					if (!positions.isEmpty())
					{
						CityLocation pop = positions.pop();
						location.x = pop.x;
						location.y = pop.y;
						z -= 2;
					}
				}
			}
		}

		// gone maxdis
		return false;
	}

	static final int [] DX = { 0, 1, 0, -1 };
	static final int [] DY = { -1, 0, 1, 0 };
	boolean tryGo(Set<CityLocation> visited, CityLocation cityLocation, Stack<CityLocation> positions)
	{

		for (int d = 0; d < 4; d++)
		{

			int nx = cityLocation.x + DX[d];
			int ny = cityLocation.y + DY[d];
			if(visited.contains(new CityLocation(nx,ny))) {
				continue;
			}
			if (roadTest(nx, ny))
			{
				cityLocation.x = nx;
				cityLocation.y = ny;

				// save pos every other move
				visited.add(positions.push(new CityLocation(nx, ny)));
				return true;
			}
		}

		return false;
	}

	boolean driveDone(int mapX, int mapY)
	{
		int low, high;
		switch (sourceZone)
		{
		case RESIDENTIAL:
			low = COMBASE;
			high = NUCLEAR;
			break;
		case COMMERCIAL:
			low = LHTHR;
			high = PORT;
			break;
		case INDUSTRIAL:
			low = LHTHR;
			high = COMBASE;
			break;
		case PRISON:
			low = POLICESTATIONStTART;
			high = POLICESTATIONEND;
			break;
		case POLICESTATION:

			low = PRISONSTART;
			high = PRISONEND;
			break;




		default:
			throw new Error("unreachable");
		}

		if (mapY > 0)
		{
			int tile = city.getTile(mapX, mapY-1);
			if (tile >= low && tile <= high)
				return true;
		}
		if (mapX + 1 < city.getWidth())
		{
			int tile = city.getTile(mapX + 1, mapY);
			if (tile >= low && tile <= high)
				return true;
		}
		if (mapY + 1 < city.getHeight())
		{
			int tile = city.getTile(mapX, mapY + 1);
			if (tile >= low && tile <= high)
				return true;
		}
		if (mapX > 0)
		{
			int tile = city.getTile(mapX - 1, mapY);
			if (tile >= low && tile <= high)
				return true;
		}
		return false;
	}

	/**
	 * The three main types of zones found in Micropolis.
	 */
	static enum ZoneType
	{
		RESIDENTIAL, COMMERCIAL, INDUSTRIAL, PRISON, POLICESTATION;
	}
}
