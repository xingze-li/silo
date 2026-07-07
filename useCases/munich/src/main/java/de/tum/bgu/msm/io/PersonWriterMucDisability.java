package de.tum.bgu.msm.io;

import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.data.person.PersonMuc;
import de.tum.bgu.msm.data.person.PersonMucDisability;
import de.tum.bgu.msm.io.output.PersonWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;

public class PersonWriterMucDisability implements PersonWriter {

    private final static Logger logger = LogManager.getLogger(PersonWriterMucDisability.class);

    private final HouseholdDataManager householdData;

    public PersonWriterMucDisability(HouseholdDataManager householdData) {
        this.householdData = householdData;
    }

    @Override
    public void writePersons(String path) {

        logger.info("  Writing person file to " + path);

        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(path, false);

        pwp.print("id,hhid,age,gender,relationShip,occupation,driversLicense,workplace,income");
        pwp.print(",nationality");
        pwp.print(",disability");
        pwp.print(",schoolType");
        pwp.print(",schoolPlace");
        pwp.print(",jobType");
        pwp.print(",jobTypeWZ08");
        pwp.print(",jobDurationType");
        pwp.print(",jobDuration");
        pwp.print(",jobStartTimeWorkday");
        pwp.print(",jobStartTimeWeekend");
        pwp.print(",p.BMI");
        pwp.print(",p.education");
        pwp.print(",p.healthStatusIndex");
        pwp.print(",p.smokeFrequency");
        pwp.print(",p.generalHealth");
        pwp.print(",p.disability");
        pwp.print(",p.physicalImpairmentIndex");
        pwp.print(",p.restriction");
        pwp.print(",p.homeOffice");
        pwp.print(",p.disabilityDegree");

        pwp.print(",p.householdRole");
        pwp.print(",p.privateHousehold");
        pwp.print(",p.partnerInHousehold");
        pwp.print(",p.municipalityType");
        pwp.print(",p.federal");
        pwp.print(",p.maritalStatus");


        pwp.println();

        for (Person pp : householdData.getPersons()) {

            pwp.print(pp.getId());
            pwp.print(",");

            pwp.print(pp.getHousehold().getId());
            pwp.print(",");

            pwp.print(pp.getAge());
            pwp.print(",");

            pwp.print(pp.getGender().getCode());
            pwp.print(",");

            pwp.print("\"");
            pwp.print(pp.getRole().toString());
            pwp.print("\"");
            pwp.print(",");

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

            pwp.print(getDisabilityOrDefault(pp));
            pwp.print(",");

            pwp.print(getSchoolTypeOrDefault(pp));
            pwp.print(",");

            pwp.print(getSchoolPlaceOrDefault(pp));
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
            pwp.print(",");

            pwp.print(getAttributeOrDefault(pp, "p.BMI", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.education", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.healthStatusIndex", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.smokeFrequency", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.generalHealth", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.disability", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.physicalImpairmentIndex", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.restriction", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.homeOffice", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.disabilityDegree", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.householdRole", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.privateHousehold", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.partnerInHousehold", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.municipalityType", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.federal", "0"));

            pwp.print(",");
            pwp.print(getAttributeOrDefault(pp, "p.maritalStatus", "0"));

            pwp.println();

            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(pp.toString());
            }
        }

        pwp.close();
    }

    private String getAttributeOrDefault(Person pp, String key, String defaultValue) {
        return pp.getAttribute(key)
                .map(Object::toString)
                .orElse(defaultValue);
    }

    private String getNationalityOrDefault(Person pp) {

        if (pp instanceof PersonMuc) {
            PersonMuc personMuc = (PersonMuc) pp;
            if (personMuc.getNationality() != null) {
                return personMuc.getNationality().toString();
            }
        }

        if (pp instanceof PersonMucDisability) {
            PersonMucDisability personMucDisability = (PersonMucDisability) pp;
            if (personMucDisability.getNationality() != null) {
                return personMucDisability.getNationality().toString();
            }
        }

        return "0";
    }

    private String getDisabilityOrDefault(Person pp) {

        String disabilityAttribute = getAttributeOrDefault(pp, "disability", null);

        if (disabilityAttribute != null) {
            return disabilityAttribute;
        }

        if (pp instanceof PersonMucDisability) {
            PersonMucDisability personMucDisability = (PersonMucDisability) pp;
            if (personMucDisability.getDisability() != null) {
                return Integer.toString(
                        personMucDisability.getDisability().getDisabilityCode()
                );
            }
        }

        return "0";
    }

    private String getSchoolTypeOrDefault(Person pp) {

        if (pp instanceof PersonMuc) {
            return Integer.toString(((PersonMuc) pp).getSchoolType());
        }

        if (pp instanceof PersonMucDisability) {
            return Integer.toString(((PersonMucDisability) pp).getSchoolType());
        }

        return "0";
    }

    private String getSchoolPlaceOrDefault(Person pp) {

        if (pp instanceof PersonMuc) {
            return Integer.toString(((PersonMuc) pp).getSchoolPlace());
        }

        if (pp instanceof PersonMucDisability) {
            return Integer.toString(((PersonMucDisability) pp).getSchoolPlace());
        }

        return "-1";
    }
}
