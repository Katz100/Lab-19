package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionCreate {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Doctor requests blank form for new prescription.
	 */
	@GetMapping("/prescription/new")
	public String getPrescriptionForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_create";
	}

	// process data entered on prescription_create form
	@PostMapping("/prescription")
	public String createPrescription(PrescriptionView p, Model model) {

		System.out.println("createPrescription " + p);

		/*
		 * valid doctor name and id
		 */
		int doctorId = 0;
		try (Connection con = getConnection();) {
			try (PreparedStatement ps = con.prepareStatement("SELECT id FROM doctor WHERE last_name = ?")) {
				ps.setString(1, p.getDoctorLastName());
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						System.out.println(p.getDoctorLastName() + " was not found");
						model.addAttribute("message", "Doctor not found");
						model.addAttribute("prescription", p);
						return "prescription_create";
					}
					doctorId = rs.getInt("id");
					System.out.println("Doctor id is " + doctorId);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQL error validating doctor:" + e.getMessage());
			model.addAttribute("message", "SQL error. " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
		/*
		 * valid patient name and id
		 */
		try (Connection con = getConnection()) {
			try (PreparedStatement ps = con.prepareStatement("SELECT id FROM patient WHERE id = ? AND last_name = ?")) {
				ps.setInt(1, p.getPatient_id());
				ps.setString(2, p.getPatientLastName());
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						System.out.println("Patient " + p.getPatient_id() + " " + p.getPatientLastName() + " was not found");
						model.addAttribute("message", "Patient not found");
						model.addAttribute("prescription", p);
						return "prescription_create";
					}
					int patientId = rs.getInt("id");
					System.out.println("Patient id is " + patientId);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQL error validating patient:" + e.getMessage());
			model.addAttribute("message", "SQL error. " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}

		/*
		 * valid drug name
		 */
		int drugId = 0;
		try (Connection con = getConnection();) {
			try (PreparedStatement ps = con.prepareStatement("SELECT DrugID FROM drug WHERE DrugName = ?")) {
				ps.setString(1, p.getDrugName());
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						System.out.println("Drug " + p.getDrugName() + " was not found");
						model.addAttribute("message", "Drug not found");
						model.addAttribute("prescription", p);
						return "prescription_create";
					}
					drugId = rs.getInt("DrugID");
					System.out.println("Drug id is " + drugId);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQL error validating drug:" + e.getMessage());
			model.addAttribute("message", "SQL error. " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}

		/*
		 * insert prescription  
		 */
		try (Connection con = getConnection()) {
			try (PreparedStatement ps = con.prepareStatement(
					"INSERT INTO prescription (Doctor_ID, Patient_ID, Drug_DrugID, Quantity, NumOfRefills) " +
							"VALUES (?, ?, ?, ?, ?)",
					java.sql.Statement.RETURN_GENERATED_KEYS)) {

				ps.setInt(1, doctorId);
				ps.setInt(2, p.getPatient_id());
				ps.setInt(3, drugId);
				ps.setInt(4, p.getQuantity());
				ps.setInt(5, p.getRefills());

				ps.executeUpdate();

				try (ResultSet rs = ps.getGeneratedKeys()) {
					if (rs.next()) {
						p.setRxid(rs.getInt(1));
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("SQL error inserting prescription: " + e.getMessage());
			model.addAttribute("message", "SQL error. " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}

		model.addAttribute("message", "Prescription created.");
		model.addAttribute("prescription", p);
		return "prescription_show";
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
