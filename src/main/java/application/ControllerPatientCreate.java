package application;

import java.sql.*;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientCreate {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /*
     * Request blank patient registration form.
     */
    @GetMapping("/patient/new")
    public String getNewPatientForm(Model model) {
        // return blank form for new patient registration
        model.addAttribute("patient", new PatientView());
        return "patient_register";
    }

    /*
     * Process data from the patient_register form
     */
    @PostMapping("/patient/new")
    public String createPatient(PatientView p, Model model) {

        /*
         * validate doctor last name and find the doctor id
         */
        try (Connection con = getConnection();) {
            int doctorId = 0;
            PreparedStatement ps = con.prepareStatement("select id from doctor where last_name = ?");
            ps.setString(1, p.getPrimaryName());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println(p.getPrimaryName() + " was not found");
                model.addAttribute("message", "Doctor not found");
                model.addAttribute("patient", p);
                return "patient_register";
            }
            doctorId = rs.getInt("id");
            System.out.println("Doctor id is " + doctorId);

			/*
			 * insert to patient table
			 */

            PreparedStatement ps3 = con.prepareStatement("INSERT INTO patient (doctor_id, ssn, first_name, last_name, birthdate, street, city, state, zipcode) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps3.setInt(1, doctorId);
            ps3.setString(2, p.getSsn());
            ps3.setString(3, p.getFirst_name());
            ps3.setString(4, p.getLast_name());
            ps3.setDate(5, Date.valueOf(LocalDate.parse(p.getBirthdate())));
            ps3.setString(6, p.getStreet());
            ps3.setString(7, p.getCity());
            ps3.setString(8, p.getState());
            ps3.setString(9, p.getZipcode());
            ps3.executeUpdate();

            ResultSet rs3 = ps3.getGeneratedKeys();
            if (rs3.next()) p.setId(rs3.getInt(1));

			// display patient data and the generated patient ID,  and success message
            model.addAttribute("message", "Registration successful.");
            model.addAttribute("patient", p);
            return "patient_show";

        } catch (SQLException e) {
			/*
			 * on error
			 * model.addAttribute("message", some error message);
			 * model.addAttribute("patient", p);
			 * return "patient_register";
			 */

            System.out.println(e.getMessage());
            model.addAttribute("message", "SQL Error." + e.getMessage());
            model.addAttribute("patient", p);
            return "patient_register";
        }
    }

    /*
     * Request blank form to search for patient by id and name
     */
    @GetMapping("/patient/edit")
    public String getSearchForm(Model model) {
        model.addAttribute("patient", new PatientView());
        return "patient_get";
    }

    /*
     * Perform search for patient by patient id and name.
     */
    @PostMapping("/patient/show")
    public String showPatient(PatientView p, Model model) {
		try (Connection con = getConnection()) {
			PreparedStatement ps = con.prepareStatement("select * from patient where id = ? and last_name = ?");
			ps.setInt(1, p.getId());
			ps.setString(2, p.getLast_name());
			System.out.println("Searching for patient with id " + p.getId() + " and last name " + p.getLast_name());
			ResultSet rs = ps.executeQuery();


			// if found, return "patient_show", else return error message and "patient_get"
			if (rs.next()) {
				String doctorName = null;

				PreparedStatement ps2 = con.prepareStatement(
						"select d.last_name from patient p " +
						" join doctor d on p.doctor_id = d.id" +
								" where p.id = ?");
				ps2.setInt(1, p.getId());
				ResultSet rs2 = ps2.executeQuery();
				if (rs2.next()) doctorName = rs2.getString(1);

				p.setId(rs.getInt("id"));
				p.setFirst_name(rs.getString("first_name"));
				p.setLast_name(rs.getString("last_name"));
				p.setBirthdate(rs.getString("birthdate"));
				p.setStreet(rs.getString("street"));
				p.setCity(rs.getString("city"));
				p.setState(rs.getString("state"));
				p.setZipcode(rs.getString("zipcode"));
				p.setPrimaryName(doctorName);
				model.addAttribute("patient", p);
				return "patient_show";
			} else {
				model.addAttribute("message", "No records found");
				model.addAttribute("patient", p);
				return "patient_get";
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			model.addAttribute("message", "SQL Error." + e.getMessage());
			model.addAttribute("patient", p);
			return "patient_get";
		}
    }

    /*
     * return JDBC Connection using jdbcTemplate in Spring Server
     */

    private Connection getConnection() throws SQLException {
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        return conn;
    }
}
