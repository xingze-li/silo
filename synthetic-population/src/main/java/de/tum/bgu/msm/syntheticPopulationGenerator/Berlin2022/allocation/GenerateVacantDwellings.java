package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.allocation;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.MunichDwellingTypes;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.dwelling.*;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation.MicroDataManager;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.util.*;

public class GenerateVacantDwellings {

    private static final Logger logger = LogManager.getLogger(GenerateVacantDwellings.class);

    private static final int BASE_YEAR = 2022;
    private static final double WARNING_RELATIVE_GAP = 0.02;
    private static final double LARGE_WARNING_RELATIVE_GAP = 0.10;

    private static final String[] VACANT_TYPE_COLUMNS = {
            "d.vacant.type.MFH13OrMoreDwelling",
            "d.vacant.type.MFH3to6Dwelling",
            "d.vacant.type.MFH7to12Dwelling",
            "d.vacant.type.detached1Dwelling",
            "d.vacant.type.detached2Dwelling",
            "d.vacant.type.other",
            "d.vacant.type.semiDetached1Dwelling",
            "d.vacant.type.semiDetached2Dwelling",
            "d.vacant.type.terraced1Dwelling",
            "d.vacant.type.terraced2Dwelling"
    };

    private static final String[] VACANT_YEAR_COLUMNS = {
            "d.vacant.year.before1950",
            "d.vacant.year.1950to1969",
            "d.vacant.year.1970to1989",
            "d.vacant.year.1990to2009",
            "d.vacant.year.2010AndLater"
    };

    private final DataSetSynPop dataSetSynPop;
    private final MicroDataManager microDataManager;
    private final DataContainer dataContainer;

    private RealEstateDataManager realEstateData;
    private int highestDwellingIdInUse;

    /** municipality/year-bracket -> quality distribution */
    private final Map<Integer, Map<Integer, Float>> ddQuality = new HashMap<>();

    /** Exact donor pool: municipality -> type -> yearBracket -> occupied dwellings. */
    private final Map<Integer, Map<DwellingType, Map<Integer, List<Dwelling>>>> donorsExact = new HashMap<>();

    /** Fallback donor pool: municipality -> type -> occupied dwellings. */
    private final Map<Integer, Map<DwellingType, List<Dwelling>>> donorsByType = new HashMap<>();

    /** Fallback donor pool: municipality -> occupied dwellings. */
    private final Map<Integer, List<Dwelling>> donorsByMunicipality = new HashMap<>();

    /** Global last-resort donor pool. */
    private final List<Dwelling> allOccupiedDonors = new ArrayList<>();

    /** Used to map source category "other" to the municipality's occupied type structure. */
    private final Map<Integer, Map<DwellingType, Integer>> occupiedTypeCounts = new HashMap<>();

    public GenerateVacantDwellings(DataContainer dataContainer, DataSetSynPop dataSetSynPop) {
        this.dataContainer = dataContainer;
        this.dataSetSynPop = dataSetSynPop;
        this.microDataManager = new MicroDataManager(dataSetSynPop);
    }

    public void run() {
        logger.info("Running module: vacant dwelling generation");
        initializeOccupiedDwellingDistributions();
        generateVacantDwellings();
    }

    private void initializeOccupiedDwellingDistributions() {
        realEstateData = dataContainer.getRealEstateDataManager();
        highestDwellingIdInUse = 0;

        for (Dwelling dwelling : realEstateData.getDwellings()) {
            highestDwellingIdInUse = Math.max(highestDwellingIdInUse, dwelling.getId());

            int municipality = (int) PropertiesSynPop.get().main.cellsMatrix
                    .getIndexedValueAt(dwelling.getZoneId(), "ID_city");

            updateQualityMap(municipality, dwelling.getYearBuilt(), dwelling.getQuality());

            // Only occupied dwellings are donors for the vacant stock.
            if (dwelling.getResidentId() <= 0) {
                continue;
            }

            int yearBracket = microDataManager.dwellingYearBracket(dwelling.getYearBuilt());
            DwellingType type = dwelling.getType();

            donorsExact
                    .computeIfAbsent(municipality, key -> new HashMap<>())
                    .computeIfAbsent(type, key -> new HashMap<>())
                    .computeIfAbsent(yearBracket, key -> new ArrayList<>())
                    .add(dwelling);

            donorsByType
                    .computeIfAbsent(municipality, key -> new HashMap<>())
                    .computeIfAbsent(type, key -> new ArrayList<>())
                    .add(dwelling);

            donorsByMunicipality
                    .computeIfAbsent(municipality, key -> new ArrayList<>())
                    .add(dwelling);

            allOccupiedDonors.add(dwelling);

            occupiedTypeCounts
                    .computeIfAbsent(municipality, key -> new HashMap<>())
                    .merge(type, 1, Integer::sum);
        }
    }

    private void generateVacantDwellings() {
        for (int municipality : dataSetSynPop.getMunicipalities()) {

            int targetVacant = Math.max(0, Math.round(getMarginal(municipality, "d.vacant")));
            if (targetVacant == 0) {
                logger.info("Municipality {}. No vacant dwellings to generate.", municipality);
                continue;
            }

            Map<String, Integer> typeCounts = normalizeMarginalCounts(
                    municipality,
                    VACANT_TYPE_COLUMNS,
                    targetVacant,
                    "vacant type"
            );

            Map<String, Integer> yearCounts = normalizeMarginalCounts(
                    municipality,
                    VACANT_YEAR_COLUMNS,
                    targetVacant,
                    "vacant year"
            );

            List<String> sourceTypes = expandCounts(typeCounts);
            List<String> yearCategories = expandCounts(yearCounts);

            Collections.shuffle(sourceTypes, SiloUtil.getRandomObject());
            Collections.shuffle(yearCategories, SiloUtil.getRandomObject());

            if (sourceTypes.size() != targetVacant || yearCategories.size() != targetVacant) {
                throw new IllegalStateException(
                        "Vacant marginal expansion failed for municipality " + municipality
                );
            }

            int generated = 0;

            for (int index = 0; index < targetVacant; index++) {
                String sourceType = sourceTypes.get(index);
                String yearCategory = yearCategories.get(index);

                DwellingType type = mapSourceTypeToMunichType(municipality, sourceType);
                int yearBuilt = sampleConstructionYear(yearCategory);
                int tazSelected = selectZone(municipality);

                Dwelling donor = selectOccupiedDonor(municipality, type, yearBuilt);
                int floorSpace = donor != null && donor.getFloorSpace() > 0
                        ? donor.getFloorSpace()
                        : defaultFloorSpace(type);

                int bedrooms = donor != null && donor.getBedrooms() >= 0
                        ? donor.getBedrooms()
                        : microDataManager.guessBedrooms(floorSpace);

                int quality = selectQualityVacant(municipality, yearBuilt);

                Map<MunichDwellingTypes.DwellingTypeMunich, Integer> zonePrices =
                        dataSetSynPop.getDwellingPriceByTypeAndZone().get(tazSelected);

                Integer groundPrice = zonePrices == null ? null : zonePrices.get(type);
                int price;
                if (groundPrice != null && groundPrice > 0) {
                    price = microDataManager.guessPrice(
                            groundPrice,
                            quality,
                            floorSpace,
                            DwellingUsage.VACANT
                    );
                } else if (donor != null && donor.getPrice() > 0) {
                    price = donor.getPrice();
                } else {
                    price = microDataManager.guessPrice(
                            0,
                            quality,
                            floorSpace,
                            DwellingUsage.VACANT
                    );
                }

                int newDwellingId = ++highestDwellingIdInUse;
                Zone zone = dataContainer.getGeoData().getZones().get(tazSelected);
                if (zone == null) {
                    throw new IllegalStateException("No model zone found for allocated TAZ " + tazSelected);
                }
                Coordinate coordinate = zone.getRandomCoordinate(SiloUtil.getRandomObject());
                Dwelling dwelling = DwellingUtils.getFactory().createDwelling(
                        newDwellingId,
                        tazSelected,
                        coordinate,
                        -1,
                        type,
                        bedrooms,
                        quality,
                        price,
                        yearBuilt
                );

                dwelling.setUsage(DwellingUsage.VACANT);
                dwelling.setAttribute("municipality", municipality);
                dwelling.setFloorSpace(floorSpace);

                populateVacantDwellingAttributes(
                        dwelling,
                        donor,
                        yearBuilt,
                        floorSpace,
                        bedrooms,
                        price,
                        sourceType,
                        yearCategory
                );

                realEstateData.addDwelling(dwelling);
                generated++;
            }

            logger.info(
                    "Municipality {}. Target vacant dwellings: {}, generated: {}, type counts: {}, year counts: {}",
                    municipality,
                    targetVacant,
                    generated,
                    typeCounts,
                    yearCounts
            );
        }
    }

    private static final String[] VACANT_DONOR_ATTRIBUTES = {
            "d.buildingSize",
            "d.heating.district",
            "d.numberOfHeatingTypes",
            "d.heating.stoves",
            "d.heatingEnergy",
            "d.heating.central",
            "d.heating.floor",
            "d.numberOfApartments",
            "d.buildingUsage"
    };

    private void populateVacantDwellingAttributes(
            Dwelling dwelling,
            Dwelling donor,
            int yearBuilt,
            int floorSpace,
            int bedrooms,
            int price,
            String sourceType,
            String yearCategory
    ) {

        /*
         * Copy building/heating characteristics from the occupied donor.
         */
        if (donor != null) {
            for (String attribute : VACANT_DONOR_ATTRIBUTES) {
                donor.getAttribute(attribute)
                        .ifPresent(value ->
                                dwelling.setAttribute(attribute, value)
                        );
            }

            /*
             * Copy the original microdata dwelling-type code only when the
             * donor has the same final SILO/Munich dwelling type.
             */
            if (donor.getType().equals(dwelling.getType())) {
                donor.getAttribute("d.type")
                        .ifPresent(value ->
                                dwelling.setAttribute("d.type", value)
                        );
            }
        }

        dwelling.setAttribute(
                "d.type",
                ((MunichDwellingTypes.DwellingTypeMunich) dwelling.getType()).getId() + 1
        );

        /*
         * Attributes controlled or generated by the vacant module.
         */
        dwelling.setAttribute(
                "d.year",
                microDataManager.dwellingYearBracket(yearBuilt)
        );

        /*
         * Microcensus code 5 means vacant.
         */
        dwelling.setAttribute("d.use", 5);

        dwelling.setAttribute("d.space", floorSpace);

        /*
         * In your occupied generation:
         * bedrooms = numberOfRooms - 1.
         * Therefore the reverse approximation is bedrooms + 1.
         */
        dwelling.setAttribute(
                "d.numberOfRooms",
                Math.max(1, bedrooms + 1)
        );

        dwelling.setAttribute("d.totalRent", price);

        float estimatedRentPerSquareMetre = 0f;

        if (floorSpace > 0) {
            /*
             * guessPrice() adds 150 euros of ancillary costs.
             * Remove it before deriving approximate rent per square metre.
             */
            estimatedRentPerSquareMetre =
                    Math.max(0, price - 150) /
                            (float) floorSpace;
        }

        dwelling.setAttribute(
                "d.rent",
                estimatedRentPerSquareMetre
        );

        dwelling.setAttribute(
                "sourceVacantType",
                sourceType
        );

        dwelling.setAttribute(
                "sourceVacantYearCategory",
                yearCategory
        );
    }

    /**
     * Reconciles detailed marginals to d.vacant and converts them to integer counts.
     * The relative category structure is preserved; the final sum equals targetTotal exactly.
     */
    private Map<String, Integer> normalizeMarginalCounts(
            int municipality,
            String[] columns,
            int targetTotal,
            String marginalName
    ) {
        Map<String, Double> rawValues = new LinkedHashMap<>();
        double rawSum = 0.0;

        for (String column : columns) {
            double value = getMarginal(municipality, column);
            if (!Double.isFinite(value) || value < 0.0) {
                logger.warn(
                        "Municipality {}: invalid {} value in {} changed to zero: {}",
                        municipality,
                        marginalName,
                        column,
                        value
                );
                value = 0.0;
            }
            rawValues.put(column, value);
            rawSum += value;
        }

        if (targetTotal == 0) {
            Map<String, Integer> zeros = new LinkedHashMap<>();
            for (String column : columns) {
                zeros.put(column, 0);
            }
            return zeros;
        }

        if (rawSum <= 0.0) {
            throw new IllegalStateException(
                    "Municipality " + municipality + ": d.vacant=" + targetTotal +
                            " but all " + marginalName + " values are zero."
            );
        }

        double relativeGap = Math.abs(rawSum - targetTotal) / Math.max(1.0, targetTotal);
        if (relativeGap > LARGE_WARNING_RELATIVE_GAP) {

            logger.warn(
                    "Municipality {}: LARGE difference between {} sum and d.vacant. " +
                            "target={}, rawSum={}, relativeGap={}%. " +
                            "Generation will continue and detailed counts will be rescaled to d.vacant.",
                    municipality,
                    marginalName,
                    targetTotal,
                    rawSum,
                    relativeGap * 100.0
            );

        } else if (relativeGap > WARNING_RELATIVE_GAP) {

            logger.warn(
                    "Municipality {}: {} sum differs from d.vacant. " +
                            "target={}, rawSum={}, relativeGap={}%. " +
                            "Detailed counts will be rescaled to d.vacant.",
                    municipality,
                    marginalName,
                    targetTotal,
                    rawSum,
                    relativeGap * 100.0
            );
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        Map<String, Double> remainders = new LinkedHashMap<>();
        int floorSum = 0;

        for (String column : columns) {
            double scaled = rawValues.get(column) / rawSum * targetTotal;
            int floor = (int) Math.floor(scaled);
            result.put(column, floor);
            remainders.put(column, scaled - floor);
            floorSum += floor;
        }

        int remaining = targetTotal - floorSum;
        List<String> order = new ArrayList<>(List.of(columns));
        Map<String, Integer> originalOrder = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            originalOrder.put(columns[i], i);
        }

        order.sort(
                Comparator
                        .comparingDouble((String column) -> remainders.get(column))
                        .reversed()
                        .thenComparingInt(originalOrder::get)
        );

        for (int i = 0; i < remaining; i++) {
            String column = order.get(i % order.size());
            result.put(column, result.get(column) + 1);
        }

        int finalSum = result.values().stream().mapToInt(Integer::intValue).sum();
        if (finalSum != targetTotal) {
            throw new IllegalStateException(
                    "Internal integerization error for municipality " + municipality +
                            ": expected=" + targetTotal + ", actual=" + finalSum
            );
        }

        return result;
    }

    private List<String> expandCounts(Map<String, Integer> counts) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Mapping to the four current Munich dwelling types.
     * All two-or-more-dwelling categories are mapped to MFH.
     */
    private DwellingType mapSourceTypeToMunichType(int municipality, String sourceType) {
        return switch (sourceType) {
            case "d.vacant.type.detached1Dwelling" ->
                    MunichDwellingTypes.DwellingTypeMunich.EFHFreistehend;

            case "d.vacant.type.semiDetached1Dwelling" ->
                    MunichDwellingTypes.DwellingTypeMunich.EFHDoppelhaus;

            case "d.vacant.type.terraced1Dwelling" ->
                    MunichDwellingTypes.DwellingTypeMunich.EFHReihenhaus;

            case "d.vacant.type.detached2Dwelling",
                 "d.vacant.type.semiDetached2Dwelling",
                 "d.vacant.type.terraced2Dwelling",
                 "d.vacant.type.MFH3to6Dwelling",
                 "d.vacant.type.MFH7to12Dwelling",
                 "d.vacant.type.MFH13OrMoreDwelling" ->
                    MunichDwellingTypes.DwellingTypeMunich.MFH;

            case "d.vacant.type.other" -> sampleOccupiedType(municipality);

            default -> throw new IllegalArgumentException(
                    "Unknown vacant dwelling type column: " + sourceType
            );
        };
    }

    private DwellingType sampleOccupiedType(int municipality) {
        Map<DwellingType, Integer> counts = occupiedTypeCounts.get(municipality);
        if (counts == null || counts.isEmpty()) {
            return MunichDwellingTypes.DwellingTypeMunich.MFH;
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return MunichDwellingTypes.DwellingTypeMunich.MFH;
        }

        int draw = SiloUtil.getRandomObject().nextInt(total);
        int cumulative = 0;
        for (Map.Entry<DwellingType, Integer> entry : counts.entrySet()) {
            cumulative += entry.getValue();
            if (draw < cumulative) {
                return entry.getKey();
            }
        }

        return MunichDwellingTypes.DwellingTypeMunich.MFH;
    }

    private int sampleConstructionYear(String category) {
        return switch (category) {
            case "d.vacant.year.before1950" -> randomIntInclusive(1900, 1949);
            case "d.vacant.year.1950to1969" -> randomIntInclusive(1950, 1969);
            case "d.vacant.year.1970to1989" -> randomIntInclusive(1970, 1989);
            case "d.vacant.year.1990to2009" -> randomIntInclusive(1990, 2009);
            case "d.vacant.year.2010AndLater" -> randomIntInclusive(2010, BASE_YEAR);
            default -> throw new IllegalArgumentException(
                    "Unknown vacant construction-year category: " + category
            );
        };
    }

    private int randomIntInclusive(int minimum, int maximum) {
        if (maximum <= minimum) {
            return minimum;
        }
        return minimum + SiloUtil.getRandomObject().nextInt(maximum - minimum + 1);
    }

    private int selectZone(int municipality) {
        Map<Integer, Float> zoneWeights = dataSetSynPop.getProbabilityZone().get(municipality);
        if (zoneWeights == null || zoneWeights.isEmpty()) {
            throw new IllegalStateException(
                    "No TAZ probabilities found for municipality " + municipality
            );
        }

        double total = 0.0;
        for (float weight : zoneWeights.values()) {
            total += Math.max(0.0, weight);
        }

        if (total <= 0.0) {
            throw new IllegalStateException(
                    "TAZ probability sum is zero for municipality " + municipality
            );
        }

        double draw = SiloUtil.getRandomNumberAsDouble() * total;
        double cumulative = 0.0;
        int fallbackZone = zoneWeights.keySet().iterator().next();

        for (Map.Entry<Integer, Float> entry : zoneWeights.entrySet()) {
            fallbackZone = entry.getKey();
            cumulative += Math.max(0.0, entry.getValue());
            if (draw <= cumulative) {
                return entry.getKey();
            }
        }

        return fallbackZone;
    }

    private Dwelling selectOccupiedDonor(int municipality, DwellingType type, int yearBuilt) {
        int yearBracket = microDataManager.dwellingYearBracket(yearBuilt);

        Map<DwellingType, Map<Integer, List<Dwelling>>> byTypeAndYear = donorsExact.get(municipality);
        if (byTypeAndYear != null) {
            Map<Integer, List<Dwelling>> byYear = byTypeAndYear.get(type);
            if (byYear != null) {
                Dwelling donor = randomElement(byYear.get(yearBracket));
                if (donor != null) {
                    return donor;
                }
            }
        }

        Map<DwellingType, List<Dwelling>> byType = donorsByType.get(municipality);
        if (byType != null) {
            Dwelling donor = randomElement(byType.get(type));
            if (donor != null) {
                return donor;
            }
        }

        Dwelling donor = randomElement(donorsByMunicipality.get(municipality));
        if (donor != null) {
            return donor;
        }

        return randomElement(allOccupiedDonors);
    }

    private Dwelling randomElement(List<Dwelling> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(SiloUtil.getRandomObject().nextInt(list.size()));
    }

    private int defaultFloorSpace(DwellingType type) {
        if (type.equals(MunichDwellingTypes.DwellingTypeMunich.EFHFreistehend)) {
            return 120;
        }
        if (type.equals(MunichDwellingTypes.DwellingTypeMunich.EFHDoppelhaus)) {
            return 105;
        }
        if (type.equals(MunichDwellingTypes.DwellingTypeMunich.EFHReihenhaus)) {
            return 95;
        }
        return 75;
    }

    private void updateQualityMap(int municipality, int yearBuilt, int quality) {
        int yearBracket = microDataManager.dwellingYearBracket(yearBuilt);
        int key = yearBracket * 10_000_000 + municipality;

        ddQuality
                .computeIfAbsent(key, ignored -> new HashMap<>())
                .merge(quality, 1f, Float::sum);
    }

    private int selectQualityVacant(int municipality, int yearBuilt) {
        int yearBracket = microDataManager.dwellingYearBracket(yearBuilt);
        int key = yearBracket * 10_000_000 + municipality;

        Map<Integer, Float> qualities = ddQuality.get(key);
        if (qualities == null || qualities.isEmpty()) {
            qualities = new HashMap<>();
            for (int quality = 1;
                 quality <= PropertiesSynPop.get().main.numberofQualityLevels;
                 quality++) {
                qualities.put(quality, 1f);
            }
            ddQuality.put(key, qualities);
        }

        return SiloUtil.select(qualities);
    }

    private float getMarginal(int municipality, String column) {
        return PropertiesSynPop.get().main.marginalsMunicipality
                .getIndexedValueAt(municipality, column);
    }
}
