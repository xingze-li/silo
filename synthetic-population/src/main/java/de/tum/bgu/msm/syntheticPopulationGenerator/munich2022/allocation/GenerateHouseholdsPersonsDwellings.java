package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.MunichDwellingTypes;
import de.tum.bgu.msm.data.MunichDwellingTypes.DwellingTypeMunich;
import de.tum.bgu.msm.data.dwelling.*;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.household.HouseholdFactory;
import de.tum.bgu.msm.data.person.*;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.preparation.MicroDataManager;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class GenerateHouseholdsPersonsDwellings {

    private static final Logger logger = LogManager.getLogger(GenerateHouseholdsPersonsDwellings.class);

    private final DataContainer dataContainer;

    private final DataSetSynPop dataSetSynPop;
    private final MicroDataManager microDataManager;
    private int previousHouseholds;
    private int previousPersons;
    private Map<Integer, Map<Integer, Float>> ddQuality;
    private int totalHouseholds;
    private float ddTypeProbOfSFAorSFD;
    private float ddTypeProbOfMF234orMF5plus;
    private Map<Integer, Float> probTAZ;
    private Map<Integer, Float> probMicroData;
    private Map<Integer, Float> probVacantBuildingSize;
    private Map<Integer, Float> probVacantFloor;
    private double[] probabilityId;
    private double sumProbabilities;
    private double[] probabilityTAZ;
    private double sumTAZs;
    private int[] ids;
    private int[] idTAZs;
    private int personCounter;
    private int householdCounter;

    private HashMap<Person, Integer> educationalLevel;

    private HouseholdDataManager householdData;


    public GenerateHouseholdsPersonsDwellings(DataContainer dataContainer, DataSetSynPop dataSetSynPop, HashMap<Person, Integer> educationalLevel){
        this.dataContainer = dataContainer;
        this.dataSetSynPop = dataSetSynPop;
        this.educationalLevel = educationalLevel;
        microDataManager = new MicroDataManager(dataSetSynPop);
    }

//    public void run(){
//        logger.info("   Running module: household, person and dwelling generation");
//        previousHouseholds = 0;
//        previousPersons = 0;
//        householdData = dataContainer.getHouseholdDataManager();
//        for (int municipality : dataSetSynPop.getMunicipalities()){
//            initializeMunicipalityData(municipality);
//            double logging = 2;
//            int it = 12;
//            int[] hhSelection = selectMultipleHouseholds(totalHouseholds);
//            int[] tazSelection = selectMultipleTAZ(totalHouseholds);
//            for (int draw = 0; draw < totalHouseholds; draw++) {
//                int hhSelected = hhSelection[draw];
//                int tazSelected = tazSelection[draw];
//                Household household = generateHousehold();
//                generateDwelling(hhSelected, household.getId(), tazSelected, municipality);
//                generatePersons(hhSelected, household);
//                if (draw == logging & draw > 2) {
//                    logger.info("   Municipality " + municipality + ". Generated household " + draw);
//                    it++;
//                    logging = Math.pow(2, it);
//                }
//            }
//        }
//    }

    public void run(){

        logger.info("   Running module: household, person and dwelling generation");

        householdData = dataContainer.getHouseholdDataManager();

        for (int municipality : dataSetSynPop.getMunicipalities()){

            initializeMunicipalityData(municipality);

            int[] hhSelection = selectBestHouseholdSimulation(municipality);

            for (int draw = 0; draw < hhSelection.length; draw++) {

                int hhSelected = hhSelection[draw];
                int tazSelected = selectTAZ();

                Household household = generateHousehold(hhSelected, municipality);

                generateDwelling(
                        hhSelected,
                        household.getId(),
                        tazSelected,
                        municipality
                );

                generatePersons(
                        hhSelected,
                        household,
                        tazSelected,
                        municipality
                );
            }

            logger.info("   Municipality " + municipality +
                    ". Finished. Generated households: " + householdCounter +
                    ", generated persons: " + personCounter);
        }
    }


    private Household generateHousehold(int hhSelected, int municipality) {

        HouseholdFactory factory = householdData.getHouseholdFactory();
        int id = householdData.getNextHouseholdId();

        Household household = factory.createHousehold(id, id, 0);

        household.setAttribute("municipality", municipality);

        copyHouseholdMicroAttributes(household, hhSelected);

        householdData.addHousehold(household);
        householdCounter++;

        return household;
    }

    private void copyHouseholdMicroAttributes(Household household, int hhSelected) {

        copyHouseholdAttribute(household, hhSelected, "personCount");
        copyHouseholdAttribute(household, hhSelected, "h.size");
        copyHouseholdAttribute(household, hhSelected, "h.type");
        copyHouseholdAttribute(household, hhSelected, "h.income");
    }

    private void copyHouseholdAttribute(Household household, int hhSelected, String columnName) {

        try {
            int value = Math.round(
                    dataSetSynPop.getHouseholdDataSet().getValueAt(hhSelected, columnName)
            );

            household.setAttribute(columnName, value);

        } catch (Exception e) {
            household.setAttribute(columnName, 0);
        }
    }


//    private void generatePersons(int hhSelected, Household hh){
//
//        int hhSize = (int) dataSetSynPop.getHouseholdDataSet().getValueAt(hhSelected, "h.Size");
//        PersonFactory factory = householdData.getPersonFactory();
//        for (int person = 0; person < hhSize; person++) {
//            int id = householdData.getNextPersonId();
//            int personSelected = (int) (dataSetSynPop.getHouseholdDataSet().getValueAt(hhSelected, "personCount") + person);
//            int age = (int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.age");
//            Gender gender = Gender.valueOf((int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.gender"));
//            Occupation occupation = Occupation.valueOf((int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.employmentStatus"));
//            Nationality nationality1 = microDataManager.translateNationality((int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.nationality"));
//            int income = microDataManager.translateIncome((int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.income"));
//            boolean license = MicroDataManager.obtainLicense(gender, age);
//            int educationDegree = (int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.education");
//            PersonRole personRole = microDataManager.translatePersonRole((int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.householdRole"));
//            int school = (int) dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.school");
//            PersonMuc pers = (PersonMuc) factory.createPerson(id, age, gender, occupation,personRole, 0, income); //(int id, int age, int gender, Race race, int occupation, int workplace, int income)
//            pers.setNationality(nationality1);
//            pers.setDriverLicense(license);
//            pers.setSchoolType(school);
//            householdData.addPerson(pers);
//            householdData.addPersonToHousehold(pers, hh);
//            educationalLevel.put(pers, educationDegree);
//            personCounter++;
//        }
//    }

    private void generatePersons(
            int hhSelected,
            Household hh,
            int tazSelected,
            int municipality
    ){

        int hhSize = Math.round(
                dataSetSynPop.getHouseholdDataSet().getValueAt(hhSelected, "h.size")
        );

        PersonFactory factory = householdData.getPersonFactory();

        for (int person = 0; person < hhSize; person++) {

            int id = householdData.getNextPersonId();

            int personSelected = Math.round(
                    dataSetSynPop.getHouseholdDataSet().getValueAt(hhSelected, "personCount")
            ) + person;

            int age = Math.round(
                    dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.age")
            );

            Gender gender = Gender.valueOf(
                    Math.round(dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.gender"))
            );

            Occupation occupation = Occupation.valueOf(
                    Math.round(dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.employmentStatus"))
            );

            Nationality nationality = microDataManager.translateNationality(
                    Math.round(dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.nationality"))
            );

            int income = microDataManager.translateIncome(
                    Math.round(dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.income"))
            );

            boolean license = MicroDataManager.obtainLicense(gender, age);

            int educationDegree = Math.round(
                    dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.education")
            );

            PersonRole personRole = microDataManager.translatePersonRole(
                    Math.round(dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.householdRole"))
            );

            PersonMuc pers = (PersonMuc) factory.createPerson(
                    id,
                    age,
                    gender,
                    occupation,
                    personRole,
                    0,
                    income
            );

            pers.setAttribute("zone", tazSelected);
            pers.setAttribute("municipality", municipality);

            copyPersonMicroAttributes(pers, personSelected);

            int rawJobType = Math.round(
                    dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.jobType")
            );

            String translatedJobType = "";

            if (occupation == Occupation.EMPLOYED) {
                translatedJobType = microDataManager.translateJobType(rawJobType);
            }

            pers.setAttribute("jobType", translatedJobType);
            pers.setAttribute("jobTypeWZ08", rawJobType);

// default work attributes
            pers.setAttribute("jobDurationType", 0);
            pers.setAttribute("jobDuration", 0);
            pers.setAttribute("jobStartTimeWorkday", 0);
            pers.setAttribute("jobStartTimeWeekend", 0);

            int school = Math.round(
                    dataSetSynPop.getPersonDataSet().getValueAt(personSelected, "p.school")
            );

            pers.setNationality(nationality);
            pers.setDriverLicense(license);
            pers.setSchoolType(school);

            householdData.addPerson(pers);
            householdData.addPersonToHousehold(pers, hh);

            educationalLevel.put(pers, educationDegree);

            personCounter++;
        }
    }

    private void copyPersonMicroAttributes(Person person, int rowPerson) {

        copyPersonAttribute(person, rowPerson, "p.BMI");
        copyPersonAttribute(person, rowPerson, "p.education");
        copyPersonAttribute(person, rowPerson, "p.privateHousehold");
        copyPersonAttribute(person, rowPerson, "p.partnerInHousehold");
        copyPersonAttribute(person, rowPerson, "p.healthStatusIndex");
        copyPersonAttribute(person, rowPerson, "p.householdRole");
        copyPersonAttribute(person, rowPerson, "p.income");
        copyPersonAttribute(person, rowPerson, "p.smokeFrequency");
        copyPersonAttribute(person, rowPerson, "p.generalHealth");
        copyPersonAttribute(person, rowPerson, "p.school");
        copyPersonAttribute(person, rowPerson, "p.disability");
        copyPersonAttribute(person, rowPerson, "p.municipalityType");
        copyPersonAttribute(person, rowPerson, "p.federal");
        copyPersonAttribute(person, rowPerson, "p.nationality");
        copyPersonAttribute(person, rowPerson, "p.maritalStatus");
        copyPersonAttribute(person, rowPerson, "p.physicalImpairmentIndex");
        copyPersonAttribute(person, rowPerson, "p.restriction");
        copyPersonAttribute(person, rowPerson, "p.homeOffice");
        copyPersonAttribute(person, rowPerson, "p.disabilityDegree");
    }

    private void copyPersonAttribute(Person person, int rowPerson, String columnName) {

        try {
            int value = Math.round(
                    dataSetSynPop.getPersonDataSet().getValueAt(rowPerson, columnName)
            );

            person.setAttribute(columnName, value);

        } catch (Exception e) {
            person.setAttribute(columnName, 0);
        }
    }

//    private void generateDwelling(int hhSelected, int idHousehold, int tazSelected, int municipality){
//
//        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();
//        int newDdId = realEstate.getNextDwellingId();
//        int yearBracket = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.year");
//        int year = microDataManager.dwellingYearfromBracket(yearBracket);
//        int floorSpace = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.space");
//        int useInteger = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.use");
//        DwellingUsage usage = DwellingUsage.valueOf(useInteger);
//        int buildingSize = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.numberOfApartments");
//        int ddHeatingEnergy = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.heatingEnergy");
//        int ddHeatingDistrict = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.heating.district");
//        int ddHeatingCentral = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.heating.central");
//        int ddAdHeating = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.numberOfHeatingTypes");
//        int quality = microDataManager.guessDwellingQuality(ddHeatingDistrict, ddHeatingCentral, ddHeatingEnergy, ddAdHeating, yearBracket);
////        DwellingType type = microDataManager.translateDwellingType(buildingSize, ddTypeProbOfSFAorSFD, ddTypeProbOfMF234orMF5plus);
//
//        //TO DO: check if this is correct
//        int typeInteger = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.type");
//        MunichDwellingTypes.DwellingTypeMunich type = MunichDwellingTypes.DwellingTypeMunich.valueOf(typeInteger);
//
////        int bedRooms = microDataManager.guessBedrooms(floorSpace);
//        int bedRooms = (int) dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.numberOfRooms");
//        int groundPrice = dataSetSynPop.getDwellingPriceByTypeAndZone().get(tazSelected).get(type);
//        int price = microDataManager.guessPrice(groundPrice, quality, floorSpace, usage);
//
//        Dwelling dwell = DwellingUtils.getFactory().createDwelling(newDdId, tazSelected, null, idHousehold, type , bedRooms, quality, price, year);
//        realEstate.addDwelling(dwell);
//        dwell.setFloorSpace(floorSpace);
//        dwell.setUsage(usage);
//    }

    private void generateDwelling(int hhSelected, int idHousehold, int tazSelected, int municipality){

        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();

        int newDdId = realEstate.getNextDwellingId();

        int yearBracket = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.year")
        );

        int year = microDataManager.dwellingYearfromBracket(yearBracket);

        int floorSpace = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.space")
        );

        int useInteger = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.use")
        );

        DwellingUsage usage = microDataManager.translateDwellingUsage(useInteger);

        int ddHeatingEnergy = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.heatingEnergy")
        );

        int ddHeatingDistrict = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.heating.district")
        );

        int ddHeatingCentral = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.heating.central")
        );

        int ddAdHeating = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.numberOfHeatingTypes")
        );

        int quality = microDataManager.guessDwellingQuality(
                ddHeatingDistrict,
                ddHeatingCentral,
                ddHeatingEnergy,
                ddAdHeating,
                yearBracket
        );

        int typeInteger = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.type")
        );

        DwellingTypeMunich type = microDataManager.translateDwellingType(typeInteger);

        Map<DwellingTypeMunich, Integer> priceMap =
                dataSetSynPop.getDwellingPriceByTypeAndZone().get(tazSelected);

        int numberOfRooms = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.numberOfRooms")
        );

        int bedRooms = microDataManager.guessBedroomsFromRoomsOrSpace(
                numberOfRooms,
                floorSpace
        );


        int groundPrice = 0;

        if (priceMap != null) {
            Integer priceForType = priceMap.get(type);

            if (priceForType != null) {
                groundPrice = priceForType;
            }
        }

        int totalRent = Math.round(
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.totalRent")
        );

        float rentPerSqm =
                dataSetSynPop.getDwellingDataSet().getValueAt(hhSelected, "d.rent");

        int price = microDataManager.getObservedOrEstimatedMonthlyCost(
                totalRent,
                rentPerSqm,
                groundPrice,
                quality,
                floorSpace,
                usage
        );

        Dwelling dwell = DwellingUtils.getFactory().createDwelling(
                newDdId,
                tazSelected,
                null,
                idHousehold,
                type,
                bedRooms,
                quality,
                price,
                year
        );

        dwell.setAttribute("municipality", municipality);

        copyDwellingMicroAttributes(dwell, hhSelected);

        realEstate.addDwelling(dwell);
        dwell.setFloorSpace(floorSpace);
        dwell.setUsage(usage);
    }

    private void copyDwellingMicroAttributes(Dwelling dwelling, int rowDwelling) {

        copyDwellingAttribute(dwelling, rowDwelling, "d.buildingSize");
        copyDwellingAttribute(dwelling, rowDwelling, "d.rent");
        copyDwellingAttribute(dwelling, rowDwelling, "d.year");
        copyDwellingAttribute(dwelling, rowDwelling, "d.heating.district");
        copyDwellingAttribute(dwelling, rowDwelling, "d.type");
        copyDwellingAttribute(dwelling, rowDwelling, "d.numberOfHeatingTypes");
        copyDwellingAttribute(dwelling, rowDwelling, "d.use");
        copyDwellingAttribute(dwelling, rowDwelling, "d.heating.stoves");
        copyDwellingAttribute(dwelling, rowDwelling, "d.space");
        copyDwellingAttribute(dwelling, rowDwelling, "d.numberOfRooms");
        copyDwellingAttribute(dwelling, rowDwelling, "d.totalRent");
        copyDwellingAttribute(dwelling, rowDwelling, "d.heatingEnergy");
        copyDwellingAttribute(dwelling, rowDwelling, "d.heating.central");
        copyDwellingAttribute(dwelling, rowDwelling, "d.heating.floor");
        copyDwellingAttribute(dwelling, rowDwelling, "d.numberOfApartments");
        copyDwellingAttribute(dwelling, rowDwelling, "d.buildingUsage");
    }

    private void copyDwellingAttribute(Dwelling dwelling, int rowDwelling, String columnName) {

        try {
            float value = dataSetSynPop.getDwellingDataSet().getValueAt(rowDwelling, columnName);

            if (Math.abs(value - Math.round(value)) < 0.0001) {
                dwelling.setAttribute(columnName, Math.round(value));
            } else {
                dwelling.setAttribute(columnName, value);
            }

        } catch (Exception e) {
            dwelling.setAttribute(columnName, 0);
        }
    }


    private int selectMicroHouseholdWithReplacement() {

        int hhSelected = SiloUtil.select(probMicroData);
        if (probMicroData.get(hhSelected) > 1){
            probMicroData.put(hhSelected, probMicroData.get(hhSelected) - 1);
        } else {
            probMicroData.remove(hhSelected);
        }
        return hhSelected;
    }


    private int selectTAZwithoutReplacement(int hhSelected){

        int taz = SiloUtil.select(probTAZ);
        return taz;
    }


//    private void initializeMunicipalityData(int municipality){
//
//        logger.info("   Municipality " + municipality + ". Starting to generate households and persons");
//        totalHouseholds = (int) PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality, "hhTotal");
////        ddTypeProbOfSFAorSFD = PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality,"ddProbSFAorSFD");
////        ddTypeProbOfMF234orMF5plus = PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality,"ddProbMF234orMF5plus");
//        probTAZ = dataSetSynPop.getProbabilityZone().get(municipality);
//        probMicroData = new HashMap<>();
//        probabilityId = new double[dataSetSynPop.getWeights().getRowCount()];
//        ids = new int[probabilityId.length];
//        sumProbabilities = 0;
//        for (int id : dataSetSynPop.getWeights().getColumnAsInt("ID")){
//            probMicroData.put(id, dataSetSynPop.getWeights().getValueAt(id, Integer.toString(municipality)));
//        }
//        for (int i = 0; i < probabilityId.length; i++){
//            sumProbabilities = sumProbabilities + dataSetSynPop.getWeights().getValueAt(i+1, Integer.toString(municipality));
//            probabilityId[i] = dataSetSynPop.getWeights().getValueAt(i+1, Integer.toString(municipality));
//            ids[i] = (int) dataSetSynPop.getWeights().getValueAt(i+1, "ID");
//        }
//        probabilityTAZ = new double[dataSetSynPop.getProbabilityZone().get(municipality).keySet().size()];
//        sumTAZs = 0;
//        probabilityTAZ = dataSetSynPop.getProbabilityZone().get(municipality).values().stream().mapToDouble(Number::doubleValue).toArray();
//        for (int i = 1; i < probabilityTAZ.length; i++){
//            probabilityTAZ[i] = probabilityTAZ[i] + probabilityTAZ[i-1];
//        }
//        idTAZs = dataSetSynPop.getProbabilityZone().get(municipality).keySet().stream().mapToInt(Number::intValue).toArray();
//        sumTAZs = dataSetSynPop.getProbabilityZone().get(municipality).values().stream().mapToDouble(Number::doubleValue).sum();
//        personCounter = 0;
//        householdCounter = 0;
//    }

    private void initializeMunicipalityData(int municipality){

        logger.info("   Municipality " + municipality + ". Starting to generate households and persons");

        totalHouseholds = Math.round(
                PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(
                        municipality,
                        "hhTotal"
                )
        );

        probTAZ = dataSetSynPop.getProbabilityZone().get(municipality);

        if (probTAZ == null || probTAZ.isEmpty()) {
            throw new RuntimeException("No TAZ probability found for municipality " + municipality);
        }

        probMicroData = new HashMap<>();

        String municipalityColumn = Integer.toString(municipality);

        for (int row = 1; row <= dataSetSynPop.getWeights().getRowCount(); row++) {

            int microHouseholdId = Math.round(
                    dataSetSynPop.getWeights().getValueAt(row, "ID")
            );

            float weight = dataSetSynPop.getWeights().getValueAt(row, municipalityColumn);

            if (weight > 0) {
                probMicroData.put(microHouseholdId, weight);
            }
        }

        if (probMicroData.isEmpty()) {
            throw new RuntimeException("No positive household weights found for municipality " + municipality);
        }

        personCounter = 0;
        householdCounter = 0;
    }


    private int selectTAZ() {
        return SiloUtil.select(probTAZ);
    }


    private int[] selectBestHouseholdSimulation(int municipality) {

        int simulationsPerMunicipality = 20; //10 times simulation and get the closest to population

        int targetPopulation = Math.round(
                PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(
                        municipality,
                        "population"
                )
        );

        int[] bestSelection = null;
        int bestError = Integer.MAX_VALUE;

        for (int simulation = 1; simulation <= simulationsPerMunicipality; simulation++) {

            int[] selected = selectHouseholdsBySizeTargets(municipality, totalHouseholds);
            int generatedPopulation = calculateGeneratedPopulation(selected);
            int error = Math.abs(generatedPopulation - targetPopulation);

            if (bestSelection == null || error < bestError) {
                bestSelection = selected;
                bestError = error;
            }
        }

        logger.info("   Municipality " + municipality +
                ". Target population: " + targetPopulation +
                ", generated population: " + calculateGeneratedPopulation(bestSelection) +
                ", absolute error: " + bestError);

        return bestSelection;
    }

    private int[] selectHouseholdsBySizeTargets(int municipality, int totalHouseholds) {

        Map<Integer, Integer> targetCountsBySizeGroup =
                readHouseholdSizeTargets(municipality, totalHouseholds);

        if (targetCountsBySizeGroup.isEmpty()) {
            return selectHouseholdsByWeightOnly(totalHouseholds);
        }

        int largestSizeGroup = Collections.max(targetCountsBySizeGroup.keySet());

        Map<Integer, Map<Integer, Float>> candidatesBySizeGroup = new LinkedHashMap<>();

        for (Map.Entry<Integer, Float> entry : probMicroData.entrySet()) {

            int microHouseholdId = entry.getKey();
            float weight = entry.getValue();

            int hhSize = Math.round(
                    dataSetSynPop.getHouseholdDataSet().getValueAt(
                            microHouseholdId,
                            "h.size"
                    )
            );

            int sizeGroup = toHouseholdSizeGroup(hhSize, largestSizeGroup);

            candidatesBySizeGroup
                    .computeIfAbsent(sizeGroup, k -> new HashMap<>())
                    .put(microHouseholdId, weight);
        }

        List<Integer> selected = new ArrayList<>();

        for (Map.Entry<Integer, Integer> targetEntry : targetCountsBySizeGroup.entrySet()) {

            int sizeGroup = targetEntry.getKey();
            int targetCount = targetEntry.getValue();

            if (targetCount <= 0) {
                continue;
            }

            Map<Integer, Float> candidates = candidatesBySizeGroup.get(sizeGroup);

            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException(
                        "No positive-weight micro households for municipality " +
                                municipality + " and household size group " + sizeGroup
                );
            }

            for (int i = 0; i < targetCount; i++) {
                selected.add(SiloUtil.select(candidates));
            }
        }

        Collections.shuffle(selected);

        return selected.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] selectHouseholdsByWeightOnly(int selections) {

        int[] selected = new int[selections];

        for (int i = 0; i < selections; i++) {
            selected[i] = SiloUtil.select(probMicroData);
        }

        return selected;
    }

    private Map<Integer, Integer> readHouseholdSizeTargets(int municipality, int totalHouseholds) {

        Map<Integer, Double> rawTargets = new LinkedHashMap<>();

        for (int size = 1; size <= 5; size++) {
            String column = "h.size." + size;

            if (PropertiesSynPop.get().main.marginalsMunicipality.containsColumn(column)) {
                double value = PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(
                        municipality,
                        column
                );
                rawTargets.put(size, Math.max(0, value));
            }
        }

        String sixOrMoreColumn = "h.size.6OrMore";
        if (PropertiesSynPop.get().main.marginalsMunicipality.containsColumn(sixOrMoreColumn)) {
            double value = PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(
                    municipality,
                    sixOrMoreColumn
            );
            rawTargets.put(6, Math.max(0, value));
        }

        if (rawTargets.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return roundTargetsToTotal(rawTargets, totalHouseholds);
    }

    private Map<Integer, Integer> roundTargetsToTotal(
            Map<Integer, Double> rawTargets,
            int totalHouseholds
    ) {

        Map<Integer, Integer> rounded = new LinkedHashMap<>();
        Map<Integer, Double> remainders = new LinkedHashMap<>();

        int floorSum = 0;

        for (Map.Entry<Integer, Double> entry : rawTargets.entrySet()) {

            int sizeGroup = entry.getKey();
            double rawValue = entry.getValue();

            int floor = (int) Math.floor(rawValue);
            double remainder = rawValue - floor;

            rounded.put(sizeGroup, floor);
            remainders.put(sizeGroup, remainder);

            floorSum += floor;
        }

        int remaining = totalHouseholds - floorSum;

        List<Integer> order = new ArrayList<>(remainders.keySet());

        if (remaining > 0) {
            order.sort((a, b) -> Double.compare(remainders.get(b), remainders.get(a)));

            for (int i = 0; i < remaining; i++) {
                int sizeGroup = order.get(i % order.size());
                rounded.put(sizeGroup, rounded.get(sizeGroup) + 1);
            }
        } else if (remaining < 0) {
            order.sort(Comparator.comparingDouble(remainders::get));

            int toRemove = -remaining;

            for (int i = 0; i < toRemove; i++) {
                int sizeGroup = order.get(i % order.size());

                if (rounded.get(sizeGroup) > 0) {
                    rounded.put(sizeGroup, rounded.get(sizeGroup) - 1);
                }
            }
        }

        return rounded;
    }

    private int toHouseholdSizeGroup(int hhSize, int largestSizeGroup) {

        if (hhSize <= 0) {
            return 1;
        }

        return Math.min(hhSize, largestSizeGroup);
    }


    private int calculateGeneratedPopulation(int[] selectedHouseholds) {

        int population = 0;

        for (int microHouseholdId : selectedHouseholds) {

            int hhSize = Math.round(
                    dataSetSynPop.getHouseholdDataSet().getValueAt(
                            microHouseholdId,
                            "h.size"
                    )
            );

            population += hhSize;
        }

        return population;
    }

    public int[] selectMultipleHouseholds(int selections) {

        int[] selected;
        selected = new int[selections];
        int completed = 0;
        for (int iteration = 0; iteration < 100; iteration++){
            int m = selections - completed;
            double[] randomChoices = new double[m];
            for (int k = 0; k < randomChoices.length; k++) {
                randomChoices[k] = SiloUtil.getRandomNumberAsDouble()*selections;
            }
            Arrays.sort(randomChoices);

            //look up for the n travellers
            int p = 0;
            double cumulative = probabilityId[p];
            for (double randomNumber : randomChoices){
                while (randomNumber > cumulative && p < probabilityId.length - 1) {
                    p++;
                    cumulative += probabilityId[p];
                }
                if (probabilityId[p] > 0) {
                    selected[completed] = ids[p];
                    completed++;
                }
            }
        }
        return selected;

    }

    private int[] selectMultipleTAZ(int selections){

        int[] selected;
        selected = new int[selections];
        int completed = 0;
        for (int iteration = 0; iteration < 100; iteration++){
            int m = selections - completed;
            //double[] randomChoice = new double[(int)(numberOfTrips*1.1) ];
            double[] randomChoices = new double[m];
            for (int k = 0; k < randomChoices.length; k++) {
                randomChoices[k] = SiloUtil.getRandomNumberAsDouble();
            }
            Arrays.sort(randomChoices);

            //look up for the n travellers
            int p = 0;
            double cumulative = probabilityTAZ[p];
            for (double randomNumber : randomChoices){
                while (randomNumber > cumulative && p < probabilityTAZ.length - 1) {
                    p++;
                    cumulative += probabilityTAZ[p];
                }
                if (probabilityTAZ[p] > 0) {
                    selected[completed] = idTAZs[p];
                    completed++;
                }
            }
        }
        return selected;
    }
}
