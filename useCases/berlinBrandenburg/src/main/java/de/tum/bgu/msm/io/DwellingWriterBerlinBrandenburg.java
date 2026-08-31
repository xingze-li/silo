package de.tum.bgu.msm.io;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.Dwelling;
import de.tum.bgu.msm.io.output.DefaultDwellingWriter;
import de.tum.bgu.msm.io.output.DwellingWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.io.PrintWriter;
import java.util.Optional;

public class DwellingWriterBerlinBrandenburg implements DwellingWriter {
    private final static Logger logger = LogManager.getLogger(DefaultDwellingWriter.class);
    private final DataContainer dataContainer;

    public DwellingWriterBerlinBrandenburg(DataContainer dataContainer) {
        this.dataContainer = dataContainer;
    }

    @Override
    public void writeDwellings(String path) {
        logger.info("  Writing dwelling file to " + path);
        PrintWriter pwd = SiloUtil.openFileForSequentialWriting(path, false);
        pwd.print("id,zone,municipality,type,hhId,bedrooms,quality,monthlyCost,yearBuilt");
        pwd.print(",");
        pwd.print("floor");
        pwd.print(",");
        pwd.print("building");
        pwd.print(",");
        pwd.print("coordX");
        pwd.print(",");
        pwd.print("coordY");
        pwd.print(",d.buildingSize,d.rent,d.year,d.heating.district,d.type");
        pwd.print(",d.numberOfHeatingTypes,d.use,d.heating.stoves,d.space,d.numberOfRooms");
        pwd.print(",d.totalRent,d.heatingEnergy,d.heating.central,d.heating.floor");
        pwd.print(",d.numberOfApartments,d.buildingUsage,sourceVacantType,sourceVacantYearCategory");

        pwd.println();

        for (Dwelling dd : dataContainer.getRealEstateDataManager().getDwellings()) {
            pwd.print(dd.getId());
            pwd.print(",");
            pwd.print(dd.getZoneId());
            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "municipality", "-1"));
            pwd.print(",\"");
            pwd.print(dd.getType());
            pwd.print("\",");
            pwd.print(dd.getResidentId());
            pwd.print(",");
            pwd.print(dd.getBedrooms());
            pwd.print(",");
            pwd.print(dd.getQuality());
            pwd.print(",");
            pwd.print(dd.getPrice());
            pwd.print(",");
            pwd.print(dd.getYearBuilt());
            pwd.print(",");
            pwd.print(dd.getFloorSpace());
            pwd.print(",");
            pwd.print(dd.getUsage());
            pwd.print(",");
            Coordinate coordinate = dd.getCoordinate();
            pwd.print(coordinate == null ? -1 : coordinate.x);
            pwd.print(",");
            pwd.print(coordinate == null ? -1 : coordinate.y);

            String[] extendedAttributes = {
                    "d.buildingSize", "d.rent", "d.year", "d.heating.district", "d.type",
                    "d.numberOfHeatingTypes", "d.use", "d.heating.stoves", "d.space",
                    "d.numberOfRooms", "d.totalRent", "d.heatingEnergy", "d.heating.central",
                    "d.heating.floor", "d.numberOfApartments", "d.buildingUsage"
            };
            for (String attribute : extendedAttributes) {
                pwd.print(",");
                pwd.print(getDwellingAttributeOrDefault(dd, attribute, "0"));
            }
            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "sourceVacantType", ""));
            pwd.print(",");
            pwd.print(getDwellingAttributeOrDefault(dd, "sourceVacantYearCategory", ""));
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
            return ((Optional<?>) value).map(Object::toString).orElse(defaultValue);
        }
        return value.toString();
    }
}
