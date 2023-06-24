/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kma.librarysmartcard;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author admin
 */
public class Validate {
    
    public boolean isEmpty(String str) {
        if(str.equals("")) {
            return  true;
        }    
        return false;
    }
    
    public boolean isContainSpace(String str) {
        if(str.contains(" ")) {
            return  true;
        }    
        return false;
    }
    
    public boolean pinRequiredLength(String pin) {
        if(pin.length() < 4 || pin.length() > 4) {
            return false;
        }
        
        return true;
    }
    
    public boolean isLegitPin(String pin) {
        String regex = "[0-9]+";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(pin);
 
        return m.matches();
    }
    
    public boolean isValidDate(String strDate)
   {
	SimpleDateFormat sdfrmt = new SimpleDateFormat("dd/MM/yyyy");
	sdfrmt.setLenient(false);
	    
	try
            {
	        Date javaDate = sdfrmt.parse(strDate); 
	        System.out.println(strDate+" is valid date format");
	    }
	    /* Date format is invalid */
	catch (ParseException e)
	    {
	        System.out.println(strDate+" is Invalid Date format");
	        return false;
	    }
	    /* Return true if date format is valid */
	    return true;
   }
}
