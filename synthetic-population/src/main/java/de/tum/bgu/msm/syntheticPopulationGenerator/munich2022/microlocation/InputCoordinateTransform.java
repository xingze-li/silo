package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.microlocation;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;

/** Converts input easting/northing coordinates once, before storing model locations. */
final class InputCoordinateTransform {
    private final MathTransform transform;

    InputCoordinateTransform(String source, String target) {
        try {
            transform = CRS.findMathTransform(CRS.decode(source.trim(), true),
                    CRS.decode(target.trim(), true), false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot initialize coordinate conversion: " + source + " -> " + target, e);
        }
    }

    Coordinate apply(double x, double y) {
        try {
            double[] point = {x, y};
            transform.transform(point, 0, point, 0, 1);
            if (!Double.isFinite(point[0]) || !Double.isFinite(point[1])) {
                throw new IllegalArgumentException("Non-finite transformed coordinate");
            }
            return new Coordinate(point[0], point[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot transform input coordinate " + x + ", " + y, e);
        }
    }

    static void validateZoneCrs(String source, String target, CoordinateReferenceSystem actual) {
        try {
            CoordinateReferenceSystem declared = CRS.decode(source.trim(), true);
            if (!CRS.equalsIgnoreMetadata(declared, CRS.decode(target.trim(), true))) {
                throw new IllegalArgumentException("spatial.target.crs must match zone.input.crs; reproject zone data before changing the target CRS.");
            }
            if (actual == null || !CRS.equalsIgnoreMetadata(declared, actual)) {
                throw new IllegalArgumentException("Zone shapefile CRS is missing or does not match zone.input.crs=" + source);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid zone/target CRS configuration", e);
        }
    }
}
