package de.tum.bgu.msm.io;

import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.vehicle.VehicleType;
import de.tum.bgu.msm.io.output.DefaultHouseholdWriter;
import de.tum.bgu.msm.io.output.HouseholdWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.PrintWriter;

public class HouseholdWriterMucDisability implements HouseholdWriter {

    private final HouseholdDataManager householdData;
    private final RealEstateDataManager realEstateData;
    private final static Logger logger = LogManager.getLogger(HouseholdWriterMucDisability.class);

    public HouseholdWriterMucDisability(HouseholdDataManager householdData, RealEstateDataManager realEstateData) {
        this.householdData = householdData;
        this.realEstateData = realEstateData;
    }

    @Override
    public void writeHouseholds(String path) {

        logger.info("  Writing household file to " + path);
        logger.info("  Using HouseholdWriterMucDisability with extended household attributes.");

        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(path, false);

        pwh.print("id,dwelling,hhSize,zone,autos");
        pwh.print(",personCount");
        pwh.print(",h.size");
        pwh.print(",h.type");
        pwh.print(",h.income");
        pwh.println();

        for (Household hh : householdData.getHouseholds()) {

            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(hh.toString());
            }

            pwh.print(hh.getId());
            pwh.print(",");

            pwh.print(hh.getDwellingId());
            pwh.print(",");

            pwh.print(hh.getHhSize());
            pwh.print(",");

            int zoneId = -1;

            if (hh.getDwellingId() > 0 &&
                    realEstateData.getDwelling(hh.getDwellingId()) != null) {
                zoneId = realEstateData.getDwelling(hh.getDwellingId()).getZoneId();
            }

            pwh.print(zoneId);
            pwh.print(",");

            pwh.print(hh.getVehicles().stream()
                    .filter(vv -> vv.getType().equals(VehicleType.CAR))
                    .count());

            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "personCount", "0"));

            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "h.size", "0"));

            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "h.type", "0"));

            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "h.income", "0"));

            pwh.println();
        }

        pwh.close();
    }

    private String getHouseholdAttributeOrDefault(Household hh, String attributeName, String defaultValue) {
        return hh.getAttribute(attributeName)
                .map(Object::toString)
                .orElse(defaultValue);
    }
}