package com.anders.reride.aws;

import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.anders.reride.clientsdk.ReRideClient;
import com.anders.reride.clientsdk.model.ReRideData;
import com.anders.reride.clientsdk.model.ReRideDataItemsItem;
import com.anders.reride.clientsdk.model.ReRideDataItemsItemPayload;

import java.util.ArrayList;
import java.util.List;

public class AWSApiClient {
    ApiClientFactory factory = new ApiClientFactory();

    final ReRideClient mReRideClient = factory.build(ReRideClient.class);

    public List<ReRideDataItemsItemPayload> getData(String id, String since) {
        ReRideData r = mReRideClient.rideDataGet(since, id);
        List<ReRideDataItemsItem> items = r.getItems();
        List<ReRideDataItemsItemPayload> payloads = new ArrayList<>();
        for (ReRideDataItemsItem item : items) {
            payloads.add(item.getPayload());
        }
        return payloads;
    }
}
