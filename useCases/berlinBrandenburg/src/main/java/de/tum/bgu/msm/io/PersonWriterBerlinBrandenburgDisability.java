package de.tum.bgu.msm.io;

import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.data.person.PersonBerlinBrandenburg;
import de.tum.bgu.msm.io.output.PersonWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.util.Optional;

public class PersonWriterBerlinBrandenburgDisability implements PersonWriter {

    private final static Logger logger = LogManager.getLogger(PersonWriterBerlinBrandenburgDisability.class);

    private final HouseholdDataManager householdData;

    public PersonWriterBerlinBrandenburgDisability(HouseholdDataManager householdData) {
        this.householdData = householdData;
    }

    @Override
    public void writePersons(String path) {

        logger.info("  Writing person file to " + path);
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(path, false);
        pwp.print("id,hhid,zone,municipality,age,gender,relationShip,occupation,driversLicense,workplace,income");
        pwp.print(",nationality,disability,schoolType,schoolPlace,schoolId");
        pwp.print(",jobType,jobTypeWZ08,jobDurationType,jobDuration,jobStartTimeWorkday,jobStartTimeWeekend");
        pwp.print(",p.BMI,p.education,p.healthStatusIndex,p.smokeFrequency,p.generalHealth,p.disability");
        pwp.print(",p.physicalImpairmentIndex,p.restriction,p.homeOffice,p.disabilityDegree");
        pwp.print(",p.householdRole,p.income,p.privateHousehold,p.partnerInHousehold");
        pwp.print(",p.municipalityType,p.federal,p.maritalStatus");

        pwp.println();
        for (Person pp : householdData.getPersons()) {
            pwp.print(pp.getId());
            pwp.print(",");
            pwp.print(pp.getHousehold().getId());
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "zone", "-1"));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "municipality", "-1"));
            pwp.print(",");
            pwp.print(pp.getAge());
            pwp.print(",");
            pwp.print(pp.getGender().getCode());
            pwp.print(",\"");
            String role = pp.getRole().toString();
            pwp.print(role);
            pwp.print("\",");
            pwp.print(pp.getOccupation().getCode());
            pwp.print(",");
            pwp.print(pp.hasDriverLicense());
            pwp.print(",");
            pwp.print(pp.getJobId());
            pwp.print(",");
            pwp.print(pp.getAnnualIncome());
            pwp.print(",");
            pwp.print(getNationalityOrDefault(pp));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "disability", "0"));
            pwp.print(",");
            pwp.print(getSchoolTypeOrDefault(pp));
            pwp.print(",");
            pwp.print(getSchoolPlaceOrDefault(pp));
            pwp.print(",");
            pwp.print(getSchoolIdOrDefault(pp));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "jobType", ""));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "jobTypeWZ08", "0"));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "jobDurationType", "0"));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "jobDuration", "0"));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "jobStartTimeWorkday", "0"));
            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "jobStartTimeWeekend", "0"));

            String[] extendedAttributes = {
                    "p.BMI", "p.education", "p.healthStatusIndex", "p.smokeFrequency",
                    "p.generalHealth", "p.disability", "p.physicalImpairmentIndex", "p.restriction",
                    "p.homeOffice", "p.disabilityDegree", "p.householdRole", "p.income",
                    "p.privateHousehold", "p.partnerInHousehold", "p.municipalityType", "p.federal",
                    "p.maritalStatus"
            };
            for (String attribute : extendedAttributes) {
                pwp.print(",");
                pwp.print(getAttributeOrDefault(pp, attribute, "0"));
            }
            pwp.println();
            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(pp.toString());
            }
        }
        pwp.close();
    }

    private String getAttributeOrDefault(Person person, String key, String defaultValue) {
        Object value = person.getAttribute(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Optional) {
            return ((Optional<?>) value).map(Object::toString).orElse(defaultValue);
        }
        return value.toString();
    }

    private String getNationalityOrDefault(Person person) {
        if (person instanceof PersonBerlinBrandenburg) {
            PersonBerlinBrandenburg berlinPerson = (PersonBerlinBrandenburg) person;
            if (berlinPerson.getNationality() != null) {
                return berlinPerson.getNationality().toString();
            }
        }
        return "0";
    }

    private int getSchoolTypeOrDefault(Person person) {
        return person instanceof PersonBerlinBrandenburg
                ? ((PersonBerlinBrandenburg) person).getSchoolType() : 0;
    }

    private int getSchoolPlaceOrDefault(Person person) {
        return person instanceof PersonBerlinBrandenburg
                ? ((PersonBerlinBrandenburg) person).getSchoolPlace() : -1;
    }

    private int getSchoolIdOrDefault(Person person) {
        return person instanceof PersonBerlinBrandenburg
                ? ((PersonBerlinBrandenburg) person).getSchoolId() : -1;
    }
}
