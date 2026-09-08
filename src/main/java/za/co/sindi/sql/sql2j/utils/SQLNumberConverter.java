/**
 * 
 */
package za.co.sindi.sql.sql2j.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Buhake Sindi
 * @since 08 August 2026
 */
public final class SQLNumberConverter {
	
	private SQLNumberConverter() {
		throw new AssertionError("Private constructor.");
	}
	
    public static Number convertStringToNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }
        
        String trimmed = input.trim();

        try {
        	// 1. Handle Binary Formats (e.g., "0x1A", "-0xFF")
        	if (trimmed.startsWith("0b") || trimmed.startsWith("0B") || 
                trimmed.startsWith("-0b") || trimmed.startsWith("-0B")) {
        		if (trimmed.startsWith("-")) {
        			return convertBytesToNumber("-" + trimmed.substring(3));
        		}
        		
        		convertBytesToNumber(trimmed.substring(2));
            }
        	
            // 2. Handle Hexadecimal Formats (e.g., "0x1A", "-0xFF")
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X") || 
                trimmed.startsWith("-0x") || trimmed.startsWith("-0X")) {
                return Long.decode(trimmed); 
            }

            // 3. Handle Exponential and Standard Decimals (e.g., "1.23E4", "150.50")
            if (trimmed.contains(".") || trimmed.toLowerCase().contains("e")) {
                BigDecimal bd = new BigDecimal(trimmed);
                // Return Double if it fits without losing precision, otherwise keep BigDecimal
                if (bd.compareTo(BigDecimal.valueOf(bd.doubleValue())) == 0) {
                    return bd.doubleValue();
                }
                return bd;
            }

            // 4. Handle Standard Integers (e.g., "123456")
            long val = Long.parseLong(trimmed);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (int) val;
            }
            return val;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric format: " + input, e);
        }
    }
    
    private static Number convertBytesToNumber(String input) {
    	
    	BigInteger bi = new BigInteger(input, 2);
    	int bitLength = bi.bitLength();
    	if (bitLength < 32) return bi.intValue();
    	if (bitLength < 64) return bi.longValue();
    	return bi;
    }
}
