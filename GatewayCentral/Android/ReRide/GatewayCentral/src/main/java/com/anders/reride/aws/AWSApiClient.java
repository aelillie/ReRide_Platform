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
    private ApiClientFactory factory = new ApiClientFactory()
            .apiKey("F1J4UOStVx7heyvZ93ZDRaUdpHXI6s4tKy2peqe0");

    private final ReRideClient mReRideClient = factory.build(ReRideClient.class);

    public List<ReRideDataItemsItemPayload> getData(String id, String from, String to,
                                                    String timezone) {
        return getData(id, Integer.parseInt(from), Integer.parseInt(to), timezone);
    }

    public List<ReRideDataItemsItemPayload> getData(String id, int from, int to,
                                                     String timezone) {
        ReRideData r = mReRideClient.rideDataGet(
                timezone,
                String.valueOf(from),
                id,
                String.valueOf(to));
        if (r == null || r.getItems() == null) return new ArrayList<>();
        List<ReRideDataItemsItem> items = r.getItems();
        List<ReRideDataItemsItemPayload> payloads = new ArrayList<>();
        for (ReRideDataItemsItem item : items) {
            payloads.add(item.getPayload());
        }
        return payloads;
    }

    public ReRideDataItemsItemPayload getDataLatest(String id, String timeZone) {
        List<ReRideDataItemsItemPayload> data = getData(id, 0, 0, timeZone);
        return data.isEmpty() ? null : data.get(0);
    }
}
