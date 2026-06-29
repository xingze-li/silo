package de.tum.bgu.msm.utils;

import de.tum.bgu.msm.data.TravelTimesWrapper;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.resources.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import java.util.Collection;

public class TravelTimeUtil {

    public static void updateTransitSkim(SkimTravelTimes travelTimes, int year, Properties properties, Collection<Zone> zones) {
        final String transitSkimFile = properties.accessibility.transitSkimFile(year);
        if (transitSkimFile.contains(".omx"))  {
            travelTimes.readSkim(TransportMode.pt, transitSkimFile,
                    properties.accessibility.transitPeakSkim, properties.accessibility.skimFileFactorTransit);
        } else if (transitSkimFile.contains(".parquet")) {
            travelTimes.readSkimFromParquet(TransportMode.pt,transitSkimFile, "FROM", "TO", "inVehTime_sec",1/60., zones);
        }
    }

    public static void updateCarSkim(SkimTravelTimes travelTimes, int year, Properties properties, Collection<Zone> zones) {
        final String carSkimFile = properties.accessibility.autoSkimFile(year);
        if (carSkimFile.contains(".omx"))  {
            travelTimes.readSkim(TransportMode.car, carSkimFile,
                    properties.accessibility.autoPeakSkim, properties.accessibility.skimFileFactorCar);
        } else if (carSkimFile.contains(".parquet")) {
            travelTimes.readSkimFromParquet(TransportMode.car,carSkimFile, "FROM", "TO", "inVehTime_sec",1/60., zones);
        }
    }
}
