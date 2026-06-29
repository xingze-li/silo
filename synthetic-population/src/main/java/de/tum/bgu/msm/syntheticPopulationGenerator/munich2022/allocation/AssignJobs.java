package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import com.google.common.math.LongMath;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.common.matrix.RowVector;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.job.Job;
import de.tum.bgu.msm.data.person.Occupation;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

public class AssignJobs {

    private static final Logger logger = LogManager.getLogger(AssignJobs.class);

    private final DataSetSynPop dataSetSynPop;
    private final DataContainer dataContainer;
    private Matrix distanceImpedance;

    private HashMap<String, Integer> jobIntTypes;
    protected HashMap<Integer, int[]> idVacantJobsByZoneType;
    protected HashMap<Integer, Integer> numberVacantJobsByType;
    protected HashMap<Integer, int[]> idZonesVacantJobsByType;
    protected HashMap<Integer, Integer> numberVacantJobsByZoneByType;
    protected HashMap<Integer, Integer> numberZonesByType;

    private String[] jobStringTypes;
    private ArrayList<Person> workerArrayList;
    private int assignedJobs;
    private int[] tazIds;

    private HashMap<Person, Integer> educationalLevel;

    public AssignJobs(DataContainer dataContainer, DataSetSynPop dataSetSynPop, HashMap<Person, Integer> educationalLevel){
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;
        this.educationalLevel = educationalLevel;
    }


    public void run() {
        logger.info("   Running module: job allocation");
        calculateDistanceImpedance();
        identifyVacantJobsByZoneType();
        shuffleWorkers();
        logger.info("Number of workers " + workerArrayList.size());
        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();
//        HouseholdDataManager households = dataContainer.getHouseholdDataManager();
        int skippedNoJobType = 0;
        int skippedUnsupportedJobType = 0;
        int skippedNoSameTypeVacancy = 0;

        for (Person pp : workerArrayList) {

            String desiredJobType = getDesiredJobType(pp);

            if (desiredJobType.isEmpty()) {
                skippedNoJobType++;
                continue;
            }

            Integer selectedJobTypeObject = jobIntTypes.get(desiredJobType);

            if (selectedJobTypeObject == null) {
                skippedUnsupportedJobType++;
                continue;
            }

            int selectedJobType = selectedJobTypeObject;

            Household hh = pp.getHousehold();
            int origin = realEstate.getDwelling(hh.getDwellingId()).getZoneId();

            int[] workplace = selectWorkplace(origin, selectedJobType);

            if (workplace[0] <= 0) {
                skippedNoSameTypeVacancy++;
                continue;
            }

            Integer numberVacant = numberVacantJobsByZoneByType.get(workplace[0]);
            int[] jobIds = idVacantJobsByZoneType.get(workplace[0]);

            if (numberVacant == null || numberVacant <= 0 || jobIds == null) {
                skippedNoSameTypeVacancy++;
                continue;
            }

            int jobID = jobIds[numberVacant - 1];

            setWorkerAndJob(pp, jobID);
            updateMaps(selectedJobType, workplace);

            assignedJobs++;

            if (LongMath.isPowerOfTwo(assignedJobs)) {
                logger.info("   Assigned " + assignedJobs + " jobs.");
            }
        }

        logger.info("   Finished job allocation. Assigned " + assignedJobs + " jobs.");
        logger.info("   Skipped employed persons without valid jobType: " + skippedNoJobType);
        logger.info("   Skipped employed persons with unsupported jobType: " + skippedUnsupportedJobType);
        logger.info("   Skipped employed persons because no same-type vacancy exists: " + skippedNoSameTypeVacancy);
        logger.info("   Finished job allocation. Assigned " + assignedJobs + " jobs.");
    }


   private void calculateDistanceImpedance(){

        distanceImpedance = new Matrix(dataSetSynPop.getDistanceTazToTaz().getRowCount(), dataSetSynPop.getDistanceTazToTaz().getColumnCount());
        Map<Integer, Float> utilityHBW = dataSetSynPop.getTripLengthDistribution().column("HBW");
        for (int i = 1; i <= dataSetSynPop.getDistanceTazToTaz().getRowCount(); i ++){
            for (int j = 1; j <= dataSetSynPop.getDistanceTazToTaz().getColumnCount(); j++){
                int distance = (int) dataSetSynPop.getDistanceTazToTaz().getValueAt(i,j);
                float utility = 0.00000001f;
                if (distance < 200){
                    utility = utilityHBW.get(distance);
                }
                distanceImpedance.setValueAt(i, j, utility);
            }
        }
    }


    private void setWorkerAndJob(Person pp, int jobID){

        dataContainer.getJobDataManager().getJobFromId(jobID).setWorkerID(pp.getId());
        int jobTAZ = dataContainer.getJobDataManager().getJobFromId(jobID).getZoneId();
        pp.setWorkplace(jobID);
    }

    private String getDesiredJobType(Person person) {

        return person.getAttribute("jobType")
                .map(Object::toString)
                .orElse("")
                .trim();
    }


    private int[] selectWorkplace(int homeTaz, int selectedJobType) {

        int[] workplace = new int[2];
        workplace[0] = -2;
        workplace[1] = -1;

        Integer numberOfZonesObject = numberZonesByType.get(selectedJobType);

        if (numberOfZonesObject == null || numberOfZonesObject <= 0) {
            return workplace;
        }

        int numberOfZones = numberOfZonesObject;

        int[] ids = idZonesVacantJobsByType.get(selectedJobType);

        if (ids == null || ids.length == 0) {
            return workplace;
        }

        double[] probs = new double[numberOfZones];

        RowVector distances = distanceImpedance.getRow(homeTaz);

        IntStream.range(0, probs.length).parallel().forEach(index -> {

            int zoneType = ids[index];

            Integer numberVacant = numberVacantJobsByZoneByType.get(zoneType);

            if (numberVacant == null || numberVacant <= 0) {
                probs[index] = 0;
                return;
            }

            int destinationTaz = zoneType / 100;

            double distanceWeight = distances.getValueAt(destinationTaz);

            if (Double.isNaN(distanceWeight) || Double.isInfinite(distanceWeight) || distanceWeight < 0) {
                probs[index] = 0;
                return;
            }

            probs[index] = distanceWeight * Math.pow(numberVacant, 0.45);
        });

        double sumProbability = Arrays.stream(probs).sum();

        if (sumProbability <= 0) {
            return workplace;
        }

        return select(probs, ids);
    }


    private void shuffleWorkers(){

        workerArrayList = new ArrayList<>();
        //All employed persons look for employment, regardless they have already assigned one. That's why also workplace and jobTAZ are set to -1
        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()){
            if (pp.getOccupation() == Occupation.EMPLOYED){
                workerArrayList.add(pp);
                pp.setWorkplace(-1);
            }
        }
        Collections.shuffle(workerArrayList);
        assignedJobs = 0;
    }


    private void identifyVacantJobsByZoneType() {

        logger.info("  Identifying vacant jobs by zone");

        Collection<Job> jobs = dataContainer.getJobDataManager().getJobs();

        /*
         * Use one consistent, trimmed job type array everywhere.
         * Expected:
         * Agri, Manu, Retail, Business, Serv
         */
        jobStringTypes = Arrays.stream(PropertiesSynPop.get().main.jobStringType)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        jobIntTypes = new HashMap<>();

        for (int i = 0; i < jobStringTypes.length; i++) {
            jobIntTypes.put(jobStringTypes[i], i);
        }

        tazIds = dataSetSynPop.getTazs().stream().mapToInt(i -> i).toArray();

        idVacantJobsByZoneType = new HashMap<>();
        numberVacantJobsByType = new HashMap<>();
        idZonesVacantJobsByType = new HashMap<>();
        numberZonesByType = new HashMap<>();
        numberVacantJobsByZoneByType = new HashMap<>();

        int[] cellsID = PropertiesSynPop.get().main.cellsMatrix.getColumnAsInt("ID_cell");

        /*
         * Initialize all counters.
         * typeZone key = type + zoneId * 100
         */
        for (String jobType : jobStringTypes) {

            int type = jobIntTypes.get(jobType);

            numberZonesByType.put(type, 0);
            numberVacantJobsByType.put(type, 0);

            for (int cellId : cellsID) {
                int typeZone = type + cellId * 100;
                numberVacantJobsByZoneByType.put(typeZone, 0);
            }
        }

        /*
         * First pass:
         * count vacant jobs by type and by zone/type.
         */
        int count = 0;

        for (Job jj : jobs) {

            jj.setWorkerID(-1);

            String jobType = jj.getType().trim();
            Integer typeObject = jobIntTypes.get(jobType);

            if (typeObject == null) {
                throw new RuntimeException(
                        "Unknown job type in generated jobs: '" + jobType + "'. " +
                                "Valid job types are: " + Arrays.toString(jobStringTypes)
                );
            }

            int type = typeObject;
            int typeZone = type + jj.getZoneId() * 100;

            if (!numberVacantJobsByZoneByType.containsKey(typeZone)) {
                throw new RuntimeException(
                        "Job " + jj.getId() + " has zone " + jj.getZoneId() +
                                ", but this zone is not found in cellsMatrix ID_cell."
                );
            }

            if (numberVacantJobsByZoneByType.get(typeZone) == 0) {
                numberZonesByType.put(type, numberZonesByType.get(type) + 1);
            }

            numberVacantJobsByType.put(type, numberVacantJobsByType.get(type) + 1);
            numberVacantJobsByZoneByType.put(
                    typeZone,
                    numberVacantJobsByZoneByType.get(typeZone) + 1
            );

            count++;
        }

        logger.info("Number of vacant jobs " + count);

        /*
         * Create arrays and reset zone/type counters.
         */
        for (String jobType : jobStringTypes) {

            int type = jobIntTypes.get(jobType);

            int[] zoneArray = SiloUtil.createArrayWithValue(
                    numberZonesByType.get(type),
                    0
            );

            idZonesVacantJobsByType.put(type, zoneArray);
            numberZonesByType.put(type, 0);

            for (int cellId : cellsID) {

                int typeZone = type + cellId * 100;

                int[] jobIdArray = SiloUtil.createArrayWithValue(
                        numberVacantJobsByZoneByType.get(typeZone),
                        0
                );

                idVacantJobsByZoneType.put(typeZone, jobIdArray);
                numberVacantJobsByZoneByType.put(typeZone, 0);
            }
        }

        /*
         * Second pass:
         * fill arrays with job IDs and active zone/type IDs.
         */
        for (Job jj : jobs) {

            String jobType = jj.getType().trim();
            int type = jobIntTypes.get(jobType);
            int typeZone = type + jj.getZoneId() * 100;

            int[] jobIds = idVacantJobsByZoneType.get(typeZone);

            if (jobIds == null) {
                throw new RuntimeException(
                        "No job ID array found for typeZone " + typeZone +
                                ", job type " + jobType +
                                ", zone " + jj.getZoneId()
                );
            }

            int currentCount = numberVacantJobsByZoneByType.get(typeZone);

            jobIds[currentCount] = jj.getId();

            if (currentCount == 0) {

                int[] activeZones = idZonesVacantJobsByType.get(type);
                int currentZoneCount = numberZonesByType.get(type);

                activeZones[currentZoneCount] = typeZone;

                numberZonesByType.put(type, currentZoneCount + 1);
            }

            numberVacantJobsByZoneByType.put(typeZone, currentCount + 1);
        }
    }


    private void updateMaps(int selectedJobType, int[] zoneType) {

        int typeZone = zoneType[0];
        int zoneArrayPosition = zoneType[1];

        int remainingJobsInZoneType =
                numberVacantJobsByZoneByType.get(typeZone) - 1;

        numberVacantJobsByZoneByType.put(
                typeZone,
                remainingJobsInZoneType
        );

        numberVacantJobsByType.put(
                selectedJobType,
                numberVacantJobsByType.get(selectedJobType) - 1
        );

        if (remainingJobsInZoneType < 1) {

            int currentNumberZones = numberZonesByType.get(selectedJobType);
            int lastIndex = currentNumberZones - 1;

            if (zoneArrayPosition != lastIndex) {
                idZonesVacantJobsByType.get(selectedJobType)[zoneArrayPosition] =
                        idZonesVacantJobsByType.get(selectedJobType)[lastIndex];
            }

            numberZonesByType.put(
                    selectedJobType,
                    currentNumberZones - 1
            );
        }
    }


    public static int[] select (double[] probabilities, int[] id) {
        // select item based on probabilities (for zero-based float array)
        double sumProb = Arrays.stream(probabilities).sum();
        int[] results = new int[2];
        double selPos = sumProb * SiloUtil.getRandomNumberAsFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                results[0] = id[i];
                results[1] = i;
                return results;
            }
        }
        results[0] = id[probabilities.length - 1];
        results[1] = probabilities.length - 1;
        return results;
    }
}
