package com.estimote.proximitycontent.estimote;

import com.estimote.sdk.cloud.CloudCallback;
import com.estimote.sdk.cloud.EstimoteCloud;
import com.estimote.sdk.cloud.model.BeaconInfo;
import com.estimote.sdk.cloud.model.Color;
import com.estimote.sdk.exception.EstimoteServerException;

public class EstimoteCloudBeaconDetailsFactory implements BeaconContentFactory {

    @Override
    public void getContent(final BeaconID beaconID, final Callback callback) {
        EstimoteCloud.getInstance().fetchBeaconDetails(
                beaconID.getProximityUUID(), beaconID.getMajor(), beaconID.getMinor(),
                new CloudCallback<BeaconInfo>() {

            @Override
            public void success(BeaconInfo beaconInfo) {
                callback.onContentReady(new EstimoteCloudBeaconDetails(
                        beaconInfo.name, beaconInfo.color));
            }

            @Override
            public void failure(EstimoteServerException e) {
                callback.onContentReady(new EstimoteCloudBeaconDetails("beacon", Color.UNKNOWN));
            }
        });
    }
}
