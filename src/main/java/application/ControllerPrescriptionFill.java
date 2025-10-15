package application;

import java.math.BigDecimal;
import java.sql.*;

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

        /*
         * validate pharmacy name and address, get pharmacy id and phone
         */
        try (Connection con = getConnection();) {
            PreparedStatement ps = con.prepareStatement("select ID, PhoneNumber from pharmacy where Name = ? and Address = ?");
            ps.setString(1, p.getPharmacyName());
            ps.setString(2, p.getPharmacyAddress());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println(p.getPharmacyName() + " does not exist.");
                model.addAttribute("message", "Pharmacy not found");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }
            pharmacyId = rs.getInt("ID");
            String pharmacyPhone = rs.getString("PhoneNumber");
            p.setPharmacyID(pharmacyId);
            p.setPharmacyPhone(pharmacyPhone);
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
                    "select pt.id, pt.first_name, pt.last_name " +
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
            p.setPatientLastName(ptLastName);
            p.setPatientFirstName(rs.getString("first_name"));
            p.setPatient_id(rs.getInt("id"));

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
                    "select Quantity, NumOfRefills from prescription where RXID = ?"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                model.addAttribute("message", "Prescription not found");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            int quantityPerFill = rs.getInt("Quantity");
            int numRefills = rs.getInt("NumOfRefills");
            p.setQuantity(quantityPerFill);
            p.setRefills(numRefills);

            PreparedStatement ps2 = con.prepareStatement(
                    "select COUNT(*) as fill_count from prescriptionfill where Prescription_RXID = ?"
            );
            ps2.setInt(1, p.getRxid());
            ResultSet rs2 = ps2.executeQuery();

            int fillCount = rs2.next() ? rs2.getInt("fill_count") : 0;
            int maxFillsAllowed = numRefills + 1;

            if (fillCount >= maxFillsAllowed) {
                model.addAttribute("message", "Maximum number of refills exceeded");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            int refillsUsed = Math.max(0, fillCount - 1);
            int refillsRemaining = numRefills - refillsUsed;
            p.setRefillsRemaining(refillsRemaining);

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
                    "select d.id, d.first_name, d.last_name from doctor d " +
                            "join prescription p on d.id = p.Doctor_ID " +
                            "where RXID = ?"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();


            if (!rs.next()) {
                model.addAttribute("message", "Doctor information not found");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }
;
            p.setDoctorLastName(rs.getString("last_name"));
            p.setDoctorFirstName(rs.getString("first_name"));
            p.setDoctor_id(rs.getInt("id"));

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
                    "select d.DrugName, phd.Price * p.Quantity as total_price " +
                            "from prescription p " +
                            "join drug d on d.DrugID = p.Drug_DrugID " +
                            "join pharmacy_has_drug phd on phd.Drug_DrugID = d.DrugID " +
                            "where p.RXID = ? and phd.Pharmacy_ID = ?"
            );
            ps.setInt(1, p.getRxid());
            ps.setInt(2, p.getPharmacyID());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                model.addAttribute("message", "Unable to calculate price — drug not stocked at this pharmacy.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            BigDecimal totalPrice = rs.getBigDecimal("total_price");
            String drugName = rs.getString("DrugName");

            p.setCost(totalPrice.toString());
            p.setDrugName(drugName);
            model.addAttribute("totalPrice", totalPrice);


        } catch (SQLException e) {
            model.addAttribute("message", "Error verifying price: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }

        //record date that prescription was filled
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "select DateFilled from prescriptionfill where Prescription_RXID = ? " +
                            "order by DateFilled desc limit 1"
            );
            ps.setInt(1, p.getRxid());
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                Date dateFilled = rs.getDate("DateFilled");
                p.setDateFilled(dateFilled.toString());
            }
        } catch(SQLException e) {
            model.addAttribute("message", "Error verifying date: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }

        // save updated prescription
        try (Connection con = getConnection();) {
            PreparedStatement ps = con.prepareStatement(
                    "insert into prescriptionfill (Prescription_RXID, Pharmacy_Id, DateFilled, Price) values (?, ?, CURRENT_DATE, ?)"

            );

            ps.setInt(1, p.getRxid());
            ps.setInt(2, p.getPharmacyID());
            ps.setBigDecimal(3, new BigDecimal(p.getCost()));
            ps.executeUpdate();

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