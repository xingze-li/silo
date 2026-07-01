package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import com.google.common.collect.Table;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.person.Gender;
import de.tum.bgu.msm.data.person.Occupation;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.data.person.PersonMuc;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class AssignSchools {

    private static final Logger logger = LogManager.getLogger(AssignSchools.class);

    private final DataSetSynPop dataSetSynPop;
    private final DataContainer dataContainer;

    private ArrayList<Person> studentArrayList;
    private int assignedStudents;

    private Matrix distanceImpedancePrimarySecondary;
    private Matrix distanceImpedanceTertiary;
    private Map<Integer, Map<Integer,Integer>> schoolCapacityMap;
    private Map<Integer, Integer> numberOfVacantPlacesByType;
    private Map<Integer, Integer> zoneIdToMatrixIndex;

    public AssignSchools(DataContainer dataContainer, DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;
    }

    public void run() {
        logger.info("   Running module: school allocation");
//        calculateDistanceImpedance();
        initializeZoneToMatrixIndex();
        initializeSchoolCapacity();
        shuffleStudents();

        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()){
            pp.setDriverLicense(obtainLicense(pp.getGender(),pp.getAge()));
        }


        double logging = 2;
        int it = 12;
        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();
        for (Person p : studentArrayList){
            PersonMuc pp = ((PersonMuc)p);
            int schooltaz;
            Household household = pp.getHousehold();
            int hometaz = realEstate.getDwelling(household.getDwellingId()).getZoneId();
            if (pp.getSchoolType() == 3){
                schooltaz = selectTertiarySchool(hometaz);
            } else {
                schooltaz = selectPrimarySecondarySchool(hometaz, pp.getSchoolType());
            }
            if (schooltaz > 0) {
                pp.setSchoolPlace(schooltaz);
            }
            if (assignedStudents == logging){
                logger.info("   Assigned " + assignedStudents + " schools.");
                it++;
                logging = Math.pow(2, it);
            }
        }

    }

    private void initializeZoneToMatrixIndex() {

        zoneIdToMatrixIndex = new HashMap<>();

        int[] zoneIds = PropertiesSynPop.get().main.cellsMatrix.getColumnAsInt("ID_cell");

        /*
         * Build both 0-based and 1-based fallback candidates.
         * The helper method will first try real zone IDs, then mapped indices.
         */
        for (int i = 0; i < zoneIds.length; i++) {
            zoneIdToMatrixIndex.put(zoneIds[i], i);
        }
    }

    private float getDistanceByZoneIds(int originZoneId, int destinationZoneId) {

        /*
         * First try using zone IDs directly.
         * This works if the matrix uses external zone IDs as labels.
         */
        try {
            return dataSetSynPop.getDistanceTazToTaz().getValueAt(originZoneId, destinationZoneId);
        } catch (Exception ignored) {
            // Fall back to index mapping below.
        }

        Integer originIndex = zoneIdToMatrixIndex.get(originZoneId);
        Integer destinationIndex = zoneIdToMatrixIndex.get(destinationZoneId);

        if (originIndex == null || destinationIndex == null) {
            return Float.POSITIVE_INFINITY;
        }

        /*
         * Try 0-based mapped indices.
         */
        try {
            return dataSetSynPop.getDistanceTazToTaz().getValueAt(originIndex, destinationIndex);
        } catch (Exception ignored) {
            // Fall back to 1-based mapped indices below.
        }

        /*
         * Try 1-based mapped indices.
         */
        try {
            return dataSetSynPop.getDistanceTazToTaz().getValueAt(originIndex + 1, destinationIndex + 1);
        } catch (Exception ignored) {
            return Float.POSITIVE_INFINITY;
        }
    }


    private void calculateDistanceImpedance(){

        distanceImpedanceTertiary = new Matrix(dataSetSynPop.getDistanceTazToTaz().getRowCount(), dataSetSynPop.getDistanceTazToTaz().getColumnCount());
        distanceImpedancePrimarySecondary = new Matrix(dataSetSynPop.getDistanceTazToTaz().getRowCount(), dataSetSynPop.getDistanceTazToTaz().getColumnCount());
        Map<Integer, Float> utilityMapTertiary = dataSetSynPop.getTripLengthDistribution().column("Tertiary");
        for (int i = 1; i <= dataSetSynPop.getDistanceTazToTaz().getRowCount(); i ++){
            for (int j = 1; j <= dataSetSynPop.getDistanceTazToTaz().getColumnCount(); j++){
                int distance = (int) dataSetSynPop.getDistanceTazToTaz().getValueAt(i,j);
                float utilityTertiary = 0.00000001f;
                if (distance < 200){
                    utilityTertiary = utilityMapTertiary.get(distance);
                }
                distanceImpedanceTertiary.setValueAt(i,j,utilityTertiary);
                distanceImpedancePrimarySecondary.setValueAt(i,j, distance);
            }
        }
    }

    private void consumeSchoolCapacity(int schoolType, int schooltaz) {

        Map<Integer, Integer> capacityByZone = schoolCapacityMap.get(schoolType);

        if (capacityByZone == null || !capacityByZone.containsKey(schooltaz)) {
            return;
        }

        int remainingCapacity = capacityByZone.get(schooltaz) - 1;

        if (remainingCapacity > 0) {
            capacityByZone.put(schooltaz, remainingCapacity);
        } else {
            capacityByZone.remove(schooltaz);
        }

        Integer totalVacantPlaces = numberOfVacantPlacesByType.get(schoolType);

        if (totalVacantPlaces != null && totalVacantPlaces > 0) {
            numberOfVacantPlacesByType.put(schoolType, totalVacantPlaces - 1);
        }
    }

    private float getTertiaryDistanceWeight(int originZoneId, int destinationZoneId) {

        float distance = getDistanceByZoneIds(originZoneId, destinationZoneId);

        if (Float.isNaN(distance) || Float.isInfinite(distance) || distance < 0) {
            return 0f;
        }

        // 如果 distance matrix 是米，使用这一行
        int distanceKm = Math.round(distance / 1000f);

        // 如果 distance matrix 已经是 km，改成：
        // int distanceKm = Math.round(distance);

        Map<Integer, Float> utilityMapTertiary =
                dataSetSynPop.getTripLengthDistribution().column("Tertiary");

        int maxDistanceKm = utilityMapTertiary.keySet()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        int lookupDistanceKm = Math.min(distanceKm, maxDistanceKm);

        Float utility = utilityMapTertiary.get(lookupDistanceKm);

        if (utility == null || utility <= 0) {
            return 0.00000001f;
        }

        return utility;
    }


    private int selectTertiarySchool(int hometaz){

        int schoolType = 3;
        int schooltaz = -2;

        Integer totalVacantPlaces = numberOfVacantPlacesByType.get(schoolType);
        Map<Integer, Integer> candidateZones = schoolCapacityMap.get(schoolType);

        if (totalVacantPlaces == null || totalVacantPlaces <= 0 ||
                candidateZones == null || candidateZones.isEmpty()) {
            return schooltaz;
        }

        Map<Integer, Float> probability = new HashMap<>();

        for (Integer zone : new ArrayList<>(candidateZones.keySet())) {

            float distanceWeight = getTertiaryDistanceWeight(hometaz, zone);

            if (distanceWeight <= 0 ||
                    Float.isNaN(distanceWeight) ||
                    Float.isInfinite(distanceWeight)) {
                continue;
            }

            int remainingCapacity = candidateZones.get(zone);

            if (remainingCapacity <= 0) {
                continue;
            }

            probability.put(zone, distanceWeight * remainingCapacity);
        }

        if (probability.isEmpty()) {
            return schooltaz;
        }

        schooltaz = SiloUtil.select(probability);

        if (schooltaz <= 0) {
            return schooltaz;
        }

        consumeSchoolCapacity(schoolType, schooltaz);

        assignedStudents++;

        return schooltaz;
    }


    private int selectPrimarySecondarySchool(int hometaz, int schoolType){

        int schooltaz = -2;

        Integer totalVacantPlaces = numberOfVacantPlacesByType.get(schoolType);
        Map<Integer, Integer> candidateZones = schoolCapacityMap.get(schoolType);

        if (totalVacantPlaces == null || totalVacantPlaces <= 0 ||
                candidateZones == null || candidateZones.isEmpty()) {
            return schooltaz;
        }

        float minDistance = Float.POSITIVE_INFINITY;

        for (Integer zone : new ArrayList<>(candidateZones.keySet())) {

            float distance = getDistanceByZoneIds(hometaz, zone);

            if (Float.isNaN(distance) || Float.isInfinite(distance)) {
                continue;
            }

            if (distance < minDistance) {
                schooltaz = zone;
                minDistance = distance;
            }
        }

        if (schooltaz <= 0) {
            return schooltaz;
        }

        consumeSchoolCapacity(schoolType, schooltaz);

        assignedStudents++;

        return schooltaz;
    }


    private void shuffleStudents(){

        studentArrayList = new ArrayList<>();
        for (Person p : dataContainer.getHouseholdDataManager().getPersons()){
            PersonMuc pp = (PersonMuc) p;
            if (pp.getOccupation() == Occupation.STUDENT){
                studentArrayList.add(pp);
                pp.setSchoolPlace(-1);
            }
        }
        Collections.shuffle(studentArrayList);
        assignedStudents = 0;
    }


    private void initializeSchoolCapacity(){

        schoolCapacityMap = new HashMap<>();
        numberOfVacantPlacesByType = new HashMap<>();
        Table<Integer, Integer, Integer> schoolCapacity = dataSetSynPop.getSchoolCapacity();
        Iterator<Integer> iteratorRow = schoolCapacity.rowKeySet().iterator();
        while (iteratorRow.hasNext()){
            int zone = iteratorRow.next();
            Iterator<Integer> iteratorCol = schoolCapacity.columnKeySet().iterator();
            while (iteratorCol.hasNext()){
                int schoolType = iteratorCol.next();
                int places = schoolCapacity.get(zone, schoolType);
                if (places > 0) {
                    Map<Integer, Integer> prevPlaces = new HashMap<>();
                    if (schoolCapacityMap.get(schoolType)!= null) {
                        prevPlaces = schoolCapacityMap.get(schoolType);
                    }
                    prevPlaces.put(zone, places);
                    schoolCapacityMap.put(schoolType, prevPlaces);
                    int previousPlaces = 0;
                    if (numberOfVacantPlacesByType.get(schoolType)!= null){
                        previousPlaces = numberOfVacantPlacesByType.get(schoolType);
                    }
                    numberOfVacantPlacesByType.put(schoolType, previousPlaces + places);
                }
            }
        }
    }

    public boolean obtainLicense(Gender gender, int age){
        boolean license = false;
        int row = 1;
        int threshold = 0;
        if (age > 17) {
            if (age < 29) {
                if (gender == Gender.MALE) {
                    threshold = 86;
                } else {
                    threshold = 87;
                }
            } else if (age < 39) {
                if (gender == Gender.MALE) {
                    threshold = 95;
                } else {
                    threshold = 94;
                }
            } else if (age < 49) {
                if (gender == Gender.MALE) {
                    threshold = 97;
                } else {
                    threshold = 95;
                }
            } else if (age < 59) {
                if (gender == Gender.MALE) {
                    threshold = 96;
                } else {
                    threshold = 89;
                }
            } else if (age < 64) {
                if (gender == Gender.MALE) {
                    threshold = 95;
                } else {
                    threshold = 86;
                }
            } else if (age < 74) {
                if (gender == Gender.MALE) {
                    threshold = 95;
                } else {
                    threshold = 71;
                }
            } else {
                if (gender == Gender.MALE) {
                    threshold = 88;
                } else {
                    threshold = 44;
                }
            }
            if (SiloUtil.getRandomNumberAsDouble() * 100 < threshold) {
                license = true;
            }
        }
        return license;
    }

}
