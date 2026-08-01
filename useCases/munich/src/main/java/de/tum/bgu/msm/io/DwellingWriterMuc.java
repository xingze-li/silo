package de.tum.bgu.msm.io;

import de.tum.bgu.msm.data.dwelling.Dwelling;
import de.tum.bgu.msm.io.output.DwellingWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import de.tum.bgu.msm.data.dwelling.DwellingUsage;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Optional;

public class DwellingWriterMuc implements DwellingWriter {
    private final static Logger logger = LogManager.getLogger(DwellingWriterMuc.class);

    private final Collection<Dwelling> dwellings;

    public DwellingWriterMuc(Collection<Dwelling> dwellings) {
        this.dwellings = dwellings;
    }

    @Override
    public void writeDwellings(String path) {

        logger.info("  Writing dwelling file to " + path);
        logger.info("  Using DwellingWriterMuc with extended dwelling attributes.");

        PrintWriter pwd = SiloUtil.openFileForSequentialWriting(path, false);

        pwd.print("id,zone,municipality,hhId,type,bedrooms,quality,monthlyCost,yearBuilt,usage,coordX,coordY");

        pwd.print(",d.buildingSize");
        pwd.print(",d.rent");
        pwd.print(",d.year");
        pwd.print(",d.heating.district");
        pwd.print(",d.type");
        pwd.print(",d.numberOfHeatingTypes");
        pwd.print(",d.use");
        pwd.print(",d.heating.stoves");
        pwd.print(",d.space");
        pwd.print(",d.numberOfRooms");
        pwd.print(",d.totalRent");
        pwd.print(",d.heatingEnergy");
        pwd.print(",d.heating.central");
        pwd.print(",d.heating.floor");
        pwd.print(",d.numberOfApartments");
        pwd.print(",d.buildingUsage");
        pwd.print(",sourceVacantType");
        pwd.print(",sourceVacantYearCategory");

        pwd.println();

        for (Dwelling dd : dwellings) {

            pwd.print(dd.getId());
            pwd.print(",");

            pwd.print(dd.getZoneId());
            pwd.print(",");

            pwd.print(getDwellingAttributeOrDefault(dd, "municipality", "-1"));
            pwd.print(",");

            pwd.print(dd.getResidentId());
            pwd.print(",");

            pwd.print(dd.getType());
            pwd.print(",");

            pwd.print(dd.getBedrooms());
            pwd.print(",");

            pwd.print(dd.getQuality());
            pwd.print(",");

            pwd.print(dd.getPrice());
            pwd.print(",");

            pwd.print(dd.getYearBuilt());
            pwd.print(",");
            pwd.print(dd.getUsage());

            Coordinate coordinate = dd.getCoordinate();

            pwd.print(",");

            if (coordinate == null) {
                pwd.print("-1");
                pwd.print(",");
                pwd.print("-1");
            } else {
                pwd.print(coordinate.x);
                pwd.print(",");
                pwd.print(coordinate.y);
            }

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.buildingSize", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.rent", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.year", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.heating.district", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.type", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.numberOfHeatingTypes", "0"));

            pwd.print(",");
            pwd.print(
                    getDwellingAttributeOrDefault(
                            dd,
                            "d.use",
                            dwellingUsageToMicroCode(dd.getUsage())
                    )
            );

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.heating.stoves", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.space", "0"));

            pwd.print(",");
            pwd.print(
                    getDwellingAttributeOrDefault(
                            dd,
                            "d.numberOfRooms",
                            Integer.toString(
                                    Math.max(1, dd.getBedrooms() + 1)
                            )
                    )
            );

            pwd.print(",");
            pwd.print(
                    getDwellingAttributeOrDefault(
                            dd,
                            "d.totalRent",
                            Integer.toString(dd.getPrice())
                    )
            );

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.heatingEnergy", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.heating.central", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.heating.floor", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.numberOfApartments", "0"));

            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "d.buildingUsage", "0"));

            pwd.print(",");
            pwd.print(
                    getDwellingAttributeOrDefault(
                            dd,
                            "sourceVacantType",
                            ""
                    )
            );

            pwd.print(",");
            pwd.print(
                    getDwellingAttributeOrDefault(
                            dd,
                            "sourceVacantYearCategory",
                            ""
                    )
            );

            pwd.println();

            if (dd.getId() == SiloUtil.trackDd) {
                SiloUtil.trackingFile("Writing dd " + dd.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(dd.toString());
            }
        }

        pwd.close();
    }

    private String getDwellingAttributeOrDefault(Dwelling dwelling, String key, String defaultValue) {

        Object value = dwelling.getAttribute(key);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Optional) {
            Optional<?> optionalValue = (Optional<?>) value;
            return optionalValue.map(Object::toString).orElse(defaultValue);
        }

        return value.toString();
    }

    private String dwellingUsageToMicroCode(
            DwellingUsage usage
    ) {
        if (usage == DwellingUsage.VACANT) {
            return "5";
        }

        if (usage == DwellingUsage.RENTED) {
            return "3";
        }

        if (usage == DwellingUsage.OWNED) {
            return "1";
        }

        return "0";
    }
}
