/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kma.librarysmartcard;

/**
 *
 * @author admin
 */
public class CardInstance {
    public static  APDUGenerator card;
    
    public static APDUGenerator getInstance() {
        if(card == null) {
           card =  new APDUGenerator();
        }
        
        return card;
    }
}
