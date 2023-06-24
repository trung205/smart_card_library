/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kma.librarysmartcard;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author admin
 */
public class GlobalInfo {
    private APDUGenerator card;

    public GlobalInfo(APDUGenerator card) {
        this.card = card;
    }
    
    public String getID() {
        String studentInfo = card.getInfo();
        byte[] bytes = card.hexStringToByteArray(studentInfo);
        String dataShow = new String(bytes , StandardCharsets.UTF_8);
        String[] result = dataShow.split("#");
        System.out.println(result);
        System.out.println("getID");
        return result[0];
       
    }

}
