package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.MunichDwellingTypes;
import de.tum.bgu.msm.data.dwelling.Dwelling;
import de.tum.bgu.msm.data.dwelling.DwellingUsage;
import de.tum.bgu.msm.data.dwelling.DwellingUtils;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.household.HouseholdFactory;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.job.JobFactoryMuc;
import de.tum.bgu.msm.data.job.JobMuc;
import de.tum.bgu.msm.data.person.*;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.io.IOException;
import java.util.HashMap;

public class ReadPopulation {

    private static final Logger logger = LogManager.getLogger(ReadPopulation.class);
    private final DataContainer dataContainer;
    private HashMap<Person, Integer> educationalLevel;

    public ReadPopulation(DataContainer dataContainer, HashMap<Person, Integer> educationalLevel){
        this.dataContainer = dataContainer;
        this.educationalLevel = educationalLevel;
    }

    public void run(){
        logger.info("   Running module: read population");
        readHouseholdData(Properties.get().main.startYear);
        readPersonData(Properties.get().main.startYear);
        readDwellingData(Properties.get().main.startYear);
        readJobData(Properties.get().main.startYear);
    }


    private void readHouseholdData(int year) {
        logger.info("Reading household micro data from ascii file");

        HouseholdDataManager householdData = dataContainer.getHouseholdDataManager();
        HouseholdFactory householdFactory = householdData.getHouseholdFactory();
        String fileName = resolvePopulationFile(Properties.get().householdData.householdFileName, year);

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",", -1);
            int posId    = SiloUtil.findPositionInArray("id", header);
            int posDwell = SiloUtil.findPositionInArray("dwelling",header);
            int posTaz   = SiloUtil.findPositionInArray("zone",header);
            int posAutos = SiloUtil.findPositionInArray("autos",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",", -1);
                int id         = Integer.parseInt(lineElements[posId]);
                int dwellingID = Integer.parseInt(lineElements[posDwell]);
                int autos      = Integer.parseInt(lineElements[posAutos]);

                Household household = householdFactory.createHousehold(id, dwellingID, autos);  // this automatically puts it in id->household map in Household class
                copyNumericAttributes(
                        header,
                        lineElements,
                        new String[]{"municipality", "personCount", "h.size", "h.type", "h.income"},
                        household::setAttribute
                );

                householdData.addHousehold(household);
                if (id == SiloUtil.trackHh) {
                    SiloUtil.trackWriter.println("Read household with following attributes from " + fileName);
                }
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("Finished reading " + recCount + " households.");
    }


    private void readPersonData(int year) {
        logger.info("Reading person micro data from ascii file");

        HouseholdDataManager householdData = dataContainer.getHouseholdDataManager();
        String fileName = resolvePopulationFile(Properties.get().householdData.personFileName, year);

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",", -1);
            int posId = SiloUtil.findPositionInArray("id", header);
            int posHhId = SiloUtil.findPositionInArray("hhid",header);
            int posAge = SiloUtil.findPositionInArray("age",header);
            int posGender = SiloUtil.findPositionInArray("gender",header);
            int posRelShp = SiloUtil.findPositionInArray("relationShip",header);
            int posOccupation = SiloUtil.findPositionInArray("occupation",header);
            int posWorkplace = SiloUtil.findPositionInArray("workplace",header);
            int posIncome = SiloUtil.findPositionInArray("income",header);
            int posNationality = SiloUtil.findPositionInArray("nationality",header);
            int posLicense = SiloUtil.findPositionInArray("driversLicense",header);


            // read line
            PersonFactoryMuc ppFactory = new PersonFactoryMuc();;
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",", -1);
                int id         = Integer.parseInt(lineElements[posId]);
                int hhid       = Integer.parseInt(lineElements[posHhId]);
                int age        = Integer.parseInt(lineElements[posAge]);
                Gender gender     = Gender.valueOf(Integer.parseInt(lineElements[posGender]));
                String relShp  = lineElements[posRelShp].replace("\"", "");
                PersonRole pr  = PersonRole.valueOf(relShp.toUpperCase());
                Occupation occupation = Occupation.valueOf(Integer.parseInt(lineElements[posOccupation]));
                int workplace  = Integer.parseInt(lineElements[posWorkplace]);
                int income     = Integer.parseInt(lineElements[posIncome]);
                PersonMuc pp = (PersonMuc) ppFactory.createPerson(id, age, gender, occupation, pr, workplace, income); //this automatically puts it in id->person map in Person class
                householdData.addPerson(pp);
                householdData.addPersonToHousehold(pp, householdData.getHouseholdFromId(hhid));
                String nationality = clean(lineElements[posNationality]);
                pp.setNationality("OTHER".equalsIgnoreCase(nationality)
                        ? Nationality.OTHER
                        : Nationality.GERMAN);
                pp.setDriverLicense(Boolean.parseBoolean(clean(lineElements[posLicense])));

                pp.setSchoolType(readInt(header, lineElements, "schoolType", 0));
                pp.setSchoolPlace(readInt(header, lineElements, "schoolPlace", -1));

                copyNumericAttributes(
                        header,
                        lineElements,
                        new String[]{
                                "zone", "municipality", "disability", "jobTypeWZ08",
                                "jobDurationType", "jobDuration", "jobStartTimeWorkday",
                                "jobStartTimeWeekend", "p.BMI", "p.education",
                                "p.healthStatusIndex", "p.smokeFrequency", "p.generalHealth",
                                "p.disability", "p.physicalImpairmentIndex", "p.restriction",
                                "p.homeOffice", "p.disabilityDegree", "p.householdRole",
                                "p.income", "p.privateHousehold", "p.partnerInHousehold",
                                "p.municipalityType", "p.federal", "p.maritalStatus"
                        },
                        pp::setAttribute
                );
                copyStringAttribute(header, lineElements, "jobType", pp::setAttribute);

                educationalLevel.put(
                        pp,
                        readInt(header, lineElements, "p.education", 0)
                );
                if (id == SiloUtil.trackPp) {
                    SiloUtil.trackWriter.println("Read person with following attributes from " + fileName);
                }
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("Finished reading " + recCount + " persons.");
    }


    private void readDwellingData(int year) {
        // read dwelling micro data from ascii file

        logger.info("Reading dwelling micro data from ascii file");
        RealEstateDataManager realEstate = dataContainer.getRealEstateDataManager();
        String fileName = resolvePopulationFile(Properties.get().realEstate.dwellingsFileName, year);

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",", -1);
            int posId      = SiloUtil.findPositionInArray("id", header);
            int posZone    = SiloUtil.findPositionInArray("zone",header);
            int posHh      = SiloUtil.findPositionInArray("hhId",header);
            int posType    = SiloUtil.findPositionInArray("type",header);
            int posRooms   = SiloUtil.findPositionInArray("bedrooms",header);
            int posQuality = SiloUtil.findPositionInArray("quality",header);
            int posCosts   = SiloUtil.findPositionInArray("monthlyCost",header);
            int posYear    = SiloUtil.findPositionInArray("yearBuilt",header);
            int posUse     = SiloUtil.findPositionInArray("usage",header);
            int posCoordX = -1;
            int posCoordY = -1;
            try {
                posCoordX = SiloUtil.findPositionInArray("coordX", header);
                posCoordY = SiloUtil.findPositionInArray("coordY", header);
            } catch (Exception e) {
                logger.warn("No coords given in dwelling input file. Models using microlocations will not work.");
            }

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",", -1);
                int id        = Integer.parseInt(lineElements[posId]);
                int zoneId      = Integer.parseInt(lineElements[posZone]);
                int hhId      = Integer.parseInt(lineElements[posHh]);
                String tp     = clean(lineElements[posType]);
                MunichDwellingTypes.DwellingTypeMunich type = MunichDwellingTypes.DwellingTypeMunich.valueOf(tp);
                int price     = Integer.parseInt(lineElements[posCosts]);
                int bedrooms  = Integer.parseInt(lineElements[posRooms]);
                int quality   = Integer.parseInt(lineElements[posQuality]);
                int yearBuilt = Integer.parseInt(lineElements[posYear]);
                Coordinate coordinate = null;
                if (posCoordX >= 0 && posCoordY >= 0) {
                    try {
                        coordinate = new Coordinate(Double.parseDouble(lineElements[posCoordX]), Double.parseDouble(lineElements[posCoordY]));
                    } catch (Exception e) {
                    }
                }
                Dwelling dd = DwellingUtils.getFactory().createDwelling(id, zoneId, coordinate, hhId, type, bedrooms, quality, price, yearBuilt);   // this automatically puts it in id->dwelling map in Dwelling class
                dd.setUsage(parseDwellingUsage(lineElements[posUse]));

                int floorSpace = readInt(header, lineElements, "d.space", 0);
                if (floorSpace > 0) {
                    dd.setFloorSpace(floorSpace);
                }

                copyNumericAttributes(
                        header,
                        lineElements,
                        new String[]{
                                "municipality", "d.buildingSize", "d.rent", "d.year",
                                "d.heating.district", "d.type", "d.numberOfHeatingTypes",
                                "d.use", "d.heating.stoves", "d.space", "d.numberOfRooms",
                                "d.totalRent", "d.heatingEnergy", "d.heating.central",
                                "d.heating.floor", "d.numberOfApartments", "d.buildingUsage"
                        },
                        dd::setAttribute
                );
                copyStringAttribute(header, lineElements, "sourceVacantType", dd::setAttribute);
                copyStringAttribute(header, lineElements, "sourceVacantYearCategory", dd::setAttribute);

                realEstate.addDwelling(dd);
                if (id == SiloUtil.trackDd) {
                    SiloUtil.trackWriter.println("Read dwelling with following attributes from " + fileName);
                }
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop dwelling file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("Finished reading " + recCount + " dwellings.");
    }


    private void readJobData(int year) {
        logger.info("Reading job micro data from ascii file");

        JobDataManager jobDataManager = dataContainer.getJobDataManager();
        JobFactoryMuc jobFactory = (JobFactoryMuc) dataContainer.getJobDataManager().getFactory();
        String fileName = resolvePopulationFile(Properties.get().jobData.jobsFileName, year);

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",", -1);
            int posId = SiloUtil.findPositionInArray("id", header);
            int posZone = SiloUtil.findPositionInArray("zone",header);
            int posWorker = SiloUtil.findPositionInArray("personId",header);
            int posType = SiloUtil.findPositionInArray("type",header);
            int posCoordX = SiloUtil.findPositionInArray("coordX", header);
            int posCoordY = SiloUtil.findPositionInArray("coordY", header);
            int posStartTime = SiloUtil.findPositionInArray("startTime", header);
            int posDuration = SiloUtil.findPositionInArray("duration", header);


            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",", -1);
                int id      = Integer.parseInt(lineElements[posId]);
                int zoneId    = Integer.parseInt(lineElements[posZone]);
                int worker  = Integer.parseInt(lineElements[posWorker]);
                String type = lineElements[posType].replace("\"", "");
                Coordinate coordinate = null;
                if (posCoordX >= 0 && posCoordY >= 0) {
                    try {
                        coordinate = new Coordinate(Double.parseDouble(lineElements[posCoordX]), Double.parseDouble(lineElements[posCoordY]));
                    } catch (Exception e) {
                    }
                }
                JobMuc jj = jobFactory.createJob(id, zoneId, coordinate, worker, type);
                copyNumericAttributes(header, lineElements, new String[]{"municipality"}, jj::setAttribute);
                int startTime = Integer.parseInt(lineElements[posStartTime]);
                int duration = Integer.parseInt(lineElements[posDuration]);
                jj.setJobWorkingTime(startTime, duration);
                jobDataManager.addJob(jj);
                if (id == SiloUtil.trackJj) {
                    SiloUtil.trackWriter.println("Read job with following attributes from " + fileName);
                }
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop job file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("Finished reading " + recCount + " jobs.");
    }

    private String resolvePopulationFile(String baseName, int year) {
        String prefix = Properties.get().main.baseDirectory + baseName + "_" + year;
        String fileWithPopulationSuffix = prefix + "P.csv";
        if (Files.exists(Path.of(fileWithPopulationSuffix))) {
            return fileWithPopulationSuffix;
        }
        return prefix + ".csv";
    }

    private void copyNumericAttributes(
            String[] header,
            String[] values,
            String[] columns,
            BiConsumer<String, Object> attributeSetter
    ) {
        for (String column : columns) {
            int position = findOptionalColumn(header, column);
            if (position < 0 || position >= values.length) {
                continue;
            }

            String value = clean(values[position]);
            if (value.isEmpty()) {
                continue;
            }

            float numericValue = Float.parseFloat(value);
            if (Math.abs(numericValue - Math.round(numericValue)) < 0.0001f) {
                attributeSetter.accept(column, Math.round(numericValue));
            } else {
                attributeSetter.accept(column, numericValue);
            }
        }
    }

    private void copyStringAttribute(
            String[] header,
            String[] values,
            String column,
            BiConsumer<String, Object> attributeSetter
    ) {
        int position = findOptionalColumn(header, column);
        if (position >= 0 && position < values.length) {
            String value = clean(values[position]);
            if (!value.isEmpty()) {
                attributeSetter.accept(column, value);
            }
        }
    }

    private int readInt(String[] header, String[] values, String column, int defaultValue) {
        int position = findOptionalColumn(header, column);
        if (position < 0 || position >= values.length) {
            return defaultValue;
        }

        String value = clean(values[position]);
        return value.isEmpty() ? defaultValue : Math.round(Float.parseFloat(value));
    }

    private int findOptionalColumn(String[] header, String column) {
        for (int position = 0; position < header.length; position++) {
            if (clean(header[position]).equalsIgnoreCase(column)) {
                return position;
            }
        }
        return -1;
    }

    private String clean(String value) {
        return value.replace("\"", "").replace("\uFEFF", "").trim();
    }

    private DwellingUsage parseDwellingUsage(String value) {
        String cleanedValue = clean(value);
        try {
            return DwellingUsage.valueOf(cleanedValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            int code = Integer.parseInt(cleanedValue);
            if (code == 1) {
                return DwellingUsage.OWNED;
            } else if (code == 2) {
                return DwellingUsage.RENTED;
            } else if (code == 5) {
                return DwellingUsage.VACANT;
            }
            return DwellingUsage.GROUP_QUARTER_OR_DEFAULT;
        }
    }

}
