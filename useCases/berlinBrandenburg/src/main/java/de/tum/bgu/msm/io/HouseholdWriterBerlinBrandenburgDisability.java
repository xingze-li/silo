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
import java.util.Optional;

public class HouseholdWriterBerlinBrandenburgDisability implements HouseholdWriter {

    private final HouseholdDataManager householdData;
    private final RealEstateDataManager realEstateData;
    private final static Logger logger = LogManager.getLogger(DefaultHouseholdWriter.class);

    public HouseholdWriterBerlinBrandenburgDisability(HouseholdDataManager householdData, RealEstateDataManager realEstateData) {
        this.householdData = householdData;
        this.realEstateData = realEstateData;
    }
   
    @Override
    public void writeHouseholds(String path) {
        logger.info("  Writing household file to " + path);
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(path, false);
        pwh.println("id,dwelling,hhSize,zone,municipality,autos,personCount,h.size,h.type,h.income");
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
            if (hh.getDwellingId() > 0 && realEstateData.getDwelling(hh.getDwellingId()) != null) {
                zoneId = realEstateData.getDwelling(hh.getDwellingId()).getZoneId();
            }
            pwh.print(zoneId);
            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "municipality", "-1"));
            pwh.print(",");
            pwh.print(hh.getVehicles().stream().filter(vv -> vv.getType().equals(VehicleType.CAR)).count());
            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "personCount", "0"));
            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "h.size", "0"));
            pwh.print(",");
            pwh.print(getHouseholdAttributeOrDefault(hh, "h.type", "0"));
            pwh.print(",");
            pwh.println(getHouseholdAttributeOrDefault(hh, "h.income", "0"));
        }
        pwh.close();
    }

    private String getHouseholdAttributeOrDefault(Household household, String key, String defaultValue) {
        Object value = household.getAttribute(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Optional) {
            return ((Optional<?>) value).map(Object::toString).orElse(defaultValue);
        }
        return value.toString();
    }
}
