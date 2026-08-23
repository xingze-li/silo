package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.data.AreaTypes;
import de.tum.bgu.msm.data.Id;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.data.MunichDwellingTypes.DwellingTypeMunich;
import de.tum.bgu.msm.io.input.AbstractParquetReader;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ReadZonalData {

    private static final Logger logger = LogManager.getLogger(ReadZonalData.class);

    private final DataSetSynPop dataSetSynPop;

    private static final Set<Integer> MUNICH_COUNTIES =
            Set.of(
                    9175,
                    9188,
                    9184,
                    9178,
                    9174,
                    9162,
                    9177,
                    9179
            );

    private static final Set<Integer> AUGSBURG_COUNTIES =
            Set.of(
                    9761,
                    9771,
                    9772
            );

    private String classifyTripLengthRegion(
            int county
    ) {
        if (MUNICH_COUNTIES.contains(county)) {
            return "Munich";
        }

        if (AUGSBURG_COUNTIES.contains(county)) {
            return "Augsburg";
        }

        return "Other";
    }

    public ReadZonalData(DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;

    }

    public void run() {
        readCities();
        if (PropertiesSynPop.get().main.runAllocation ||
                PropertiesSynPop.get().main.runMicrolocation ||
                PropertiesSynPop.get().main.runJobMicrolocation ||
                PropertiesSynPop.get().main.runSchoolAllocation ||
                PropertiesSynPop.get().main.runSchoolMicrolocation) {
            readZones();
        }
        if (PropertiesSynPop.get().main.runJobAllocation ||
                PropertiesSynPop.get().main.runSchoolAllocation) {
            readDistanceMatrix();
            readTripLengthFrequencyDistribution();
        }
    }

    private void readCities() {
        int[] cityID;
        int[] countyID;
        HashMap<Integer, ArrayList> municipalitiesByCounty;
        //List of municipalities and counties that are used for IPU and allocation
        ArrayList<Integer> municipalities = new ArrayList<>();
        ArrayList<Integer> counties = new ArrayList<>();
        municipalitiesByCounty = new HashMap<>();
        ArrayList<Integer> municipalitiesWithZero = new ArrayList<>();
        for (int row = 1; row <= PropertiesSynPop.get().main.selectedMunicipalities.getRowCount(); row++) {
            boolean selected = true;
            try {
                selected = PropertiesSynPop.get().main.selectedMunicipalities.getValueAt(row, "Select") == 1f;
            } catch (RuntimeException e) {
                selected = true;
            }
            if (selected) {
                int city = (int) PropertiesSynPop.get().main.selectedMunicipalities.getValueAt(row, "ID_city");
                municipalities.add(city);
                int county = (int) PropertiesSynPop.get().main.selectedMunicipalities.getValueAt(row, "ID_county");
                if (!SiloUtil.containsElement(counties, county)) {
                    counties.add(county);
                }
                if (municipalitiesByCounty.containsKey(county)) {
                    ArrayList<Integer> citiesInThisCounty = municipalitiesByCounty.get(county);
                    citiesInThisCounty.add(city);
                    municipalitiesByCounty.put(county, citiesInThisCounty);
                } else {
                    ArrayList<Integer> citiesInThisCounty = new ArrayList<>();
                    citiesInThisCounty.add(city);
                    municipalitiesByCounty.put(county, citiesInThisCounty);
                }
            }
        }
        cityID = SiloUtil.convertArrayListToIntArray(municipalities);
        countyID = SiloUtil.convertArrayListToIntArray(counties);
        dataSetSynPop.setCityIDs(cityID);
        dataSetSynPop.setCountyIDs(countyID);
        dataSetSynPop.setMunicipalities(municipalities);
        dataSetSynPop.setCounties(counties);
        dataSetSynPop.setMunicipalitiesByCounty(municipalitiesByCounty);
        dataSetSynPop.setMunicipalitiesWithZeroPopulation(municipalitiesWithZero);

        if (PropertiesSynPop.get().main.boroughIPU) {
            HashMap<Integer, ArrayList> boroughsByCounty = new HashMap<>();
            ArrayList<Integer> boroughs = new ArrayList<>();
            ArrayList<Integer> countieswithBoroughs = new ArrayList<>();
            for (int row = 1; row <= PropertiesSynPop.get().main.selectedBoroughs.getRowCount(); row++) {
                if (PropertiesSynPop.get().main.selectedBoroughs.getValueAt(row, "Select") == 1f) {
                    int borough = (int) PropertiesSynPop.get().main.selectedBoroughs.getValueAt(row, "ID_borough");
                    int county = (int) PropertiesSynPop.get().main.selectedBoroughs.getValueAt(row, "ID_county");
                    boroughs.add(borough);
                    if (boroughsByCounty.containsKey(county)) {
                        ArrayList<Integer> boroughsInThisCity = boroughsByCounty.get(county);
                        boroughsInThisCity.add(borough);
                        boroughsByCounty.put(county, boroughsInThisCity);
                    } else {
                        ArrayList<Integer> boroughsInThisCity = new ArrayList<>();
                        boroughsInThisCity.add(borough);
                        boroughsByCounty.put(county, boroughsInThisCity);
                        countieswithBoroughs.add(county);
                    }
                }
            }
            dataSetSynPop.setBoroughsByCounty(boroughsByCounty);
            dataSetSynPop.setBoroughs(boroughs);
        }
    }


    private void readZones(){
        //TAZ attributes
        Map<Integer, String> tripLengthRegionByTaz = new HashMap<>();
        HashMap<Integer, int[]> cityTAZ = new HashMap<>();
        Map<Integer, Id> zones = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Float>> probabilityZone = new HashMap<>();
        Map<Integer, Map<DwellingTypeMunich, Integer>> dwellingPriceByTypeAndZone = new HashMap<>();
        Table<Integer, Integer, Integer> schoolCapacity = HashBasedTable.create();
        ArrayList<Integer> tazs = new ArrayList<>();
        TableDataSet zoneAttributes;
        if (!PropertiesSynPop.get().main.boroughIPU){
            zoneAttributes = PropertiesSynPop.get().main.cellsMatrix;
        } else {
            zoneAttributes = PropertiesSynPop.get().main.cellsMatrixBoroughs;
        }
        String zoneIdColumn = findColumn(zoneAttributes, "ID_cell", "Zone");
        String cityColumn = findColumn(zoneAttributes, "ID_city", "Gemeinde_ID");
        String countyColumn = findColumn(zoneAttributes, "ID_county", "Landkreis_ID");
        String populationColumn = findOptionalColumn(zoneAttributes, "population", "Population");
        String areaTypeColumn = findColumn(zoneAttributes, "BBSR", "BBSR_Type");
        if (populationColumn == null) {
            logger.warn("The TAZ definition has no population column. Using equal allocation weights within each municipality.");
        }
        if (findOptionalColumn(zoneAttributes, "ddEFHFreistehend") == null) {
            logger.warn("The TAZ definition has no dwelling-price columns. Occupied dwellings will use observed microdata costs; vacant dwellings will use donor costs.");
        }
        for (int i = 1; i <= zoneAttributes.getRowCount(); i++){
            int city = (int) zoneAttributes.getValueAt(i, cityColumn);
            int taz = (int) zoneAttributes.getValueAt(i, zoneIdColumn);
            int county = (int) zoneAttributes.getValueAt(i, countyColumn);
            float probability = populationColumn == null ? 1f : zoneAttributes.getValueAt(i, populationColumn);
            int priceEFHFreistehend = getOptionalInt(zoneAttributes, i,"ddEFHFreistehend");
            int priceEFHDoppelhaus = getOptionalInt(zoneAttributes, i,"ddEFHDoppelhaus");
            int priceEFHReihenhaus = getOptionalInt(zoneAttributes, i,"ddEFHReihenhaus");
            int priceMFH = getOptionalInt(zoneAttributes, i,"ddMFH");
            int capacityPrimary = getOptionalInt(zoneAttributes, i, "capacityPrimary");
            int capacitySecondary = getOptionalInt(zoneAttributes, i, "capacitySecondary");
            int capacityTertiary = getOptionalInt(zoneAttributes, i, "capacityTertiary");
            int bbsr = (int)zoneAttributes.getValueAt(i, areaTypeColumn);
            String tripLengthRegion = classifyTripLengthRegion(county);
            tripLengthRegionByTaz.put(taz, tripLengthRegion);

            if (!tazs.contains(taz)) {
                tazs.add(taz);
            }
            if (cityTAZ.containsKey(city)){
                int[] previousTaz = cityTAZ.get(city);
                previousTaz = SiloUtil.expandArrayByOneElement(previousTaz, taz);
                cityTAZ.put(city, previousTaz);
                Map<Integer, Float> probabilities = probabilityZone.get(city);
                probabilities.put(taz, probability);
            } else {
                int[] previousTaz = {taz};
                cityTAZ.put(city,previousTaz);
                Map<Integer, Float> probabilities = new HashMap<>();
                probabilities.put(taz, probability);
                probabilityZone.put(city, probabilities);
            }
            Map<DwellingTypeMunich, Integer> prices = new HashMap<>();
            prices.put(DwellingTypeMunich.EFHFreistehend, priceEFHFreistehend);
            prices.put(DwellingTypeMunich.EFHDoppelhaus, priceEFHDoppelhaus);
            prices.put(DwellingTypeMunich.EFHReihenhaus, priceEFHReihenhaus);
            prices.put(DwellingTypeMunich.MFH, priceMFH);
            dwellingPriceByTypeAndZone.put(taz,prices);
            schoolCapacity.put(taz,1,capacityPrimary);
            schoolCapacity.put(taz, 2, capacitySecondary);
            schoolCapacity.put(taz, 3, capacityTertiary);

            MitoZone currentZone =
                    new MitoZone(
                            taz,
                            AreaTypes.SGType.valueOf(bbsr)
                    );

            zones.put(taz, currentZone);
        }
        dataSetSynPop.setProbabilityZone(probabilityZone);
        dataSetSynPop.setDwellingPriceByTypeAndZone(dwellingPriceByTypeAndZone);
        dataSetSynPop.setTazByMunicipality(cityTAZ);
        dataSetSynPop.setSchoolCapacity(schoolCapacity);
        dataSetSynPop.setTazs(tazs);
        dataSetSynPop.setTazIDs(tazs.stream().mapToInt(i -> i).toArray());
        this.dataSetSynPop.setZones(zones);
        dataSetSynPop.setTripLengthRegionByTaz(tripLengthRegionByTaz);

        Map<String, Long> tazCountByTripLengthRegion =
                tripLengthRegionByTaz
                        .values()
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        region -> region,
                                        Collectors.counting()
                                )
                        );

        logger.info(
                "Trip length regions by TAZ: " +
                        tazCountByTripLengthRegion
        );
    }

    private String findColumn(TableDataSet table, String... candidates) {
        String column = findOptionalColumn(table, candidates);
        if (column != null) {
            return column;
        }
        throw new IllegalArgumentException(
                "None of the required columns " + Arrays.toString(candidates) + " exists in the TAZ definition.");
    }

    private String findOptionalColumn(TableDataSet table, String... candidates) {
        List<String> labels = Arrays.asList(table.getColumnLabels());
        for (String candidate : candidates) {
            if (labels.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private int getOptionalInt(TableDataSet table, int row, String column) {
        if (Arrays.asList(table.getColumnLabels()).contains(column)) {
            return Math.round(table.getValueAt(row, column));
        }
        return 0;
    }

//    private void readZones(){
//
//        HashMap<Integer, int[]> cityTAZ = new HashMap<>();
//        Map<Integer, Map<Integer, Float>> probabilityZone = new HashMap<>();
//        Map<Integer, Map<DwellingTypeMunich, Integer>> dwellingPriceByTypeAndZone = new HashMap<>();
//
//        Table<Integer, Integer, Integer> schoolCapacity = HashBasedTable.create();
//        ArrayList<Integer> tazs = new ArrayList<>();
//
//        TableDataSet zoneAttributes;
//        if (!PropertiesSynPop.get().main.boroughIPU){
//            zoneAttributes = PropertiesSynPop.get().main.cellsMatrix;
//        } else {
//            zoneAttributes = PropertiesSynPop.get().main.cellsMatrixBoroughs;
//        }
//
//        String municipalityColumn = PropertiesSynPop.get().main.boroughIPU
//                ? "ID_borough"
//                : "ID_city";
//
//        for (int i = 1; i <= zoneAttributes.getRowCount(); i++){
//
//            int city = (int) zoneAttributes.getValueAt(i, municipalityColumn);
//            int taz = (int) zoneAttributes.getValueAt(i, "ID_cell");
//
//            float probability = zoneAttributes.getValueAt(i, "Population");
//
//            int priceEFHFreistehend = (int) zoneAttributes.getValueAt(i, "ddEFHFreistehend");
//            int priceEFHDoppelhaus = (int) zoneAttributes.getValueAt(i, "ddEFHDoppelhaus");
//            int priceEFHReihenhaus = (int) zoneAttributes.getValueAt(i, "ddEFHReihenhaus");
//            int priceMFH = (int) zoneAttributes.getValueAt(i, "ddMFH");
//
//            int capacityPrimary = (int) zoneAttributes.getValueAt(i, "capacityPrimary");
//            int capacitySecondary = (int) zoneAttributes.getValueAt(i, "capacitySecondary");
//            int capacityTertiary = (int) zoneAttributes.getValueAt(i, "capacityTertiary");
//
//            if (!tazs.contains(taz)) {
//                tazs.add(taz);
//            }
//
//            if (cityTAZ.containsKey(city)){
//                int[] previousTaz = cityTAZ.get(city);
//                previousTaz = SiloUtil.expandArrayByOneElement(previousTaz, taz);
//                cityTAZ.put(city, previousTaz);
//
//                Map<Integer, Float> probabilities = probabilityZone.get(city);
//                probabilities.put(taz, probability);
//            } else {
//                int[] previousTaz = {taz};
//                cityTAZ.put(city, previousTaz);
//
//                Map<Integer, Float> probabilities = new HashMap<>();
//                probabilities.put(taz, probability);
//                probabilityZone.put(city, probabilities);
//            }
//
//            Map<DwellingTypeMunich, Integer> prices = new HashMap<>();
//            prices.put(DwellingTypeMunich.EFHFreistehend, priceEFHFreistehend);
//            prices.put(DwellingTypeMunich.EFHDoppelhaus, priceEFHDoppelhaus);
//            prices.put(DwellingTypeMunich.EFHReihenhaus, priceEFHReihenhaus);
//            prices.put(DwellingTypeMunich.MFH, priceMFH);
//
//            dwellingPriceByTypeAndZone.put(taz, prices);
//
//            schoolCapacity.put(taz, 1, capacityPrimary);
//            schoolCapacity.put(taz, 2, capacitySecondary);
//            schoolCapacity.put(taz, 3, capacityTertiary);
//        }
//
//        dataSetSynPop.setProbabilityZone(probabilityZone);
//        dataSetSynPop.setDwellingPriceByTypeAndZone(dwellingPriceByTypeAndZone);
//        dataSetSynPop.setTazByMunicipality(cityTAZ);
//        dataSetSynPop.setSchoolCapacity(schoolCapacity);
//        dataSetSynPop.setTazs(tazs);
//        dataSetSynPop.setTazIDs(tazs.stream().mapToInt(i -> i).toArray());
//    }

    private void readDistanceMatrix(){
        //Read the skim matrix
        logger.info("   Starting to read Parquet matrix");

        Collection<Id> lookup;
        lookup = this.dataSetSynPop.getZones().values();
        String parquetFile =
                PropertiesSynPop.get()
                        .main
                        .omxFileName;

        IndexedDoubleMatrix2D distanceSkimAuto =
                AbstractParquetReader.readAndConvertToDoubleMatrix(
                        parquetFile,
                        "FROM",
                        "TO",
                        "inVehDistance_m",
                        0.001,
                        lookup
                );
        this.dataSetSynPop.setDistanceTazToTaz(distanceSkimAuto);
        logger.info("   Read Parquet distance matrix from " + parquetFile);
    }

    private Table<Integer, String, Float> readTripLengthFrequencyDistributionFile(
            String fileName
    ) {
        logger.info(
                "   Reading trip length frequency distribution: " +
                        fileName
        );

        Table<Integer, String, Float> frequencies =
                HashBasedTable.create();

        String recString = "";
        int recCount = 0;

        try {
            BufferedReader in =
                    new BufferedReader(
                            new FileReader(fileName)
                    );

            recString = in.readLine();

            String[] header =
                    recString.split(",");

            int posKm =
                    SiloUtil.findPositionInArray(
                            "km",
                            header
                    );

            int posHBW =
                    SiloUtil.findPositionInArray(
                            "HBW",
                            header
                    );

            int posPrimary =
                    SiloUtil.findPositionInArray(
                            "Primary",
                            header
                    );

            int posSecondary =
                    SiloUtil.findPositionInArray(
                            "Secondary",
                            header
                    );

            int posTertiary =
                    SiloUtil.findPositionInArray(
                            "Tertiary",
                            header
                    );

            while ((recString = in.readLine()) != null) {

                recCount++;

                String[] lineElements =
                        recString.split(",");

                int length =
                        Integer.parseInt(
                                lineElements[posKm].trim()
                        );

                if (posHBW >= 0) {
                    frequencies.put(
                            length,
                            "HBW",
                            Float.parseFloat(lineElements[posHBW].trim())
                    );
                }

                if (posPrimary >= 0) {
                    frequencies.put(
                            length,
                            "Primary",
                            Float.parseFloat(lineElements[posPrimary].trim())
                    );
                }

                if (posSecondary >= 0) {
                    frequencies.put(
                            length,
                            "Secondary",
                            Float.parseFloat(lineElements[posSecondary].trim())
                    );
                }

                if (posTertiary >= 0) {
                    frequencies.put(
                            length,
                            "Tertiary",
                            Float.parseFloat(lineElements[posTertiary].trim())
                    );
                }
            }

            in.close();

        } catch (IOException e) {
            logger.fatal(
                    "IO Exception caught reading trip length distribution file: " +
                            fileName
            );
            logger.fatal(
                    "recCount = " +
                            recCount +
                            ", recString = <" +
                            recString +
                            ">"
            );
            throw new RuntimeException(e);
        }

        return frequencies;
    }


//    private void readTripLengthFrequencyDistribution(){
//        logger.info("   Starting to read trip length frequency distributions");
//        String fileName = PropertiesSynPop.get().main.tripLengthDistributionFileName;
//        String recString = "";
//        Table<Integer, String, Float> frequencies = HashBasedTable.create();
//        int recCount = 0;
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(fileName));
//            recString = in.readLine();
//
//            // read header
//            String[] header = recString.split(",");
//            int posId = SiloUtil.findPositionInArray("km", header);
//            int posHBW = SiloUtil.findPositionInArray("HBW",header);
//            int posPrimarySecondary = SiloUtil.findPositionInArray("Primary",header);
//            int posTertiary = SiloUtil.findPositionInArray("Tertiary",header);
//
//            // read line
//            while ((recString = in.readLine()) != null) {
//                recCount++;
//                String[] lineElements = recString.split(",");
//                int length  = Integer.parseInt(lineElements[posId]);
//                float hbw  = Float.parseFloat(lineElements[posHBW]);
//                float primary  = Float.parseFloat(lineElements[posPrimarySecondary]);
//                float tertiary  = Float.parseFloat(lineElements[posTertiary]);
//                frequencies.put(length,"HBW", hbw);
//                frequencies.put(length, "Primary", primary);
//                frequencies.put(length, "Tertiary", tertiary);
//            }
//
//
//        } catch (IOException e) {
//            logger.fatal("IO Exception caught reading synpop job file: " + fileName);
//            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
//        }
//        dataSetSynPop.setTripLengthDistribution(frequencies);
//
//    }

    private void readTripLengthFrequencyDistribution() {

        logger.info(
                "   Starting to read regional trip length frequency distributions"
        );

        Map<String, Table<Integer, String, Float>> distributionsByRegion =
                new HashMap<>();

        Table<Integer, String, Float> munichDistribution =
                readTripLengthFrequencyDistributionFile(
                        PropertiesSynPop.get()
                                .main
                                .tripLengthDistributionMunichFileName
                );

        Table<Integer, String, Float> augsburgDistribution =
                readTripLengthFrequencyDistributionFile(
                        PropertiesSynPop.get()
                                .main
                                .tripLengthDistributionAugsburgFileName
                );

        Table<Integer, String, Float> otherDistribution =
                readTripLengthFrequencyDistributionFile(
                        PropertiesSynPop.get()
                                .main
                                .tripLengthDistributionOtherFileName
                );

        distributionsByRegion.put(
                "Munich",
                munichDistribution
        );

        distributionsByRegion.put(
                "Augsburg",
                augsburgDistribution
        );

        distributionsByRegion.put(
                "Other",
                otherDistribution
        );

        logTripLengthDistributionSummary(
                "Munich",
                munichDistribution
        );

        logTripLengthDistributionSummary(
                "Augsburg",
                augsburgDistribution
        );

        logTripLengthDistributionSummary(
                "Other",
                otherDistribution
        );

        dataSetSynPop.setTripLengthDistributionByRegion(
                distributionsByRegion
        );

        // keep the old interface
        dataSetSynPop.setTripLengthDistribution(
                otherDistribution
        );
    }

    private void logTripLengthDistributionSummary(
            String region,
            Table<Integer, String, Float> distribution
    ) {
        if (distribution == null || distribution.isEmpty()) {
            logger.warn(
                    "Trip length distribution for " +
                            region +
                            " is empty."
            );
            return;
        }

        int minKm =
                distribution
                        .rowKeySet()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElse(-1);

        int maxKm =
                distribution
                        .rowKeySet()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(-1);

        logger.info(
                "Trip length distribution loaded for " +
                        region +
                        ": minKm=" +
                        minKm +
                        ", maxKm=" +
                        maxKm +
                        ", columns=" +
                        distribution.columnKeySet()
        );

        logger.info(
                "Trip length sample for " +
                        region +
                        ": HBW[5]=" +
                        distribution.get(5, "HBW") +
                        ", Primary[5]=" +
                        distribution.get(5, "Primary") +
                        ", Secondary[5]=" +
                        distribution.get(5, "Secondary") +
                        ", Tertiary[5]=" +
                        distribution.get(5, "Tertiary")
        );
    }
}
