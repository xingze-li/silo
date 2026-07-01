package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.microlocation;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.job.Job;
//import de.tum.bgu.msm.data.job.JobImpl;
//import de.tum.bgu.msm.data.job.JobMuc;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GenerateJobMicrolocation {

    private static final Logger logger = LogManager.getLogger(GenerateJobMicrolocation.class);
    
    private final DataContainer dataContainer;
    private final DataSetSynPop dataSetSynPop;
    private Map<Integer, Float> jobX = new HashMap<>();
    private Map<Integer, Float> jobY = new HashMap<>();
    Map<Integer, Integer> jobZone = new HashMap<Integer, Integer>();
    Map<Integer, Map<String,Map<Integer,Float>>> zoneJobTypeJobLocationArea = new HashMap<>();
    Map<Integer, Map<String,Float>> zoneJobTypeDensity = new HashMap<>();
    Map<Integer, Map<String,Integer>> jobsByJobTypeInTAZ = new HashMap<>();
    
    public GenerateJobMicrolocation(DataContainer dataContainer, DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;
    }

    public void run() {

        logger.info("   Running module: job microlocation");
        logger.info("   Start parsing jobs information to hashmap");

        readJobFile();
        calculateDensity();
        initializeMunicipalityLookup();

        logger.info("   Start selecting job locations");

        int fallbackJobs = 0;
        int assignedJobs = 0;

        for (Job jj : dataContainer.getJobDataManager().getJobs()) {

            int zoneID = jj.getZoneId();
            jj.setAttribute("municipality", getMunicipalityByZone(zoneID));
            String jobType = normalizeJobType(jj.getType());

            Zone zone = dataContainer.getGeoData().getZones().get(zoneID);

            if (zone == null) {
                logger.warn("Job " + jj.getId() +
                        " has zone " + zoneID +
                        ", but this zone is not found in geoData. Coordinate not assigned.");
                fallbackJobs++;
                continue;
            }

            Map<String, Float> densityByType = zoneJobTypeDensity.get(zoneID);
            Map<String, Map<Integer, Float>> locationAreaByType =
                    zoneJobTypeJobLocationArea.get(zoneID);

            if (densityByType == null || locationAreaByType == null) {
                relocateRandomlyInZone(jj, zone);
                fallbackJobs++;
                continue;
            }

            Float densityObject = densityByType.get(jobType);
            Map<Integer, Float> areaByLocation = locationAreaByType.get(jobType);

            if (densityObject == null || densityObject <= 0 ||
                    areaByLocation == null || areaByLocation.isEmpty() ||
                    getSum(areaByLocation.values()) <= 0) {

                relocateRandomlyInZone(jj, zone);
                fallbackJobs++;
                continue;
            }

            int selectedJobLocationID = selectPositiveJobLocation(areaByLocation);

            if (selectedJobLocationID <= 0 ||
                    !jobX.containsKey(selectedJobLocationID) ||
                    !jobY.containsKey(selectedJobLocationID)) {

                relocateRandomlyInZone(jj, zone);
                fallbackJobs++;
                continue;
            }

            Coordinate coordinate = new Coordinate(
                    jobX.get(selectedJobLocationID),
                    jobY.get(selectedJobLocationID)
            );

            jj.relocateJob(zone, coordinate);

            float remainingArea =
                    areaByLocation.get(selectedJobLocationID) - densityObject;

            areaByLocation.put(
                    selectedJobLocationID,
                    Math.max(remainingArea, 0.0f)
            );

            assignedJobs++;
        }

        logger.info("   Finished job microlocation.");
        logger.info("   Jobs assigned to specific job locations: " + assignedJobs);
        logger.warn("   Jobs assigned randomly in TAZ because no specific job location was available: " + fallbackJobs);
    }

    private final Map<Integer, Integer> municipalityByZone = new HashMap<>();

    private void initializeMunicipalityLookup() {

        for (int row = 1; row <= PropertiesSynPop.get().main.cellsMatrix.getRowCount(); row++) {

            int zone = Math.round(
                    PropertiesSynPop.get().main.cellsMatrix.getValueAt(row, "ID_cell")
            );

            int municipality = Math.round(
                    PropertiesSynPop.get().main.cellsMatrix.getValueAt(row, "ID_city")
            );

            municipalityByZone.put(zone, municipality);
        }
    }

    private int getMunicipalityByZone(int zoneId) {
        return municipalityByZone.getOrDefault(zoneId, -1);
    }


    private String normalizeJobType(String rawJobType) {

        if (rawJobType == null) {
            return "";
        }

        String jobType = rawJobType.trim();

        /*
         * Some SILO job writers/types may represent jobs as Agri-1, Manu-1, etc.
         * For microlocation we only need the sector key: Agri, Manu, Retail, Business, Serv.
         */
        if (jobType.contains("-")) {
            jobType = jobType.substring(0, jobType.indexOf("-")).trim();
        }

        return jobType;
    }


    private void relocateRandomlyInZone(Job job, Zone zone) {

        Coordinate coordinate = zone.getRandomCoordinate(SiloUtil.getRandomObject());

        if (coordinate != null) {
            job.relocateJob(zone, coordinate);
        }
    }


    private int selectPositiveJobLocation(Map<Integer, Float> areaByLocation) {

        Map<Integer, Float> positiveAreaByLocation = new HashMap<>();

        for (Map.Entry<Integer, Float> entry : areaByLocation.entrySet()) {

            Integer locationId = entry.getKey();
            Float area = entry.getValue();

            if (locationId == null || area == null) {
                continue;
            }

            if (area <= 0 || Float.isNaN(area) || Float.isInfinite(area)) {
                continue;
            }

            if (!jobX.containsKey(locationId) || !jobY.containsKey(locationId)) {
                continue;
            }

            positiveAreaByLocation.put(locationId, area);
        }

        if (positiveAreaByLocation.isEmpty()) {
            return -1;
        }

        return SiloUtil.select(positiveAreaByLocation);
    }


    private float cleanArea(float area) {

        if (Float.isNaN(area) || Float.isInfinite(area) || area < 0) {
            return 0.0f;
        }

        return area;
    }



    private void readJobFile() {

        for (int zone : dataSetSynPop.getTazs()){
            Map<String,Map<Integer,Float>> jobLocationListForThisJobType = new HashMap<>();
            for (String jobType : PropertiesSynPop.get().main.jobStringType){
                Map<Integer,Float> jobLocationAndArea = new HashMap<>();
                jobLocationListForThisJobType.put(jobType,jobLocationAndArea);
            }
            zoneJobTypeJobLocationArea.put(zone,jobLocationListForThisJobType);
        }
        
        for (int row = 1; row <= PropertiesSynPop.get().main.jobLocationlist.getRowCount(); row++) {

            int id = (int) PropertiesSynPop.get().main.jobLocationlist.getValueAt(row,"OBJECTID");
            int zone = (int) PropertiesSynPop.get().main.jobLocationlist.getValueAt(row,"zoneID");
            float xCoordinate = PropertiesSynPop.get().main.jobLocationlist.getValueAt(row,"x");
            float yCoordinate = PropertiesSynPop.get().main.jobLocationlist.getValueAt(row,"y");
            float agriArea = cleanArea(
                    PropertiesSynPop.get().main.jobLocationlist.getValueAt(row, "jobArea1")
            );

            float manuArea = cleanArea(
                    PropertiesSynPop.get().main.jobLocationlist.getValueAt(row, "jobArea2")
            );

            float retailArea = cleanArea(
                    PropertiesSynPop.get().main.jobLocationlist.getValueAt(row, "jobArea3")
            );

            float businessArea = cleanArea(
                    PropertiesSynPop.get().main.jobLocationlist.getValueAt(row, "jobArea4")
            );

            float servArea = cleanArea(
                    PropertiesSynPop.get().main.jobLocationlist.getValueAt(row, "jobArea5")
            );
            jobZone.put(id,zone);
            jobX.put(id,xCoordinate);
            jobY.put(id,yCoordinate);


            final Map<String, Map<Integer, Float>> stringMapMap = zoneJobTypeJobLocationArea.get(zone);

            if (stringMapMap != null) {
                if (stringMapMap.get("Agri") != null) {
                    stringMapMap.get("Agri").put(id, agriArea);
                }

                if (stringMapMap.get("Manu") != null) {
                    stringMapMap.get("Manu").put(id, manuArea);
                }

                if (stringMapMap.get("Retail") != null) {
                    stringMapMap.get("Retail").put(id, retailArea);
                }

                if (stringMapMap.get("Business") != null) {
                    stringMapMap.get("Business").put(id, businessArea);
                }

                if (stringMapMap.get("Serv") != null) {
                    stringMapMap.get("Serv").put(id, servArea);
                }
            }

        }
    }

    private void calculateDensity() {

        for (int zone : dataSetSynPop.getTazs()) {

            Map<String, Integer> jobsByJobType = new HashMap<>();
            Map<String, Float> densityByJobType = new HashMap<>();

            for (String rawJobType : PropertiesSynPop.get().main.jobStringType) {
                String jobType = normalizeJobType(rawJobType);
                jobsByJobType.put(jobType, 0);
                densityByJobType.put(jobType, 0.0f);
            }

            jobsByJobTypeInTAZ.put(zone, jobsByJobType);
            zoneJobTypeDensity.put(zone, densityByJobType);
        }

        for (Job jj : dataContainer.getJobDataManager().getJobs()) {

            int zoneID = jj.getZoneId();
            String jobType = normalizeJobType(jj.getType());

            jobsByJobTypeInTAZ.putIfAbsent(zoneID, new HashMap<>());
            jobsByJobTypeInTAZ.get(zoneID).putIfAbsent(jobType, 0);

            int numberOfJobs = jobsByJobTypeInTAZ.get(zoneID).get(jobType);
            jobsByJobTypeInTAZ.get(zoneID).put(jobType, numberOfJobs + 1);
        }

        for (Map.Entry<Integer, Map<String, Integer>> zoneEntry : jobsByJobTypeInTAZ.entrySet()) {

            int zone = zoneEntry.getKey();

            Map<String, Integer> jobsByJobType = zoneEntry.getValue();
            Map<String, Map<Integer, Float>> locationAreaByJobType =
                    zoneJobTypeJobLocationArea.get(zone);

            zoneJobTypeDensity.putIfAbsent(zone, new HashMap<>());

            if (locationAreaByJobType == null) {
                continue;
            }

            for (String rawJobType : PropertiesSynPop.get().main.jobStringType) {

                String jobType = normalizeJobType(rawJobType);

                Integer numberOfJobs = jobsByJobType.get(jobType);
                Map<Integer, Float> locationAreas = locationAreaByJobType.get(jobType);

                if (numberOfJobs == null || numberOfJobs <= 0 ||
                        locationAreas == null || locationAreas.isEmpty()) {

                    zoneJobTypeDensity.get(zone).put(jobType, 0.0f);
                    continue;
                }

                float totalArea = getSum(locationAreas.values());

                if (totalArea <= 0) {
                    zoneJobTypeDensity.get(zone).put(jobType, 0.0f);
                    continue;
                }

                float density = totalArea / numberOfJobs;

                zoneJobTypeDensity.get(zone).put(jobType, density);
            }
        }
    }


    private static float getSum(Collection<? extends Number> values) {
        float sm = 0.f;
        for (Number value : values) {
            sm += value.doubleValue();
        }
        return sm;
    }
}
