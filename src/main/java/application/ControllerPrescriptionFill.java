package application;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionFill {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * Patient requests form to fill prescription.
     */
    @GetMapping("/prescription/fill")
    public String getfillForm(Model model) {
        model.addAttribute("prescription", new PrescriptionView());
        return "prescription_fill";
    }

    // process data from prescription_fill form
    @PostMapping("/prescription/fill")
    public String processFillForm(PrescriptionView p, Model model) {
        int pharmacyId = 0;
        int quantity = 0;
        int numRefills = 0;
        int patientId = 0;
        int drugId = 0;
        int fillCount = 0;
        BigDecimal totalPrice = BigDecimal.valueOf(0.0);


        /*
         * validate pharmacy name and address, get pharmacy id and phone
         */
        try (Connection con = getConnection();) {
            PreparedStatement ps = con.prepareStatement("select pharmacy_id, phone from pharmacy where name = ? and address = ?");
            ps.setString(1, p.getPharmacyName());
            ps.setString(2, p.getPharmacyAddress());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println(p.getPharmacyName() + " does not exist.");
                model.addAttribute("message", "Pharmacy not found");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }
            pharmacyId = rs.getInt("pharmacy_id");
            p.setPharmacyID(pharmacyId);
            p.setPharmacyPhone(p.getPharmacyPhone());
            p.setPharmacyAddress(p.getPharmacyAddress());

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            model.addAttribute("message", "Error accessing pharmacy data");
            model.addAttribute("prescription", p);
            return "prescription_fill";

        }

        /*
         * validate rxid and patient last name matches name on prescription
         */
        try (Connection con = getConnection();) {
            PreparedStatement ps = con.prepareStatement(
                    "select pr.RXID, pt.last_name, pr.Quantity, pr.NumOfRefills, pr.Patient_ID, pr.Drug_DrugID " +
                            "from prescription pr " +
                            "join patient pt on pr.Patient_ID = pt.id " +
                            "where pr.RXID = ?"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                model.addAttribute("message", "Rxid not found");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            String ptLastName = rs.getString("last_name");
            if (!p.getPatientLastName().equalsIgnoreCase(ptLastName)) {
                model.addAttribute("message", "Patient last name does not match");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            model.addAttribute("message", "Error validating prescription");
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }


        /*
         * have we exceeded the number of allowed refills
         * the first fill is not considered a refill.
         */

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "select count(*) as fill_count from prescription_fill where rxid = ?"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                fillCount = rs.getInt("fill_count");

                if (fillCount >= numRefills + 1) {
                    model.addAttribute("message", "Refill limit exceeded");
                    model.addAttribute("prescription", p);
                    return "prescription_fill";
                }
                p.setRefillsRemaining((numRefills + 1) - fillCount);
            }
        } catch (SQLException e) {
            model.addAttribute("message", "Error checking refill count: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }

        /*
         * get doctor information
         */
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "select d.id from doctor d " +
                            "join prescription p on d.id = p.Doctor_ID " +
                            "where rxid = ?"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                model.addAttribute("message", "Doctor information not found");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

        } catch (SQLException e) {
            model.addAttribute("message", "Error verifying refill: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }

        /*
         * calculate cost of prescription
         */
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "select d.IndvDrugPrice * p.Quantity as total_price " +
                            "from drug d " +
                            "join prescription p on d.DrugID = p.Drug_DrugID " +
                            "where p.rxid = ?"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                totalPrice = rs.getBigDecimal("total_price");
                model.addAttribute("totalPrice", totalPrice);
                p.setCost(totalPrice.toString());
            }
            else {
                model.addAttribute("message", "Unable to calculate price — prescription not found.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

        } catch (SQLException e) {
            model.addAttribute("message", "Error verifying price: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }

        // save updated prescription
        try (Connection con = getConnection();) {
            PreparedStatement ps = con.prepareStatement(
                    "insert into prescription_fill (RXID, pharmacy_id, date_filled, price) VALUES (?, ?, CURRENT_DATE, ?)"
            );
            ps.setInt(1, p.getRxid());
            ps.setInt(2, pharmacyId);
            ps.setBigDecimal(3, totalPrice);
            ps.executeUpdate();
            p.setDateFilled(p.getDateFilled());
        } catch (SQLException e) {
            model.addAttribute("message", "Error updating prescription: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }

        // show the updated prescription with the most recent fill information
        model.addAttribute("message", "Prescription filled.");
        model.addAttribute("prescription", p);
        return "prescription_show";
    }

    private Connection getConnection() throws SQLException {
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        return conn;
    }

}