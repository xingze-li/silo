package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.preparation;


//import de.tum.bgu.msm.data.dwelling.DefaultDwellingTypes;
//import de.tum.bgu.msm.data.dwelling.DwellingType;
import de.tum.bgu.msm.data.MunichDwellingTypes.DwellingTypeMunich;
import de.tum.bgu.msm.data.dwelling.DwellingUsage;
import de.tum.bgu.msm.data.person.Gender;
import de.tum.bgu.msm.data.person.Nationality;
import de.tum.bgu.msm.data.person.PersonRole;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class MicroDataManager {

    private static final Logger logger = LogManager.getLogger(MicroDataManager.class);

    private final DataSetSynPop dataSetSynPop;

    public MicroDataManager(DataSetSynPop dataSetSynPop){this.dataSetSynPop = dataSetSynPop;}


    public HashMap<String, String[]> attributesMicroData(){

        HashMap<String, String[]> attributesMicroData = new HashMap<>();
        String[] attributesPerson = {"p.gender", "p.age", "p.maritalStatus", "p.income", "p.nationality", "p.federal", "p.municipalityType", "p.employmentStatus", "p.school", "p.education", "p.privateHousehold", "p.partnerInHousehold", "p.householdRole", "p.disability", "p.disabilityDegree", "p.jobType", "p.homeOffice", "p.BMI", "p.smokeFrequency", "p.generalHealth", "p.restriction", "p.healthStatusIndex", "p.physicalImpairmentIndex"};
        String[] attributesHousehold = {"h.size", "h.income", "h.type"};
        String[] attributesDwelling = { "d.numberOfRooms", "d.space", "d.buildingUsage", "d.year","d.use", "d.heating.district", "d.heating.central", "d.heating.floor", "d.heating.stoves", "d.heatingEnergy", "d.numberOfHeatingTypes", "d.buildingSize", "d.type", "d.rent", "d.totalRent", "d.groupQuarter"};
        attributesMicroData.put("person", attributesPerson);
        attributesMicroData.put("household", attributesHousehold);
        attributesMicroData.put("dwelling", attributesDwelling);
        return attributesMicroData;
    }

    public DwellingTypeMunich translateDwellingType(int dwellingTypeCode) {

        try {
            return DwellingTypeMunich.valueOf(dwellingTypeCode);
        } catch (IllegalArgumentException e) {
            return DwellingTypeMunich.MFH;
        }
    }



    public Map<String, String> attributesPersonMicroData(){

        Map<String, String> attributesIPU = new HashMap<>();

        // IPU attributes
        attributesIPU.put("p.gender","tpgeschlecht");
        attributesIPU.put("p.age","tpalter_1");
        attributesIPU.put("p.federal","land");
        attributesIPU.put("p.nationality","db2000p");
        attributesIPU.put("p.employmentStatus","tperwerbstyp");

        // Additional attributes
        attributesIPU.put("p.maritalStatus","ab0500p");
        attributesIPU.put("p.income","dg0102p");
        attributesIPU.put("p.municipalityType","gemeindegroessenklasse");
        attributesIPU.put("p.school","dc0301p");
        attributesIPU.put("p.education","tb0007p");
        attributesIPU.put("p.privateHousehold","tpprivathh");
        attributesIPU.put("p.partnerInHousehold","tl0001p");
        attributesIPU.put("p.householdRole","th0601p");
        attributesIPU.put("p.disability","dd0100p");
        attributesIPU.put("p.disabilityDegree","dd0200p");
        attributesIPU.put("p.jobType","eb1203pug03");
        attributesIPU.put("p.homeOffice","ed4600p");
        attributesIPU.put("p.BMI", "index_bmi");
        attributesIPU.put("p.smokeFrequency", "ph171");
        attributesIPU.put("p.generalHealth","er0900p");
        attributesIPU.put("p.restriction","er0902p");
        attributesIPU.put("p.healthStatusIndex","index_gesundheitszustand");
        attributesIPU.put("p.physicalImpairmentIndex","index_koerperl_beeintraechtig");

        return attributesIPU;
    }


    public Map<String, String> attributesHouseholdMicroData(){

        Map<String, String> attributesIPU = new HashMap<>();

        // IPU attributes
        attributesIPU.put("h.size", "th0202h");
        attributesIPU.put("h.type", "tl0102p");

        // Additional attributes
        attributesIPU.put("h.income", "th0291h");//household monthly net income in euros (after correction)

        return attributesIPU;
    }


    public Map<String, String> attributesDwellingMicroData(){

        Map<String, String> attributesIPU = new HashMap<>();

        // IPU attributes
        attributesIPU.put("d.year", "ba0600h");
        attributesIPU.put("d.numberOfApartments", "ba0300h");//include the vacant ones
        attributesIPU.put("d.type", "ba0200h");
        attributesIPU.put("d.use", "ba1901h");

        // Additional attributes
        attributesIPU.put("d.space", "ba0800h"); //apartment area in qm
        attributesIPU.put("d.buildingSize", "tw0023g");
        attributesIPU.put("d.numberOfRooms", "ba1100h");
        attributesIPU.put("d.buildingUsage", "ba0100h");
        attributesIPU.put("d.heating.district", "ba1300hu01");
        attributesIPU.put("d.heating.central", "ba1300hu02");
        attributesIPU.put("d.heating.floor", "ba1300hu03");
        attributesIPU.put("d.heating.stoves", "ba1300hu04");
        attributesIPU.put("d.heatingEnergy", "ba1400h");
        attributesIPU.put("d.numberOfHeatingTypes", "tw0212w");
        attributesIPU.put("d.rent", "tw0413w");
        attributesIPU.put("d.totalRent", "ba3200h");
//        attributesIPU.put("d.groupQuarter", "dj0400h");

        return attributesIPU;
    }

    public Map<String, Set<Integer>> exceptionsMicroData() {

        Map<String, Set<Integer>> ex = new HashMap<>();

        // Helper to avoid repetition
        BiConsumer<String,Integer> add = (col, val) ->
                ex.computeIfAbsent(col, k -> new HashSet<>()).add(val);

        // tpprivathh exclusions
        add.accept("tpprivathh", 2);   // sharedAccommodation
        add.accept("tpprivathh", -4);  // onlyIKT

        // ba1100h exclusions
        add.accept("ba0100h", 2);  // mixedBuilding1
        add.accept("ba0100h", 3);  // mixedBuilding2
        add.accept("ba0100h", 4);  // dormitory
        add.accept("ba0100h", 5);  // temporalResidence
        add.accept("ba0100h", -1); // sharedAccommodation
        add.accept("ba0100h", -4); // LFSRepeatSurvey
        add.accept("ba0100h", -7); // AWBWithoutSurveyHousehold

        // land exclusions (non-Bavaria states)
        for (int state : new int[]{1,2,3,4,5,6,7,8,10,11,12,13,14,15,16}) {
            add.accept("land", state);
        }

        return ex;
    }

//    public Map<String, Map<String, Integer>> exceptionsMicroData(){
//
//        Map<String, Map<String, Integer>> exceptionsMicroData = new HashMap<>();
//
//        Map<String, Integer> sharedAccommodation = new HashMap<>();
//            sharedAccommodation.put("tpprivathh", 2);
//            exceptionsMicroData.put("sharedAccommodation", sharedAccommodation);
//
//        Map<String, Integer> mixedBuilding1 = new HashMap<>();
//            mixedBuilding1.put("ba1100h", 2);
//            exceptionsMicroData.put("mixedBuilding1", mixedBuilding1);
//
//        Map<String, Integer> mixedBuilding2 = new HashMap<>();
//            mixedBuilding2.put("ba1100h", 3);
//            exceptionsMicroData.put("mixedBuilding2", mixedBuilding2);
//
//        Map<String, Integer> dormitory = new HashMap<>();
//            dormitory.put("ba1100h", 4);
//            exceptionsMicroData.put("dormitory", dormitory);
//
//        Map<String, Integer> temporalResidence = new HashMap<>();
//            temporalResidence.put("ba1100h", 5);
//            exceptionsMicroData.put("temporalResidence", temporalResidence);
//
//        Map<String, Integer> onlyIKT = new HashMap<>();
//            onlyIKT.put("tpprivathh", -4);
//            exceptionsMicroData.put("onlyIKT", onlyIKT);
//
//        Map<String, Integer> LFSRepeatSurvey = new HashMap<>();
//            LFSRepeatSurvey.put("awbauswahlteil", 4);
//            exceptionsMicroData.put("LFSRepeatSurvey", LFSRepeatSurvey);
//
//        Map<String, Integer> AWBWithoutSurveyHousehold = new HashMap<>();
//            AWBWithoutSurveyHousehold.put("awbauswahlteil", -7);
//            exceptionsMicroData.put("AWBWithoutSurveyHousehold", AWBWithoutSurveyHousehold);
//
//        Map<String, Integer> SchleswigHolstein = new HashMap<>();
//            SchleswigHolstein.put("land", 1);
//            exceptionsMicroData.put("schleswigHolstein", SchleswigHolstein);
//        Map<String, Integer> Hamburg = new HashMap<>();
//            Hamburg.put("land", 2);
//            exceptionsMicroData.put("hamburg", Hamburg);
//        Map<String, Integer> Niedersachsen = new HashMap<>();
//            Niedersachsen.put("land", 3);
//            exceptionsMicroData.put("niedersachsen", Niedersachsen);
//        Map<String, Integer> Bremen = new HashMap<>();
//            Bremen.put("land", 4);
//            exceptionsMicroData.put("bremen", Bremen);
//        Map<String, Integer> NordrheinWestfalen = new HashMap<>();
//            NordrheinWestfalen.put("land", 5);
//            exceptionsMicroData.put("nordrheinWestfalen", NordrheinWestfalen);
//        Map<String, Integer> Hessen = new HashMap<>();
//            Hessen.put("land", 6);
//            exceptionsMicroData.put("hessen", Hessen);
//        Map<String, Integer> RheinlandPfalz = new HashMap<>();
//            RheinlandPfalz.put("land", 7);
//            exceptionsMicroData.put("rheinlandPfalz", RheinlandPfalz);
//        Map<String, Integer> BadenWuerttemberg = new HashMap<>();
//            BadenWuerttemberg.put("land", 8);
//            exceptionsMicroData.put("badenWuerttemberg", BadenWuerttemberg);
//        Map<String, Integer> Saarland = new HashMap<>();
//            Saarland.put("land", 10);
//            exceptionsMicroData.put("saarland", Saarland);
//        Map<String, Integer> Berlin = new HashMap<>();
//            Berlin.put("land", 11);
//            exceptionsMicroData.put("berlin", Berlin);
//        Map<String, Integer> Brandenburg = new HashMap<>();
//            Brandenburg.put("land", 12);
//            exceptionsMicroData.put("brandenburg", Brandenburg);
//        Map<String, Integer> MecklenburgVorpommern = new HashMap<>();
//            MecklenburgVorpommern.put("land", 13);
//            exceptionsMicroData.put("mecklenburgVorpommern", MecklenburgVorpommern);
//        Map<String, Integer> Sachsen = new HashMap<>();
//            Sachsen.put("land", 14);
//            exceptionsMicroData.put("sachsen", Sachsen);
//        Map<String, Integer> SachsenAnhalt = new HashMap<>();
//            SachsenAnhalt.put("land", 15);
//            exceptionsMicroData.put("sachsenAnhalt", SachsenAnhalt);
//        Map<String, Integer> Thueringen = new HashMap<>();
//            Thueringen.put("land", 16);
//            exceptionsMicroData.put("thueringen", Thueringen);
//
//        return exceptionsMicroData;
//    }


    public int translateIncome(int valueMicroData) {

        if (valueMicroData == 90) {
            return 0;
        }

        double[][] incomeBounds = {
                {0, 250},          // 1
                {250, 500},        // 2
                {500, 750},        // 3
                {750, 1000},       // 4
                {1000, 1250},      // 5
                {1250, 1500},      // 6
                {1500, 1750},      // 7
                {1750, 2000},      // 8
                {2000, 2250},      // 9
                {2250, 2500},      // 10
                {2500, 2750},      // 11
                {2750, 3000},      // 12
                {3000, 3250},      // 13
                {3250, 3500},      // 14
                {3500, 4000},      // 15
                {4000, 4500},      // 16
                {4500, 5000},      // 17
                {5000, 6000},      // 18
                {6000, 7000},      // 19
                {7000, 8000},      // 20
                {8000, 10000},     // 21
                {10000, 15000},    // 22
                {15000, 25000},    // 23
                {25000, Double.POSITIVE_INFINITY} // 24
        };

        if (valueMicroData < 1 || valueMicroData > incomeBounds.length) {
            return 0;
        }

        double lower = incomeBounds[valueMicroData - 1][0];
        double upper = incomeBounds[valueMicroData - 1][1];

        double lowProb =
                PropertiesSynPop.get()
                        .main
                        .incomeGammaDistribution
                        .cumulativeProbability(lower);

        double highProb =
                Double.isInfinite(upper)
                        ? 0.999999
                        : PropertiesSynPop.get()
                          .main
                          .incomeGammaDistribution
                          .cumulativeProbability(upper);

        double cumulativeProb =
                SiloUtil.getRandomNumberAsDouble()
                        * (highProb - lowProb)
                        + lowProb;

        double income =
                PropertiesSynPop.get()
                        .main
                        .incomeGammaDistribution
                        .inverseCumulativeProbability(cumulativeProb);

        return Math.max(0, (int) Math.round(income));
    }

    public Nationality translateNationality (int nationality){
        Nationality nationality1 = Nationality.GERMAN;
        if (nationality == 8){
            nationality1 = Nationality.OTHER;
        }
        return nationality1;
    }


    public PersonRole translatePersonRole (int role){
        PersonRole personRole = PersonRole.SINGLE;
        if (role == 2) {
            personRole = PersonRole.MARRIED;
        } else if (role == 3) {
            personRole = PersonRole.CHILD;
        }
        return personRole;
    }

    public PersonRole determinePersonRole(
            int householdRole,
            int age,
            int partnerInHousehold,
            int maritalStatus
    ) {
        boolean hasPartnerInHousehold =
                partnerInHousehold == 1 ||
                        partnerInHousehold == 2;

        boolean marriedByStatus =
                maritalStatus == 2 ||
                        maritalStatus == 5;

        return switch (householdRole) {

            /*
             * Household head / reference person.
             */
            case 1 -> {
                if (hasPartnerInHousehold || marriedByStatus) {
                    yield PersonRole.MARRIED;
                } else {
                    yield PersonRole.SINGLE;
                }
            }

            /*
             * Partner / spouse.
             */
            case 2 -> PersonRole.MARRIED;

            /*
             * Child.
             */
            case 3 -> PersonRole.CHILD;

            /*
             * Other household member.
             */
            case 4 -> {
                if (hasPartnerInHousehold || marriedByStatus) {
                    yield PersonRole.MARRIED;
                } else if (age >= 0 && age < 18) {
                    yield PersonRole.CHILD;
                } else {
                    yield PersonRole.SINGLE;
                }
            }

            default -> PersonRole.SINGLE;
        };
    }

    public static boolean obtainLicense(Gender gender, int age){
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


    public int guessDwellingQuality(int heatingDistrict, int heatingCentral, int heatingEnergy, int additionalHeating, int yearBuilt){
        //guess quality of dwelling based on construction year and heating characteristics.
        //kitchen and bathroom quality are not coded on the micro data
        int quality = PropertiesSynPop.get().main.numberofQualityLevels;
        if ((heatingDistrict != 1) & (heatingCentral != 1)) quality--; //reduce quality if not central or district heating
        if ((heatingEnergy > 4)|(heatingEnergy < 0)) quality--; //reduce quality if energy is not gas, electricity or heating oil (i.e. coal, wood, biomass, solar energy)
        if (additionalHeating >= 2) quality++; //increase quality if there is additional heating in the house (regardless the used energy)
        if (yearBuilt > 0) {
            float[] deteriorationProbability = {
                    0.9f, 0.8f, 0.6f, 0.3f, 0.12f,
                    0.08f, 0.05f, 0.04f, 0.04f
            };

            int index = Math.max(0, Math.min(yearBuilt - 1, deteriorationProbability.length - 1));
            float prob = deteriorationProbability[index];

            quality = quality - SiloUtil.select(new double[]{1 - prob, prob});
            quality = quality - SiloUtil.select(new double[]{1 - prob, prob});
        }
        quality = Math.max(quality, 1);      // ensure that quality never drops below 1
        quality = Math.min(quality, PropertiesSynPop.get().main.numberofQualityLevels);      // ensure that quality never excess the number of quality levels
        return quality;
    }

//    public DwellingType translateDwellingType (int buildingSize, float ddType1Prob, float ddType3Prob){
//        DefaultDwellingTypes.DefaultDwellingTypeImpl type = DefaultDwellingTypes.DefaultDwellingTypeImpl.MF234;
//        if (buildingSize < 3){
//            if (SiloUtil.getRandomNumberAsFloat() < ddType1Prob){
//                type = DefaultDwellingTypes.DefaultDwellingTypeImpl.SFD;
//            } else {
//                type = DefaultDwellingTypes.DefaultDwellingTypeImpl.SFA;
//            }
//        } else {
//            if (SiloUtil.getRandomNumberAsFloat() < ddType3Prob){
//                type = DefaultDwellingTypes.DefaultDwellingTypeImpl.MF5plus;
//            }
//        }
//        return type;
//    }


    public int guessBedrooms (int floorSpace){
        int bedrooms = 0;
        if (floorSpace < 40){
            bedrooms = 0;
        } else if (floorSpace < 60){
            bedrooms = 1;
        } else if (floorSpace < 80){
            bedrooms = 2;
        } else if (floorSpace < 100){
            bedrooms = 3;
        } else if (floorSpace < 120){
            bedrooms = 4;
        } else {
            bedrooms = 5;
        }
        return bedrooms;
    }

    public int guessBedroomsFromRoomsOrSpace(int numberOfRooms, int floorSpace) {

        if (numberOfRooms > 0) {
            return Math.max(0, Math.min(numberOfRooms, 5) - 1);
        }

        return guessBedrooms(floorSpace);
    }

    public DwellingUsage translateDwellingUsage(int use){
        DwellingUsage usage = DwellingUsage.OWNED;
        if ((use == 3)|(use == 4)){
            usage = DwellingUsage.RENTED;
        } else if (use == 5){
            usage = DwellingUsage.VACANT;
        }
        return usage;
    }

    public int guessPrice(float brw, int quality, int size, DwellingUsage use) {

        //coefficient by quality of the dwelling
        float qualityReduction = 1;
        if (quality == 1){
            qualityReduction = 0.7f;
        } else if (quality == 2){
            qualityReduction = 0.9f;
        } else if (quality == 4){
            qualityReduction = 1.1f;
        }
        //conversion from land price to the monthly rent
        float convertToMonth = 0.0057f;
        //increase price for rented dwellings
        float rentedIncrease = 1; //by default, the price is not reduced/increased
        if (use.equals(DwellingUsage.RENTED)){
            rentedIncrease = 1.2f; //rented dwelling
        } else if (use.equals(DwellingUsage.VACANT)){
            rentedIncrease = 1; //vacant dwelling
        }
        //extra costs for power, water, etc (Nebenkosten)
        int nebenKost = 150;

        float price = brw * size * qualityReduction * convertToMonth * rentedIncrease + nebenKost;
        return (int) price;
    }

    public int getObservedOrEstimatedMonthlyCost(
            int totalRent,
            float rentPerSqm,
            int groundPrice,
            int quality,
            int floorSpace,
            DwellingUsage usage
    ) {

        if (totalRent > 0) {
            return totalRent;
        }

        if (rentPerSqm > 0 && floorSpace > 0) {
            return Math.round(rentPerSqm * floorSpace + 150);
        }

        return guessPrice(groundPrice, quality, floorSpace, usage);
    }

//    public int guessFloorSpace(int floorSpace){
//        //provide the size of the building
//        int floorSpaceDwelling = 0;
//        switch (floorSpace){
//            case 60:
//                floorSpaceDwelling = (int) (30 + SiloUtil.getRandomNumberAsFloat() * 50);
//                break;
//            case 80:
//                floorSpaceDwelling = (int) (60 + SiloUtil.getRandomNumberAsFloat() * 20);
//                break;
//            case 100:
//                floorSpaceDwelling = (int) (80 + SiloUtil.getRandomNumberAsFloat() * 20);
//                break;
//            case 120:
//                floorSpaceDwelling = (int) (100 + SiloUtil.getRandomNumberAsFloat() * 20);
//                break;
//            case 2000:
//                floorSpaceDwelling = (int) (120 + SiloUtil.getRandomNumberAsFloat() * 50);
//                break;
//        }
//        return floorSpaceDwelling;
//    }

    public int dwellingYearBracket(int year){

        int yearBracket ;
        if (year < 1919){
            yearBracket = 1;
        } else if (year < 1949){
            yearBracket = 2;
        } else if (year < 1979){
            yearBracket = 3;
        } else if (year < 1991){
            yearBracket = 4;
        } else if (year < 2001){
            yearBracket = 5;
        } else if (year < 2011){
            yearBracket = 6;
        } else if (year < 2020){
            yearBracket = 7;
        }else {
            yearBracket = 10;
        }
        return yearBracket;
    }

    public int dwellingYearfromBracket(int yearBracket) {

        int year;
        int baseYear = de.tum.bgu.msm.properties.Properties.get().main.baseYear;
        int last_year_gap = baseYear - 2020;
        int max_year_gap = baseYear - 1900;
        if ((yearBracket < 3 )&&(yearBracket > 0) ) {
            year = (int) (1900 + SiloUtil.getRandomNumberAsFloat() * 49);
        } else if (yearBracket < 4) {
            year = (int) (1949 + SiloUtil.getRandomNumberAsFloat() * 30);
        } else if (yearBracket < 6) {
            year = (int) (1979 + SiloUtil.getRandomNumberAsFloat() * 22);
        } else if (yearBracket < 8) {
            year = (int) (2001 + SiloUtil.getRandomNumberAsFloat() * 19);
        } else if (yearBracket == 10) {
            year = (int) (2020 + SiloUtil.getRandomNumberAsFloat() * (last_year_gap+1));
        } else {
            year = (int) (1900 + SiloUtil.getRandomNumberAsFloat() * (max_year_gap+1));
        }

        return year;
    }

    public String translateJobType(int wz08Code) {

        if (wz08Code <= 0) {
            return "";
        }

        /*
         * WZ08 section mapping:
         * A       011-032      -> Agri
         * B-F     051-439      -> Manu
         * G-I     451-563      -> Retail
         * J-N     581-829      -> Business
         * O-U     841-990      -> Serv
         * In the microdata, leading zeros are usually lost:
         * 011 becomes 11.
         */

        if (wz08Code >= 11 && wz08Code <= 32) {
            return "Agri";
        }

        if ((wz08Code >= 51 && wz08Code <= 99)
                || (wz08Code >= 101 && wz08Code <= 439)) {
            return "Manu";
        }

        if (wz08Code >= 451 && wz08Code <= 563) {
            return "Retail";
        }

        if (wz08Code >= 581 && wz08Code <= 829) {
            return "Business";
        }

        if (wz08Code >= 841 && wz08Code <= 990) {
            return "Serv";
        }

        return "";
    }
}
