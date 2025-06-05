package com.bankapp.util;

import java.math.BigInteger;

/**
 * Utility class for generating and validating International Bank Account Numbers (IBANs).
 * Implements the standard IBAN format and checksum validation according to ISO 13616.
 */
public class IbanGenerator {
    
    // Constants
    private static final String COUNTRY_CODE = "NL"; // Netherlands
    private static final String BANK_CODE = "BANK";  // Our bank code
    
    /**
     * Generates a valid IBAN from an account ID.
     * Format: NL + check digits + BANK + account number (padded to 10 digits)
     *
     * @param accountId The account ID to convert to IBAN
     * @return A valid IBAN string
     */
    public static String generateIban(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        // Format account number (pad with leading zeros to 10 digits)
        String paddedAccountId = String.format("%010d", accountId);
        
        // Create the IBAN without check digits
        String ibanWithoutChecksum = COUNTRY_CODE + "00" + BANK_CODE + paddedAccountId;
        
        // Calculate check digits
        int checksum = calculateChecksum(ibanWithoutChecksum);
        String checksumStr = String.format("%02d", checksum);
        
        // Construct final IBAN: country code + check digits + bank code + account number
        return COUNTRY_CODE + checksumStr + BANK_CODE + paddedAccountId;
    }
    
    /**
     * Validates an IBAN according to the ISO standard.
     *
     * @param iban The IBAN to validate
     * @return true if the IBAN is valid, false otherwise
     */
    public static boolean validateIban(String iban) {
        if (iban == null || iban.length() < 5) {
            return false;
        }
        
        // Basic format validation
        if (!iban.startsWith(COUNTRY_CODE)) {
            return false;
        }
        
        try {
            // Check if this is our bank's IBAN
            if (!iban.substring(4, 8).equals(BANK_CODE)) {
                return false;
            }
            
            // Extract components
            String countryCode = iban.substring(0, 2);
            String checkDigits = iban.substring(2, 4);
            String bankAndAccount = iban.substring(4);
            
            // Recalculate checksum for validation
            int calculatedChecksum = calculateChecksum(countryCode + "00" + bankAndAccount);
            int providedChecksum;
            
            try {
                providedChecksum = Integer.parseInt(checkDigits);
            } catch (NumberFormatException e) {
                return false;
            }
            
            return calculatedChecksum == providedChecksum;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculates the checksum for an IBAN.
     * Replaces letters with numbers (A=10, B=11, ..., Z=35)
     * Moves the first 4 characters to the end
     * Interprets the string as a decimal integer and computes modulo 97
     * Subtracts the remainder from 98, and uses the result as the check digit
     *
     * @param iban IBAN string with "00" as the check digits
     * @return The calculated check digits (0-97)
     */
    private static int calculateChecksum(String iban) {
        // Rearrange: move first 4 chars to the end
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        
        // Replace letters with their numeric values (A=10, B=11, ..., Z=35)
        StringBuilder numericString = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numericString.append(10 + (Character.toUpperCase(c) - 'A'));
            } else {
                numericString.append(c);
            }
        }
        
        // Calculate mod-97 value (98 minus the remainder of the numeric string divided by 97)
        BigInteger numeric = new BigInteger(numericString.toString());
        BigInteger modResult = numeric.mod(BigInteger.valueOf(97));
        return 98 - modResult.intValue();
    }
    
    /**
     * Extracts the account ID from a valid IBAN.
     *
     * @param iban The IBAN to extract from
     * @return The account ID as a Long
     * @throws IllegalArgumentException if the IBAN is invalid
     */
    public static Long extractAccountId(String iban) {
        if (!validateIban(iban)) {
            throw new IllegalArgumentException("Invalid IBAN: " + iban);
        }
        
        // Account number is the last 10 digits
        String accountNumber = iban.substring(iban.length() - 10);
        return Long.parseLong(accountNumber);
    }
    
    /**
     * Formats an IBAN with spaces for readability.
     * Example: NL12BANK0123456789 -> NL12 BANK 0123 4567 89
     *
     * @param iban The IBAN to format
     * @return Formatted IBAN with spaces
     */
    public static String formatIban(String iban) {
        if (iban == null || iban.length() < 8) {
            return iban;
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append(iban.substring(0, 4)).append(" "); // NL12
        formatted.append(iban.substring(4, 8)).append(" "); // BANK
        
        // Format the account number in groups of 4
        String accountNumber = iban.substring(8);
        for (int i = 0; i < accountNumber.length(); i += 4) {
            int end = Math.min(i + 4, accountNumber.length());
            formatted.append(accountNumber.substring(i, end));
            if (end < accountNumber.length()) {
                formatted.append(" ");
            }
        }
        
        return formatted.toString();
    }
}