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
//    public void writePersons(String path) {
//
//        logger.info("  Writing person file to " + path);
//        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(path, false);
//        pwp.print("id,hhid,age,gender,relationShip,occupation,driversLicense,workplace,income");
//        pwp.print(",");
//        pwp.print("nationality");
//        pwp.print(",");
//        pwp.print("disability");
//        pwp.print(",");
//        pwp.print("schoolId");
//        pwp.print(",");
//        pwp.print("jobType");
//        pwp.print(",");
//        pwp.print("jobDuration");
//        pwp.print(",");
//        pwp.print("jobStartTimeWorkdays");
//        pwp.print(",");
//        pwp.print("jobStartTimeWeekends");
//
//        pwp.println();
//        for (Person pp : householdData.getPersons()) {
//            pwp.print(pp.getId());
//            pwp.print(",");
//            pwp.print(pp.getHousehold().getId());
//            pwp.print(",");
//            pwp.print(pp.getAge());
//            pwp.print(",");
//            pwp.print(pp.getGender().getCode());
//            pwp.print(",\"");
//            String role = pp.getRole().toString();
//            pwp.print(role);
//            pwp.print("\",");
//            pwp.print(pp.getOccupation().getCode());
//            pwp.print(",");
//            pwp.print(pp.hasDriverLicense());
//            pwp.print(",");
//            pwp.print(pp.getJobId());
//            pwp.print(",");
//            pwp.print(pp.getAnnualIncome());
//            pwp.print(",");
//            pwp.print("0");
//            pwp.print(",");
//            pwp.print(pp.getAttribute("disability").get().toString());
//            pwp.print(",");
//            pwp.print("0");
//            pwp.print(",");
//            pwp.print(pp.getAttribute("jobDurationType").get().toString());
//            pwp.print(",");
//            pwp.print(pp.getAttribute("jobDuration").get().toString());
//            pwp.print(",");
//            pwp.print(pp.getAttribute("jobStartTimeWorkday").get().toString());
//            pwp.print(",");
//            pwp.print(pp.getAttribute("jobStartTimeWeekend").get().toString());
//            pwp.println();
//            if (pp.getId() == SiloUtil.trackPp) {
//                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
//                SiloUtil.trackWriter.println(pp.toString());
//            }
//        }
//        pwp.close();
//    }

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
