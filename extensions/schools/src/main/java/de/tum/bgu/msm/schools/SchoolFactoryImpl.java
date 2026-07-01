package de.tum.bgu.msm.schools;

import org.locationtech.jts.geom.Coordinate;

public class SchoolFactoryImpl implements SchoolFactory {

    @Override
    public School createSchool(
            int id,
            int type,
            int capacity,
            int occupancy,
            Coordinate coordinate,
            int zoneId,
            int municipality
    ) {
        return new SchoolImpl(
                id,
                type,
                capacity,
                occupancy,
                coordinate,
                zoneId,
                municipality
        );
    }
}