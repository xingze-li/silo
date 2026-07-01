package de.tum.bgu.msm.schools;

import de.tum.bgu.msm.data.MicroLocation;
import org.locationtech.jts.geom.Coordinate;

public class SchoolImpl implements School, MicroLocation {
    private final int id;
    private final int type;
    private final int capacity;
    private int occupancy;
    private final Coordinate coordinate;
    private final int zoneId;
    private double startTimeInSeconds;
    private double studyTimeInSeconds;
    private final int municipality;


    public SchoolImpl(
            int id,
            int type,
            int capacity,
            int occupancy,
            Coordinate coordinate,
            int zoneId,
            int municipality
    ) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.occupancy = occupancy;
        this.coordinate = coordinate;
        this.zoneId = zoneId;
        this.municipality = municipality;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getOccupancy() {
        return occupancy;
    }

    @Override
    public int getMunicipality() {
        return municipality;
    }

    @Override
    public void setOccupancy(int occupancy) {
        this.occupancy = occupancy;
    }

    public void setSchoolStudyingTime(double startTimeInSeconds, double studyTimeInSeconds) {
        this.startTimeInSeconds = startTimeInSeconds;
        this.studyTimeInSeconds = studyTimeInSeconds;
    }

    public double getStartTimeInSeconds() {
        return startTimeInSeconds;
    }

    public double getStudyTimeInSeconds() {
        return studyTimeInSeconds;
    }

    @Override
    public int getZoneId() {
        return zoneId;
    }
}
