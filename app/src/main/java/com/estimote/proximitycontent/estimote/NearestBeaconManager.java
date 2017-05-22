package com.estimote.proximitycontent.estimote;

import android.content.Context;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearestBeaconManager {

    private static final String IDP1 = "[F8:BC:8F:6B:10:CD]";
    private static final String IDP2 = "[D9:41:F3:6E:E5:4F]";
    private static final String IDP3 = "[EE:F0:2A:CC:14:43]";

    private static final double d = 8;
    private static final double i = 4;
    private static final double j = 8;

    private static final Region ALL_ESTIMOTE_BEACONS = new Region("all Estimote beacons", null, null, null);

    private List<BeaconID> beaconIDs;

    private Listener listener;

    private BeaconID currentlyNearestBeaconID;
    private boolean firstEventSent = false;

    private BeaconManager beaconManager;

    private static Map<String, List<Double>> beaconsDistance = new HashMap<>();


    public NearestBeaconManager(Context context, List<BeaconID> beaconIDs) {
        this.beaconIDs = beaconIDs;

        beaconManager = new BeaconManager(context);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                checkForNearestBeacon(list);
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onNearestBeaconChanged(BeaconID beaconID);
    }

    public void startNearestBeaconUpdates() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
            }
        });
    }

    public void stopNearestBeaconUpdates() {
        beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
    }

    public void destroy() {
        beaconManager.disconnect();
    }

    private void checkForNearestBeacon(List<Beacon> allBeacons) {
        List<Beacon> beaconsOfInterest = filterOutBeaconsByIDs(allBeacons, beaconIDs);
        Beacon nearestBeacon = findNearestBeacon(beaconsOfInterest);
        if (nearestBeacon != null) {
            BeaconID nearestBeaconID = BeaconID.fromBeacon(nearestBeacon);
            if (!nearestBeaconID.equals(currentlyNearestBeaconID) || !firstEventSent) {
                updateNearestBeacon(nearestBeaconID);
            }
        } else if (currentlyNearestBeaconID != null || !firstEventSent) {
            updateNearestBeacon(null);
        }
    }

    private void updateNearestBeacon(BeaconID beaconID) {
        currentlyNearestBeaconID = beaconID;
        firstEventSent = true;
        if (listener != null) {
            listener.onNearestBeaconChanged(beaconID);
        }
    }

    private static List<Beacon> filterOutBeaconsByIDs(List<Beacon> beacons, List<BeaconID> beaconIDs) {
        List<Beacon> filteredBeacons = new ArrayList<>();
        for (Beacon beacon : beacons) {
            BeaconID beaconID = BeaconID.fromBeacon(beacon);
            if (beaconIDs.contains(beaconID)) {
                filteredBeacons.add(beacon);
            }
        }
        return filteredBeacons;
    }

    private static Beacon findNearestBeacon(List<Beacon> beacons) {
        Beacon nearestBeacon = null;
        double nearestBeaconsDistance = -1;
        for (Beacon beacon : beacons) {
            double distance = Utils.computeAccuracy(beacon);
            if (distance > -1 &&
                    (distance < nearestBeaconsDistance || nearestBeacon == null)) {
                nearestBeacon = beacon;
                nearestBeaconsDistance = distance;
            }
            System.out.println("Nearest beacon: " + beacon.getMacAddress() + ", distance: " + Utils.computeAccuracy(beacon));
        }



        playerPosition(beacons);

        return nearestBeacon;
    }

    public static void playerPosition(List<Beacon> beacons) {

        // * (ICE)[2]  * (BBR)[3]
        //       * (MNT)[1]

        int enough = 3; // Enough beacons to operate.

        Map<String, Double> beaconDistance = new HashMap<>();

        for (Beacon beacon : beacons) {

            if (beacons.size() == enough) { // Enough beacons.

                beaconDistance.put(beacon.getMacAddress()
                        .toString(), Utils.computeAccuracy(beacon));

                if (!beaconsDistance.containsKey(beacon.getMacAddress().toString()))
                    beaconsDistance.put(beacon.getMacAddress()
                            .toString(), new ArrayList<Double>());

                beaconsDistance.get(beacon.getMacAddress().toString())
                        .add(Utils.computeAccuracy(beacon));
            }
        }

        System.out.println("X: " + getPlayerPositionX(beaconDistance));
        System.out.println("Y: " + getPlayerPositionY(beaconDistance));
    }

    public static double getPlayerPositionX(Map<String, Double> intensity) {

        double ip1 = 0, ip2 = 0;

        for (Map.Entry<String, Double> entry : intensity.entrySet()) {
            switch (entry.getKey()) {
                case IDP2:
                    ip2 = entry.getValue();
                    break;
                case IDP1:
                    ip1 = entry.getValue();
                    break;
            }
        }

        return (Math.pow(ip1, 2) - Math.pow(ip2, 2) + Math.pow(d, 2)) / (2 * d);
    }

    public static double getPlayerPositionY(Map<String, Double> intensity) {

        double ip1 = 0, ip3 = 0;

        for (Map.Entry<String, Double> entry : intensity.entrySet()) {
            switch (entry.getKey()) {
                case IDP3:
                    ip3 = entry.getValue();
                    break;
                case IDP1:
                    ip1 = entry.getValue();
                    break;
            }
        }

        return ((Math.pow(ip1, 2) + Math.pow(ip3, 2) + Math.pow(i, 2) + Math.pow(j, 2))
                / (2 * j)) - ((i / j) * getPlayerPositionX(intensity));
    }
}