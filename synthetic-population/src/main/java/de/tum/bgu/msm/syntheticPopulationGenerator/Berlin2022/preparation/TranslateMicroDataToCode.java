package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation;

import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TranslateMicroDataToCode {

    private static final Logger logger = LogManager.getLogger(TranslateMicroDataToCode.class);

    private final DataSetSynPop dataSetSynPop;

    public TranslateMicroDataToCode(DataSetSynPop dataSetSynPop) {
        this.dataSetSynPop = dataSetSynPop;
    }

    public void run() {
        logger.info("   Starting to translate the micro data");

        for (int personCount = 1; personCount <= dataSetSynPop.getPersonTable().rowKeySet().size(); personCount++) {
            boolean attendingSchool = translateSchoolAttendance(personCount);
            translateHighestEducationalDegree(personCount);
            translateOccupation(personCount, attendingSchool);
            translateRelationshipToHouseholdHead(personCount);
        }

        for (int ddCount = 1; ddCount <= dataSetSynPop.getDwellingTable().rowKeySet().size(); ddCount++) {
            translateDwellingUsage(ddCount);
            translateDwellingType(ddCount);
            translateDwellingNumberOfApartments(ddCount);
            translateDwellingYear(ddCount);
        }

        logger.info("   Finished translating the micro data");
    }

    private void translateOccupation(int personCount, boolean attendingSchool) {
        int occupation = (int) dataSetSynPop.getPersonDataSet().getValueAt(personCount, "p.employmentStatus");
        if (occupation > 1) {
            dataSetSynPop.getPersonDataSet().setValueAt(personCount, "p.employmentStatus", attendingSchool ? 3 : 2);
        }
    }

    private void translateRelationshipToHouseholdHead(int personCount) {
        int valueMicroData = (int) dataSetSynPop.getPersonDataSet().getValueAt(personCount, "p.householdRole");
        int valueCode = switch (valueMicroData) {
            case 1 -> 1;
            case 2 -> 2;
            case 3, 4 -> 3;
            case 5, 6, 7 -> 4;
            case 8, 9 -> (int) dataSetSynPop.getPersonDataSet().getValueAt(personCount, "p.age") < 16 ? 3 : 4;
            default -> 0;
        };
        dataSetSynPop.getPersonDataSet().setValueAt(personCount, "p.householdRole", valueCode);
    }

    private boolean translateSchoolAttendance(int personCount) {
        int valueMicroData = (int) dataSetSynPop.getPersonDataSet().getValueAt(personCount, "p.school");
        int valueCode = switch (valueMicroData) {
            case 1 -> 1;
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> 2;
            case 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 -> 3;
            default -> 0;
        };
        dataSetSynPop.getPersonDataSet().setValueAt(personCount, "p.school", valueCode);
        return valueCode > 0;
    }

    private void translateHighestEducationalDegree(int personCount) {
        int valueMicroData = (int) dataSetSynPop.getPersonDataSet().getValueAt(personCount, "p.education");
        int valueCode = switch (valueMicroData) {
            case -3, 88 -> 1;
            case 1, 2, 3, 4, 6, 7, 8, 9, 10, 14, 15, 16 -> 2;
            case 18, 21, 22, 23, 31, 32, 33 -> 3;
            case 5, 11, 12, 13, 41, 42, 43, 50, 60 -> 4;
            default -> 0;
        };
        dataSetSynPop.getPersonDataSet().setValueAt(personCount, "p.education", valueCode);
    }

    private void translateDwellingUsage(int ddCount) {
        int valueMicroData = (int) dataSetSynPop.getDwellingDataSet().getValueAt(ddCount, "d.use");
        int valueCode = switch (valueMicroData) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            default -> 0;
        };
        dataSetSynPop.getDwellingDataSet().setValueAt(ddCount, "d.use", valueCode);
    }

    private void translateDwellingYear(int ddCount) {
        int valueMicroData = (int) dataSetSynPop.getDwellingDataSet().getValueAt(ddCount, "d.year");
        int valueCode = switch (valueMicroData) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            case 5, 6 -> 3;
            case 7, 10 -> 4;
            default -> 0;
        };
        dataSetSynPop.getDwellingDataSet().setValueAt(ddCount, "d.year", valueCode);
    }

    public void translateDwellingType(int ddCount) {
        int valueMicroData = (int) dataSetSynPop.getDwellingDataSet().getValueAt(ddCount, "d.type");
        int valueCode = switch (valueMicroData) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4, 5 -> 4;
            default -> 0;
        };
        dataSetSynPop.getDwellingDataSet().setValueAt(ddCount, "d.type", valueCode);
    }

    public void translateDwellingNumberOfApartments(int ddCount) {
        int valueMicroData = (int) dataSetSynPop.getDwellingDataSet().getValueAt(ddCount, "d.numberOfApartments");
        int valueCode = switch (valueMicroData) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            case 5, 6, 7 -> 3;
            default -> 0;
        };
        dataSetSynPop.getDwellingDataSet().setValueAt(ddCount, "d.numberOfApartments", valueCode);
    }
}
