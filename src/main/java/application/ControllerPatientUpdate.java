package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;
/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */

@Controller
public class ControllerPatientUpdate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 *  Display patient profile for patient id.
	 */

	@GetMapping("/patient/edit/{id}")
	public String getUpdateForm(@PathVariable int id, Model model) {
    String sql = """
        SELECT p.id, p.first_name, p.last_name, p.ssn, p.birthdate,
               p.street, p.city, p.state, p.zipcode,
               d.last_name AS primary_doctor
        FROM patient p
        JOIN doctor d ON p.doctor_id = d.id
        WHERE p.id = ?
    """;
    try {
      PatientView pv = jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) ->{
        PatientView p = new PatientView();
        p.setId(rs.getInt("id"));
        p.setFirst_name(rs.getString("first_name"));
        p.setLast_name(rs.getString("last_name"));
        p.setSsn(rs.getString("ssn"));
        p.setBirthdate(String.valueOf(rs.getDate("birthdate").toLocalDate()));
        p.setPrimaryName(rs.getString("primary_doctor"));
        p.setStreet(rs.getString("street"));
        p.setCity(rs.getString("city"));
        p.setState(rs.getString("state"));
        p.setZipcode(rs.getString("zipcode"));
        return p;
      });

      model.addAttribute("patient", pv);
      model.addAttribute("message", "Edit patient profile below:");
      return "patient_edit";

    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      model.addAttribute("message", "No patient record found!");
      return "index";
    }

}

	/*
	 * Process changes from patient_edit form
	 *  Primary doctor, street, city, state, zip can be changed
	 *  ssn, patient id, name, birthdate, ssn are read only in template.
	 */

	@PostMapping("/patient/edit")
	public String updatePatient(PatientView p, Model model) {
    String doctorCheckSql = "SELECT id FROM doctor WHERE last_name = ?";
    Integer doctorId;

    try {
      doctorId = jdbcTemplate.queryForObject(doctorCheckSql, Integer.class, p.getPrimaryName());
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      model.addAttribute("message", "Primary doctor not found: " + p.getPrimaryName());
      model.addAttribute("patient", p);
      return "patient_edit";
    }

    String updateSql = "UPDATE patient SET doctor_id = ?, street = ?, city = ?, state = ?, zipcode = ? WHERE id = ?";
    int updated = jdbcTemplate.update(updateSql, doctorId, p.getStreet(), p.getCity(), p.getState(), p.getZipcode(), p.getId());
    if (updated > 0) {
      model.addAttribute("message", "Patient updated!");
    } else {
      model.addAttribute("message", "Patient update failed! Patient not found.");
    }

    model.addAttribute("patient", p);
    return "patient_show";
	}
}
