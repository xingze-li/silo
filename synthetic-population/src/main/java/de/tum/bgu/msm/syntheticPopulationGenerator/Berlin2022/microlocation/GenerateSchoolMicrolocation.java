package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.microlocation;

import de.tum.bgu.msm.data.person.Occupation;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.schools.DataContainerWithSchools;
import de.tum.bgu.msm.schools.PersonWithSchool;
import de.tum.bgu.msm.schools.School;
import de.tum.bgu.msm.schools.SchoolData;
import de.tum.bgu.msm.schools.SchoolUtils;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.util.HashMap;
import java.util.Map;

public class GenerateSchoolMicrolocation {

    private static final Logger logger = LogManager.getLogger(GenerateSchoolMicrolocation.class);

    private final DataContainerWithSchools dataContainer;
    Map<Integer, Map<Integer,Map<Integer,Integer>>> zoneSchoolTypeSchoolLocationCapacity = new HashMap<>();
    private final Map<Integer, Integer> municipalityByZone = new HashMap<>();


    public GenerateSchoolMicrolocation(DataContainerWithSchools dataContainer){
        this.dataContainer = dataContainer;
    }

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

    public void run() {
        logger.info("   Running module: school microlocation");
        if (PropertiesSynPop.get().main.schoolLocationlist == null) {
            throw new IllegalStateException(
                    "School microlocation is enabled, but schoolLocation.list was not loaded.");
        }
        logger.info("   Start creating school objects from school location list");
        initializeMunicipalityLookup();
        createSchools();
        logger.info("   Start Selecting the school to allocate the student");

        int assignedAtRequestedZone = 0;
        int assignedToClosestSchool = 0;
        int unassignedStudents = 0;
        Map<Integer, Integer> studentsBySchool = new HashMap<>();

        for (Person p : dataContainer.getHouseholdDataManager().getPersons()) {
            if (p.getOccupation() != Occupation.STUDENT) {
                continue;
            }
            if (!(p instanceof PersonWithSchool)) {
                throw new IllegalStateException(
                        "Student " + p.getId() + " does not implement PersonWithSchool.");
            }

            PersonWithSchool student = (PersonWithSchool) p;
            School school = selectSchoolAtAssignedZone(
                    student.getSchoolPlace(),
                    student.getSchoolType()
            );

            if (school != null) {
                assignedAtRequestedZone++;
            } else {
                school = dataContainer.getSchoolData().getClosestSchool(
                        student,
                        student.getSchoolType()
                );
                if (school == null) {
                    student.setSchoolId(-1);
                    unassignedStudents++;
                    continue;
                }
                student.setSchoolPlace(school.getZoneId());
                assignedToClosestSchool++;
            }

            student.setSchoolId(school.getId());
            studentsBySchool.merge(school.getId(), 1, Integer::sum);
        }

        for (School ss : dataContainer.getSchoolData().getSchools()){
            ss.setOccupancy(studentsBySchool.getOrDefault(ss.getId(), 0));
        }

        logger.info("   Students assigned to a school in their allocated TAZ: " + assignedAtRequestedZone);
        logger.warn("   Students assigned to the closest school as fallback: " + assignedToClosestSchool);
        logger.warn("   Students without a school location: " + unassignedStudents);
        logger.info("   Finished school microlocation.");
    }

    private School selectSchoolAtAssignedZone(int zoneId, int schoolType) {
        Map<Integer, Map<Integer, Integer>> capacityByType =
                zoneSchoolTypeSchoolLocationCapacity.get(zoneId);
        if (capacityByType == null) {
            return null;
        }

        Map<Integer, Integer> capacityBySchool = capacityByType.get(schoolType);
        if (capacityBySchool == null || capacityBySchool.isEmpty()) {
            return null;
        }

        Map<Integer, Integer> positiveCapacity = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : capacityBySchool.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                positiveCapacity.put(entry.getKey(), entry.getValue());
            }
        }
        if (positiveCapacity.isEmpty()) {
            return null;
        }

        int selectedSchoolId = SiloUtil.select(positiveCapacity);
        School school = dataContainer.getSchoolData().getSchoolFromId(selectedSchoolId);
        if (school == null) {
            throw new IllegalStateException(
                    "Selected school " + selectedSchoolId + " is not present in SchoolData.");
        }

        capacityBySchool.put(
                selectedSchoolId,
                capacityBySchool.get(selectedSchoolId) - 1
        );
        return school;
    }



    private void createSchools() {

        for (int zone : dataContainer.getGeoData().getZones().keySet()){
            Map<Integer,Map<Integer,Integer>> schoolLocationListForThisSchoolType = new HashMap<>();
            for (int type = 1 ; type <= 3; type++){
                Map<Integer,Integer> schoolCapacity = new HashMap<>();
                schoolLocationListForThisSchoolType.put(type,schoolCapacity);
            }
            zoneSchoolTypeSchoolLocationCapacity.put(zone,schoolLocationListForThisSchoolType);
        }

        SchoolData schoolData = dataContainer.getSchoolData();

        for (int row = 1; row <= PropertiesSynPop.get().main.schoolLocationlist.getRowCount(); row++) {

            int id = (int) PropertiesSynPop.get().main.schoolLocationlist.getValueAt(row,"OBJECTID");
            int zone = (int) PropertiesSynPop.get().main.schoolLocationlist.getValueAt(row,"zoneID");
            int municipality = getMunicipalityByZone(zone);
            float xCoordinate = PropertiesSynPop.get().main.schoolLocationlist.getValueAt(row,"x");
            float yCoordinate = PropertiesSynPop.get().main.schoolLocationlist.getValueAt(row,"y");
            int schoolCapacity = Math.round(
                    PropertiesSynPop.get().main.schoolLocationlist.getValueAt(row, "capacity")
            );
            int schoolType = (int) PropertiesSynPop.get().main.schoolLocationlist.getValueAt(row,"schoolType");

            if (!dataContainer.getGeoData().getZones().containsKey(zone)) {
                throw new IllegalArgumentException(
                        "School " + id + " refers to unknown zone " + zone + ".");
            }
            if (schoolType < 1 || schoolType > 3) {
                throw new IllegalArgumentException(
                        "School " + id + " has invalid schoolType " + schoolType + ".");
            }
            if (schoolCapacity <= 0) {
                throw new IllegalArgumentException(
                        "School " + id + " has non-positive capacity " + schoolCapacity + ".");
            }

            Coordinate coordinate = new Coordinate(xCoordinate,yCoordinate);
            schoolData.addSchool(SchoolUtils.getFactory().createSchool(id, schoolType, schoolCapacity,0,coordinate, zone, municipality));

            if (zoneSchoolTypeSchoolLocationCapacity.get(zone) != null){
                zoneSchoolTypeSchoolLocationCapacity.get(zone).get(schoolType).put(id,schoolCapacity);
            }else{
                logger.info("Error zoneID" + zone);
            }

        }
        schoolData.setup();
    }
}
