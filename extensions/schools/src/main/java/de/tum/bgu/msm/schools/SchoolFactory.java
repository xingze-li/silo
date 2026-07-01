package de.tum.bgu.msm.schools;

import org.locationtech.jts.geom.Coordinate;

public interface SchoolFactory {

    School createSchool(
            int id,
            int type,
            int capacity,
            int occupancy,
            Coordinate coordinate,
            int zoneId,
            int municipality
    );

    default School createSchool(
            int id,
            int type,
            int capacity,
            int occupancy,
            Coordinate coordinate,
            int zoneId
    ) {
        return createSchool(
                id,
                type,
                capacity,
                occupancy,
                coordinate,
                zoneId,
                -1
        );
    }
}