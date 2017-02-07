package cs455.overlay.utils;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Random;

public class HelperUtils {
    public static int getInt(final String stringToConvert) {
        try {
            return Integer.parseInt(stringToConvert);
        } catch (final NumberFormatException nfe) {
            nfe.printStackTrace();
            return -1;
        }
    }

    public static String getLocalHostIpAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException uh) {
            System.out.println("Unable to get the local ipaddress");
            uh.printStackTrace();
            return "-1";
       }
    }

    public static String convertLoopBackToValidIpAddress(String hostName) {
        final String LOOP_BACK_IP = "127.0.0.1";
        if(hostName.contains(LOOP_BACK_IP) || hostName.contains("localhost")) {
            return HelperUtils.getLocalHostIpAddress();
        }
        return hostName.replaceAll("/", "");
    }

    public static int generateRandomNumber(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

}
