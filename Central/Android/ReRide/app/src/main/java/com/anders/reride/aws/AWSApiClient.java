package com.anders.reride.aws;

import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.anders.reride.ReRideClient;
import com.anders.reride.model.ReRideData;
import com.anders.reride.model.ReRideDataItemsItem;
import com.anders.reride.model.ReRideDataItemsItemPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS API Gateway SDK for custom requests
 */
public class AWSApiClient {
    ApiClientFactory factory = new ApiClientFactory();

    final ReRideClient mReRideClient = factory.build(ReRideClient.class);

    public List<ReRideDataItemsItemPayload> getData(String id, String from, String to,
                                                    String timeZone) {
        ReRideData r = mReRideClient.rideDataGet(from, timeZone, id, to);
        List<ReRideDataItemsItem> items = r.getItems();
        List<ReRideDataItemsItemPayload> payloads = new ArrayList<>();
        for (ReRideDataItemsItem item : items) {
            payloads.add(item.getPayload());
        }
        return payloads;
    }

    public ReRideDataItemsItemPayload getDataLatest(String id, String timeZone) {
        List<ReRideDataItemsItemPayload> data = getData(id, "0", "0", timeZone);
        return data.get(0);
    }
}
