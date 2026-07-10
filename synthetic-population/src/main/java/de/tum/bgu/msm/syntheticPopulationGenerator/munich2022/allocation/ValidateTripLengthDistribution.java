package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.job.Job;

import de.tum.bgu.msm.data.person.Occupation;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.data.person.PersonMuc;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.commons.math3.stat.Frequency;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class ValidateTripLengthDistribution {

    private static final Logger logger = LogManager.getLogger(ValidateTripLengthDistribution.class);

    private final DataSetSynPop dataSetSynPop;
    private final DataContainer dataContainer;
    private TableDataSet cellsMatrix;
    private TableDataSet municipalityODMatrix;
    private TableDataSet countyODMatrix;

    public ValidateTripLengthDistribution(DataContainer dataContainer, DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;
    }

    public void run(){
        logger.info("   Running module: read population");
        initializeODmatrices();
        summarizeCommutersTripLength();
        summarizeStudentsTripLength();
    }


//    private void summarizeCommutersTripLength(){
//        ArrayList<Person> workerArrayList = obtainWorkers();
//        Frequency travelTimes = obtainWorkerFlows(workerArrayList);
//        summarizeFlows(travelTimes, "microData/interimFiles/tripLengthDistributionWork.csv");
//        SiloUtil.writeTableDataSet(municipalityODMatrix, "microData/interimFiles/odMatrixMunicipalityFinal.csv");
//        SiloUtil.writeTableDataSet(countyODMatrix, "microData/interimFiles/odMatrixCountyFinal.csv");
//    }
private void summarizeCommutersTripLength() {

    ArrayList<Person> workerArrayList =
            obtainWorkers();

    /*
     * Keep the total work trip-length distribution.
     */
    Frequency allWorkDistances =
            obtainWorkerFlows(workerArrayList);

    summarizeFlows(
            allWorkDistances,
            "microData/interimFiles/tripLengthDistributionWork.csv"
    );

    /*
     * Additional work trip-length distributions by job type.
     */
    Map<String, Frequency> workDistancesByJobType =
            obtainWorkerFlowsByJobType(workerArrayList);

    for (Map.Entry<String, Frequency> entry :
            workDistancesByJobType.entrySet()) {

        String jobType =
                entry.getKey();

        Frequency distances =
                entry.getValue();

        summarizeFlows(
                distances,
                "microData/interimFiles/tripLengthDistributionWork_" +
                        safeFileName(jobType) +
                        ".csv"
        );
    }

    SiloUtil.writeTableDataSet(
            municipalityODMatrix,
            "microData/interimFiles/odMatrixMunicipalityFinal.csv"
    );

    SiloUtil.writeTableDataSet(
            countyODMatrix,
            "microData/interimFiles/odMatrixCountyFinal.csv"
    );
}


    private void summarizeStudentsTripLength() {

        for (int schoolType = 1;
             schoolType <= 3;
             schoolType++) {

            ArrayList<Person> students =
                    obtainStudents(schoolType);

            Frequency distances =
                    obtainStudentFlows(students);

            summarizeFlows(
                    distances,
                    "microData/interimFiles/" +
                            "tripLengthDistributionSchool" +
                            schoolType +
                            ".csv"
            );
        }
    }


    private ArrayList<Person> obtainWorkers(){
        ArrayList<Person> workerArrayList = new ArrayList<>();
        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()){
            if (pp.getOccupation() == Occupation.EMPLOYED){
                workerArrayList.add(pp);
            }
        }
        return workerArrayList;
    }


//    private Frequency obtainFlows(ArrayList<Person> personArrayList){
//        Frequency commuteDistance = new Frequency();
//        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();
//        JobDataManager jobDataManager = dataContainer.getJobDataManager();
//        for (Person pp : personArrayList){
//            //TODO not part of the public person api anymore
//            if (pp.getJobId() > 0){
//                Household hh = pp.getHousehold();
//                int origin = realEstate.getDwelling(hh.getDwellingId()).getZoneId();
//                int destination = jobDataManager.getJobFromId(pp.getJobId()).getZoneId();
//                int value = (int) dataSetSynPop.getDistanceTazToTaz().getValueAt(origin, destination);
//                commuteDistance.addValue(value);
//            }
//        }
//        return commuteDistance;
//    }


//    private Frequency obtainWorkerFlows(
//            ArrayList<Person> workers
//    ) {
//        Frequency commuteDistance = new Frequency();
//
//        RealEstateDataManager realEstate =
//                dataContainer.getRealEstateDataManager();
//
//        JobDataManager jobDataManager =
//                dataContainer.getJobDataManager();
//
//        for (Person pp : workers) {
//
//            if (pp.getJobId() > 0) {
//
//                Household hh = pp.getHousehold();
//
//                int origin =
//                        realEstate
//                                .getDwelling(hh.getDwellingId())
//                                .getZoneId();
//
//                int destination =
//                        jobDataManager
//                                .getJobFromId(pp.getJobId())
//                                .getZoneId();
//
//                int distance =
//                        (int) dataSetSynPop
//                                .getDistanceTazToTaz()
//                                .getValueAt(origin, destination);
//
//                commuteDistance.addValue(distance);
//            }
//        }
//
//        return commuteDistance;
//    }
private Frequency obtainWorkerFlows(
        ArrayList<Person> workers
) {
    Frequency commuteDistance =
            new Frequency();

    RealEstateDataManager realEstate =
            dataContainer.getRealEstateDataManager();

    JobDataManager jobDataManager =
            dataContainer.getJobDataManager();

    for (Person pp : workers) {

        if (pp.getJobId() <= 0) {
            continue;
        }

        Job job =
                jobDataManager.getJobFromId(
                        pp.getJobId()
                );

        if (job == null) {
            continue;
        }

        Household hh =
                pp.getHousehold();

        int origin =
                realEstate
                        .getDwelling(
                                hh.getDwellingId()
                        )
                        .getZoneId();

        int destination =
                job.getZoneId();

        int distance =
                Math.round(
                        dataSetSynPop
                                .getDistanceTazToTaz()
                                .getValueAt(
                                        origin,
                                        destination
                                )
                );

        commuteDistance.addValue(distance);
    }

    return commuteDistance;
}

    private Map<String, Frequency> obtainWorkerFlowsByJobType(
            ArrayList<Person> workers
    ) {
        Map<String, Frequency> result =
                new TreeMap<>();

        RealEstateDataManager realEstate =
                dataContainer.getRealEstateDataManager();

        JobDataManager jobDataManager =
                dataContainer.getJobDataManager();

        for (Person pp : workers) {

            if (pp.getJobId() <= 0) {
                continue;
            }

            Job job =
                    jobDataManager.getJobFromId(
                            pp.getJobId()
                    );

            if (job == null) {
                continue;
            }

            String jobType =
                    String.valueOf(job.getType());

            if (jobType == null
                    || jobType.isBlank()
                    || jobType.equalsIgnoreCase("null")) {
                jobType = "UNKNOWN";
            }

            Household hh =
                    pp.getHousehold();

            int origin =
                    realEstate
                            .getDwelling(
                                    hh.getDwellingId()
                            )
                            .getZoneId();

            int destination =
                    job.getZoneId();

            int distance =
                    Math.round(
                            dataSetSynPop
                                    .getDistanceTazToTaz()
                                    .getValueAt(
                                            origin,
                                            destination
                                    )
                    );

            result
                    .computeIfAbsent(
                            jobType,
                            key -> new Frequency()
                    )
                    .addValue(distance);
        }

        return result;
    }

    private Frequency obtainStudentFlows(
            ArrayList<Person> students
    ) {
        Frequency schoolDistance = new Frequency();

        RealEstateDataManager realEstate =
                dataContainer.getRealEstateDataManager();

        for (Person person : students) {

            PersonMuc student = (PersonMuc) person;

            int schoolZone = student.getSchoolPlace();

            if (schoolZone <= 0) {
                continue;
            }

            Household household = student.getHousehold();

            int homeZone =
                    realEstate
                            .getDwelling(household.getDwellingId())
                            .getZoneId();

            int distance =
                    (int) dataSetSynPop
                            .getDistanceTazToTaz()
                            .getValueAt(homeZone, schoolZone);

            schoolDistance.addValue(distance);
        }

        return schoolDistance;
    }



    private void summarizeFlows(Frequency travelTimes, String fileName){
        //to obtain the trip length distribution
        int[] timeThresholds1 = new int[79];
        double[] frequencyTT1 = new double[79];
        for (int row = 0; row < timeThresholds1.length; row++) {
            timeThresholds1[row] = row + 1;
            frequencyTT1[row] = travelTimes.getCumPct(timeThresholds1[row]);
        }
        writeVectorToCSV(timeThresholds1, frequencyTT1, fileName);

    }


    private void writeVectorToCSV(
            int[] thresholds,
            double[] frequencies,
            String outputFile
    ) {
        try (
                PrintWriter writer =
                        new PrintWriter(
                                new FileWriter(outputFile, false)
                        )
        ) {
            writer.println("threshold,frequency");

            for (int i = 0; i < thresholds.length; i++) {
                writer.println(
                        thresholds[i] + "," +
                                frequencies[i]
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Cannot write trip-length distribution: " +
                            outputFile,
                    e
            );
        }
    }


    private ArrayList<Person> obtainStudents (int school){
        ArrayList<Person> workerArrayList = new ArrayList<>();
        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()) {
            if (pp.getOccupation() == Occupation.STUDENT & ((PersonMuc)pp).getSchoolType() == school) {
                workerArrayList.add(pp);
            }
        }
        return workerArrayList;
    }


    private void initializeODmatrices(){
        cellsMatrix = PropertiesSynPop.get().main.cellsMatrix;
        cellsMatrix.buildIndex(cellsMatrix.getColumnPosition("ID_cell"));
        municipalityODMatrix = new TableDataSet();
        municipalityODMatrix.appendColumn(dataSetSynPop.getCityIDs(),"id");
        for (int municipality : dataSetSynPop.getMunicipalities()){
            SiloUtil.addIntegerColumnToTableDataSet(municipalityODMatrix, Integer.toString(municipality));
        }
        municipalityODMatrix.buildIndex(municipalityODMatrix.getColumnPosition("id"));
        countyODMatrix = new TableDataSet();
        countyODMatrix.appendColumn(dataSetSynPop.getCountyIDs(), "id");
        for (int county : dataSetSynPop.getCounties()){
            SiloUtil.addIntegerColumnToTableDataSet(countyODMatrix, Integer.toString(county));
        }
        countyODMatrix.buildIndex(countyODMatrix.getColumnPosition("id"));
    }

    private String safeFileName(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value
                .trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "_"
                );
    }

}
